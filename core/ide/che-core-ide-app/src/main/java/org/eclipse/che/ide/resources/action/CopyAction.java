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
 * Copy resources action.
 * Move selected resources from the application context into clipboard manager.
 *
 * @author Vlad Zhukovskiy
 * @see ClipboardManager
 * @see ClipboardManager#getCopyProvider()
 * @since 4.0.0-RC14
 */
@Singleton
public class CopyAction extends AbstractPerspectiveAction {

    private final ClipboardManager clipboardManager;
    private final AppContext       appContext;

    @Inject
    public CopyAction(CoreLocalizationConstant localization,
                      Resources resources,
                      ClipboardManager clipboardManager,
                      AppContext appContext) {
        super(singletonList(PROJECT_PERSPECTIVE_ID),
              localization.copyItemsActionText(),
              localization.copyItemsActionDescription(),
              null,
              resources.copy());
        this.clipboardManager = clipboardManager;
        this.appContext = appContext;
    }

    /** {@inheritDoc} */
    @Override
    public void updateInPerspective(@NotNull ActionEvent event) {
        event.getPresentation().setVisible(true);
        event.getPresentation().setEnabled(clipboardManager.getCopyProvider().isCopyEnable(appContext));
    }

    /** {@inheritDoc} */
    @Override
    public void actionPerformed(ActionEvent e) {
        checkState(clipboardManager.getCopyProvider().isCopyEnable(appContext), "Copy is not enabled");

        clipboardManager.getCopyProvider().performCopy(appContext);
    }
}
