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
package org.eclipse.che.ide.api.workspace;

import com.google.common.annotations.Beta;

import org.eclipse.che.api.core.model.machine.Command;
import org.eclipse.che.api.core.model.workspace.Environment;
import org.eclipse.che.api.core.model.workspace.UsersWorkspace;
import org.eclipse.che.api.core.model.workspace.WorkspaceConfig;
import org.eclipse.che.ide.api.resources.Container;
import org.eclipse.che.ide.api.resources.Project;

/**
 * Workspace context. Responsible for providing current state of active workspace in the moment.
 * <p/>
 * When workspace becomes active, context initializes with registered projects, which can be
 * retrieved by calling {@link Workspace#getProjects()}.
 * <p/>
 * Workspace root is representing by default resource container and can be retrieved by calling
 * {@link Workspace#getWorkspaceRoot()}. To create a new project in the workspace root method
 * {@link Container#newProject(String, String, boolean)} can be called.
 * <p/>
 * Note. That folders and files are disallowed to be created in workspace root. When this operation
 * performs, exception will be thrown.
 * <p/>
 * This interface is not intended to be implemented by third party components.
 *
 * @author Vlad Zhukovskiy
 * @since 4.0.0-RC14
 */
@Beta
public interface Workspace {
    /**
     * Returns the registered projects in current workspace. If no projects were registered before,
     * then empty array is returned.
     *
     * @return the registered projects
     * @see Container#newProject(String, String, boolean)
     * @since 4.0.0-RC14
     */
    Project[] getProjects();

    /**
     * Returns the workspace root container, which is holder of registered projects.
     *
     * @return the workspace root
     * @since 4.0.0-RC14
     */
    Container getWorkspaceRoot();

    /**
     * Returns the workspace identifier.
     *
     * @return the workspace identifier
     * @see UsersWorkspace#getId()
     * @since 4.0.0-RC14
     */
    String getId();

    /**
     * Returns the workspace name.
     *
     * @return the workspace name
     * @see WorkspaceConfig#getName()
     * @since 4.0.0-RC14
     */
    String getName();

    /**
     * Returns the workspace description.
     *
     * @return the workspace description
     * @see WorkspaceConfig#getDescription()
     * @since 4.0.0-RC14
     */
    String getDescription();

    /**
     * Returns the default environment name.
     *
     * @return the default environment name
     * @see WorkspaceConfig#getDefaultEnv()
     * @since 4.0.0-RC14
     */
    String getDefaultEnvironment();

    /**
     * Returns the workspace environments. Workspace must contain at least 1 default environment and may contain N environments.
     *
     * @return the environments array
     * @see WorkspaceConfig#getEnvironments()
     * @since 4.0.0-RC14
     */
    Environment[] getEnvironments();

    /**
     * Returns the commands which are related to workspace, when workspace doesn't contain commands returns empty array. Workspace
     * may contain 0 or N commands.
     *
     * @return the workspace commands array
     * @see WorkspaceConfig#getCommands()
     * @since 4.0.0-RC14
     */
    Command[] getCommands();
}
