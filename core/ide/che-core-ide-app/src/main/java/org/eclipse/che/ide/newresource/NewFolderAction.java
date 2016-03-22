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
package org.eclipse.che.ide.newresource;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.google.web.bindery.event.shared.EventBus;

import org.eclipse.che.api.analytics.client.logger.AnalyticsEventLogger;
import org.eclipse.che.commons.annotation.Nullable;
import org.eclipse.che.ide.CoreLocalizationConstant;
import org.eclipse.che.ide.Resources;
import org.eclipse.che.ide.api.action.ActionEvent;
import org.eclipse.che.ide.api.app.AppContext;
import org.eclipse.che.ide.api.resources.Container;
import org.eclipse.che.ide.api.resources.Resource;
import org.eclipse.che.ide.ui.dialogs.DialogFactory;
import org.eclipse.che.ide.ui.dialogs.InputCallback;
import org.eclipse.che.ide.ui.dialogs.input.InputDialog;
import org.eclipse.che.ide.ui.dialogs.input.InputValidator;
import org.eclipse.che.ide.util.NameUtils;

import static com.google.common.base.Preconditions.checkState;

/**
 * Action to create new folder.
 *
 * @author Artem Zatsarynnyi
 * @author Vlad Zhukovskyi
 */
@Singleton
public class NewFolderAction extends AbstractNewResourceAction {

    private InputValidator folderNameValidator;

    @Inject
    public NewFolderAction(CoreLocalizationConstant localizationConstant,
                           Resources resources,
                           AnalyticsEventLogger eventLogger,
                           DialogFactory dialogFactory,
                           EventBus eventBus,
                           AppContext appContext) {
        super(localizationConstant.actionNewFolderTitle(),
              localizationConstant.actionNewFolderDescription(),
              resources.defaultFolder(), eventLogger, dialogFactory, localizationConstant, eventBus, appContext);
        this.folderNameValidator = new FolderNameValidator();
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        eventLogger.log(this);

        InputDialog inputDialog = dialogFactory.createInputDialog(
                coreLocalizationConstant.newResourceTitle(coreLocalizationConstant.actionNewFolderTitle()),
                coreLocalizationConstant.newResourceLabel(coreLocalizationConstant.actionNewFolderTitle().toLowerCase()),
                new InputCallback() {
                    @Override
                    public void accepted(String value) {
                        onAccepted(value);
                    }
                }, null).withValidator(folderNameValidator);
        inputDialog.show();
    }

    private void onAccepted(String value) {
        final Resource resource = appContext.getResource();

        checkState(resource instanceof Container, "Parent should be a container");

        ((Container)resource).newFolder(value);
    }

    private class FolderNameValidator implements InputValidator {
        @Nullable
        @Override
        public Violation validate(String value) {
            if (!NameUtils.checkFolderName(value)) {
                return new Violation() {
                    @Override
                    public String getMessage() {
                        return coreLocalizationConstant.invalidName();
                    }

                    @Nullable
                    @Override
                    public String getCorrectedValue() {
                        return null;
                    }
                };
            }
            return null;
        }
    }
}
