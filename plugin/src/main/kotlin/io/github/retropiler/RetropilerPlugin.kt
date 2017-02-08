package io.github.retropiler

import com.android.build.gradle.AppExtension
import org.gradle.api.Plugin
import org.gradle.api.Project

class RetropilerPlugin: Plugin<Project> {
    override fun apply(project: Project) {
        val extension = project.extensions.findByType(AppExtension::class.java)
        extension.registerTransform(RetropilerTransform(project))
    }
}
