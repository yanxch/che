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
package org.eclipse.che.ide.extension.maven.client.resource;

import com.google.common.annotations.Beta;
import com.google.inject.Singleton;

import org.eclipse.che.ide.api.resources.Project;
import org.eclipse.che.ide.ext.java.client.resource.JavaSourceFolderInterceptor;
import org.eclipse.che.ide.resource.Path;

import java.util.Arrays;
import java.util.List;

import static org.eclipse.che.ide.extension.maven.shared.MavenAttributes.SOURCE_FOLDER;
import static org.eclipse.che.ide.extension.maven.shared.MavenAttributes.TEST_SOURCE_FOLDER;

/**
 * @author Vlad Zhukovskiy
 */
@Beta
@Singleton
public class MavenSourceFolderInterceptor extends JavaSourceFolderInterceptor {

    public MavenSourceFolderInterceptor() {
    }

    @Override
    protected Path[] getSourceFolder(Project project) {
        return getPaths(project, SOURCE_FOLDER);
    }

    @Override
    protected Path[] getTestFolder(Project project) {
        return getPaths(project, TEST_SOURCE_FOLDER);
    }

    protected Path[] getPaths(Project project, String srcType) {
        final List<String> srcFolders = project.getAttributes().get(srcType);

        if (srcFolders == null || srcFolders.isEmpty()) {
            return new Path[0];
        }

        Path[] paths = new Path[0];

        for (String srcFolder : srcFolders) {
            final int index = paths.length;
            paths = Arrays.copyOf(paths, index + 1);
            paths[index] = project.getLocation().append(srcFolder);
        }

        return paths;
    }
}
