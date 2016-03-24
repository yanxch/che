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
import com.google.common.base.Joiner;
import com.google.common.base.Optional;
import com.google.common.base.Predicate;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Sets;
import com.google.inject.Inject;
import com.google.inject.assistedinject.Assisted;
import com.google.inject.name.Named;
import com.google.web.bindery.event.shared.EventBus;

import org.eclipse.che.api.core.model.project.ProjectConfig;
import org.eclipse.che.api.core.model.project.SourceStorage;
import org.eclipse.che.api.core.rest.shared.dto.Link;
import org.eclipse.che.api.project.gwt.client.ProjectServiceClient;
import org.eclipse.che.api.project.gwt.client.QueryExpression;
import org.eclipse.che.api.project.shared.dto.ItemReference;
import org.eclipse.che.api.project.shared.dto.SourceEstimation;
import org.eclipse.che.api.project.shared.dto.TreeElement;
import org.eclipse.che.api.promises.client.Function;
import org.eclipse.che.api.promises.client.FunctionException;
import org.eclipse.che.api.promises.client.Operation;
import org.eclipse.che.api.promises.client.OperationException;
import org.eclipse.che.api.promises.client.Promise;
import org.eclipse.che.api.promises.client.PromiseProvider;
import org.eclipse.che.api.workspace.shared.dto.ProjectConfigDto;
import org.eclipse.che.api.workspace.shared.dto.ProjectProblemDto;
import org.eclipse.che.api.workspace.shared.dto.SourceStorageDto;
import org.eclipse.che.ide.api.project.type.ProjectTypeRegistry;
import org.eclipse.che.ide.api.resources.Container;
import org.eclipse.che.ide.api.resources.File;
import org.eclipse.che.ide.api.resources.Folder;
import org.eclipse.che.ide.api.resources.Project;
import org.eclipse.che.ide.api.resources.Project.ProblemProjectMarker;
import org.eclipse.che.ide.api.resources.Project.ProjectRequest;
import org.eclipse.che.ide.api.resources.Resource;
import org.eclipse.che.ide.api.resources.ResourceChangedEvent;
import org.eclipse.che.ide.api.resources.marker.Marker;
import org.eclipse.che.ide.api.resources.marker.event.MarkerCreatedEvent;
import org.eclipse.che.ide.api.resources.marker.event.MarkerDeletedEvent;
import org.eclipse.che.ide.api.workspace.Workspace;
import org.eclipse.che.ide.api.workspace.WorkspaceConfigurationChangedEvent;
import org.eclipse.che.ide.dto.DtoFactory;
import org.eclipse.che.ide.resource.Path;
import org.eclipse.che.ide.ui.loaders.request.LoaderFactory;
import org.eclipse.che.ide.ui.loaders.request.MessageLoader;
import org.eclipse.che.ide.workspace.WorkspaceComponent;
import org.eclipse.che.ide.workspace.WorkspaceImpl;

import javax.annotation.Nullable;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Preconditions.checkState;
import static com.google.common.base.Strings.isNullOrEmpty;
import static com.google.common.collect.Iterables.removeIf;
import static com.google.common.collect.Iterables.transform;
import static com.google.common.collect.Iterables.tryFind;
import static com.google.common.collect.Maps.uniqueIndex;
import static com.google.common.collect.Sets.filter;
import static com.google.common.collect.Sets.newHashSet;
import static java.util.Collections.unmodifiableSet;
import static org.eclipse.che.ide.api.resources.Resource.FILE;
import static org.eclipse.che.ide.api.resources.Resource.PROJECT;
import static org.eclipse.che.ide.api.resources.ResourceDelta.CHANGED;
import static org.eclipse.che.ide.api.resources.ResourceDelta.CONTENT;
import static org.eclipse.che.ide.api.resources.ResourceDelta.CREATED;
import static org.eclipse.che.ide.api.resources.ResourceDelta.LOADED_INTO_CACHE;
import static org.eclipse.che.ide.api.resources.ResourceDelta.MOVED_FROM;
import static org.eclipse.che.ide.api.resources.ResourceDelta.MOVED_TO;
import static org.eclipse.che.ide.api.resources.ResourceDelta.REMOVED;
import static org.eclipse.che.ide.util.NameUtils.checkFileName;
import static org.eclipse.che.ide.util.NameUtils.checkFolderName;
import static org.eclipse.che.ide.util.NameUtils.checkProjectName;

/**
 * Acts as the service lay between the user interactions with resources and data transfer layer.
 * Main necessity of this manager is to encapsulate business logic which serves resources from
 * the interfaces.
 * <p/>
 * Each instance of {@link ResourceManager} is bound to own workspace id. So, when {@link WorkspaceComponent}
 * starts, it handles workspace configuration and sends {@link WorkspaceConfigurationChangedEvent}
 * event which implementation of {@link Workspace} handles. Based on this configuration it creates
 * new instance of {@link ResourceManager} and requests project lists from the server by calling
 * {@link #getWorkspaceProjects()} and storing received project lists in workspace context.
 * <p/>
 * This manager is not intended to be operated with third-party components. Only resources can
 * operate with it. To operate with resources use {@link Workspace#getWorkspaceRoot()} and
 * {@link Workspace#getProjects()}.
 *
 * @author Vlad Zhukovskiy
 * @see Workspace
 * @see WorkspaceImpl
 * @since 4.0.0-RC14
 */
@Beta
public final class ResourceManager {
    /**
     * Describes zero depth level for the descendants.
     */
    private static final int DEPTH_ZERO = 0;

    /**
     * Describes first depth level for the descendants.
     */
    private static final int DEPTH_ONE = 1;

    /**
     * Relative link for the content url.
     *
     * @see #newResourceFrom(ItemReference)
     */
    private static final String GET_CONTENT_REL = "get content";

    /**
     * Empty projects container.
     *
     * @see #getWorkspaceProjects()
     */
    private static final Project[] NO_PROJECTS = new Project[0];

    private final ProjectServiceClient ps;
    private final EventBus             eventBus;
    private final ResourceFactory      resourceFactory;
    private final PromiseProvider      promise;
    private final DtoFactory           dtoFactory;
    private final ProjectTypeRegistry  typeRegistry;
    private final String               wsAgentPath;
    private final LoaderFactory        loaderFactory;
    private final String               wsId;

    /**
     * Link to the workspace content root. Immutable among the workspace life.
     */
    private final Container workspaceRoot;

    /**
     * Internal store, which caches requested resources from the server.
     */
    private ResourceStore resourceStore;

    /**
     * Cached dto project configuration.
     */
    private Set<ProjectConfigDto> cachedDtoConfigs = newHashSet();

    @Inject
    public ResourceManager(@Assisted String wsId,
                           ProjectServiceClient ps,
                           EventBus eventBus,
                           ResourceFactory resourceFactory,
                           PromiseProvider promise,
                           DtoFactory dtoFactory,
                           ProjectTypeRegistry typeRegistry,
                           @Named("cheExtensionPath") String wsAgentPath,
                           LoaderFactory loaderFactory) {
        this.wsId = wsId;
        this.ps = ps;
        this.eventBus = eventBus;
        this.resourceFactory = resourceFactory;
        this.promise = promise;
        this.dtoFactory = dtoFactory;
        this.typeRegistry = typeRegistry;
        this.wsAgentPath = wsAgentPath;
        this.loaderFactory = loaderFactory;
        this.resourceStore = new ResourceStore();

        this.workspaceRoot = resourceFactory.newFolderImpl(Path.ROOT, this);
    }

    /**
     * Returns the workspace registered projects.
     *
     * @return the {@link Promise} with registered projects
     * @see Project
     * @since 4.0.0-RC14
     */
    public Promise<Project[]> getWorkspaceProjects() {
        return ps.getProjects(wsId).then(new Function<List<ProjectConfigDto>, Project[]>() {
            @Override
            public Project[] apply(List<ProjectConfigDto> dtoConfigs) throws FunctionException {
                cachedDtoConfigs.clear();
                resourceStore.truncate();

                if (dtoConfigs.isEmpty()) {
                    return NO_PROJECTS;
                }

                cachedDtoConfigs.addAll(dtoConfigs);

                Set<ProjectConfigDto> rootProjectDtos = filter(cachedDtoConfigs, new Predicate<ProjectConfigDto>() {
                    @Override
                    public boolean apply(@Nullable ProjectConfigDto input) {
                        checkNotNull(input, "Null project configuration occurred");

                        return Path.valueOf(input.getPath()).segmentCount() == 1;
                    }
                });

                final Set<Project> projects = newHashSet();

                for (ProjectConfigDto dto : rootProjectDtos) {
                    final ProjectImpl newProject = resourceFactory.newProjectImpl(dto, ResourceManager.this);
                    resourceStore.init(newProject);
                    projects.add(newProject);

                    eventBus.fireEvent(new ResourceChangedEvent(new ResourceDeltaImpl(newProject, LOADED_INTO_CACHE)));
                }

                return projects.toArray(new Project[projects.size()]);
            }
        });
    }

    /**
     * Returns the workspace root container. This container is a holder which may contains only {@link Project}s.
     *
     * @return the workspace container
     * @see Container
     * @since 4.0.0-RC14
     */
    public Container getWorkspaceRoot() {
        return workspaceRoot;
    }

    /**
     * Update state of specific properties in project and save this state on the server.
     * As the result method should return the {@link Promise} with new {@link Project} object.
     * <p/>
     * During the update method have to iterate on children of updated resource and if any of
     * them has changed own type, e.g. folder -> project, project -> folder, specific event
     * has to be fired.
     * <p/>
     * Method is not intended to be called in third party components. It is the service method
     * for {@link Project}.
     *
     * @param path
     *         the path to project which should be updated
     * @param request
     *         the update request
     * @return the {@link Promise} with new {@link Project} object.
     * @see ResourceChangedEvent
     * @see ProjectRequest
     * @see Project#update()
     * @since 4.0.0-RC14
     */
    protected Promise<Project> update(final Path path, final ProjectRequest request) {

        final ProjectConfigDto dto = dtoFactory.createDto(ProjectConfigDto.class)
                                               .withPath(path.toString())
                                               .withDescription(request.getBody().getDescription())
                                               .withType(request.getBody().getType())
                                               .withMixins(request.getBody().getMixins())
                                               .withAttributes(request.getBody().getAttributes());

        return ps.updateProject(wsId, dto).thenPromise(new Function<ProjectConfigDto, Promise<Project>>() {
            @Override
            public Promise<Project> apply(ProjectConfigDto reference) throws FunctionException {

                /* Note: After update, project may become to be other type,
                   e.g. blank -> java or maven, or ant, or etc. And this may
                   cause sub-project creations. Simultaneously on the client
                   side there is outdated information about sub-projects, so
                   we need to get updated project list. */

                final int readDepth[] = new int[1];

                resourceStore.traverse(new ResourceVisitor() {
                    @Override
                    public void visit(Resource resource) {
                        final Path seekPath = resource.getLocation();

                        if (path.isPrefixOf(seekPath) && seekPath.segmentCount() > readDepth[0]) {
                            readDepth[0] = seekPath.segmentCount();
                        }
                    }
                });

                //dispose outdated resource
                final Optional<Resource> optOutdatedResource = resourceStore.get(path);
                checkState(optOutdatedResource.isPresent(), "Resource '" + path + "' doesn't exists");
                resourceStore.dispose(optOutdatedResource.get(), false);

                //register new one
                final ProjectImpl updatedResource = resourceFactory.newProjectImpl(reference, ResourceManager.this);
                resourceStore.init(updatedResource);

                //fetch updated configuration from the server
                return ps.getProjects(wsId).then(new Function<List<ProjectConfigDto>, Project>() {
                    @Override
                    public Project apply(List<ProjectConfigDto> updatedConfiguration) throws FunctionException {

                        //store
                        cachedDtoConfigs.clear();
                        cachedDtoConfigs.addAll(updatedConfiguration);

                        getRemoteResources(updatedResource, readDepth[0], true, false).then(new Operation<Set<Resource>>() {
                            @Override
                            public void apply(Set<Resource> ignored) throws OperationException {
                                eventBus.fireEvent(new ResourceChangedEvent(new ResourceDeltaImpl(updatedResource, CHANGED)));
                            }
                        });

                        return updatedResource;
                    }
                });
            }
        });
    }

    protected Promise<Folder> createFolder(final Container parent, final String name) {
        checkArgument(checkFolderName(name), "Invalid folder name");

        return findResource(parent.getLocation().append(name), true).thenPromise(new Function<Optional<Resource>, Promise<Folder>>() {
            @Override
            public Promise<Folder> apply(Optional<Resource> resource) throws FunctionException {
                checkState(!resource.isPresent(), "Resource already exists");
                checkArgument(!parent.getLocation().isRoot(), "Failed to create folder in workspace root");

                return ps.createFolder(wsId, parent.getLocation().append(name)).then(new Function<ItemReference, Folder>() {
                    @Override
                    public Folder apply(ItemReference reference) throws FunctionException {
                        final FolderImpl newResource =
                                resourceFactory.newFolderImpl(Path.valueOf(reference.getPath()), ResourceManager.this);
                        resourceStore.init(newResource);

                        eventBus.fireEvent(new ResourceChangedEvent(new ResourceDeltaImpl(newResource, CREATED)));

                        return newResource;
                    }
                });
            }
        });
    }

    protected Promise<File> createFile(final Container parent, final String name, final String content) {
        checkArgument(checkFileName(name), "Invalid file name");

        return findResource(parent.getLocation().append(name), true).thenPromise(new Function<Optional<Resource>, Promise<File>>() {
            @Override
            public Promise<File> apply(Optional<Resource> resource) throws FunctionException {
                checkState(!resource.isPresent(), "Resource already exists");
                checkArgument(!parent.getLocation().isRoot(), "Failed to create file in workspace root");

                return ps.createFile(wsId, parent.getLocation().append(name), content).then(new Function<ItemReference, File>() {
                    @Override
                    public File apply(ItemReference reference) throws FunctionException {
                        final Link contentUrl = reference.getLink("get content");
                        final FileImpl newResource = resourceFactory.newFileImpl(Path.valueOf(reference.getPath()),
                                                                                 contentUrl.getHref(),
                                                                                 ResourceManager.this);
                        resourceStore.init(newResource);

                        eventBus.fireEvent(new ResourceChangedEvent(new ResourceDeltaImpl(newResource, CREATED)));

                        return newResource;
                    }
                });
            }
        });
    }

    protected Promise<Project> createProject(final Project.ProjectRequest createRequest) {
        final Path path = Path.valueOf(createRequest.getBody().getName()).makeAbsolute();

        return findResource(path, true).thenPromise(new Function<Optional<Resource>, Promise<Project>>() {
            @Override
            public Promise<Project> apply(Optional<Resource> resource) throws FunctionException {
                checkState(!resource.isPresent(), "Resource already exists");

                checkArgument(checkProjectName(createRequest.getBody().getName()), "Invalid project name");
                checkArgument(typeRegistry.getProjectType(createRequest.getBody().getType()) != null, "Invalid project type");

                final ProjectConfigDto dto = dtoFactory.createDto(ProjectConfigDto.class)
                                                       .withPath(path.toString())
                                                       .withDescription(createRequest.getBody().getDescription())
                                                       .withType(createRequest.getBody().getType())
                                                       .withMixins(createRequest.getBody().getMixins())
                                                       .withAttributes(createRequest.getBody().getAttributes());

                return ps.createProject(wsId, dto).then(new Function<ProjectConfigDto, Project>() {
                    @Override
                    public Project apply(ProjectConfigDto config) throws FunctionException {
                        final ProjectImpl newResource = resourceFactory.newProjectImpl(config, ResourceManager.this);
                        resourceStore.init(newResource);

                        eventBus.fireEvent(new ResourceChangedEvent(new ResourceDeltaImpl(newResource, CREATED)));

                        return newResource;
                    }
                });
            }
        });
    }

    protected Promise<Project> importProject(final Project.ProjectRequest importRequest) {
        final Path path = Path.valueOf(importRequest.getBody().getPath());

        return findResource(path, true).thenPromise(new Function<Optional<Resource>, Promise<Project>>() {
            @Override
            public Promise<Project> apply(Optional<Resource> resource) throws FunctionException {
                checkState(!resource.isPresent(), "Resource already exists");

                checkArgument(checkProjectName(importRequest.getBody().getName()), "Invalid project name");
                checkNotNull(importRequest.getBody().getSource(), "Null source configuration occurred");

                final SourceStorage sourceStorage = importRequest.getBody().getSource();
                final SourceStorageDto sourceStorageDto = dtoFactory.createDto(SourceStorageDto.class)
                                                                    .withType(sourceStorage.getType())
                                                                    .withLocation(sourceStorage.getLocation())
                                                                    .withParameters(sourceStorage.getParameters());

                return ps.importProject(wsId, path.lastSegment(), false, sourceStorageDto)
                         .thenPromise(new Function<Void, Promise<Project>>() {
                             @Override
                             public Promise<Project> apply(Void ignored) throws FunctionException {
                                 return ps.getProject(wsId, path).then(new Function<ProjectConfigDto, Project>() {
                                     @Override
                                     public Project apply(ProjectConfigDto config) throws FunctionException {
                                         final ProjectImpl newResource = resourceFactory.newProjectImpl(config, ResourceManager.this);
                                         resourceStore.init(newResource);

                                         eventBus.fireEvent(new ResourceChangedEvent(new ResourceDeltaImpl(newResource, CREATED)));

                                         return newResource;
                                     }
                                 });
                             }
                         });
            }
        });
    }

    protected Promise<Resource> move(final Resource source, final Path destination, final boolean force) {
        checkArgument(!source.getLocation().isRoot(), "Workspace root is not allowed to be moved");

        return findResource(destination, true).thenPromise(new Function<Optional<Resource>, Promise<Resource>>() {
            @Override
            public Promise<Resource> apply(Optional<Resource> resource) throws FunctionException {
                checkState(!resource.isPresent(), "Resource already exists");

                return ps.move(wsId, source.getLocation(), destination.parent(), destination.lastSegment(), force)
                         .thenPromise(new Function<Void, Promise<Resource>>() {
                             @Override
                             public Promise<Resource> apply(Void ignored) throws FunctionException {
                                 return ps.getItem(wsId, destination).thenPromise(new Function<ItemReference, Promise<Resource>>() {
                                     @Override
                                     public Promise<Resource> apply(ItemReference reference) throws FunctionException {

                                         final Resource movedResource = newResourceFrom(reference);
                                         resourceStore.init(movedResource);

                                         if (source instanceof Container) {
                                             final int readDepth[] = new int[1];

                                             resourceStore.traverse(new ResourceVisitor() {
                                                 final Path originPath = source.getLocation();

                                                 @Override
                                                 public void visit(Resource resource) {
                                                     final Path seekPath = resource.getLocation();

                                                     if (originPath.isPrefixOf(seekPath) && seekPath.segmentCount() > readDepth[0]) {
                                                         readDepth[0] = seekPath.segmentCount();
                                                     }
                                                 }
                                             });

                                             resourceStore.dispose(source, true);

                                             return getRemoteResources((Container)movedResource, readDepth[0], true, false)
                                                     .then(new Function<Set<Resource>, Resource>() {
                                                         @Override
                                                         public Resource apply(Set<Resource> ignored) throws FunctionException {
                                                             eventBus.fireEvent(new ResourceChangedEvent(
                                                                     new ResourceDeltaImpl(movedResource, source,
                                                                                           CREATED | MOVED_FROM | MOVED_TO)));

                                                             return movedResource;
                                                         }
                                                     });
                                         } else {
                                             resourceStore.dispose(source, false);
                                             eventBus.fireEvent(new ResourceChangedEvent(
                                                     new ResourceDeltaImpl(movedResource, source, CREATED | MOVED_FROM | MOVED_TO)));
                                         }

                                         return promise.resolve(movedResource);
                                     }
                                 });
                             }
                         });
            }
        });
    }

    protected Promise<Resource> copy(final Resource source, final Path destination, final boolean force) {
        checkArgument(!source.getLocation().isRoot(), "Workspace root is not allowed to be copied");

        return findResource(destination, true).thenPromise(new Function<Optional<Resource>, Promise<Resource>>() {
            @Override
            public Promise<Resource> apply(Optional<Resource> resource) throws FunctionException {
                checkState(!resource.isPresent(), "Resource already exists");

                return ps.copy(wsId, source.getLocation(), destination.parent(), destination.lastSegment(), force)
                         .thenPromise(new Function<Void, Promise<Resource>>() {
                             @Override
                             public Promise<Resource> apply(Void ignored) throws FunctionException {

                                 return ps.getItem(wsId, destination).then(new Function<ItemReference, Resource>() {
                                     @Override
                                     public Resource apply(ItemReference reference) throws FunctionException {
                                         final Resource copiedResource = newResourceFrom(reference);

                                         resourceStore.init(copiedResource);
                                         eventBus.fireEvent(new ResourceChangedEvent(new ResourceDeltaImpl(source, CREATED)));

                                         return copiedResource;
                                     }
                                 });
                             }
                         });
            }
        });
    }

    protected Promise<Void> delete(final Resource resource) {
        checkArgument(!resource.getLocation().isRoot(), "Workspace root is not allowed to be moved");

        return ps.delete(wsId, resource.getLocation()).then(new Function<Void, Void>() {
            @Override
            public Void apply(Void ignored) throws FunctionException {
                resourceStore.dispose(resource, true);
                eventBus.fireEvent(new ResourceChangedEvent(new ResourceDeltaImpl(resource, REMOVED)));

                return null;
            }
        });
    }

    protected Promise<Void> write(final File file, String content) {
        checkArgument(content != null, "Null content occurred");

        return ps.writeFile(wsId, file.getLocation(), content).then(new Function<Void, Void>() {
            @Override
            public Void apply(Void ignored) throws FunctionException {
                eventBus.fireEvent(new ResourceChangedEvent(new ResourceDeltaImpl(file, CHANGED | CONTENT)));

                return null;
            }
        });
    }

    protected Promise<String> read(File file) {
        return ps.readFile(wsId, file.getLocation());
    }

    protected Promise<Set<Resource>> getRemoteResources(final Container container, int depth, boolean includeFiles, final boolean force) {
        checkArgument(depth > -1, "Invalid depth");

        final Set<Resource> resources = newHashSet();

        if (depth == DEPTH_ZERO) {
            return promise.resolve(unmodifiableSet(resources));
        }

        return ps.getTree(wsId, container.getLocation(), depth, includeFiles).then(new Function<TreeElement, Set<Resource>>() {
            @Override
            public Set<Resource> apply(TreeElement tree) throws FunctionException {

                final MessageLoader progressLoader = loaderFactory.newLoader("Traversing the project...");
                progressLoader.show();

                traverse(tree, new ResourceVisitor() {
                    @Override
                    public void visit(Resource resource) {
                        progressLoader.setMessage("Processing " + resource.getName() + "...");
                        final Optional<Resource> optionalCachedResource = resourceStore.get(resource.getLocation());
                        final boolean isPresent = optionalCachedResource.isPresent();

                        if (isPresent) {
                            resourceStore.dispose(optionalCachedResource.get(), force);
                        }

                        progressLoader.setMessage("Caching " + resource.getLocation() + "...");
                        resourceStore.init(resource);

                        if (resource.getResourceType() == PROJECT) {
                            final Optional<ProjectConfigDto> optionalConfig = findProjectConfigDto(resource.getLocation());

                            if (optionalConfig.isPresent()) {
                                final Optional<ProblemProjectMarker> optionalMarker = getProblemMarker(optionalConfig.get());

                                if (optionalMarker.isPresent()) {
                                    resource.addMarker(optionalMarker.get());
                                    progressLoader.setMessage("Marking " + resource.getName() + " as problematic project...");
                                }
                            }
                        }

                        int status = isPresent ? CHANGED : LOADED_INTO_CACHE;

                        eventBus.fireEvent(new ResourceChangedEvent(new ResourceDeltaImpl(resource, status)));

                        progressLoader.setMessage("Processing " + resource.getName() + " finished...");
                        resources.add(resource);
                    }
                });

                progressLoader.hide();

                return resources;
            }
        });
    }

    protected Promise<Optional<Container>> getContainer(final Path absolutePath) {
        return findResource(absolutePath, false).thenPromise(new Function<Optional<Resource>, Promise<Optional<Container>>>() {
            @Override
            public Promise<Optional<Container>> apply(Optional<Resource> optional) throws FunctionException {
                if (optional.isPresent()) {
                    final Resource resource = optional.get();
                    checkState(resource instanceof Container, "Not a container");

                    return promise.resolve(Optional.of((Container)resource));
                }

                return promise.resolve(Optional.<Container>absent());
            }
        });
    }

    protected Promise<Optional<File>> getFile(final Path absolutePath) {
        return findResource(absolutePath, false).thenPromise(new Function<Optional<Resource>, Promise<Optional<File>>>() {
            @Override
            public Promise<Optional<File>> apply(Optional<Resource> optional) throws FunctionException {
                if (optional.isPresent()) {
                    final Resource resource = optional.get();
                    checkState(resource.getResourceType() == FILE, "Not a file");

                    return promise.resolve(Optional.of((File)resource));
                }

                return promise.resolve(Optional.<File>absent());
            }
        });
    }

    protected Optional<Container> parentOf(Resource resource) {
        final Path parentLocation = resource.getLocation().parent();
        final Optional<Resource> optionalParent = resourceStore.get(parentLocation);

        if (!optionalParent.isPresent()) {
            return Optional.absent();
        }

        final Resource parentResource = optionalParent.get();

        if (parentResource instanceof Container) {
            return Optional.of((Container)parentResource);
        }

        throw new IllegalStateException("Failed to locate parent for the '" + resource.getLocation() + "'");
    }

    protected Promise<Set<Resource>> childrenOf(final Container container, boolean forceUpdate) {
        if (forceUpdate) {
            return getRemoteResources(container, DEPTH_ONE, true, true).then(new Function<Set<Resource>, Set<Resource>>() {
                @Override
                public Set<Resource> apply(Set<Resource> resources) throws FunctionException {
                    return resources;
                }
            });
        }

        final Set<Resource> resources = newHashSet();

        resourceStore.traverse(new ResourceVisitor() {
            @Override
            public void visit(Resource resource) {
                final Optional<Container> optionalParent = resource.getParent();
                if (optionalParent.isPresent() && optionalParent.get().equals(container)) {
                    resources.add(resource);
                }
            }
        });

        return promise.resolve(unmodifiableSet(resources));
    }

    protected Promise<Optional<Resource>> findResource(final Path absolutePath, boolean quiet) {

        //search resource in local cache
        final Optional<Resource> optionalCachedResource = resourceStore.get(absolutePath);
        if (optionalCachedResource.isPresent()) {
            return promise.resolve(optionalCachedResource);
        }

        //request from server
        final Path projectPath = Path.valueOf(absolutePath.segment(0)).makeAbsolute();
        final Optional<Resource> optionalResource = resourceStore.get(projectPath);
        final boolean isPresent = optionalResource.isPresent();

        checkState(isPresent || quiet, "Resource with path '" + projectPath + "' doesn't exists");

        if (!isPresent) {
            return promise.resolve(Optional.<Resource>absent());
        }

        final Resource resource = optionalResource.get();
        checkState(resource.getResourceType() == PROJECT, "Resource with path '" + projectPath + "' isn't a project");

        final int seekDepth = absolutePath.segmentCount() - 1;

        return getRemoteResources((Container)resource, seekDepth, true, false).then(new Function<Set<Resource>, Optional<Resource>>() {
            @Override
            public Optional<Resource> apply(Set<Resource> resources) throws FunctionException {
                return tryFind(resources, new Predicate<Resource>() {
                    @Override
                    public boolean apply(@Nullable Resource input) {
                        checkNotNull(input, "Null resource passed");

                        return input.getLocation().equals(absolutePath);
                    }
                });
            }
        });
    }

    protected void traverse(TreeElement tree, ResourceVisitor visitor) {
        for (final TreeElement element : tree.getChildren()) {

            final Resource resource = newResourceFrom(element.getNode());
            visitor.visit(resource);

            if (resource instanceof Container) {
                traverse(element, visitor);
            }
        }
    }

    protected Resource newResourceFrom(final ItemReference reference) {
        switch (reference.getType()) {
            case "file":
                final Link link = reference.getLink(GET_CONTENT_REL);

                return resourceFactory.newFileImpl(Path.valueOf(reference.getPath()), link.getHref(), this);
            case "folder":
                return resourceFactory.newFolderImpl(Path.valueOf(reference.getPath()), this);
            case "project":

                Optional<ProjectConfigDto> optionalConfiguration = findProjectConfigDto(Path.valueOf(reference.getPath()));

                if (optionalConfiguration.isPresent()) {
                    final ProjectConfigDto configDto = optionalConfiguration.get();

                    return resourceFactory.newProjectImpl(configDto, this);
                } else {
                    return resourceFactory.newFolderImpl(Path.valueOf(reference.getPath()), this);
                }
            default:
                throw new IllegalArgumentException("Failed to recognize resource type to create.");
        }
    }

    private Optional<ProjectConfigDto> findProjectConfigDto(final Path path) {
        return tryFind(cachedDtoConfigs, new Predicate<ProjectConfigDto>() {
            @Override
            public boolean apply(@Nullable ProjectConfigDto input) {
                checkNotNull(input, "Null project configuration occurred");

                return Path.valueOf(input.getPath()).equals(path);
            }
        });
    }

    private Optional<ProblemProjectMarker> getProblemMarker(ProjectConfigDto projectConfigDto) {
        List<ProjectProblemDto> problems = projectConfigDto.getProblems();
        if (problems == null || problems.isEmpty()) {
            return Optional.absent();
        }

        final String warnings = Joiner.on('\n').join(transform(problems, new com.google.common.base.Function<ProjectProblemDto, String>() {
            @Nullable
            @Override
            public String apply(@Nullable ProjectProblemDto input) {
                checkNotNull(input);

                return input.getMessage();
            }
        }));

        final ProblemProjectMarker problemMarker = new ProblemProjectMarker();
        problemMarker.setAttribute(Marker.SEVERITY, Marker.SEVERITY_WARNING);
        problemMarker.setAttribute(Marker.MESSAGE, warnings);

        return Optional.of(problemMarker);
    }

    protected Promise<Set<Resource>> synchronize(final Container container) {
        final Path path = container.getLocation();
        final int maxDepth[] = new int[1];

        resourceStore.traverse(new ResourceVisitor() {
            @Override
            public void visit(Resource resource) {
                final Path seekPath = resource.getLocation();

                if (path.isPrefixOf(seekPath) && seekPath.segmentCount() > maxDepth[0]) {
                    maxDepth[0] = seekPath.segmentCount();
                }
            }
        });

        return ps.getProjects(wsId).thenPromise(new Function<List<ProjectConfigDto>, Promise<Set<Resource>>>() {
            @Override
            public Promise<Set<Resource>> apply(List<ProjectConfigDto> updatedConfiguration) throws FunctionException {
                cachedDtoConfigs.clear();
                cachedDtoConfigs.addAll(updatedConfiguration);

                return getRemoteResources(container, maxDepth[0], true, false);
            }
        });
    }

    protected Promise<Set<Resource>> search(final Container container, String fileMask, String contentMask) {
        QueryExpression queryExpression = new QueryExpression();
        queryExpression.setText(contentMask + '*');
        if (!isNullOrEmpty(fileMask)) {
            queryExpression.setName(fileMask);
        }
        if (!container.getLocation().isRoot()) {
            queryExpression.setPath(container.getLocation().toString());
        }

        return ps.search(wsId, queryExpression).thenPromise(new Function<List<ItemReference>, Promise<Set<Resource>>>() {
            @Override
            public Promise<Set<Resource>> apply(final List<ItemReference> foundReference) throws FunctionException {
                if (foundReference.isEmpty()) {
                    return promise.resolve(Collections.<Resource>emptySet());
                }

                final int containerSegmentCount = container.getLocation().segmentCount();
                final int maxDepth[] = new int[1];

                final ImmutableMap<Path, ItemReference> pathReferenceMap =
                        uniqueIndex(foundReference, new com.google.common.base.Function<ItemReference, Path>() {
                            @Nullable
                            @Override
                            public Path apply(@Nullable ItemReference input) {
                                checkNotNull(input, "Null item reference occurred");

                                final Path path = Path.valueOf(input.getPath());

                                //found the deepest entry
                                int segmentCount = path.segmentCount() - containerSegmentCount;
                                if (maxDepth[0] < segmentCount) {
                                    maxDepth[0] = segmentCount;
                                }

                                return path;
                            }
                        });

                return getRemoteResources(container, maxDepth[0], true, false).then(new Function<Set<Resource>, Set<Resource>>() {
                    @Override
                    public Set<Resource> apply(Set<Resource> readTree) throws FunctionException {
                        return Sets.filter(readTree, new Predicate<Resource>() {
                            @Override
                            public boolean apply(@Nullable Resource input) {
                                checkNotNull(input, "Null resource occurred");

                                return input.getResourceType() == FILE && pathReferenceMap.containsKey(input.getLocation());
                            }
                        });
                    }
                });
            }
        });
    }

    protected Optional<Marker> getMarker(Resource resource, String type) {
        return resourceStore.getMarker(resource, type);
    }

    protected Marker[] getMarkers(Resource resource) {
        return resourceStore.getMarkers(resource);
    }

    protected void addMarker(Resource resource, Marker marker) {
        resourceStore.addMarker(resource, marker);
    }

    protected boolean deleteMarker(Resource resource, String type) {
        return resourceStore.deleteMarker(resource, type);
    }

    protected String getUrl(Resource resource) {
        checkArgument(!resource.getLocation().isRoot(), "Workspace root doesn't have export URL");

        final String baseUrl = wsAgentPath + "/project/" + wsId + "/export";

        if (resource.getResourceType() == FILE) {
            return baseUrl + "/file" + resource.getLocation();
        }

        return baseUrl + resource.getLocation();
    }

    public Promise<List<SourceEstimation>> resolve(Project project) {
        return ps.resolveSources(wsId, project.getLocation());
    }

    protected class ResourceStore {

//        Table<Path, Resource, Set<Marker>> internalCacheV2 = HashBasedTable.create();

        private Map<Resource, Set<Marker>> internalCache = new HashMap<>();

        public void init(Resource resource) {
            checkNotNull(resource, "Null resource occurred");
            checkArgument(!internalCache.containsKey(resource), "Store record for '" + resource.getLocation() + "' already exists");

            //construct markers

//            final Path parent = resource.getLocation().parent();
//            internalCacheV2.put(parent, resource, Collections.<Marker>emptySet());

            internalCache.put(resource, Sets.<Marker>newHashSet());
        }

        public void dispose(Resource resource, boolean disposeChildren) {
            checkNotNull(resource, "Null resource occurred");
            checkArgument(internalCache.containsKey(resource), "Store record for '" + resource.getLocation() + "' not found");

            //dispose the entry from the cache
            boolean disposeSuccess = internalCache.keySet().remove(resource);
            checkState(disposeSuccess, "Failed to dispose record for '" + resource.getLocation() + "'");

            if (resource instanceof Container && disposeChildren) {
                //dispose nested descendants from the cache
                final Path disposedLocation = resource.getLocation();

                removeIf(internalCache.keySet(), new Predicate<Resource>() {
                    @Override
                    public boolean apply(@Nullable Resource input) {
                        checkNotNull(input, "Null resource occurred");

                        final Path locationToCheck = input.getLocation();

                        return disposedLocation.isPrefixOf(locationToCheck);
                    }
                });
            }
        }

        public Optional<Resource> get(final Path path) {
            checkArgument(!path.isEmpty(), "Empty path occurred");

            return tryFind(internalCache.keySet(), new Predicate<Resource>() {
                @Override
                public boolean apply(@Nullable Resource input) {
                    checkNotNull(input, "Null resource occurred");

                    return input.getLocation().equals(path);
                }
            });
        }

        public void traverse(ResourceVisitor visitor) {
            for (Resource resource : internalCache.keySet()) {
                visitor.visit(resource);
            }
        }

        public void truncate() {
            internalCache.clear();
        }

        public Optional<Marker> getMarker(Resource resource, final String type) {
            return tryFind(internalCache.get(resource), new Predicate<Marker>() {
                @Override
                public boolean apply(@Nullable Marker input) {
                    checkNotNull(input, "Null marker occurred");

                    return input.getType().equals(type);
                }
            });
        }

        public Marker[] getMarkers(Resource resource) {
            Set<Marker> markers = internalCache.get(resource);
            return markers.toArray(new Marker[markers.size()]);
        }

        public void addMarker(Resource resource, Marker marker) {
            internalCache.get(resource).add(marker);

            eventBus.fireEvent(new MarkerCreatedEvent(resource, marker));
        }

        public boolean deleteMarker(Resource resource, final String type) {
            final Optional<Marker> optionalMarker = getMarker(resource, type);

            if (!optionalMarker.isPresent()) {
                return false;
            }

            final Marker marker = optionalMarker.get();
            final boolean success = internalCache.get(resource).remove(marker);

            if (success) {
                eventBus.fireEvent(new MarkerDeletedEvent(resource, marker));
            }

            return success;
        }
    }

    protected interface ResourceVisitor {
        void visit(Resource resource);
    }

    public interface ResourceFactory {
        ProjectImpl newProjectImpl(ProjectConfig reference, ResourceManager resourceManager);

        FolderImpl newFolderImpl(Path path, ResourceManager resourceManager);

        FileImpl newFileImpl(Path path, String contentUrl, ResourceManager resourceManager);
    }

    public interface ResourceManagerFactory {
        ResourceManager newResourceManager(String wsId);
    }
}
