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
package org.eclipse.che.api.core.model.workspace;

import org.eclipse.che.api.core.model.machine.MachineConfig2;

import java.util.Map;

/**
 * Defines environment for machines network.
 *
 * @author gazarenkov
 */
public interface Environment {
    /**
     * Returns the recipe (the main script) to define this environment (compose, kubernetes pod).
     * Type of this recipe defines engine for composing machines network runtime
     */
    EnvironmentRecipe getRecipe();

    Map<String, ? extends MachineConfig2> getMachines();
}
