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
package org.eclipse.che.ide.ext.git.client;

import org.eclipse.che.ide.api.resources.Project;

import java.util.List;
import java.util.Map;

import static com.google.common.base.Preconditions.checkArgument;

/**
 * @author Vlad Zhukovskiy
 */
public class GitUtil {

    public static final String VCS_ATTRIBUTE = "vcs.provider.name";

    public static boolean isUnderGit(Project project) {
        checkArgument(project != null, "Null project occurred");

        final Map<String, List<String>> attributes = project.getAttributes();
        final List<String> values = attributes.get(VCS_ATTRIBUTE);

        return values != null && values.contains("git");
    }
}
