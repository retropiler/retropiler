package io.github.retropiler

import com.android.SdkConstants
import com.android.build.api.transform.*
import com.android.build.gradle.AppExtension
import javassist.ClassPool
import javassist.CtMethod
import org.gradle.api.Project
import org.slf4j.LoggerFactory
import java.io.File
import java.util.*
import java.util.jar.JarFile
import java.util.regex.Pattern

class RetropilerTransform(val project: Project) : Transform() {

    val logger = LoggerFactory.getLogger(RetropilerTransform::class.java)

    override fun getName(): String {
        return "retropiler"
    }

    override fun getInputTypes(): Set<QualifiedContent.ContentType> {
        return setOf(QualifiedContent.DefaultContentType.CLASSES)
    }

    override fun isIncremental(): Boolean {
        return true
    }

    override fun getScopes(): Set<QualifiedContent.Scope> {
        return EnumSet.of(
                QualifiedContent.Scope.PROJECT,
                QualifiedContent.Scope.PROJECT_LOCAL_DEPS)
    }

    override fun getReferencedScopes(): Set<QualifiedContent.Scope> {
        return EnumSet.of(
                QualifiedContent.Scope.EXTERNAL_LIBRARIES)
    }

    override fun transform(invocation: TransformInvocation) {
        val t0 = System.currentTimeMillis();

        val outputDir = invocation.outputProvider.getContentLocation(name, outputTypes, scopes, Format.DIRECTORY)

        val classNames = collectClassNames(invocation)

        val classPool = ClassPool(null)
        classPool.appendSystemPath()
        project.extensions.findByType(AppExtension::class.java).bootClasspath.forEach {
            classPool.appendClassPath(it.absolutePath)
        }

        collectClassPath(invocation).forEach {
            System.out.println("classPath: $it")
            classPool.appendClassPath(it.absolutePath)
        }

        classPool.appendClassPath(project.rootProject.file("runtime/build/classes/main").absolutePath)

        val lambdaPattern = Pattern.compile(".+\\\$\\\$Lambda\\\$\\d+")

        classNames.forEach { className ->
            if (lambdaPattern.matcher(className).matches()) {
                val lambdaClass = classPool.get(className)
                lambdaClass.addInterface(classPool.get("io.github.retropiler.runtime.JavaUtilFunctionConsumer"))

                val lambdaFactory = lambdaClass.getDeclaredMethod("lambdaFactory$")
                val newLambdaFactory = CtMethod.make("""
                    public static ${lambdaClass.name} lambdaFactory$() { return instance; }
                """, lambdaClass);
                lambdaClass.addMethod(newLambdaFactory)
            }
        }

        classNames.forEach { className ->
            val ctClass = classPool.get(className)
            ctClass.instrument(RetropilerExprEditor(classPool))

        }

        classNames.forEach { className ->
            val ctClass = classPool.get(className)

            if (lambdaPattern.matcher(className).matches()) {
                ctClass.interfaces = ctClass.interfaces.filter {
                    it.name != "java.util.function.Consumer"
                }.toTypedArray()
            }

            ctClass.writeFile(outputDir.canonicalPath)
        }

        copyResourceFiles(invocation.inputs, outputDir)

        logger.info("transform: ${System.currentTimeMillis() - t0}ms")
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
