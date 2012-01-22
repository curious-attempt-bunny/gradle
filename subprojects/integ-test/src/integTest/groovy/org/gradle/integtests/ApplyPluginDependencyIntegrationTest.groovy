package org.gradle.integtests

import org.gradle.integtests.fixtures.AbstractIntegrationTest
import org.gradle.integtests.fixtures.ArtifactBuilder
import org.junit.Test

class ApplyPluginDependencyIntegrationTest extends AbstractIntegrationTest {
    @Test
    public void testStandardPlugin() {
        ArtifactBuilder builder = artifactBuilder()
        builder.sourceFile('CustomPlugin.java') << '''
            package org.custom.plugin;
            import org.gradle.api.*;
            public class CustomPlugin implements Plugin<Project> {
                public void apply(Project p) { }
            }
        '''

        builder.sourceFile('CustomTask.java') << '''
            package org.custom.plugin;
            import org.gradle.api.*;
            public class CustomTask extends DefaultTask { }
        '''

        builder.resourceFile('META-INF/gradle-plugins/custom.properties') << 'implementation-class=org.custom.plugin.CustomPlugin'

        builder.buildJar(file('customArtifactId-1.0.jar'))

        File buildFile = file('build.gradle')

        buildFile << """
            apply plugin: 'custom'

            buildscript {
                repositories {
                    flatDir {
                        dir '.'
                    }
                }
                dependencies {
                    classpath 'customGroup:customArtifactId:1.0'
                }
            }

            import org.custom.plugin.CustomTask

            task impl(type:CustomTask)
        """

        usingBuildFile(buildFile).run();
    }
}