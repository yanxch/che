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
package org.eclipse.che.ide.extension.maven.client.tree;

import com.google.common.annotations.Beta;
import com.google.inject.Inject;
import com.google.inject.Singleton;

import org.eclipse.che.api.promises.client.PromiseProvider;
import org.eclipse.che.ide.api.data.tree.settings.SettingsProvider;
import org.eclipse.che.ide.api.resources.Project;
import org.eclipse.che.ide.ext.java.client.tree.JavaNodeFactory;
import org.eclipse.che.ide.ext.java.client.tree.LibraryNodeProvider;
import org.eclipse.che.ide.extension.maven.shared.MavenAttributes;

import java.util.List;

/**
 * @author Vlad Zhukovskiy
 */
@Beta
@Singleton
public class MavenLibraryNodeProvider extends LibraryNodeProvider {

    @Inject
    public MavenLibraryNodeProvider(JavaNodeFactory nodeFactory,
                                    PromiseProvider promises,
                                    SettingsProvider settingsProvider) {
        super(nodeFactory, promises, settingsProvider);
    }

    @Override
    public boolean isDisplayLibraries(Project project) {
        List<String> packaging = project.getAttributes().get(MavenAttributes.PACKAGING);
        return packaging != null && !packaging.isEmpty() && !packaging.get(0).equals("pom");
    }
}
