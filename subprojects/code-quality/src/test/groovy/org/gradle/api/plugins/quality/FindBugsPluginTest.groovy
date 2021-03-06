/*
 * Copyright 2011 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.gradle.api.plugins.quality

import static org.gradle.util.Matchers.*
import static org.hamcrest.Matchers.*

import org.gradle.api.Project
import org.gradle.api.plugins.JavaBasePlugin
import org.gradle.util.HelperUtil
import org.gradle.api.tasks.SourceSet

import spock.lang.Specification

import static spock.util.matcher.HamcrestSupport.that

class FindBugsPluginTest extends Specification {
    Project project = HelperUtil.createRootProject()

    def setup() {
        project.plugins.apply(FindBugsPlugin)
    }

    def "applies java-base plugin"() {
        expect:
        project.plugins.hasPlugin(JavaBasePlugin)
    }

    def "configures findbugs configuration"() {
        def config = project.configurations.findByName("findbugs")

        expect:
        config != null
        !config.visible
        config.transitive
        config.description == 'The FindBugs libraries to be used for this project.'
    }

    def "configures findbugs extension"() {
        expect:
        FindBugsExtension extension = project.extensions.findbugs
        extension.reportsDir == project.file("build/reports/findbugs")
        extension.ignoreFailures == false
    }

    def "configures findbugs task for each source set"() {
        project.sourceSets {
            main
            test
            other
        }

        expect:
        configuresFindBugsTask("findbugsMain", project.sourceSets.main)
        configuresFindBugsTask("findbugsTest", project.sourceSets.test)
        configuresFindBugsTask("findbugsOther", project.sourceSets.other)
    }

    private void configuresFindBugsTask(String taskName, SourceSet sourceSet) {
        def task = project.tasks.findByName(taskName)
        assert task instanceof FindBugs
        task.with {
            assert description == "Run FindBugs analysis for ${sourceSet.name} classes"
            assert defaultSource == sourceSet.allJava
            assert findbugsClasspath == project.configurations.findbugs
            assert classes.empty // no classes to analyze
            assert reportFile == project.file("build/reports/findbugs/${sourceSet.name}.xml")
            assert ignoreFailures == false
        }
    }

    def "adds findbugs tasks to check lifecycle task"() {
        project.sourceSets {
            main
            test
            other
        }

        expect:
        that(project.check, dependsOn(hasItems("findbugsMain", "findbugsTest", "findbugsOther")))
    }

    def "can customize settings via extension"() {
        project.sourceSets {
            main
            test
            other
        }

        project.findbugs {
            sourceSets = [project.sourceSets.main]
            reportsDir = project.file("findbugs-reports")
            ignoreFailures = true
        }

        expect:
        hasCustomizedSettings("findbugsMain", project.sourceSets.main)
        hasCustomizedSettings("findbugsTest", project.sourceSets.test)
        hasCustomizedSettings("findbugsOther", project.sourceSets.other)
        that(project.check, dependsOn(hasItem("findbugsMain")))
        that(project.check, dependsOn(not(hasItems("findbugsTest", "findbugsOther"))))
    }

    private void hasCustomizedSettings(String taskName, SourceSet sourceSet) {
        def task = project.tasks.findByName(taskName)
        assert task instanceof FindBugs
        task.with {
            assert description == "Run FindBugs analysis for ${sourceSet.name} classes"
            assert defaultSource == sourceSet.allJava
            assert findbugsClasspath == project.configurations.findbugs
            assert reportFile == project.file("findbugs-reports/${sourceSet.name}.xml")
            assert ignoreFailures == true
        }
    }
}
