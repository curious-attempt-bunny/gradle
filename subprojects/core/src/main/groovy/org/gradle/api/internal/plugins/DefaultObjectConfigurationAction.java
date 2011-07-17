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

package org.gradle.api.internal.plugins;

import org.gradle.api.Plugin;
import org.gradle.api.Project;
import org.gradle.api.artifacts.Configuration;
import org.gradle.api.artifacts.ConfigurationContainer;
import org.gradle.api.artifacts.Dependency;
import org.gradle.api.internal.GradleInternal;
import org.gradle.api.internal.artifacts.dsl.dependencies.DependencyFactory;
import org.gradle.api.internal.file.FileResolver;
import org.gradle.api.internal.project.ProjectInternal;
import org.gradle.api.plugins.ObjectConfigurationAction;
import org.gradle.configuration.ScriptPlugin;
import org.gradle.configuration.ScriptPluginFactory;
import org.gradle.groovy.scripts.UriScriptSource;
import org.gradle.util.GUtil;
import org.gradle.util.ObservableUrlClassLoader;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URI;
import java.util.LinkedHashSet;
import java.util.Set;

public class DefaultObjectConfigurationAction implements ObjectConfigurationAction {
    private final FileResolver resolver;
    private final ScriptPluginFactory configurerFactory;
    private final Set<Object> targets = new LinkedHashSet<Object>();
    private final Set<Runnable> actions = new LinkedHashSet<Runnable>();
    private final Object[] defaultTargets;
    private String group;
    private String name;
    private String version;

    public DefaultObjectConfigurationAction(FileResolver resolver, ScriptPluginFactory configurerFactory,
                                            Object... defaultTargets) {
        this.resolver = resolver;
        this.configurerFactory = configurerFactory;
        this.defaultTargets = defaultTargets;
    }

    public ObjectConfigurationAction to(Object... targets) {
        GUtil.flatten(targets, this.targets);
        return this;
    }

    public ObjectConfigurationAction from(final Object script) {
        actions.add(new Runnable() {
            public void run() {
                applyScript(script);
            }
        });
        return this;
    }

    public ObjectConfigurationAction plugin(final Class<? extends Plugin> pluginClass) {
        actions.add(new Runnable() {
            public void run() {
                applyPlugin(pluginClass);
            }
        });
        return this;
    }

    public ObjectConfigurationAction plugin(final String pluginId) {
        actions.add(new Runnable() {
            public void run() {
                applyPlugin(pluginId);
            }
        });
        return this;
    }

    public void setGroup(String group) {
        this.group = group;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setVersion(String version) {
        this.version = version;
    }

    private void applyScript(Object script) {
        URI scriptUri = resolver.resolveUri(script);
        ScriptPlugin configurer = configurerFactory.create(new UriScriptSource("script", scriptUri));
        for (Object target : targets) {
            configurer.apply(target);
        }
    }

    private void applyPlugin(Class<? extends Plugin> pluginClass) {
        for (Object target : targets) {
            if (target instanceof Project) {
                Project project = (Project) target;
                project.getPlugins().apply(pluginClass);
            } else {
                throw new UnsupportedOperationException(String.format("Cannot apply plugin of class '%s' to '%s' (class: %s) as it is not a Project", pluginClass.getName(), target.toString(), target.getClass().getName()));
            }
        }
    }

    private void applyPlugin(String pluginId) {
        for (Object target : targets) {
            if (target instanceof Project) {
                Project project = (Project) target;

                if (isDependencyProvided()) {
                    Configuration configuration = createConfigurationWithDependency(project, group, name, version);
                    ClassLoader parentClassLoader = getParentClassLoader(project);
                    ClassLoader classLoader = createIsolatedClassLoader(configuration, parentClassLoader);
                    project.getPlugins().apply(pluginId, classLoader);
                } else {
                    project.getPlugins().apply(pluginId);
                }
            } else {
                throw new UnsupportedOperationException(String.format("Cannot apply plugin with id '%s' to '%s' (class: %s) as it is not a Project", pluginId, target.toString(), target.getClass().getName()));
            }
        }
    }

    private ClassLoader getParentClassLoader(Project project) {
        GradleInternal gradleInternal = (GradleInternal) project.getGradle();
        ClassLoader parent = gradleInternal.getScriptClassLoader();
        return parent;
    }

    private ClassLoader createIsolatedClassLoader(Configuration configuration, ClassLoader parentClassLoader) {
        ObservableUrlClassLoader classLoader = new ObservableUrlClassLoader(parentClassLoader);
        for (File file : configuration.resolve()) {
            try {
                classLoader.addURL(file.toURI().toURL());
            } catch (MalformedURLException e) {
                throw new RuntimeException(e);
            }
        }
        return classLoader;
    }

    private Configuration createConfigurationWithDependency(Project project, String group, String name, String version) {
        ConfigurationContainer configurationContainer = project.getBuildscript().getConfigurations();
        String configurationName = "plugin_"+group+"_"+name+"_"+version;
        Configuration configuration = configurationContainer.findByName(configurationName);
        if (configuration == null) {
            configuration = configurationContainer.add(configurationName);
            project.getBuildscript().getDependencies().add(configurationName, group + ":" + name + ":" + version);
        }
        return configuration;
    }

    private boolean isDependencyProvided() {
        return group != null || name != null || version != null;
    }

    public void execute() {
        if (targets.isEmpty()) {
            to(defaultTargets);
        }

        for (Runnable action : actions) {
            action.run();
        }
    }
}
