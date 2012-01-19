package org.gradle

import org.gradle.integtests.fixtures.AbstractIntegrationTest
import org.junit.Test

class ApplyPluginIntegrationTest extends AbstractIntegrationTest {
    @Test
    public void testStandardPlugin() {
        File buildFile = file('build.gradle')

        buildFile << """
//            apply plugin: 'artifactory'
            apply plugin: 'org.jfrog.buildinfo:build-info-extractor-gradle:2.0.10:artifactory'

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
