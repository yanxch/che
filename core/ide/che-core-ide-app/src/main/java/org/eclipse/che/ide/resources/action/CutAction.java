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
package org.eclipse.che.ide.resources.action;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import org.eclipse.che.ide.CoreLocalizationConstant;
import org.eclipse.che.ide.Resources;
import org.eclipse.che.ide.api.action.AbstractPerspectiveAction;
import org.eclipse.che.ide.api.action.ActionEvent;
import org.eclipse.che.ide.api.app.AppContext;
import org.eclipse.che.ide.api.resources.modification.ClipboardManager;

import javax.validation.constraints.NotNull;

import static com.google.common.base.Preconditions.checkState;
import static java.util.Collections.singletonList;
import static org.eclipse.che.ide.workspace.perspectives.project.ProjectPerspective.PROJECT_PERSPECTIVE_ID;

/**
 * Cut resources action.
 * Move selected resources from the application context into clipboard manager.
 *
 * @author Vlad Zhukovskiy
 * @see ClipboardManager
 * @see ClipboardManager#getCutProvider()
 * @since 4.0.0-RC14
 */
@Singleton
public class CutAction extends AbstractPerspectiveAction {

    private final ClipboardManager clipboardManager;
    private final AppContext       appContext;

    @Inject
    public CutAction(CoreLocalizationConstant localization,
                     Resources resources,
                     ClipboardManager clipboardManager,
                     AppContext appContext) {
        super(singletonList(PROJECT_PERSPECTIVE_ID),
              localization.cutItemsActionText(),
              localization.cutItemsActionDescription(),
              null,
              resources.cut());
        this.clipboardManager = clipboardManager;
        this.appContext = appContext;
    }

    /** {@inheritDoc} */
    @Override
    public void updateInPerspective(@NotNull ActionEvent event) {
        event.getPresentation().setVisible(true);
        event.getPresentation().setEnabled(clipboardManager.getCutProvider().isCutEnable(appContext));
    }

    /** {@inheritDoc} */
    @Override
    public void actionPerformed(ActionEvent e) {
        checkState(clipboardManager.getCutProvider().isCutEnable(appContext), "Cut is not enabled");

        clipboardManager.getCutProvider().performCut(appContext);
    }
}
