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
package org.eclipse.che.plugin.docker.machine.ext;

import com.google.inject.AbstractModule;
import com.google.inject.TypeLiteral;
import com.google.inject.multibindings.Multibinder;
import com.google.inject.name.Names;

import org.eclipse.che.api.core.model.machine.ServerConf;
import org.eclipse.che.api.machine.server.terminal.MachineImplSpecificTerminalLauncher;
import org.eclipse.che.plugin.docker.machine.DockerMachineImplTerminalLauncher;

import java.util.Set;

/**
 * Guice module for terminal feature in docker machines
 *
 * @author Alexander Garagatyi
 */
// Not a DynaModule, install manually
public class DockerTerminalModule extends AbstractModule {
    @Override
    protected void configure() {
        bindConstant().annotatedWith(Names.named(DockerMachineImplTerminalLauncher.START_TERMINAL_COMMAND))
                      .to("mkdir -p ~/che " +
                          "&& cp /mnt/che/terminal -R ~/che" +
                          "&& ~/che/terminal/che-websocket-terminal -addr :4411 -cmd /bin/bash -static ~/che/terminal/");

        Multibinder<Set<ServerConf>> machineServersSets =
                Multibinder.newSetBinder(binder(),
                                         new TypeLiteral<Set<ServerConf>>() {},
                                         Names.named("machine.docker.machine_servers_sets"));
        machineServersSets.addBinding().toProvider(org.eclipse.che.plugin.docker.machine.ext.provider.TerminalServerConfProvider.class);

        Multibinder<Set<String>> machineVolumesSets =
                Multibinder.newSetBinder(binder(),
                                         new TypeLiteral<Set<String>>() {},
                                         Names.named("machine.docker.machine_volumes_sets"));
        machineVolumesSets.addBinding().toProvider(org.eclipse.che.plugin.docker.machine.ext.provider.TerminalVolumeProvider.class);

        Multibinder<MachineImplSpecificTerminalLauncher> terminalLaunchers = Multibinder.newSetBinder(binder(),
                                                                                                      MachineImplSpecificTerminalLauncher.class);
        terminalLaunchers.addBinding().to(org.eclipse.che.plugin.docker.machine.DockerMachineImplTerminalLauncher.class);
    }
}
