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
package org.eclipse.che.ide.ext.git.client.init;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import org.eclipse.che.api.git.gwt.client.GitServiceClient;
import org.eclipse.che.api.promises.client.Operation;
import org.eclipse.che.api.promises.client.OperationException;
import org.eclipse.che.api.promises.client.PromiseError;
import org.eclipse.che.ide.api.app.AppContext;
import org.eclipse.che.ide.api.notification.NotificationManager;
import org.eclipse.che.ide.api.resources.Project;
import org.eclipse.che.ide.api.workspace.Workspace;
import org.eclipse.che.ide.ext.git.client.GitLocalizationConstant;
import org.eclipse.che.ide.ext.git.client.outputconsole.GitOutputConsole;
import org.eclipse.che.ide.ext.git.client.outputconsole.GitOutputConsoleFactory;
import org.eclipse.che.ide.extension.machine.client.processes.ConsolesPanelPresenter;

import javax.validation.constraints.NotNull;

import static org.eclipse.che.ide.api.notification.StatusNotification.Status.FAIL;

/**
 * Presenter for Git command Init Repository.
 *
 * @author Ann Zhuleva
 * @author Roman Nikitenko
 * @author Vlad Zhukovskyi
 */
@Singleton
public class InitRepositoryPresenter {
    public static final String INIT_COMMAND_NAME = "Git init";

    private final GitOutputConsoleFactory  gitOutputConsoleFactory;
    private final ConsolesPanelPresenter   consolesPanelPresenter;
    private final GitServiceClient service;
    private final Workspace workspace;
    private final AppContext               appContext;
    private final GitLocalizationConstant  constant;
    private final NotificationManager      notificationManager;

    @Inject
    public InitRepositoryPresenter(AppContext appContext,
                                   GitLocalizationConstant constant,
                                   NotificationManager notificationManager,
                                   GitOutputConsoleFactory gitOutputConsoleFactory,
                                   ConsolesPanelPresenter consolesPanelPresenter,
                                   GitServiceClient service,
                                   Workspace workspace) {
        this.appContext = appContext;
        this.constant = constant;
        this.notificationManager = notificationManager;
        this.gitOutputConsoleFactory = gitOutputConsoleFactory;
        this.consolesPanelPresenter = consolesPanelPresenter;
        this.service = service;
        this.workspace = workspace;
    }

    public void initRepository(final Project project) {
        final GitOutputConsole console = gitOutputConsoleFactory.create(INIT_COMMAND_NAME);

        service.init(workspace.getId(), project.getLocation(), false).then(new Operation<Void>() {
            @Override
            public void apply(Void ignored) throws OperationException {
                console.print(constant.initSuccess());
                consolesPanelPresenter.addCommandOutput(appContext.getDevMachineId(), console);
                notificationManager.notify(constant.initSuccess());

                project.synchronize();
            }
        }).catchError(new Operation<PromiseError>() {
            @Override
            public void apply(PromiseError error) throws OperationException {
                handleError(error.getCause(), console);
                consolesPanelPresenter.addCommandOutput(appContext.getDevMachineId(), console);
            }
        });
    }

    /**
     * Handler some action whether some exception happened.
     *
     * @param e
     *         exception what happened
     */
    private void handleError(@NotNull Throwable e, GitOutputConsole console) {
        String errorMessage = (e.getMessage() != null && !e.getMessage().isEmpty()) ? e.getMessage() : constant.initFailed();
        console.printError(errorMessage);
        notificationManager.notify(constant.initFailed(), FAIL, true, appContext.getCurrentProject().getRootProject());
    }
}
