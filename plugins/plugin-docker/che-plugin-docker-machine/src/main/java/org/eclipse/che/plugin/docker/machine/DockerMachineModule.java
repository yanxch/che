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
package org.eclipse.che.plugin.docker.machine;

import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import com.google.inject.TypeLiteral;
import com.google.inject.multibindings.Multibinder;
import com.google.inject.name.Names;

import org.eclipse.che.api.core.model.machine.ServerConf;

import javax.inject.Named;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

/**
 * Module for components that are needed for {@link DockerInstanceProvider}
 *
 * @author Alexander Garagatyi
 */
// Not a DynaModule, install manually
public class DockerMachineModule extends AbstractModule {
    @Override
    protected void configure() {
        Multibinder<org.eclipse.che.api.machine.server.spi.InstanceProvider> machineImageProviderMultibinder =
                Multibinder.newSetBinder(binder(), org.eclipse.che.api.machine.server.spi.InstanceProvider.class);
        machineImageProviderMultibinder.addBinding()
                                       .to(org.eclipse.che.plugin.docker.machine.DockerInstanceProvider.class);

        // Bind multibinders of sets of values instead of multibinders of values to allow
        // insertion of 0, 1+ values
        // Regular multibinder doesn't allow that

        Multibinder<Set<String>> devMachineEnvVarsSets =
                Multibinder.newSetBinder(binder(),
                                         new TypeLiteral<Set<String>>() {},
                                         Names.named("machine.docker.dev_machine.machine_env_sets"));

        Multibinder<Set<String>> machineEnvVarsSets =
                Multibinder.newSetBinder(binder(),
                                         new TypeLiteral<Set<String>>() {},
                                         Names.named("machine.docker.machine_env_sets"));

        Multibinder<Set<ServerConf>> devMachineServersSets =
                Multibinder.newSetBinder(binder(),
                                         new TypeLiteral<Set<ServerConf>>() {},
                                         Names.named("machine.docker.dev_machine.machine_servers_sets"));

        Multibinder<Set<ServerConf>> machineServersSets =
                Multibinder.newSetBinder(binder(),
                                         new TypeLiteral<Set<ServerConf>>() {},
                                         Names.named("machine.docker.machine_servers_sets"));

        Multibinder<Set<String>> devMachineVolumesSets =
                Multibinder.newSetBinder(binder(),
                                         new TypeLiteral<Set<String>>() {},
                                         Names.named("machine.docker.dev_machine.machine_volumes_sets"));

        Multibinder<Set<String>> machineVolumesSets =
                Multibinder.newSetBinder(binder(),
                                         new TypeLiteral<Set<String>>() {},
                                         Names.named("machine.docker.machine_volumes_sets"));

        Multibinder<Set<String>> machineExtraHostsSets =
                Multibinder.newSetBinder(binder(),
                                         new TypeLiteral<Set<String>>() {},
                                         Names.named("machine.docker.machine_extra_hosts_sets"));

        machineExtraHostsSets.addBinding()
                             .toProvider(org.eclipse.che.plugin.docker.machine.UserDefinedMachineExtraHostsProvider.class);
    }

    @Provides
    @Named("machine.docker.dev_machine.machine_env")
    private Set<String> providesDevMachineEnv(@Named("machine.docker.dev_machine.machine_env_sets") Set<Set<String>> sets) {
        return convertSetOfSetsToSet(sets);
    }

    @Provides
    @Named("machine.docker.machine_env")
    private Set<String> providesMachineEnv(@Named("machine.docker.machine_env_sets") Set<Set<String>> sets) {
        return convertSetOfSetsToSet(sets);
    }

    @Provides
    @Named("machine.docker.dev_machine.machine_servers")
    private Set<ServerConf> providesDevMachineServers(@Named("machine.docker.dev_machine.machine_servers_sets") Set<Set<ServerConf>> sets) {
        return convertSetOfSetsToSet(sets);
    }

    @Provides
    @Named("machine.docker.machine_servers")
    private Set<ServerConf> providesMachineServers(@Named("machine.docker.machine_servers_sets") Set<Set<ServerConf>> sets) {
        return convertSetOfSetsToSet(sets);
    }

    @Provides
    @Named("machine.docker.dev_machine.machine_volumes")
    private Set<String> providesDevMachineVolumes(@Named("machine.docker.dev_machine.machine_volumes_sets") Set<Set<String>> sets) {
        return convertSetOfSetsToSet(sets);
    }

    @Provides
    @Named("machine.docker.machine_volumes")
    private Set<String> providesMachineVolumes(@Named("machine.docker.machine_volumes_sets") Set<Set<String>> sets) {
        return convertSetOfSetsToSet(sets);
    }

    @Provides
    @Named("machine.docker.machine_hosts")
    private Set<String> providesMachineExtraHosts(@Named("machine.docker.machine_extra_hosts_sets") Set<Set<String>> sets) {
        return convertSetOfSetsToSet(sets);
    }

    private <T> Set<T> convertSetOfSetsToSet(Set<Set<T>> sets) {
        return Collections.unmodifiableSet(sets.stream().collect(HashSet::new, Set::addAll, Set::addAll));
    }
}
