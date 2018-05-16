/*
 * Copyright (c) 2017 FUJI Goro (gfx).
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.github.retropiler

import com.android.build.gradle.AppExtension
import com.android.build.gradle.AppPlugin
import org.gradle.api.Plugin
import org.gradle.api.Project

class RetropilerPlugin: Plugin<Project> {

    companion object {
        const val DEFAULT_USE_RETROLAMBDA = true
    }

    override fun apply(project: Project) {
        project.extensions.create(RetropilerExtension.NAME, RetropilerExtension::class.java)

        project.plugins.withType(AppPlugin::class.java) {
            if(when (project.extensions.extraProperties.has("useRetrolambda")) {
                true -> project.extensions.extraProperties.get("useRetrolambda")
                else -> DEFAULT_USE_RETROLAMBDA
            } as Boolean) {
                project.apply(mapOf("plugin" to "me.tatarka.retrolambda"))
            }

            val extension = project.extensions.findByType(AppExtension::class.java)
            extension.registerTransform(RetropilerTransform(project))

            if (isReleased()) {
                project.dependencies.add("compile", "io.github.retropiler:retropiler-runtime:${version()}")
            } else {
                project.dependencies.add("compile", project.rootProject.project(":runtime"))
            }
        }
    }

    fun isReleased(): Boolean {
        return javaClass.getPackage().implementationVersion != null
    }

    fun version(): String {
        return javaClass.getPackage().implementationVersion
    }
}
