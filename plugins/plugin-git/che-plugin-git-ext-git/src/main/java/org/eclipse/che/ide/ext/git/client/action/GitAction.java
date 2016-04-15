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
package org.eclipse.che.ide.ext.git.client.action;

import com.google.gwt.resources.client.ImageResource;

import org.eclipse.che.ide.api.action.AbstractPerspectiveAction;
import org.eclipse.che.ide.api.action.ActionEvent;
import org.eclipse.che.ide.api.app.AppContext;
import org.eclipse.che.ide.api.resources.Project;
import org.eclipse.che.ide.ext.git.client.GitUtil;
import org.vectomatic.dom.svg.ui.SVGResource;

import javax.validation.constraints.NotNull;

import static java.util.Collections.singletonList;
import static org.eclipse.che.ide.workspace.perspectives.project.ProjectPerspective.PROJECT_PERSPECTIVE_ID;

/**
 * @author Roman Nikitenko
 * @author Dmitry Shnurenko
 * @author Vlad Zhukovskyi
 */
public abstract class GitAction extends AbstractPerspectiveAction {

    protected final AppContext               appContext;

    public GitAction(String title, String description, Object icon, AppContext appContext) {
        super(singletonList(PROJECT_PERSPECTIVE_ID),
              title,
              description,
              icon instanceof ImageResource ? (ImageResource)icon : null,
              icon instanceof SVGResource ? (SVGResource)icon : null,
              icon instanceof String ? String.valueOf(icon) : null);
        this.appContext = appContext;
    }

//    /**
//     * Creates an action without Icon.
//     */
//    public GitAction(String text,
//                     String description,
//                     AppContext appContext) {
//        super(singletonList(PROJECT_PERSPECTIVE_ID), text, description, null, null);
//        this.appContext = appContext;
//    }
//
//    /**
//     * Creates an action using SVG resource for Icon.
//     */
//    public GitAction(String text,
//                     String description,
//                     SVGResource svgIcon,
//                     AppContext appContext) {
//        super(singletonList(PROJECT_PERSPECTIVE_ID), text, description, null, svgIcon);
//        this.appContext = appContext;
//    }
//
//    /**
//     * Creates an action using HTML resource for Icon.
//     */
//    public GitAction(String text,
//                     String description,
//                     String htmlResource,
//                     AppContext appContext) {
//        super(singletonList(PROJECT_PERSPECTIVE_ID), text, description, null, null, htmlResource);
//        this.appContext = appContext;
//    }

    @Override
    public void updateInPerspective(@NotNull ActionEvent event) {
        event.getPresentation().setVisible(true);

        final Project project = appContext.getRootProject();

        event.getPresentation().setEnabled(project != null && GitUtil.isUnderGit(project));
    }
}
