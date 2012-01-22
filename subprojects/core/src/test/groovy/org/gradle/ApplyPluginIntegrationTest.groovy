package org.gradle

import org.gradle.integtests.fixtures.AbstractIntegrationTest
import org.junit.Test

class ApplyPluginIntegrationTest extends AbstractIntegrationTest {
    @Test
    public void testStandardPlugin() {
        File buildFile = file('build.gradle')
        println buildFile.absolutePath

        buildFile << """
            import org.jfrog.gradle.plugin.artifactory.extractor.BuildInfoTask


//            apply plugin: ':artifactory'
            apply plugin: 'artifactory', dependency: "org.jfrog.buildinfo:build-info-extractor-gradle:2.0.10"fire
            buildscript.classpathDependency 'org.jfrog.buildinfo:build-info-extractor-gradle:2.0.10'

            buildscript {
                repositories {
                    maven { url 'http://repo.jfrog.org/artifactory/gradle-plugins' }
                }

//                dependencies {
//                    classpath(group: 'org.jfrog.buildinfo', name: 'build-info-extractor-gradle', version: '2.0.10')
//                }
            }

            assert tasks.artifactoryPublish != null
        """

        usingBuildFile(buildFile).run();
    }
}
