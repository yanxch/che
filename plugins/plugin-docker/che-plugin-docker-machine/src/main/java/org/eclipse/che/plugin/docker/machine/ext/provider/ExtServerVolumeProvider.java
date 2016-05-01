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
import org.eclipse.che.plugin.docker.machine.WindowsHostUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Provider;
import javax.inject.Singleton;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.Set;

import static java.nio.file.StandardCopyOption.REPLACE_EXISTING;

/**
 * Reads path to extensions server archive to mount it to docker machine
 *
 * <p>On Windows hosts MUST be locate in "user.home" directory in case limitation windows+docker.
 *
 * @author Vitalii Parfonov
 * @author Alexander Garagatyi
 */
@Singleton
public class ExtServerVolumeProvider implements Provider<Set<String>> {

    private static final String CONTAINER_TARGET = ":/mnt/che/ws-agent.zip:ro,Z";
    private static final String WS_AGENT         = "ws-agent.zip";

    private static final Logger LOG = LoggerFactory.getLogger(ExtServerVolumeProvider.class);

    private Set<String> volume;

    @Inject
    public ExtServerVolumeProvider(@Named("machine.server.ext.archive") String extServerArchivePath) {
        if (SystemInfo.isWindows()) {
            try {
                final Path cheHome = WindowsHostUtils.ensureCheHomeExist();
                final Path path = Files.copy(Paths.get(extServerArchivePath), cheHome.resolve(WS_AGENT), REPLACE_EXISTING);
                volume = Collections.singleton(path.toString() + CONTAINER_TARGET);
            } catch (IOException e) {
                LOG.warn(e.getMessage());
                throw new RuntimeException(e);
            }
        } else {
            volume = Collections.singleton(extServerArchivePath + CONTAINER_TARGET);
        }
    }

    @Override
    public Set<String> get() {
        return volume;
    }
}
