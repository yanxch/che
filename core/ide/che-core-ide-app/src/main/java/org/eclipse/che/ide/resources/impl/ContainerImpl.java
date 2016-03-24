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

import org.eclipse.che.api.core.model.project.ProjectConfig;
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
    public Project.ProjectRequest importProject() {
        return new Project.ProjectRequest() {
            private ProjectConfig config;

            /** {@inheritDoc} */
            @Override
            public Request<Project, ProjectConfig> withBody(ProjectConfig object) {
                this.config = object;
                return this;
            }

            /** {@inheritDoc} */
            @Override
            public ProjectConfig getBody() {
                return config;
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
    public Project.ProjectRequest newProject() {
        return new Project.ProjectRequest() {
            private ProjectConfig config;

            /** {@inheritDoc} */
            @Override
            public Request<Project, ProjectConfig> withBody(ProjectConfig object) {
                this.config = object;
                return this;
            }

            /** {@inheritDoc} */
            @Override
            public ProjectConfig getBody() {
                return config;
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
