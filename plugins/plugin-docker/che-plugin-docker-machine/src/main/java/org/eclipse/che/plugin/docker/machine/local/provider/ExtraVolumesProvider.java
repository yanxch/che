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
package org.eclipse.che.plugin.docker.machine.local.provider;

import com.google.inject.Inject;

import javax.inject.Named;
import javax.inject.Provider;
import javax.inject.Singleton;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

/**
 * Provides volume configuration of machine for any local directories
 * that a user may want to mount into a dev machine.
 * <p/>
 * {@code machine.server.extra.volumes} property is optional and provided as
 * <br/>/path/on/host:/path/in/container
 * or
 * <br/>/path/on/host:/path/in/container,/anotherPath/on/host:/anotherPath/in/container,...
 * <br/>Property can be empty or {@code NULL} or not set
 *
 * @author Alexander Garagatyi
 */
@Singleton
public class ExtraVolumesProvider implements Provider<Set<String>> {
    private Set<String> userDefinedVolumes;

    @Inject(optional = true)
    public ExtraVolumesProvider(@Named("machine.server.extra.volumes") String volumes) {
        if (volumes == null || volumes.isEmpty()) {
            this.userDefinedVolumes = Collections.emptySet();
        } else {
            this.userDefinedVolumes = new HashSet<>(Arrays.asList(volumes.split(",")));
        }
    }

    @Override
    public Set<String> get() {
        return userDefinedVolumes;
    }
}
