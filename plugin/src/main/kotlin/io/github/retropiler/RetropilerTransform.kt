package io.github.retropiler

import com.android.SdkConstants
import com.android.build.api.transform.*
import com.android.build.gradle.AppExtension
import javassist.ClassPool
import javassist.CtClass
import javassist.CtMethod
import javassist.Modifier
import javassist.bytecode.Descriptor
import javassist.bytecode.LocalVariableAttribute
import org.gradle.api.Project
import org.slf4j.LoggerFactory
import java.io.File
import java.util.*
import java.util.jar.JarFile
import java.util.regex.Pattern

class RetropilerTransform(val project: Project) : Transform() {

    val retropilerRuntimePackage = "io.github.retropiler.runtime"

    val logger = LoggerFactory.getLogger(RetropilerTransform::class.java)

    val lambdaClassPattern = Pattern.compile(".+\\\$\\\$Lambda\\\$\\d+")

    val lambdaFactoryName = "lambdaFactory$"

    override fun getName(): String {
        return "retropiler"
    }

    override fun getInputTypes(): Set<QualifiedContent.ContentType> {
        return setOf(QualifiedContent.DefaultContentType.CLASSES)
    }

    override fun isIncremental(): Boolean {
        return true
    }

    override fun getScopes(): MutableSet<QualifiedContent.Scope> {
        return EnumSet.of(QualifiedContent.Scope.PROJECT)
    }

    override fun getReferencedScopes(): MutableSet<QualifiedContent.Scope> {
        // to refer JARs in dependencies
        return EnumSet.of(
                QualifiedContent.Scope.EXTERNAL_LIBRARIES,
                QualifiedContent.Scope.PROJECT_LOCAL_DEPS)
    }

    override fun transform(invocation: TransformInvocation) {
        val t0 = System.currentTimeMillis();

        val outputDir = invocation.outputProvider.getContentLocation(name, outputTypes, scopes, Format.DIRECTORY)

        val classPool = ClassPool(null)
        classPool.appendSystemPath()
        project.extensions.findByType(AppExtension::class.java).bootClasspath.forEach {
            classPool.appendClassPath(it.absolutePath)
        }

        collectClassPath(invocation).forEach {
            System.out.println("classPath: $it")
            classPool.appendClassPath(it.absolutePath)
        }

        classPool.appendClassPath(project.rootProject.file("runtime/build/classes/main").absolutePath) // FIXME

        val ctClasses = collectClassNames(invocation).map { className -> classPool.get(className) }

        val optionalType = classPool.get("java.util.Optional")
        val optionalDesc = Descriptor.of(optionalType)
        val retroOptionalType = classPool.get("io.github.retropiler.runtime.java.util._Optional")
        val retroOptionalDesc = Descriptor.of(retroOptionalType)

        // pre process
        ctClasses.forEach { ctClass ->
            if (lambdaClassPattern.matcher(ctClass.simpleName).matches()) {
                fixupLambdaClass(ctClass, classPool)
            }

            // replace java.util.Optional to retropiler's in local variables
            ctClass.methods.forEach { method ->
                val ca = method.methodInfo2.codeAttribute
                val lva = ca?.getAttribute(LocalVariableAttribute.tag) as LocalVariableAttribute?
                if (lva != null) {

                    val cp = method.methodInfo2.constPool
                    val newLva = LocalVariableAttribute(cp)
                    var entryIndex = 0

                    (0 .. lva.tableLength()  - 1).forEach { i ->
                        if (lva.descriptor(i) == optionalDesc) {
                            newLva.addEntry(lva.startPc(i),
                                    lva.codeLength(i),
                                    lva.nameIndex(i),
                                    cp.addUtf8Info(retroOptionalDesc),
                                    entryIndex)
                            entryIndex += Descriptor.dataSize(retroOptionalDesc)
                        } else {
                            newLva.addEntry(lva.startPc(i),
                                    lva.codeLength(i),
                                    lva.nameIndex(i),
                                    lva.descriptorIndex(i),
                                    entryIndex)
                            entryIndex += Descriptor.dataSize(lva.descriptor(i))
                        }
                    }

                    lva.set(newLva.get())
                    ca.maxLocals = entryIndex
                }
            }
        }

        // main process
        ctClasses.forEach { ctClass ->
            ctClass.instrument(RetropilerExprEditor(classPool))
        }

        // post process and write it down
        ctClasses.forEach { ctClass ->
            if (lambdaClassPattern.matcher(ctClass.simpleName).matches()) {
                cleanupLambdaClass(ctClass, classPool)
            }

            ctClass.writeFile(outputDir.canonicalPath)
        }

        copyResourceFiles(invocation.inputs, outputDir)

        System.out.println("Retropiler transform: ${System.currentTimeMillis() - t0}ms")
        logger.info("transform: ${System.currentTimeMillis() - t0}ms")
    }

    // retrolambda generates `Consumer lambdaFactory$(...)` so it replaces the return type
    // to retropiler runtime classes
    private fun fixupLambdaClass(lambdaClass: CtClass, classPool: ClassPool) {
        val retroClass = getRetroClass(lambdaClass.interfaces[0], classPool)

        lambdaClass.addInterface(retroClass)

        val lambdaFactory = lambdaClass.getDeclaredMethod(lambdaFactoryName)
        if (lambdaFactory.parameterTypes.isEmpty()) {
            val newLambdaFactory = CtMethod.make("""
                            public static ${lambdaClass.name} _${lambdaFactoryName}() {
                                return instance;
                            }
                        """, lambdaClass);
            lambdaClass.addMethod(newLambdaFactory)
        } else {
            val newLambdaFactory = CtMethod(lambdaClass, "_" + lambdaFactoryName, lambdaFactory.parameterTypes, lambdaClass)
            newLambdaFactory.modifiers = newLambdaFactory.modifiers.or(Modifier.STATIC)
            newLambdaFactory.setBody("""
                            { return new ${lambdaClass.name}($$); }
                        """)
            lambdaClass.addMethod(newLambdaFactory)
        }
    }

    private fun cleanupLambdaClass(lambdaClass: CtClass, classPool: ClassPool) {
        // assume that lambdaClass implements just the single functional interface
        val retroClass = getRetroClass(lambdaClass.interfaces[0], classPool)

        lambdaClass.interfaces = lambdaClass.interfaces.filter {
            it == retroClass
        }.toTypedArray()

        lambdaClass.removeMethod(lambdaClass.getDeclaredMethod(lambdaFactoryName))
    }

    fun getRetroClass(ctClass: CtClass, classPool: ClassPool): CtClass {
        val packageName = ctClass.packageName
        val simpleName = ctClass.simpleName
        return classPool.get("$retropilerRuntimePackage.$packageName._$simpleName")
    }

    fun collectClassPath(invocation: TransformInvocation): Set<File> {
        val files = HashSet<File>()
        invocation.inputs.forEach { input ->
            files.addAll(input.directoryInputs.map { it.file })
            files.addAll(input.jarInputs.map { it.file })
        }
        invocation.referencedInputs.forEach { input ->
            files.addAll(input.directoryInputs.map { it.file })
            files.addAll(input.jarInputs.map { it.file })
        }
        return files;
    }

    fun collectClassNames(invocation: TransformInvocation): List<String> {
        val classNames = ArrayList<String>()

        if (invocation.isIncremental) {
            classNames.addAll(
                    invocation.inputs
                            .flatMap { it.directoryInputs }
                            .flatMap { it.changedFiles.entries }
                            .filter { requiresTransform(it.value) }
                            .map { pathToClassName(it.key.canonicalPath) }
            )

            invocation.inputs
                    .flatMap { it.jarInputs }
                    .filter { requiresTransform(it.status) }
                    .map { JarFile(it.file) }
                    .flatMap { it.entries().toList() }
                    .filter { !it.isDirectory && it.name.endsWith(SdkConstants.DOT_CLASS) }
                    .map { jarEntry -> pathToClassName(jarEntry.name) }
        } else {
            classNames.addAll(
                    invocation.inputs
                            .flatMap { it.directoryInputs }
                            .flatMap { listFilesRecursively(it.file).map { file -> file.relativeTo(it.file) } }
                            .map { it.path }
                            .filter { it.endsWith(SdkConstants.DOT_CLASS) }
                            .map { pathToClassName(it) }
            )
        }

        return classNames
    }

    fun requiresTransform(status: Status): Boolean {
        return status == Status.ADDED || status == Status.CHANGED || status == Status.NOTCHANGED
    }

    fun pathToClassName(path: String): String {
        return path.substring(0, path.length - SdkConstants.DOT_CLASS.length)
                .replace("/", ".")
                .replace("\\", ".")
    }

    fun listFilesRecursively(dir: File): Collection<File> {
        val list = ArrayList<File>()

        dir.listFiles().forEach { file ->
            if (file.isDirectory) {
                list.addAll(listFilesRecursively(file))
            } else if (file.isFile) {
                list.add(file)
            }
        }

        return list
    }


    fun copyResourceFiles(inputs: Collection<TransformInput>, outputDir: File) {
        inputs.forEach {
            it.directoryInputs.forEach {
                val dirPath = it.file.absoluteFile;

                listFilesRecursively(dirPath)
                        .filter { !it.name.endsWith(SdkConstants.DOT_CLASS) }
                        .forEach { file ->
                            val dest = File(outputDir, file.relativeTo(dirPath).path)
                            System.out.println(dest)
                        }
            }
        }
    }
}
