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

import com.google.web.bindery.event.shared.EventBus;

import org.eclipse.che.api.analytics.client.logger.AnalyticsEventLogger;
import org.eclipse.che.api.promises.client.Operation;
import org.eclipse.che.api.promises.client.OperationException;
import org.eclipse.che.commons.annotation.Nullable;
import org.eclipse.che.ide.CoreLocalizationConstant;
import org.eclipse.che.ide.api.action.AbstractPerspectiveAction;
import org.eclipse.che.ide.api.action.Action;
import org.eclipse.che.ide.api.action.ActionEvent;
import org.eclipse.che.ide.api.app.AppContext;
import org.eclipse.che.ide.api.event.FileEvent;
import org.eclipse.che.ide.api.resources.Container;
import org.eclipse.che.ide.api.resources.File;
import org.eclipse.che.ide.api.resources.Resource;
import org.eclipse.che.ide.ui.dialogs.DialogFactory;
import org.eclipse.che.ide.ui.dialogs.InputCallback;
import org.eclipse.che.ide.ui.dialogs.input.InputDialog;
import org.eclipse.che.ide.ui.dialogs.input.InputValidator;
import org.eclipse.che.ide.util.NameUtils;
import org.vectomatic.dom.svg.ui.SVGResource;

import javax.validation.constraints.NotNull;

import static com.google.common.base.Preconditions.checkState;
import static java.util.Collections.singletonList;
import static org.eclipse.che.ide.api.event.FileEvent.FileOperation.OPEN;
import static org.eclipse.che.ide.workspace.perspectives.project.ProjectPerspective.PROJECT_PERSPECTIVE_ID;

/**
 * Implementation of an {@link Action} that provides an ability to create new resource (e.g. file, folder).
 * After performing this action, it asks user for the resource's name
 * and then creates resource in the selected folder.
 *
 * @author Artem Zatsarynnyi
 * @author Dmitry Shnurenko
 * @author Vlad Zhukovskyi
 */
public abstract class AbstractNewResourceAction extends AbstractPerspectiveAction {
    private final   InputValidator           fileNameValidator;
    protected final String                   title;
    protected final AnalyticsEventLogger     eventLogger;
    protected final DialogFactory            dialogFactory;
    protected final CoreLocalizationConstant coreLocalizationConstant;
    protected final EventBus                 eventBus;
    protected final AppContext               appContext;

    public AbstractNewResourceAction(String title,
                                     String description,
                                     SVGResource svgIcon,
                                     AnalyticsEventLogger eventLogger,
                                     DialogFactory dialogFactory,
                                     CoreLocalizationConstant coreLocalizationConstant,
                                     EventBus eventBus,
                                     AppContext appContext) {
        super(singletonList(PROJECT_PERSPECTIVE_ID), title, description, null, svgIcon);
        this.eventLogger = eventLogger;
        this.dialogFactory = dialogFactory;
        this.coreLocalizationConstant = coreLocalizationConstant;
        this.eventBus = eventBus;
        this.appContext = appContext;
        this.fileNameValidator = new FileNameValidator();
        this.title = title;
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        if (eventLogger != null) {
            eventLogger.log(this);
        }

        InputDialog inputDialog = dialogFactory.createInputDialog(
                coreLocalizationConstant.newResourceTitle(title),
                coreLocalizationConstant.newResourceLabel(title.toLowerCase()),
                new InputCallback() {
                    @Override
                    public void accepted(String value) {
                        onAccepted(value);
                    }
                }, null).withValidator(fileNameValidator);
        inputDialog.show();
    }

    private void onAccepted(String value) {
        final String name = getExtension().isEmpty() ? value : value + '.' + getExtension();

        final Resource resource = appContext.getResource();

        checkState(resource instanceof Container, "Parent should be a container");

        ((Container)resource).newFile(name, getDefaultContent()).then(new Operation<File>() {
            @Override
            public void apply(File newFile) throws OperationException {
                eventBus.fireEvent(new FileEvent(newFile, OPEN));
            }
        });
    }

    @Override
    public void updateInPerspective(@NotNull ActionEvent e) {
        e.getPresentation().setVisible(true);

        final Resource[] resources = appContext.getResources();

        e.getPresentation().setEnabled(resources != null && resources.length == 1 && resources[0] instanceof Container);
    }

    /**
     * Returns extension (without dot) for a new resource.
     * By default, returns an empty string.
     */
    protected String getExtension() {
        return "";
    }

    /**
     * Returns default content for a new resource.
     * By default, returns an empty string.
     */
    protected String getDefaultContent() {
        return "";
    }

    private class FileNameValidator implements InputValidator {
        @Nullable
        @Override
        public Violation validate(String value) {
            if (!NameUtils.checkFileName(value)) {
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
