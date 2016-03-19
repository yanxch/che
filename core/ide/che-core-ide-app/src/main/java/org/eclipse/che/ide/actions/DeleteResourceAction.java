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

import com.google.gwt.core.client.Callback;
import com.google.inject.Inject;
import com.google.inject.Singleton;

import org.eclipse.che.api.analytics.client.logger.AnalyticsEventLogger;
import org.eclipse.che.api.promises.client.Operation;
import org.eclipse.che.api.promises.client.OperationException;
import org.eclipse.che.api.promises.client.Promise;
import org.eclipse.che.api.promises.client.PromiseError;
import org.eclipse.che.api.promises.client.callback.CallbackPromiseHelper;
import org.eclipse.che.ide.CoreLocalizationConstant;
import org.eclipse.che.ide.Resources;
import org.eclipse.che.ide.api.action.AbstractPerspectiveAction;
import org.eclipse.che.ide.api.action.ActionEvent;
import org.eclipse.che.ide.api.action.PromisableAction;
import org.eclipse.che.ide.api.app.AppContext;
import org.eclipse.che.ide.api.resources.Resource;
import org.eclipse.che.ide.resources.DeleteResourceManager;

import javax.validation.constraints.NotNull;

import static java.util.Collections.singletonList;
import static org.eclipse.che.api.promises.client.callback.CallbackPromiseHelper.createFromCallback;
import static org.eclipse.che.ide.workspace.perspectives.project.ProjectPerspective.PROJECT_PERSPECTIVE_ID;

/**
 * Deletes resources which are in application context.
 *
 * @author Artem Zatsarynnyi
 * @author Dmitry Shnurenko
 * @author Vlad Zhukovskyi
 * @see DeleteResourceManager
 */
@Singleton
public class DeleteResourceAction extends AbstractPerspectiveAction implements PromisableAction {
    private final AnalyticsEventLogger  eventLogger;
    private final DeleteResourceManager deleteResourceManager;
    private final AppContext            appContext;

    private Callback<Void, Throwable> actionCompletedCallBack;

    @Inject
    public DeleteResourceAction(Resources resources,
                                AnalyticsEventLogger eventLogger,
                                DeleteResourceManager deleteResourceManager,
                                CoreLocalizationConstant localization,
                                AppContext appContext) {
        super(singletonList(PROJECT_PERSPECTIVE_ID),
              localization.deleteItemActionText(),
              localization.deleteItemActionDescription(),
              null,
              resources.delete());
        this.eventLogger = eventLogger;
        this.deleteResourceManager = deleteResourceManager;
        this.appContext = appContext;
    }

    /** {@inheritDoc} */
    @Override
    public void actionPerformed(ActionEvent e) {
        eventLogger.log(this);

        deleteResourceManager.delete(true, appContext.getResources()).then(new Operation<Void>() {
            @Override
            public void apply(Void arg) throws OperationException {
                if (actionCompletedCallBack != null) {
                    actionCompletedCallBack.onSuccess(null);
                }
            }
        }).catchError(new Operation<PromiseError>() {
            @Override
            public void apply(PromiseError arg) throws OperationException {
                if (actionCompletedCallBack != null) {
                    actionCompletedCallBack.onFailure(arg.getCause());
                }
            }
        });
    }

    /** {@inheritDoc} */
    @Override
    public void updateInPerspective(@NotNull ActionEvent event) {
        final Resource[] resources = appContext.getResources();

        event.getPresentation().setVisible(true);
        event.getPresentation().setEnabled(resources != null && resources.length > 0 && appContext.getCurrentUser().isUserPermanent());
    }

    /** {@inheritDoc} */
    @Override
    public Promise<Void> promise(final ActionEvent event) {
        final CallbackPromiseHelper.Call<Void, Throwable> call = new CallbackPromiseHelper.Call<Void, Throwable>() {
            @Override
            public void makeCall(Callback<Void, Throwable> callback) {
                actionCompletedCallBack = callback;
                actionPerformed(event);
            }
        };

        return createFromCallback(call);
    }
}
