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
package org.eclipse.che.ide.actions;

import org.eclipse.che.ide.CoreLocalizationConstant;
import org.eclipse.che.ide.api.action.AbstractPerspectiveAction;
import org.eclipse.che.ide.api.action.ActionEvent;
import org.eclipse.che.ide.api.workspace.Workspace;

import javax.inject.Inject;
import javax.inject.Singleton;
import javax.validation.constraints.NotNull;

import static java.util.Collections.singletonList;

/**
 * @author Yevhenii Voevodin
 * @author Vlad Zhukovskyi
 */
@Singleton
public class CreateSnapshotAction extends AbstractPerspectiveAction {

    private static final String MACHINE_PERSPECTIVE_ID = "Machine Perspective";

    private final WorkspaceSnapshotCreator snapshotCreator;
    private final Workspace                workspace;

    @Inject
    public CreateSnapshotAction(CoreLocalizationConstant locale,
                                WorkspaceSnapshotCreator snapshotCreator,
                                Workspace workspace) {
        super(singletonList(MACHINE_PERSPECTIVE_ID), locale.createSnapshotTitle(), locale.createSnapshotDescription(), null, null);
        this.snapshotCreator = snapshotCreator;
        this.workspace = workspace;
    }

    @Override
    public void updateInPerspective(@NotNull ActionEvent event) {
        event.getPresentation().setEnabled(!snapshotCreator.isInProgress());
    }

    @Override
    public void actionPerformed(ActionEvent event) {
        snapshotCreator.createSnapshot(workspace.getId());
    }
}
