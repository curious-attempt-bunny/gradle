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

package org.gradle.integtests.tooling.m8

import org.gradle.integtests.tooling.fixture.MinTargetGradleVersion
import org.gradle.integtests.tooling.fixture.MinToolingApiVersion
import org.gradle.integtests.tooling.fixture.ToolingApiSpecification
import org.gradle.tooling.model.Project
import org.gradle.tooling.model.build.BuildEnvironment

@MinToolingApiVersion('1.0-milestone-8')
@MinTargetGradleVersion('1.0-milestone-8')
class BuildEnvironmentModelIntegrationTest extends ToolingApiSpecification {

    def "informs about build environment"() {
        when:
        BuildEnvironment model = withConnection { it.getModel(BuildEnvironment.class) }

        then:
        model.gradle.gradleVersion == targetDist.version
        model.java.javaHome
        !model.java.jvmArguments.empty
    }

    def "informs about java args as in the build script"() {
        given:
        dist.file('build.gradle') <<
            "project.description = java.lang.management.ManagementFactory.runtimeMXBean.inputArguments.join('##')"

        when:
        BuildEnvironment env = withConnection { it.getModel(BuildEnvironment.class) }
        Project project = withConnection { it.getModel(Project.class) }

        then:
        def inputArgsInBuild = project.description.split('##')
        inputArgsInBuild.length == env.java.jvmArguments.size()
        inputArgsInBuild.each { env.java.jvmArguments.contains(it) }
    }

    def "informs about java home as in the build script"() {
        given:
        dist.file('build.gradle') << "description = Jvm.current().javaHome.toString()"

        when:
        BuildEnvironment env = withConnection { it.getModel(BuildEnvironment.class) }
        Project project = withConnection { it.getModel(Project.class) }

        then:
        env.java.javaHome.toString() == project.description
    }

    def "informs about gradle version as in the build script"() {
        given:
        dist.file('build.gradle') << "description = GradleVersion.current().getVersion()"

        when:
        BuildEnvironment env = withConnection { it.getModel(BuildEnvironment.class) }
        Project project = withConnection { it.getModel(Project.class) }

        then:
        env.gradle.gradleVersion == project.description
    }
}
