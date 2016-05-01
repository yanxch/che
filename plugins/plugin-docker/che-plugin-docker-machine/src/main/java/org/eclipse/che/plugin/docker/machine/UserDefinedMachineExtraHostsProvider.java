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

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Provider;
import javax.inject.Singleton;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

/**
 * Parses property {@code machine.docker.machine_extra_hosts} into set of hosts values.
 * <p/>
 * Value of property contains list of comma-separated hosts entries. Can be {@code NULL} or empty.
 *
 * @author Alexander Garagatyi
 */
@Singleton
public class UserDefinedMachineExtraHostsProvider implements Provider<Set<String>> {
    private Set<String> userDefinedExtraHosts;

    @Inject
    public UserDefinedMachineExtraHostsProvider(@Named("machine.docker.machine_extra_hosts") String userDefinedExtraHosts) {
        if (userDefinedExtraHosts == null || userDefinedExtraHosts.isEmpty()) {
            this.userDefinedExtraHosts = Collections.emptySet();
        } else {
            this.userDefinedExtraHosts = new HashSet<>(Arrays.asList(userDefinedExtraHosts.split(",")));
        }
    }

    @Override
    public Set<String> get() {
        return userDefinedExtraHosts;
    }
}
