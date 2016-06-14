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

import com.google.common.reflect.TypeToken;
import com.google.inject.Inject;
import com.google.inject.Singleton;

import org.eclipse.che.api.core.ServerException;
import org.eclipse.che.api.user.server.dao.PreferenceDao;
import org.eclipse.che.commons.env.EnvironmentContext;
import org.eclipse.che.commons.json.JsonHelper;
import org.eclipse.che.commons.json.JsonParseException;
import org.eclipse.che.dto.server.DtoFactory;
import org.eclipse.che.plugin.docker.client.dto.AuthConfig;
import org.eclipse.che.plugin.docker.client.dto.AuthConfigs;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.UnsupportedEncodingException;
import java.lang.reflect.Type;
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
    private static final Type   DOCKER_REGISTRY_CREDENTIALS_TYPE_TOKEN = new TypeToken<Map<String, AuthConfig>>() {}.getType();

    private static final Logger LOG = LoggerFactory.getLogger(DockerRegistryCredentialsReader.class);

    private PreferenceDao preferenceDao;

    @Inject
    public DockerRegistryCredentialsReader(PreferenceDao preferenceDao) {
        this.preferenceDao = preferenceDao;
    }

    /**
     * Gets and decode credentials for docker registries which was saved in the dashboard.
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
        try {
            return DtoFactory.newDto(AuthConfigs.class).withConfigs(
                    JsonHelper.fromJson(credentials, Map.class, DOCKER_REGISTRY_CREDENTIALS_TYPE_TOKEN));
        } catch (JsonParseException e) {
            LOG.error("Wrong format of credentials");
            return null;
        }
    }

}
