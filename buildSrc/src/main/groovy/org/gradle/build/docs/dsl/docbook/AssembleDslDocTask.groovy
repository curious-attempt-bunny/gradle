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
package org.gradle.build.docs.dsl.docbook

import org.gradle.api.DefaultTask
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.InputFile
import org.gradle.api.tasks.TaskAction
import org.gradle.api.file.FileCollection
import org.gradle.api.tasks.InputFiles
import org.w3c.dom.Document
import groovy.xml.dom.DOMCategory
import org.w3c.dom.Element
import org.gradle.api.tasks.InputDirectory
import org.gradle.build.docs.XIncludeAwareXmlProvider
import org.gradle.build.docs.BuildableDOMCategory
import org.gradle.build.docs.dsl.model.ClassMetaData
import org.gradle.build.docs.model.ClassMetaDataRepository
import org.gradle.build.docs.model.SimpleClassMetaDataRepository
import org.gradle.build.docs.dsl.LinkMetaData
import org.gradle.api.Project
import org.gradle.build.docs.dsl.ClassLinkMetaData
import org.gradle.build.docs.dsl.model.ClassExtensionMetaData

/**
 * Generates the docbook source for the DSL reference guide.
 *
 * Uses the following as input:
 * <ul>
 * <li>Meta-data extracted from the source by {@link org.gradle.build.docs.dsl.ExtractDslMetaDataTask}.</li>
 * <li>Meta-data about the plugins, in the form of an XML file.</li>
 * <li>{@code sourceFile} - A main docbook template file containing the introductory material and a list of classes to document.</li>
 * <li>{@code classDocbookDir} - A directory that should contain docbook template for each class referenced in main docbook template.</li>
 * </ul>
 *
 * Produces the following:
 * <ul>
 * <li>A docbook book XML file containing the reference.</li>
 * <li>A meta-data file containing information about where the canonical documentation for each class can be found:
 * as dsl doc, javadoc or groovydoc.</li>
 * </ul>
 */
class AssembleDslDocTask extends DefaultTask {
    @InputFile
    File sourceFile
    @InputFile
    File classMetaDataFile
    @InputFile
    File pluginsMetaDataFile
    @InputDirectory
    File classDocbookDir //TODO SF - it would be nice to do some renames, docbookTemplatesDir, destLinksFile
    @OutputFile
    File destFile
    @OutputFile
    File linksFile
    @InputFiles
    FileCollection classpath;

    @TaskAction
    def transform() {
        XIncludeAwareXmlProvider provider = new XIncludeAwareXmlProvider(classpath)
        provider.parse(sourceFile)
        transformDocument(provider.document)
        provider.write(destFile)
    }

    private def transformDocument(Document mainDocbookTemplate) {
        ClassMetaDataRepository<ClassMetaData> classRepository = new SimpleClassMetaDataRepository<ClassMetaData>()
        classRepository.load(classMetaDataFile)
        ClassMetaDataRepository<ClassLinkMetaData> linkRepository = new SimpleClassMetaDataRepository<ClassLinkMetaData>()
        //for every method found in class meta, create a javadoc/groovydoc link
        classRepository.each {name, ClassMetaData metaData ->
            linkRepository.put(name, new ClassLinkMetaData(metaData))
        }

        use(DOMCategory) {
            use(BuildableDOMCategory) {
                Map<String, ClassExtensionMetaData> extensions = loadPluginsMetaData()
                DslDocModel model = new DslDocModel(classDocbookDir, mainDocbookTemplate, classpath, classRepository, extensions)
                def root = mainDocbookTemplate.documentElement
                root.section.table.each { Element table ->
                    mergeContent(table, model, linkRepository)
                }
            }
        }

        linkRepository.store(linksFile)
    }

    def loadPluginsMetaData() {
        XIncludeAwareXmlProvider provider = new XIncludeAwareXmlProvider(classpath)
        provider.parse(pluginsMetaDataFile)
        Map<String, ClassExtensionMetaData> extensions = [:]
        provider.root.plugin.each { Element plugin ->
            def pluginId = plugin.'@id'
            plugin.extends.each { Element e ->
                def targetClass = e.'@targetClass'
                def extension = extensions[targetClass]
                if (!extension) {
                    extension = new ClassExtensionMetaData(targetClass)
                    extensions[targetClass] = extension
                }
                def mixinClass = e.'@mixinClass'
                if (mixinClass) {
                    extension.addMixin(pluginId, mixinClass)
                }
                def extensionClass = e.'@extensionClass'
                if (extensionClass) {
                    def extensionId = e.'@id'
                    extension.addExtension(pluginId, extensionId, extensionClass)
                }
            }
        }
        return extensions
    }

    def mergeContent(Element typeTable, DslDocModel model, ClassMetaDataRepository<ClassLinkMetaData> linkRepository) {
        def title = typeTable.title[0].text()

        //TODO below checks makes it harder to add new sections
        //because the new section will work correctly only when the section title ends with 'types' :)
        if (title.matches('(?i).* types')) {
            mergeTypes(typeTable, model, linkRepository)
        } else if (title.matches('(?i).* blocks')) {
            mergeBlocks(typeTable, model)
        } else {
            return
        }

        typeTable['@role'] = 'dslTypes'
    }

    def mergeBlocks(Element blocksTable, DslDocModel model) {
        blocksTable.addFirst {
            thead {
                tr {
                    td('Block')
                    td('Description')
                }
            }
        }

        def project = model.getClassDoc(Project.class.name)

        blocksTable.tr.each { Element tr ->
            mergeBlock(tr, project)
        }
    }

    def mergeBlock(Element tr, ClassDoc project) {
        String blockName = tr.td[0].text().trim()
        BlockDoc blockDoc = project.getBlock(blockName)
        tr.children = {
            td { link(linkend: blockDoc.id) { literal("$blockName { }")} }
            td(blockDoc.description)
        }
    }

    def mergeTypes(Element typeTable, DslDocModel model, ClassMetaDataRepository<ClassLinkMetaData> linkRepository) {
        typeTable.addFirst {
            thead {
                tr {
                    td('Type')
                    td('Description')
                }
            }
        }

        typeTable.tr.each { Element tr ->
            mergeType(tr, model, linkRepository)
        }
    }

    def mergeType(Element typeTr, DslDocModel model, ClassMetaDataRepository<ClassLinkMetaData> linkRepository) {
        String className = typeTr.td[0].text().trim()
        ClassDoc classDoc = model.getClassDoc(className)
        try {
            //classDoc renderer renders the content of the class and also links to properties/methods
            new ClassDocRenderer(new LinkRenderer(typeTr.ownerDocument, model)).mergeContent(classDoc)
            def linkMetaData = linkRepository.get(className)
            linkMetaData.style = LinkMetaData.Style.Dsldoc
            classDoc.classMethods.each { methodDoc ->
                linkMetaData.addMethod(methodDoc.metaData, LinkMetaData.Style.Dsldoc)
            }
            classDoc.classBlocks.each { blockDoc ->
                linkMetaData.addBlockMethod(blockDoc.blockMethod.metaData)
            }
            classDoc.classProperties.each { propertyDoc ->
                linkMetaData.addGetterMethod(propertyDoc.name, propertyDoc.metaData.getter)
            }
            Element root = typeTr.ownerDocument.documentElement
            root << classDoc.classSection
            typeTr.children = {
                td {
                    link(linkend: classDoc.id) { literal(classDoc.simpleName) }
                }
                td(classDoc.description)
            }
        } catch (Exception e) {
            throw new RuntimeException("Failed to generate documentation for class '$className'.", e)
        }
    }
}
