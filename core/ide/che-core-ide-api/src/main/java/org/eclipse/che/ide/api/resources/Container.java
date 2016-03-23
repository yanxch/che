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
import com.google.common.base.Optional;

import org.eclipse.che.api.promises.client.Promise;
import org.eclipse.che.ide.api.resources.Project.CreateRequest;
import org.eclipse.che.ide.api.resources.Project.ImportRequest;
import org.eclipse.che.ide.api.workspace.Workspace;
import org.eclipse.che.ide.resource.Path;
import org.eclipse.che.ide.util.NameUtils;

/**
 * Interface for resource which may contain other resources (termed its members).
 * <p/>
 * If {@code location} of current container is equals to {@link Path#ROOT} then it means that current container
 * represent the workspace root. To obtain the workspace root {@link Workspace} should be injected into third-
 * party component and method {@link Workspace#getWorkspaceRoot()} should be called. Only {@link Project}s are
 * allowed to be created in workspace root.
 * <p/>
 * Note. This interface is not intended to be implemented by clients.
 *
 * @author Vlad Zhukovskyi
 * @see Project
 * @see Folder
 * @see Workspace
 * @see Workspace#getWorkspaceRoot()
 * @since 4.0.0-RC14
 */
@Beta
public interface Container extends Resource {

    /**
     * Returns the {@code Promise} with handle to the file resource identified by the given path in this container.
     * <p/>
     * The supplied path should represent relative path to file in this container.
     *
     * @param relativePath
     *         the path of the member file
     * @return the {@code Promise} with the handle of the member file
     * @throws IllegalStateException
     *         if during resource search failed has been occurred. Reasons include:
     *         <ul>
     *         <li>Resource with path '/project_path' doesn't exists</li>
     *         <li>Resource with path '/project_path' isn't a project</li>
     *         <li>Not a file</li>
     *         </ul>
     * @see #getContainer(Path)
     * @since 4.0.0-RC14
     */
    Promise<Optional<File>> getFile(Path relativePath);

    /**
     * Returns the {@code Promise} with handle to the container identified by the given path in this container.
     * <p/>
     * The supplied path should represent relative path to folder.
     *
     * @param relativePath
     *         the path of the member container
     * @return the {@code Promise} with the handle of the member container
     * @throws IllegalStateException
     *         if during resource search failed has been occurred. Reasons include:
     *         <ul>
     *         <li>Resource with path '/project_path' doesn't exists</li>
     *         <li>Resource with path '/project_path' isn't a project</li>
     *         <li>Not a container</li>
     *         </ul>
     * @see #getFile(Path)
     * @since 4.0.0-RC14
     */
    Promise<Optional<Container>> getContainer(Path relativePath);

    /**
     * Returns the {@code Promise} with array of existing member resources (projects, folders and files) in this resource,
     * in particular order. Order is organized by alphabetic resource name ignoring case.
     * <p/>
     * Supplied parameter {@code force} instructs that stored children should be updated.
     * <p/>
     * Note, that if the result array is empty, then method thinks that children may not be loaded from the server and send
     * a request ot the server to load the children.
     *
     * @return the {@code Promise} with array of members of this resource
     * @see #getChildren()
     * @since 4.0.0-RC14
     */
    Promise<Resource[]> getChildren();

    /**
     * Returns the {@code Promise} with array of existing member resources (projects, folders and files) in this resource,
     * in particular order. Order is organized by alphabetic resource name ignoring case.
     * <p/>
     * Supplied parameter {@code force} instructs that stored children should be updated.
     * <p/>
     * Note, that if supplied argument {@code force} is set to {@code false} and result array is empty, then method thinks
     * that children may not be loaded from the server and send a request ot the server to load the children.
     *
     * @return the {@code Promise} with array of members of this resource
     * @see #getChildren()
     * @since 4.0.0-RC14
     */
    Promise<Resource[]> getChildren(boolean force);

    /**
     * Creates the new {@link Project} in current container.
     * <p/>
     * Fires following events:
     * {@link ResourceChangedEvent} when project has successfully created.
     * <p/>
     * Calling this method doesn't create a project immediately. To complete the request method {@link CreateRequest#send()} should be
     * called. {@link CreateRequest} has ability to reconfigure project during update/create operations.
     * <p/>
     * Calling {@link CreateRequest#send()} produces new {@link Project} resource.
     * <p/>
     * The supplied argument {@code name} should be a valid and pass validation within {@link NameUtils#checkProjectName(String)}.
     * The supplied argument {@code type} should be a valid and registered project type.
     * <p/>
     * <p/>
     * Example of usage for creating a new project:
     * <pre>
     *     Container workspace = ... ;
     *
     *     Promise<Project> newProjectPromise = workspace.newProject()
     *                                                   .setName("name")
     *                                                   .setDescription("Some description")
     *                                                   .send();
     *
     *     newProjectPromise.then(new Operation<Project>() {
     *         public void apply(Project newProject) throws OperationException {
     *              //do something with new project
     *         }
     *     });
     * </pre>
     *
     * @return the create project request
     * @throws IllegalArgumentException
     *         if arguments is not a valid. Reasons include:
     *         <ul>
     *         <li>Invalid project name</li>
     *         <li>Invalid project type</li>
     *         </ul>
     * @throws IllegalStateException
     *         if creation was failed. Reasons include:
     *         <ul>
     *         <li>Resource already exists</li>
     *         </ul>
     * @see NameUtils#checkProjectName(String)
     * @see CreateRequest
     * @see CreateRequest#send()
     * @since 4.0.0-RC14
     */
    CreateRequest newProject();

    /**
     * Creates the new {@link Project} in current container with specified source storage (in other words, imports a remote project).
     * <p/>
     * Fires following events:
     * {@link ResourceChangedEvent} when project has successfully created.
     * <p/>
     * Calling this method doesn't import a project immediately. To complete the request method {@link ImportRequest#send()} should be
     * called.
     * <p/>
     * Calling {@link ImportRequest#send()} produces new {@link Project} resource.
     * <p/>
     * The supplied argument {@code name} should be a valid and pass validation within {@link NameUtils#checkProjectName(String)}.
     * <p/>
     * <p/>
     * Example of usage for creating a new project:
     * <pre>
     *     SourceStorage sourceConfig ... ;
     *     Container workspace = ... ;
     *
     *     Promise<Project> newProjectPromise = workspace.importProject()
     *                                                   .setName("name")
     *                                                   .setSourceStorage(sourceConfig)
     *                                                   .send();
     *
     *     newProjectPromise.then(new Operation<Project>() {
     *         public void apply(Project newProject) throws OperationException {
     *              //do something with new project
     *         }
     *     });
     * </pre>
     *
     * @return the create project request
     * @throws IllegalArgumentException
     *         if arguments is not a valid. Reasons include:
     *         <ul>
     *         <li>Invalid project name</li>
     *         </ul>
     * @throws IllegalStateException
     *         if creation was failed. Reasons include:
     *         <ul>
     *         <li>Resource already exists</li>
     *         </ul>
     * @see NameUtils#checkProjectName(String)
     * @see ImportRequest
     * @see ImportRequest#send()
     * @since 4.0.0-RC14
     */
    ImportRequest importProject();

    /**
     * Creates the new {@link Folder} in current container.
     * <p/>
     * Fires following events:
     * {@link ResourceChangedEvent} when folder has successfully created.
     * <p/>
     * Method produces new {@link Folder}.
     * <p/>
     * The supplied argument {@code name} should be a valid and pass validation within {@link NameUtils#checkFolderName(String)}.
     * <p/>
     * Note. That folders can not be created in workspace root (obtained by {@link Workspace#getWorkspaceRoot()}).
     * Creating folder in this container will be failed.
     * <p/>
     * Example of usage:
     * <pre>
     *     Container workspace = ... ;
     *
     *     workspace.newFolder("name").then(new Operation<Folder>() {
     *         public void apply(Folder newFolder) throws OperationException {
     *              //do something with new folder
     *         }
     *     });
     * </pre>
     *
     * @param name
     *         the name of the folder
     * @return the {@link Promise} with created {@link Folder}
     * @throws IllegalArgumentException
     *         if arguments is not a valid. Reasons include:
     *         <ul>
     *         <li>Invalid folder name</li>
     *         <li>Failed to create folder in workspace root</li>
     *         </ul>
     * @throws IllegalStateException
     *         if creation was failed. Reasons include:
     *         <ul>
     *         <li>Resource already exists</li>
     *         </ul>
     * @see NameUtils#checkFolderName(String)
     * @since 4.0.0-RC14
     */
    Promise<Folder> newFolder(String name);

    /**
     * Creates the new {@link File} in current container.
     * <p/>
     * Fires following events:
     * {@link ResourceChangedEvent} when file has successfully created.
     * <p/>
     * Method produces new {@link File}.
     * <p/>
     * The supplied argument {@code name} should be a valid and pass validation within {@link NameUtils#checkFileName(String)} (String)}.
     * <p/>
     * Note. That files can not be created in workspace root (obtained by {@link Workspace#getWorkspaceRoot()}).
     * Creating folder in this container will be failed.
     * <p/>
     * The file content may be a {@code null} or empty.
     * <p/>
     * Example of usage:
     * <pre>
     *     Container workspace = ... ;
     *
     *     workspace.newFile("name", "content").then(new Operation<File>() {
     *         public void apply(File newFile) throws OperationException {
     *              //do something with new file
     *         }
     *     });
     * </pre>
     *
     * @param name
     *         the name of the file
     * @param content
     *         the file content
     * @return the {@link Promise} with created {@link File}
     * @throws IllegalArgumentException
     *         if arguments is not a valid. Reasons include:
     *         <ul>
     *         <li>Invalid file name</li>
     *         <li>Failed to create file in workspace root</li>
     *         </ul>
     * @throws IllegalStateException
     *         if creation was failed. Reasons include:
     *         <ul>
     *         <li>Resource already exists</li>
     *         </ul>
     * @see NameUtils#checkFileName(String)
     * @since 4.0.0-RC14
     */
    Promise<File> newFile(String name, String content);

    /**
     * Synchronizes the cached container and its children with the local file system.
     * <p/>
     * For refreshing entire workspace root this method should be called on the container, which obtained
     * from {@link Workspace#getWorkspaceRoot()}.
     * <p/>
     * Fires following events:
     * {@link ResourceChangedEvent} when the synchronized resource has changed.
     *
     * @return the array of resource which where affected by synchronize operation
     * @since 4.0.0-RC14
     */
    Promise<Resource[]> synchronize();

    /**
     * Searches the all possible files which matches given file or content mask.
     * <p/>
     * Supplied file mask may supports wildcard:
     * <ul>
     * <li>{@code *} - which matches any character sequence (including the empty one)</li>
     * <li>{@code ?} - which matches any single character</li>
     * </ul>
     *
     * @param fileMask
     *         the file name mask
     * @param contentMask
     *         the content entity mask
     * @return the {@link Promise} with array of found results
     * @since 4.0.0-RC14
     */
    Promise<Resource[]> search(String fileMask, String contentMask);
}
