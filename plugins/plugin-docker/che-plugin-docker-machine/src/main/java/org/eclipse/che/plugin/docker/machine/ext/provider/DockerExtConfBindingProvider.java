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
package org.eclipse.che.plugin.docker.machine.ext.provider;

import org.eclipse.che.api.core.util.SystemInfo;
import org.eclipse.che.commons.lang.IoUtil;
import org.eclipse.che.inject.CheBootstrap;
import org.eclipse.che.plugin.docker.machine.WindowsHostUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Provider;
import javax.inject.Singleton;
import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.Collections;
import java.util.Set;

/**
 * Provides path to the configuration folder on hosted machine for mounting it to docker machine.
 *
 * <p/> It provides different bindings value of Unix and Window OS. <br/>
 * Also it can return null value if binding is useless.
 *
 * @author Sergii Leschenko
 */
@Singleton
public class DockerExtConfBindingProvider implements Provider<Set<String>> {

    public static final String EXT_CHE_LOCAL_CONF_DIR = "/mnt/che/conf";

    private static final String PLUGIN_CONF      = "plugin-conf";
    private static final String CONTAINER_TARGET = ":" + EXT_CHE_LOCAL_CONF_DIR + ":ro";
    private static final Logger LOG              = LoggerFactory.getLogger(DockerExtConfBindingProvider.class);

    private Set<String> volume;

    public DockerExtConfBindingProvider() {
        String localConfDir = System.getenv(CheBootstrap.CHE_LOCAL_CONF_DIR);
        if (localConfDir == null) {
            volume = Collections.emptySet();
        }
        File extConfDir = new File(localConfDir, PLUGIN_CONF);
        if (!extConfDir.isDirectory()) {
            LOG.warn("DockerExtConfBindingProvider: {} set to the {} but it must be directory not file",
                     CheBootstrap.CHE_LOCAL_CONF_DIR,
                     localConfDir);
            volume = Collections.emptySet();
        }

        if (SystemInfo.isWindows()) {
            try {
                final Path cheHome = WindowsHostUtils.ensureCheHomeExist();
                final Path plgConfDir = cheHome.resolve(PLUGIN_CONF);
                IoUtil.copy(extConfDir, plgConfDir.toFile(), null, true);
                volume = Collections.singleton(plgConfDir.toString() + CONTAINER_TARGET);
            } catch (IOException e) {
                LOG.warn(e.getMessage());
                throw new RuntimeException(e);
            }
        } else {
            volume = Collections.singleton(extConfDir.getAbsolutePath() + CONTAINER_TARGET);
        }
    }

    @Override
    public Set<String> get() {
        return volume;
    }
}
