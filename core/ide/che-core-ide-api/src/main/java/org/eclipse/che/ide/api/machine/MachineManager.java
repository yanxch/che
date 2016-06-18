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
package org.eclipse.che.ide.api.machine;

import org.eclipse.che.api.core.model.machine.Machine;
import org.eclipse.che.api.machine.shared.dto.MachineConfigDto;
import org.eclipse.che.api.promises.client.Promise;
import org.eclipse.che.api.workspace.shared.dto.WorkspaceDto;

/**
 * Manager for machine.
 *
 * @author Roman Nikitenko
 */
public interface MachineManager {

    /** The set of machine operation types */
    enum MachineOperationType {
        START, RESTART, DESTROY
    }

    /**
     * Performs some actions when dev machine is creating.
     *
     * @param machineConfig
     *         contains information about dev machine configuration
     */
    void onWsStarting(WorkspaceDto workspace);

    /**
     * Performs some actions when machine is running.
     *
     * @param machineId
     *         ID of machine
     */
    void onMachineRunning(String machineId);

    /**
     * Start new machine as dev-machine (bind workspace to running machine).
     *
     * @param recipeURL
     *         special recipe url to get docker image.
     * @param displayName
     *         display name for machine
     */
    void startDevMachine(String recipeURL, String displayName);

    /**
     * Start new machine in workspace.
     *
     * @param recipeURL
     *         special recipe url to get docker image.
     * @param displayName
     *         display name for machine
     */
    void startMachine(String recipeURL, String displayName);

    /**
     * Destroy machine.
     *
     * @param machine
     *         contains information about machine state
     */
    Promise<Void> destroyMachine(Machine machine);


    /**
     * Restart machine.
     *
     * @param machine
     *         contains information about machine state
     */
    void restartMachine(final Machine machine);

}
