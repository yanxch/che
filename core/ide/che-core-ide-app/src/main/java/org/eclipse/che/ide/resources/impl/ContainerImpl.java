/*******************************************************************************
 * Copyright (c) 2012-2016 Codenvy, S.A.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *   Codenvy, S.A. - initial API and implementation
 *******************************************************************************/
package org.eclipse.che.ide.resources.impl;

import com.google.common.annotations.Beta;
import com.google.common.base.Optional;

import org.eclipse.che.api.core.model.project.SourceStorage;
import org.eclipse.che.api.promises.client.Function;
import org.eclipse.che.api.promises.client.FunctionException;
import org.eclipse.che.api.promises.client.Promise;
import org.eclipse.che.api.promises.client.PromiseProvider;
import org.eclipse.che.ide.api.resources.Container;
import org.eclipse.che.ide.api.resources.File;
import org.eclipse.che.ide.api.resources.Folder;
import org.eclipse.che.ide.api.resources.Project;
import org.eclipse.che.ide.api.resources.Resource;
import org.eclipse.che.ide.api.resources.ResourcePathComparator;
import org.eclipse.che.ide.resource.Path;

import java.util.List;
import java.util.Map;
import java.util.Set;

import static com.google.common.base.Preconditions.checkNotNull;
import static java.util.Arrays.sort;

/**
 * Default implementation of the {@code Container}.
 *
 * @author Vlad Zhukovskyi
 * @see ResourceImpl
 * @see Container
 * @since 4.0.0-RC14
 */
@Beta
abstract class ContainerImpl extends ResourceImpl implements Container {

    protected PromiseProvider promiseProvider;

    protected ContainerImpl(Path path, ResourceManager resourceManager, PromiseProvider promiseProvider) {
        super(path, resourceManager);

        this.promiseProvider = checkNotNull(promiseProvider);
    }

    /** {@inheritDoc} */
    @Override
    public Promise<Optional<File>> getFile(final Path relativePath) {
        return resourceManager.getFile(getLocation().append(relativePath));
    }

    /** {@inheritDoc} */
    @Override
    public Promise<Optional<Container>> getContainer(Path relativePath) {
        return resourceManager.getContainer(getLocation().append(relativePath));
    }

    /** {@inheritDoc} */
    @Override
    public Promise<Resource[]> getChildren(final boolean forceUpdate) {
        return resourceManager.childrenOf(this, forceUpdate).thenPromise(new Function<Set<Resource>, Promise<Resource[]>>() {
            /** {@inheritDoc} */
            @Override
            public Promise<Resource[]> apply(Set<Resource> children) throws FunctionException {
                if (children.isEmpty() && !forceUpdate) {
                    return getChildren(true);
                }

                Resource[] resources = children.toArray(new Resource[children.size()]);
                sort(resources, ResourcePathComparator.getInstance());
                return promiseProvider.resolve(resources);
            }
        });
    }

    /** {@inheritDoc} */
    @Override
    public Promise<Resource[]> getChildren() {
        return getChildren(false);
    }

    /** {@inheritDoc} */
    @Override
    public Project.ImportRequest importProject() {
        return new Project.ImportRequest() {
            private String name;
            private SourceStorage sourceStorage;

            /** {@inheritDoc} */
            @Override
            public String getName() {
                return name;
            }

            /** {@inheritDoc} */
            @Override
            public void setName(String name) {
                this.name = name;
            }

            /** {@inheritDoc} */
            @Override
            public SourceStorage getSourceStorage() {
                return sourceStorage;
            }

            /** {@inheritDoc} */
            @Override
            public void setSourceStorage(SourceStorage sourceStorage) {
                this.sourceStorage = sourceStorage;
            }

            /** {@inheritDoc} */
            @Override
            public Promise<Project> send() {
                return resourceManager.importProject(this);
            }
        };
    }

    /** {@inheritDoc} */
    @Override
    public Project.CreateRequest newProject() {
        return new Project.CreateRequest() {
            private String name;
            private String description;
            private String type;
            private List<String> mixins;
            private Map<String, List<String>> attributes;

            /** {@inheritDoc} */
            @Override
            public String getName() {
                return name;
            }

            /** {@inheritDoc} */
            @Override
            public void setName(String name) {
                this.name = name;
            }

            /** {@inheritDoc} */
            @Override
            public String getDescription() {
                return description;
            }

            /** {@inheritDoc} */
            @Override
            public void setDescription(String description) {
                this.description = description;
            }

            /** {@inheritDoc} */
            @Override
            public String getType() {
                return type;
            }

            /** {@inheritDoc} */
            @Override
            public void setType(String type) {
                this.type = type;
            }

            /** {@inheritDoc} */
            @Override
            public List<String> getMixins() {
                return mixins;
            }

            /** {@inheritDoc} */
            @Override
            public void setMixins(List<String> mixins) {
                this.mixins = mixins;
            }

            /** {@inheritDoc} */
            @Override
            public Map<String, List<String>> getAttributes() {
                return attributes;
            }

            /** {@inheritDoc} */
            @Override
            public void setAttributes(Map<String, List<String>> attributes) {
                this.attributes = attributes;
            }

            /** {@inheritDoc} */
            @Override
            public Promise<Project> send() {
                return resourceManager.createProject(this);
            }
        };
    }

    /** {@inheritDoc} */
    @Override
    public Promise<Folder> newFolder(String name) {
        return resourceManager.createFolder(this, name);
    }

    /** {@inheritDoc} */
    @Override
    public Promise<File> newFile(String name, String content) {
        return resourceManager.createFile(this, name, content);
    }

    /** {@inheritDoc} */
    @Override
    public Promise<Resource[]> synchronize() {
        return resourceManager.synchronize(this).then(new Function<Set<Resource>, Resource[]>() {
            /** {@inheritDoc} */
            @Override
            public Resource[] apply(Set<Resource> affectedResources) throws FunctionException {
                return affectedResources.toArray(new Resource[affectedResources.size()]);
            }
        });
    }

    /** {@inheritDoc} */
    @Override
    public Promise<Resource[]> search(String fileMask, String contentMask) {
        return resourceManager.search(this, fileMask, contentMask).then(new Function<Set<Resource>, Resource[]>() {
            @Override
            public Resource[] apply(Set<Resource> foundResult) throws FunctionException {
                return foundResult.toArray(new Resource[foundResult.size()]);
            }
        });
    }
}
