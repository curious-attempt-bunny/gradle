/*
 * Copyright 2010 the original author or authors.
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

package org.gradle.api.internal.tasks;

import org.gradle.api.internal.file.CompositeFileCollection;
import org.gradle.api.internal.file.FileResolver;
import org.gradle.api.internal.file.collections.DefaultConfigurableFileCollection;
import org.gradle.api.internal.file.collections.FileCollectionResolveContext;
import org.gradle.api.tasks.SourceSetOutput;

import java.io.File;
import java.util.concurrent.Callable;

/**
 * @author: Szczepan Faber, created at: 5/4/11
 */
public class DefaultSourceSetOutput extends CompositeFileCollection implements SourceSetOutput {
    private DefaultConfigurableFileCollection outputDirectories;
    private Object classesDir;
    private Object resourcesDir;
    private FileResolver fileResolver;

    public DefaultSourceSetOutput(String sourceSetDisplayName, FileResolver fileResolver, TaskResolver taskResolver) {
        this.fileResolver = fileResolver;
        //TODO SF: rename classes to ouput later
        String displayName = String.format("%s classes", sourceSetDisplayName);
        outputDirectories = new DefaultConfigurableFileCollection(displayName, fileResolver, taskResolver, new Callable() {
            public Object call() throws Exception {
                return getClassesDir();
            }
        }, new Callable() {
            public Object call() throws Exception {
                return getResourcesDir();
            }
        });
    }

    @Override
    public void resolve(FileCollectionResolveContext context) {
        context.add(outputDirectories);
    }

    @Override
    public String getDisplayName() {
        return outputDirectories.getDisplayName();
    }

    public File getClassesDir() {
        if (classesDir == null) {
            return null;
        }
        return fileResolver.resolve(classesDir);
    }

    public void setClassesDir(Object classesDir) {
        this.classesDir = classesDir;
    }

    public File getResourcesDir() {
        if (resourcesDir == null) {
            return null;
        }
        return fileResolver.resolve(resourcesDir);
    }

    public void setResourcesDir(Object resourcesDir) {
       this.resourcesDir = resourcesDir;
    }

    public void builtBy(Object ... taskPaths) {
        outputDirectories.builtBy(taskPaths);
    }
}