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
import org.gradle.api.UncheckedIOException;
import org.gradle.api.artifacts.Configuration;
import org.gradle.api.artifacts.ConfigurationContainer;
import org.gradle.api.artifacts.Dependency;
import org.gradle.api.initialization.dsl.ScriptHandler;
import org.gradle.api.internal.artifacts.dsl.dependencies.DependencyFactory;
import org.gradle.api.internal.file.FileResolver;
import org.gradle.api.plugins.ObjectConfigurationAction;
import org.gradle.api.plugins.PluginContainer;
import org.gradle.configuration.ScriptPlugin;
import org.gradle.configuration.ScriptPluginFactory;
import org.gradle.groovy.scripts.UriScriptSource;
import org.gradle.initialization.ClassLoaderRegistry;
import org.gradle.util.GUtil;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.Set;

public class DefaultObjectConfigurationAction implements ObjectConfigurationAction {
    private final FileResolver resolver;
    private final ScriptPluginFactory configurerFactory;
    private final Set<Object> targets = new LinkedHashSet<Object>();
    private final Set<Runnable> actions = new LinkedHashSet<Runnable>();
    private final ConfigurationContainer configurationContainer;
    private final DependencyFactory dependencyFactory;
    private final ClassLoaderRegistry classLoaderRegistry;
    private final Object[] defaultTargets;

    public DefaultObjectConfigurationAction(FileResolver resolver, ScriptPluginFactory configurerFactory, ConfigurationContainer configurationContainer, DependencyFactory dependencyFactory, ClassLoaderRegistry classLoaderRegistry,
                                            Object... defaultTargets) {
        this.resolver = resolver;
        this.configurerFactory = configurerFactory;
        this.configurationContainer = configurationContainer;
        this.dependencyFactory = dependencyFactory;
        this.classLoaderRegistry = classLoaderRegistry;
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

    private void applyPlugin(String pluginNotation) {
        for (Object target : targets) {
            if (target instanceof Project) {
                Project project = (Project) target;

                final PluginContainer plugins;
                String pluginId;

                if (pluginNotation.contains(":")) {
                    final int pos = pluginNotation.lastIndexOf(':');
                    pluginId = pluginNotation.substring(pos +1);
                    String dependencyNotation = pluginNotation.substring(0,pos);

                    Dependency dependency = dependencyFactory.createDependency(dependencyNotation);

                    String configurationName = ScriptHandler.CLASSPATH_CONFIGURATION+"_"+pluginNotation.replaceAll("[^A-Za-z]", "_");

                    Configuration configuration;
                    configuration = project.getBuildscript().getConfigurations().findByName(configurationName);
                    if (configuration == null) {
                        configuration = project.getBuildscript().getConfigurations().add(configurationName);
                    }

                    configuration.getDependencies().add(dependency);

                    PluginRegistry customPluginsRegistry = new DefaultPluginRegistry(getClassLoader(configuration, classLoaderRegistry.getPluginsClassLoader()));

                    plugins = new DefaultProjectsPluginContainer(customPluginsRegistry, project);
                } else {
                    pluginId = pluginNotation;
                    plugins = project.getPlugins();
                }

                plugins.apply(pluginId);
            } else {
                throw new UnsupportedOperationException(String.format("Cannot apply plugin with id '%s' to '%s' (class: %s) as it is not a Project", pluginNotation, target.toString(), target.getClass().getName()));
            }
        }
    }

    // duplicates BuildSourceBuilder#buildAndCreateClassLoader
    private ClassLoader getClassLoader(Configuration configuration, ClassLoader rootClassLoader) {
        Set<File> classpath = configuration.getFiles();
        Iterator<File> classpathIterator = classpath.iterator();
        URL[] urls = new URL[classpath.size()];
        for (int i = 0; i < urls.length; i++) {
            try {
                urls[i] = classpathIterator.next().toURI().toURL();
            } catch (MalformedURLException e) {
                throw new UncheckedIOException(e);
            }
        }
        return new URLClassLoader(urls, rootClassLoader);
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
