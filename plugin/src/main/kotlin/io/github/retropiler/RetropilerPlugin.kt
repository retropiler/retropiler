package io.github.retropiler

import com.android.build.gradle.AppExtension
import org.gradle.api.Plugin
import org.gradle.api.Project

class RetropilerPlugin: Plugin<Project> {
    override fun apply(project: Project) {

        project.apply(mapOf("plugin" to "me.tatarka.retrolambda"))

        val extension = project.extensions.findByType(AppExtension::class.java)
        if (extension == null) {
            throw RuntimeException("Apply plugin 'com.android.application' before retropiler")
        }

        extension.registerTransform(RetropilerTransform(project))

        if (isReleased()) {
            project.dependencies.add("compile", "io.github.retropiler:retropiler-runtime:${version()}")
        } else {
            project.dependencies.add("compile", project.rootProject.project(":runtime"))
        }
    }

    fun isReleased(): Boolean {
        return javaClass.getPackage().implementationVersion != null
    }

    fun version(): String {
        return javaClass.getPackage().implementationVersion
    }
}
