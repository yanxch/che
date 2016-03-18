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
package org.eclipse.che.ide.resources.tree;

import com.google.common.annotations.Beta;
import com.google.inject.Inject;
import com.google.inject.assistedinject.Assisted;

import org.eclipse.che.ide.api.data.tree.settings.NodeSettings;
import org.eclipse.che.ide.api.resources.Folder;
import org.eclipse.che.ide.project.shared.NodesResources;
import org.eclipse.che.ide.ui.smartTree.presentation.NodePresentation;

import javax.validation.constraints.NotNull;

/**
 * @author Vlad Zhukovskiy
 */
@Beta
public class FolderNode extends ResourceNode<Folder> {

    @Inject
    public FolderNode(@Assisted Folder resource,
                      @Assisted NodeSettings nodeSettings,
                      NodeFactory nodeFactory,
                      NodesResources nodesResources) {
        super(resource, nodeSettings, nodeFactory, nodesResources);
    }

    @Override
    public void updatePresentation(@NotNull NodePresentation presentation) {
        super.updatePresentation(presentation);

        presentation.setPresentableIcon(getData().getName().startsWith(".") ? nodesResources.hiddenSimpleFolder()
                                                                            : nodesResources.simpleFolder());
    }
}
