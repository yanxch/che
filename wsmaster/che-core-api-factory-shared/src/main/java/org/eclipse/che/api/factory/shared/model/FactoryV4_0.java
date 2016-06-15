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
package org.eclipse.che.api.factory.shared.model;

import org.eclipse.che.api.core.model.workspace.WorkspaceConfig;

/**
 * Factory of version 4.0
 *
 * @author Max Shaposhnik
 */
public interface FactoryV4_0 {
    /**
     * @return Version for Codenvy Factory API.
     */
    String getVersion();

    /**
     * Describes parameters of the workspace that should be used for factory
     */
    WorkspaceConfig getWorkspace();

    /**
     * Describe restrictions of the factory
     */
    Policies getPolicies();

    /**
     * Identifying information of author
     */
    Author getCreator();

    /**
     * Describes factory button
     */
    Button getButton();

    /**
     * Describes ide look and feel.
     */
    Ide getIde();

    /**
     * @return - id of stored factory object
     */
    String getId();

    /**
     * @return - name of stored factory object
     */
    String getName();
}
