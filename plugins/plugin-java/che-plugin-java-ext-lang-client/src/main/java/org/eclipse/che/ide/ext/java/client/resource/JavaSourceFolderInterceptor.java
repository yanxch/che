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
package org.eclipse.che.ide.ext.java.client.resource;

import org.eclipse.che.ide.api.resources.Project;
import org.eclipse.che.ide.api.resources.Resource;
import org.eclipse.che.ide.api.resources.ResourceInterceptor;
import org.eclipse.che.ide.resource.Path;

import static com.google.common.base.Preconditions.checkArgument;
import static org.eclipse.che.ide.api.resources.Resource.FOLDER;
import static org.eclipse.che.ide.ext.java.client.util.JavaUtil.isJavaProject;
import static org.eclipse.che.ide.ext.java.shared.ContentRoot.SOURCE;
import static org.eclipse.che.ide.ext.java.shared.ContentRoot.TEST_SOURCE;

/**
 * @author Vlad Zhukovskiy
 */
public abstract class JavaSourceFolderInterceptor implements ResourceInterceptor {
    @Override
    public Resource intercept(Resource resource) {
        checkArgument(resource != null, "Null resource occurred");

        if (resource.getResourceType() != FOLDER) {
            return resource;
        }

        final Project project = resource.getRelatedProject();

        if (isJavaProject(project)) {
            final Path resourcePath = resource.getLocation();

            for (Path path : getSourceFolder(project)) {
                if (path.equals(resourcePath)) {
                    resource.addMarker(new JavaSourceFolderMarker(SOURCE));
                    return resource;
                }
            }

            for (Path path : getTestFolder(project)) {
                if (path.equals(resourcePath)) {
                    resource.addMarker(new JavaSourceFolderMarker(TEST_SOURCE));
                    return resource;
                }
            }
        }

        return resource;
    }

    protected abstract Path[] getSourceFolder(Project project);

    protected abstract Path[] getTestFolder(Project project);
}
