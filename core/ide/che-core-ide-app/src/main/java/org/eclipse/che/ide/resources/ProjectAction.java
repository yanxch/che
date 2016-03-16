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
package org.eclipse.che.ide.resources;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.google.web.bindery.event.shared.EventBus;

import org.eclipse.che.api.promises.client.Function;
import org.eclipse.che.api.promises.client.FunctionException;
import org.eclipse.che.api.promises.client.Operation;
import org.eclipse.che.api.promises.client.OperationException;
import org.eclipse.che.api.promises.client.Promise;
import org.eclipse.che.ide.api.action.Action;
import org.eclipse.che.ide.api.action.ActionEvent;
import org.eclipse.che.ide.api.resources.Project;
import org.eclipse.che.ide.api.resources.Resource;
import org.eclipse.che.ide.api.resources.ResourceChangedEvent;
import org.eclipse.che.ide.api.resources.ResourceChangedEvent.ResourceChangedHandler;
import org.eclipse.che.ide.api.workspace.Workspace;
import org.eclipse.che.ide.download.DownloadContainer;
import org.eclipse.che.ide.util.UUID;
import org.eclipse.che.ide.util.loging.Log;

/**
 * @author Vlad Zhukovskiy
 */
@Singleton
public class ProjectAction extends Action implements ResourceChangedHandler {

    private final Workspace workspace;
    private final EventBus  eventBus;
    private final DownloadContainer downloadContainer;

    @Inject
    public ProjectAction(Workspace workspace,
                         EventBus eventBus,
                         DownloadContainer downloadContainer) {
        super("FIRE!");
        this.workspace = workspace;
        this.eventBus = eventBus;
        this.downloadContainer = downloadContainer;

        eventBus.addHandler(ResourceChangedEvent.getType(), this);
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        Project[] projects = workspace.getProjects();

        for (Project project : projects) {
            Log.info(this.getClass(), "actionPerformed():31: loaded project: " + project);
        }

        final Project spring = getProject("spring");

        createProject("ss" + UUID.uuid(5)).thenPromise(new Function<Project, Promise<Project>>() {
            @Override
            public Promise<Project> apply(Project arg) throws FunctionException {
                return createProject("ss" + UUID.uuid(5)).thenPromise(new Function<Project, Promise<Project>>() {
                    @Override
                    public Promise<Project> apply(Project arg) throws FunctionException {
                        return createProject("ss" + UUID.uuid(5)).thenPromise(new Function<Project, Promise<Project>>() {
                            @Override
                            public Promise<Project> apply(Project arg) throws FunctionException {
                                return createProject("foooo").thenPromise(new Function<Project, Promise<Project>>() {
                                    @Override
                                    public Promise<Project> apply(Project arg) throws FunctionException {
                                        return arg.delete().then(new Function<Void, Project>() {
                                            @Override
                                            public Project apply(Void arg) throws FunctionException {
                                                return null;
                                            }
                                        });
                                    }
                                });
                            }
                        });
                    }
                });
            }
        });
    }

    private Promise<Project> createProject(String name) {
        return workspace.getWorkspaceRoot().newProject(name, "blank", false).send();
    }

    private Project getProject(String name) {
        for (Project project : workspace.getProjects()) {
            if (project.getName().equals(name)) {
                return project;
            }
        }

        return null;
    }

    @Override
    public void onResourceChanged(ResourceChangedEvent event) {
        Log.info(this.getClass(), "onResourceChanged():202: " + event.getDelta());
    }
}
