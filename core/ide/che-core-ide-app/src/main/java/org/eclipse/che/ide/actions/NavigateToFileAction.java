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

import com.google.inject.Inject;
import com.google.inject.Singleton;

import org.eclipse.che.api.analytics.client.logger.AnalyticsEventLogger;
import org.eclipse.che.ide.CoreLocalizationConstant;
import org.eclipse.che.ide.Resources;
import org.eclipse.che.ide.api.action.AbstractPerspectiveAction;
import org.eclipse.che.ide.api.action.ActionEvent;
import org.eclipse.che.ide.api.app.AppContext;
import org.eclipse.che.ide.api.resources.Project;
import org.eclipse.che.ide.navigation.NavigateToFilePresenter;

import javax.validation.constraints.NotNull;

import static java.util.Collections.singletonList;
import static org.eclipse.che.ide.workspace.perspectives.project.ProjectPerspective.PROJECT_PERSPECTIVE_ID;

/**
 * Action for finding file by name and opening it.
 *
 * @author Ann Shumilova
 * @author Dmitry Shnurenko
 * @author Vlad Zhukovskyi
 */
@Singleton
public class NavigateToFileAction extends AbstractPerspectiveAction {

    private final NavigateToFilePresenter  presenter;
    private final AnalyticsEventLogger     eventLogger;
    private final AppContext appContext;

    @Inject
    public NavigateToFileAction(NavigateToFilePresenter presenter,
                                AnalyticsEventLogger eventLogger,
                                Resources resources,
                                CoreLocalizationConstant localizationConstant,
                                AppContext appContext) {
        super(singletonList(PROJECT_PERSPECTIVE_ID),
              localizationConstant.actionNavigateToFileText(),
              localizationConstant.actionNavigateToFileDescription(),
              null,
              resources.navigateToFile());
        this.presenter = presenter;
        this.eventLogger = eventLogger;
        this.appContext = appContext;
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        eventLogger.log(this);
        presenter.showDialog();
    }

    @Override
    public void updateInPerspective(@NotNull ActionEvent event) {
        final Project project = appContext.getRootProject();

        event.getPresentation().setVisible(true);
        event.getPresentation().setEnabled(project != null && appContext.getCurrentUser().isUserPermanent());
    }
}
