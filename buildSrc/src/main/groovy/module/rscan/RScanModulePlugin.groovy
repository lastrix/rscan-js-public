/*
 * Copyright (C) 2019-2020.  RScan-js-public project
 *
 * This file is part of RScan-js-public project.
 *
 * RScan-js-public is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 * RScan-js-public is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with RScan-js-public.  If not, see <http://www.gnu.org/licenses/>.
 */

package module.rscan

import antlr4.Antlr4Scanner
import org.gradle.api.JavaVersion
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.plugins.JavaPlugin
import org.gradle.api.plugins.scala.ScalaPlugin
import org.gradle.api.tasks.scala.ScalaCompile
import org.gradle.plugins.ide.idea.IdeaPlugin
import org.gradle.plugins.ide.idea.model.IdeaModel

// through config in build.gradle
@SuppressWarnings("unused")
class RScanModulePlugin implements Plugin<Project> {
    @Override
    void apply(Project project) {
        def extension = project.extensions.create("RScanModule", RScanModuleExtension)

        project.plugins.apply(JavaPlugin)
        applyJavaConfig(project)
        project.plugins.apply(IdeaPlugin)
        applyIdeaConfig(project)
        project.plugins.apply(ScalaPlugin)
        applyScalaConfig(project)

        project.afterEvaluate {
            if (extension.antlr4Patches != null)
                Antlr4Scanner.scanAntlr4Grammars(project, extension.antlr4Patches)

            configureExtension(project, extension)
        }
    }

    private static void applyIdeaConfig(Project project) {
        IdeaModel model = project.getExtensions().findByType(IdeaModel)
        def module = model.getModule()
        module.setDownloadJavadoc(true)
        module.setDownloadSources(true)
        module.setOutputDir(new File(project.buildDir, "idea"))
        module.setTestOutputDir(new File(project.buildDir, "ideaTest"))
        module.getGeneratedSourceDirs().add(new File(project.buildDir, "generated-src"))
    }

    def applyScalaConfig(Project project) {
        project.tasks.withType(ScalaCompile) {
            sourceCompatibility = JavaVersion.VERSION_1_8
            targetCompatibility = JavaVersion.VERSION_1_8
            scalaCompileOptions.additionalParameters = ["-encoding", "utf8"]
        }

        project.sourceSets {
            main {
                scala { srcDirs += new File("$project.buildDir/generated-src") }
                resources { srcDirs += new File("$project.buildDir/generated-resources") }
            }
        }
    }

    private void applyJavaConfig(Project project) {
        project.sourceCompatibility = JavaVersion.VERSION_11
        project.targetCompatibility = JavaVersion.VERSION_11

        project.test {
            systemProperty 'test.property', 'true'
            jvmArgs '-XX:+UseG1GC'
            jvmArgs '-XX:MaxHeapFreeRatio=30'
            jvmArgs '-XX:MinHeapFreeRatio=10'
            minHeapSize = "512m"
            maxHeapSize = "8192m"
        }
    }

    def configureExtension(Project project, RScanModuleExtension ext) {
        project.jar {
            inputs.property("moduleName", ext.moduleName)
            manifest {
                attributes(
                        'Implementation-Title': ext.jarImplTitle,
                        'Implementation-Version': project.version,
                        "Class-Path": project.configurations.runtime.collect { it.getName() }.join(' '),
                        "Automatic-Module-Name": ext.moduleName
                )
            }
        }
    }
}
