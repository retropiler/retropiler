package io.github.retropiler

import com.android.SdkConstants
import com.android.build.api.transform.*
import com.android.build.gradle.AppExtension
import javassist.ClassPool
import org.gradle.api.Project
import org.slf4j.LoggerFactory
import java.io.File
import java.util.*
import java.util.jar.JarFile

class RetropilerTransform(val project: Project) : Transform() {

    val logger = LoggerFactory.getLogger(RetropilerPlugin::class.java)!!

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
        val t0 = System.currentTimeMillis()

        val outputDir = invocation.outputProvider.getContentLocation(name, outputTypes, scopes, Format.DIRECTORY)

        val classPool = ClassPool(null)
        classPool.appendSystemPath()
        project.extensions.findByType(AppExtension::class.java).bootClasspath.forEach {
            classPool.appendClassPath(it.absolutePath)
        }

        collectClassPath(invocation).forEach {
            logger.trace("classPath: $it")
            classPool.appendClassPath(it.absolutePath)
        }

        classPool.appendClassPath(project.rootProject.file("runtime/build/classes/main").absolutePath) // FIXME

        val runtime = Runtime(classPool)
        val ctClasses = collectClassNames(invocation).map { className -> classPool.get(className) }

        // pre process
        ctClasses.forEach { ctClass ->
            runtime.preprocess(ctClass)
        }

        // main process
        ctClasses.forEach { ctClass ->
            ctClass.instrument(RetropilerExprEditor(classPool))
        }

        // post process and write it down
        ctClasses.forEach { ctClass ->
            runtime.postprocess(ctClass)

            ctClass.writeFile(outputDir.canonicalPath)
        }

        copyResourceFiles(invocation.inputs, outputDir)

        logger.trace("transform: ${System.currentTimeMillis() - t0}ms")
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
        return files
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
                val dirPath = it.file.absoluteFile

                listFilesRecursively(dirPath)
                        .filter { !it.name.endsWith(SdkConstants.DOT_CLASS) }
                        .forEach { file ->
                            val dest = File(outputDir, file.relativeTo(dirPath).path)
                            logger.trace("DEBUG: resource files: $dest")
                        }
            }
        }
    }
}
