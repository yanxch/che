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
package org.eclipse.che.ide.api.resources;

import com.google.common.annotations.Beta;

import org.eclipse.che.api.core.model.project.ProjectConfig;
import org.eclipse.che.api.core.model.project.SourceStorage;
import org.eclipse.che.api.promises.client.Promise;
import org.eclipse.che.ide.api.resources.marker.AbstractMarker;
import org.eclipse.che.ide.api.workspace.Workspace;
import org.eclipse.che.ide.resource.Path;

import java.util.List;
import java.util.Map;

/**
 * An object that represents client side project.
 * <p/>
 * Features of projects include:
 * <ul>
 * <li>A project collects together a set of files and folders.</li>
 * <li>A project's location controls where the project's resources are stored in the local file system.</li>
 * </ul>
 * Project also extends {@link ProjectConfig} which contains the meta-data required to define a project.
 * <p/>
 * To get list of currently of all loaded projects in the IDE, use {@link Workspace#getProjects()}
 * <p/>
 * Note. This interface is not intended to be implemented by clients.
 *
 * @author Vlad Zhukovskyi
 * @see Workspace#getProjects()
 * @since 4.0.0-RC14
 */
@Beta
public interface Project extends Container, ProjectConfig {

    /**
     * Changes this project resource to match the given configuration provided by the request.
     * Request contains all necessary data for manipulating with the project.
     * <p/>
     * Example of usage:
     * <pre>
     *     Project project = ... ;
     *
     *     Project.Request updateRequest = project.update()
     *                                            .withType("blank")
     *                                            .withDescription("New description");
     *
     *     Promise<Project> updatedProject = updateRequest.send();
     * </pre>
     * <p/>
     * Note. Calling this method doesn't update the project immediately. To complete request
     * method {@link UpdateRequest#send()} should be called.
     *
     * @return the request to update the project
     * @see UpdateRequest
     * @see UpdateRequest#send()
     * @since 4.0.0-RC14
     */
    UpdateRequest update();

    /**
     * Check whether current project has problems. Problem project calculates in a runtime, so it is not affects stored
     * configuration on the server. To find out the reasons why project has problems, following code snippet may be helpful:
     * <p/>
     * Example of usage:
     * <pre>
     *     Project project = ... ;
     *     if (project.isProblem()) {
     *         Marker problemMarker = getMarker(ProblemProjectMarker.PROBLEM_PROJECT).get();
     *
     *         String message = String.valueOf(problemMarker.getAttribute(Marker.MESSAGE));
     *     }
     * </pre>
     *
     * @return {@code true} if current project has problems, otherwise {@code false}
     * @see ProblemProjectMarker
     * @since 4.0.0-RC14
     */
    boolean isProblem();

    /**
     * Marker that describe problematic project.
     *
     * @see #isProblem()
     * @since 4.0.0-RC14
     */
    @Beta
    class ProblemProjectMarker extends AbstractMarker {

        /**
         * Marker type, which should be used when marker requests.
         *
         * @see Resource#getMarker(String)
         */
        public static final String PROBLEM_PROJECT = "problem.project.marker";

        /** {@inheritDoc} */
        @Override
        public String getType() {
            return PROBLEM_PROJECT;
        }
    }

    /**
     * Base interface for project update and create operation.
     *
     * @see Container#newProject(String, String)
     * @see Project#update()
     * @since 4.0.0-RC14
     */
    @Beta
    interface UpdateRequest {
        /**
         * Returns the {@link Project} resource if performs update operation.
         * For project creating workflow this method should return a {@code null}.
         *
         * @return the {@link Project} or a {@code null}
         * @see #update()
         * @since 4.0.0-RC14
         */
        Project getProject();

        /**
         * Returns the path for new project or path from {@link #getProject()} if no path was provided.
         *
         * @return path to project
         * @since 4.0.0-RC14
         */
        Path getPath();

        /**
         * Sets the path for the project.
         *
         * @param path
         *         the project path
         * @return instance of {@link UpdateRequest}
         * @see #getPath()
         * @since 4.0.0-RC14
         */
        UpdateRequest withPath(Path path);

        /**
         * Returns the project description or description from {@link #getProject()}  if no description was provided.
         *
         * @return the project description
         * @see ProjectConfig#getDescription()
         * @since 4.0.0-RC14
         */
        String getDescription();

        /**
         * Sets the description for the project.
         *
         * @param description
         *         the project description
         * @return instance of {@link UpdateRequest}
         * @see #getDescription()
         * @since 4.0.0-RC14
         */
        UpdateRequest withDescription(String description);

        /**
         * Returns the project type or type from {@link #getProject()} if no project type was provided.
         *
         * @return the project type
         * @see ProjectConfig#getType()
         * @since 4.0.0-RC14
         */
        String getType();

        /**
         * Sets the project type.
         *
         * @param type
         *         the project type
         * @return instance of {@link UpdateRequest}
         * @see #getType()
         * @since 4.0.0-RC14
         */
        UpdateRequest withType(String type);

        /**
         * Returns the project mixins or the mixins from {@link #getProject()} if no mixins were provided.
         *
         * @return the project mixins
         * @see ProjectConfig#getMixins()
         * @since 4.0.0-RC14
         */
        List<String> getMixins();

        /**
         * Sets the project mixins.
         *
         * @param mixins
         *         the project mixins
         * @return instance of {@link UpdateRequest}
         * @see #getMixins()
         * @since 4.0.0-RC14
         */
        UpdateRequest withMixins(List<String> mixins);

        /**
         * Returns the project attributes or the attributes from {@link #getProject()} if no attributes were provided.
         *
         * @return the project attributes
         * @see ProjectConfig#getAttributes()
         * @since 4.0.0-RC14
         */
        Map<String, List<String>> getAttributes();

        /**
         * Sets the project attributes.
         *
         * @param attributes
         *         the project attributes
         * @return instance of {@link UpdateRequest}
         * @see #getAttributes()
         * @since 4.0.0-RC14
         */
        UpdateRequest withAttributes(Map<String, List<String>> attributes);

        /**
         * Returns the source storage or the storage from {@link #getProject()} if no storage were provided.
         *
         * @return the source storage
         * @see ProjectConfig#getSource()
         * @since 4.0.0-RC14
         */
        SourceStorage getSourceStorage();

        /**
         * Sets the project source storage.
         *
         * @param sourceStorage
         *         the project source storage
         * @return instance of {@link UpdateRequest}
         * @see #getSource()
         * @since 4.0.0-RC14
         */
        UpdateRequest withSourceStorage(SourceStorage sourceStorage);

        /**
         * Sends the request to server and returns the {@link Promise} with new instance of {@link Project} which belongs
         * to provided request configuration.
         *
         * @return the {@link Promise} with new instance {@link Project}
         * @see Container#newProject(String, String)
         * @see Project#update()
         */
        Promise<Project> send();
    }
}
