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
                QualifiedContent.Scope.PROJECT
        )
    }

    override fun getReferencedScopes(): Set<QualifiedContent.Scope> {
        return EnumSet.of(
                QualifiedContent.Scope.PROJECT_LOCAL_DEPS,
                QualifiedContent.Scope.SUB_PROJECTS,
                QualifiedContent.Scope.SUB_PROJECTS_LOCAL_DEPS,
                QualifiedContent.Scope.EXTERNAL_LIBRARIES,
                QualifiedContent.Scope.TESTED_CODE
        )
    }

    override fun transform(invocation: TransformInvocation) {
        val t0 = System.currentTimeMillis();

        val outputDir = invocation.outputProvider.getContentLocation(name, outputTypes, scopes, Format.DIRECTORY)

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
                    .filter { !it.name.startsWith("android/") } // skip official classes
                    .filter { !it.isDirectory && it.name.endsWith(SdkConstants.DOT_CLASS) }
                    .map { jarEntry -> pathToClassName(jarEntry.name) }
        } else {
            classNames.addAll(
                    invocation.inputs
                            .flatMap { it.directoryInputs }
                            .flatMap { listFilesRecursively(it.file, it.file) }
                            .map { it.path }
                            .filter { it.startsWith("android/") } // skip official classes
                            .filter { it.endsWith(SdkConstants.DOT_CLASS) }
                            .map { pathToClassName(it) }
            )
        }

        val classPool = ClassPool(null)
        classPool.appendSystemPath()
        project.extensions.findByType(AppExtension::class.java).bootClasspath.forEach {
            classPool.appendClassPath(it.absolutePath)
        }

        invocation.inputs.forEach {
            it.directoryInputs.forEach {
                classPool.appendClassPath(it.file.absolutePath)
            }
            it.jarInputs.forEach {
                classPool.appendClassPath(it.file.absolutePath)
            }
        }
        invocation.referencedInputs.forEach {
            it.directoryInputs.forEach {
                classPool.appendClassPath(it.file.absolutePath)
            }
            it.jarInputs.forEach {
                classPool.appendClassPath(it.file.absolutePath)
            }
        }

        classNames.forEach { className ->
            System.out.println("XXX className=$className")
            val ctClass = classPool.get(className)

            ctClass.writeFile(outputDir.canonicalPath)
        }

        invocation.inputs.forEach {
            it.directoryInputs.forEach {
                // TODO: copy resources
            }
        }

        //RetropilerMain(inputClassFiles, outputDir).run()

        logger.info("transform: ${System.currentTimeMillis() - t0}ms")
    }

    fun requiresTransform(status: Status): Boolean {
        return status == Status.ADDED || status == Status.CHANGED || status == Status.NOTCHANGED
    }

    fun pathToClassName(path: String): String {
        return path.substring(0, path.length - SdkConstants.DOT_CLASS.length)
                .replace("/", ".")
                .replace("\\", ".")
    }

    fun listFilesRecursively(root: File, dir: File): Collection<File> {
        val list = ArrayList<File>()

        dir.listFiles().forEach { file ->
            if (file.isDirectory) {
                list.addAll(listFilesRecursively(root, file))
            } else if (file.isFile) {
                list.add(file.relativeTo(root))
            }
        }

        return list
    }
}
