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
import org.eclipse.che.inject.CheBootstrap;

import java.util.Set;

import static java.util.Collections.singleton;

/**
 * Guice module for extension servers feature in docker machines
 *
 * @author Alexander Garagatyi
 * @author Sergii Leschenko
 */
// Not a DynaModule, install manually
public class DockerExtServerModule extends AbstractModule {
    @Override
    protected void configure() {
        Multibinder<Set<ServerConf>> devMachineServersSets =
                Multibinder.newSetBinder(binder(),
                                         new TypeLiteral<Set<ServerConf>>() {},
                                         Names.named("machine.docker.dev_machine.machine_servers_sets"));
        devMachineServersSets.addBinding().toProvider(org.eclipse.che.plugin.docker.machine.ext.provider.WsAgentServerConfProvider.class);

        Multibinder<Set<String>> devMachineVolumesSets =
                Multibinder.newSetBinder(binder(),
                                         new TypeLiteral<Set<String>>() {},
                                         Names.named("machine.docker.dev_machine.machine_volumes_sets"));
        devMachineVolumesSets.addBinding()
                             .toProvider(org.eclipse.che.plugin.docker.machine.ext.provider.ExtServerVolumeProvider.class);

        Multibinder<Set<String>> devMachineEnvVarsSets =
                Multibinder.newSetBinder(binder(),
                                         new TypeLiteral<Set<String>>() {},
                                         Names.named("machine.docker.dev_machine.machine_env_sets"));
        devMachineEnvVarsSets.addBinding()
                             .toProvider(org.eclipse.che.plugin.docker.machine.ext.provider.ApiEndpointEnvVariableProvider.class);
        devMachineEnvVarsSets.addBinding()
                             .toProvider(org.eclipse.che.plugin.docker.machine.ext.provider.ProjectsRootEnvVariableProvider.class);
        devMachineEnvVarsSets.addBinding()
                             .toInstance(singleton(CheBootstrap.CHE_LOCAL_CONF_DIR
                                                   + '='
                                                   +
                                                   org.eclipse.che.plugin.docker.machine.ext.provider.DockerExtConfBindingProvider.EXT_CHE_LOCAL_CONF_DIR));

        devMachineVolumesSets.addBinding()
                             .toProvider(org.eclipse.che.plugin.docker.machine.ext.provider.DockerExtConfBindingProvider.class);
    }
}
