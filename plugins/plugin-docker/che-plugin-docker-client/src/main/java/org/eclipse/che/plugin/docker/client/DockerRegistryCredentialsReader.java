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
package org.eclipse.che.plugin.docker.client;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import org.eclipse.che.api.core.ServerException;
import org.eclipse.che.api.user.server.dao.PreferenceDao;
import org.eclipse.che.commons.env.EnvironmentContext;
import org.eclipse.che.dto.server.DtoFactory;
import org.eclipse.che.plugin.docker.client.dto.AuthConfig;
import org.eclipse.che.plugin.docker.client.dto.AuthConfigs;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.UnsupportedEncodingException;
import java.util.Base64;
import java.util.Map;

/**
 * Class for reading credentials for docker registries from user preferences.
 *
 * @author Mykola Morhun
 */
@Singleton
public class DockerRegistryCredentialsReader {
    private static final String DOCKER_REGISTRY_CREDENTIALS_KEY        = "dockerCredentials";

    private static final Logger LOG = LoggerFactory.getLogger(DockerRegistryCredentialsReader.class);

    private PreferenceDao preferenceDao;

    @Inject
    public DockerRegistryCredentialsReader(PreferenceDao preferenceDao) {
        this.preferenceDao = preferenceDao;
    }

    /**
     * Gets and decode credentials for docker registries which was saved in the dashboard.
     *
     * @throws IllegalStateException
     *         when user preferences contains json which do not represent AuthConfigs
     * @throws com.google.gson.JsonSyntaxException
     *         when data format from user preferences is corrupted and contains invalid json
     */
    public AuthConfigs getDockerCredentialsFromUserPreferences() {
        String encodedCredentials;
        try {
            encodedCredentials = preferenceDao.getPreferences(EnvironmentContext.getCurrent().getSubject().getUserId(),
                                                              DOCKER_REGISTRY_CREDENTIALS_KEY)
                                              .get(DOCKER_REGISTRY_CREDENTIALS_KEY);
        } catch (ServerException e) {
            LOG.error("Cannot read credentials from user preferences");
            return null;
        }

        String credentials = null;
        try {
            credentials = new String(Base64.getDecoder().decode(encodedCredentials), "UTF-8");
        } catch (UnsupportedEncodingException ignore) {
        }

        return DtoFactory.newDto(AuthConfigs.class).withConfigs(
                DtoFactory.getInstance().createMapDtoFromJson(credentials, AuthConfig.class));
    }

}
