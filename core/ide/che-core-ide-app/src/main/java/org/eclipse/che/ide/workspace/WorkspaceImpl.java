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
package org.eclipse.che.ide.workspace;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.google.web.bindery.event.shared.EventBus;

import org.eclipse.che.api.core.model.machine.Command;
import org.eclipse.che.api.core.model.workspace.Environment;
import org.eclipse.che.api.core.model.workspace.WorkspaceConfig;
import org.eclipse.che.api.promises.client.Operation;
import org.eclipse.che.api.promises.client.OperationException;
import org.eclipse.che.ide.api.resources.Container;
import org.eclipse.che.ide.api.resources.Project;
import org.eclipse.che.ide.api.resources.Resource;
import org.eclipse.che.ide.api.resources.ResourceChangedEvent;
import org.eclipse.che.ide.api.resources.ResourceChangedEvent.ResourceChangedHandler;
import org.eclipse.che.ide.api.resources.ResourceDelta;
import org.eclipse.che.ide.api.resources.ResourcePathComparator;
import org.eclipse.che.ide.api.workspace.Workspace;
import org.eclipse.che.ide.api.workspace.WorkspaceConfigurationAppliedEvent;
import org.eclipse.che.ide.api.workspace.WorkspaceConfigurationChangedEvent;
import org.eclipse.che.ide.api.workspace.WorkspaceConfigurationChangedEvent.WorkspaceConfigurationChangedHandler;
import org.eclipse.che.ide.resources.internal.ResourceManager;
import org.eclipse.che.ide.resources.internal.ResourceManager.ResourceManagerFactory;

import java.util.Arrays;
import java.util.List;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Preconditions.checkState;
import static java.util.Arrays.binarySearch;
import static java.util.Arrays.copyOf;
import static java.util.Arrays.sort;
import static org.eclipse.che.ide.api.resources.Resource.PROJECT;
import static org.eclipse.che.ide.api.resources.ResourceDelta.CHANGED;
import static org.eclipse.che.ide.api.resources.ResourceDelta.CREATED;
import static org.eclipse.che.ide.api.resources.ResourceDelta.MOVED_FROM;
import static org.eclipse.che.ide.api.resources.ResourceDelta.REMOVED;

/**
 * Default implementation of {@link Workspace}.
 *
 * @author Vlad Zhukovskiy
 * @see Workspace
 * @since 4.0.0-RC14
 */
@Singleton
public final class WorkspaceImpl implements Workspace, WorkspaceConfigurationChangedHandler, ResourceChangedHandler {

    private final EventBus               eventBus;
    private final ResourceManagerFactory resourceManagerFactory;

    private ResourceManager resourceManager;
    private Project[]       projects;
    private String          wsId;
    private WorkspaceConfig wsConfiguration;

    @Inject
    public WorkspaceImpl(EventBus eventBus,
                         ResourceManagerFactory resourceManagerFactory) {
        this.eventBus = eventBus;
        this.resourceManagerFactory = resourceManagerFactory;

        eventBus.addHandler(WorkspaceConfigurationChangedEvent.getType(), this);
        eventBus.addHandler(ResourceChangedEvent.getType(), this);
    }

    /** {@inheritDoc} */
    @Override
    public Project[] getProjects() {
        return checkNotNull(projects, "Projects is not initialized");
    }

    /** {@inheritDoc} */
    @Override
    public String getId() {
        return wsId;
    }

    /** {@inheritDoc} */
    @Override
    public Container getWorkspaceRoot() {
        checkState(resourceManager != null, "Workspace configuration has not been received yet");

        return resourceManager.getWorkspaceRoot();
    }

    /** {@inheritDoc} */
    @Override
    public String getName() {
        return wsConfiguration.getName();
    }

    /** {@inheritDoc} */
    @Override
    public String getDescription() {
        return wsConfiguration.getDescription();
    }

    /** {@inheritDoc} */
    @Override
    public String getDefaultEnvironment() {
        return wsConfiguration.getDefaultEnv();
    }

    /** {@inheritDoc} */
    @Override
    public Environment[] getEnvironments() {
        final List<? extends Environment> environments = wsConfiguration.getEnvironments();

        return environments.toArray(new Environment[environments.size()]);
    }

    /** {@inheritDoc} */
    @Override
    public Command[] getCommands() {
        final List<? extends Command> commands = wsConfiguration.getCommands();

        return commands.toArray(new Command[commands.size()]);
    }

    /** {@inheritDoc} */
    @Override
    public void onConfigurationChanged(final WorkspaceConfigurationChangedEvent event) {
        this.wsConfiguration = event.getConfiguration();
        this.wsId = event.getID();

        resourceManager = resourceManagerFactory.newResourceManager(event.getID());
        resourceManager.getWorkspaceProjects().then(new Operation<Project[]>() {
            @Override
            public void apply(Project[] projects) throws OperationException {
                WorkspaceImpl.this.projects = projects;
                Arrays.sort(WorkspaceImpl.this.projects, ResourcePathComparator.getInstance());
                eventBus.fireEvent(new WorkspaceConfigurationAppliedEvent(projects));
            }
        });
    }

    /** {@inheritDoc} */
    @Override
    public void onResourceChanged(ResourceChangedEvent event) {
        final ResourceDelta delta = event.getDelta();
        final Resource resource = delta.getResource();

        /* Note: There is important to keep projects array in sorted state, because it is mutable and removing projects from it
           need array to be sorted. Search specific projects realized with binary search. */

        if (!(resource.getResourceType() == PROJECT && resource.getLocation().segmentCount() == 1)) {
            return;
        }

        if (projects == null) {
            return; //Normal situation, workspace config updated and project has not been loaded fully. Just skip this situation.
        }

        if (delta.getKind() == CREATED) {
            Project[] newProjects = copyOf(projects, projects.length + 1);
            newProjects[projects.length] = (Project)resource;
            projects = newProjects;
            sort(projects, ResourcePathComparator.getInstance());
        } else if (delta.getKind() == REMOVED) {
            int size = projects.length;
            int index = Arrays.binarySearch(projects, resource, ResourcePathComparator.getInstance());
            int numMoved = projects.length - index - 1;
            if (numMoved > 0) {
                System.arraycopy(projects, index + 1, projects, index, numMoved);
            }
            projects = copyOf(projects, --size);
        } else if (delta.getKind() == CHANGED) {
            int index = -1;

            // Project may be moved to another location, so we need to remove previous one and store new project in cache.

            if (delta.getFlags() == MOVED_FROM) {
                for (int i = 0; i < projects.length; i++) {
                    if (projects[i].getLocation().equals(delta.getFromPath())) {
                        index = i;
                        break;
                    }
                }
            } else {
                index = binarySearch(projects, resource);
            }

            if (index != -1) {
                projects[index] = (Project)resource;
            }

            sort(projects, ResourcePathComparator.getInstance());
        }
    }
}
