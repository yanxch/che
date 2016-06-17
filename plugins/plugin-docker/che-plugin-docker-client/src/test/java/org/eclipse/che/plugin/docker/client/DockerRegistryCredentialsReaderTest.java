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

import org.eclipse.che.api.core.ServerException;
import org.eclipse.che.api.user.server.dao.PreferenceDao;
import org.eclipse.che.commons.env.EnvironmentContext;
import org.eclipse.che.commons.subject.SubjectImpl;
import org.eclipse.che.dto.server.DtoFactory;
import org.eclipse.che.plugin.docker.client.dto.AuthConfig;
import org.eclipse.che.plugin.docker.client.dto.AuthConfigs;
import org.mockito.Mock;
import org.mockito.testng.MockitoTestNGListener;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Listeners;
import org.testng.annotations.Test;

import java.util.HashMap;
import java.util.Map;

import static org.mockito.Matchers.anyObject;
import static org.mockito.Mockito.when;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertNotNull;
import static org.testng.Assert.assertNull;

/**
 * @author Mykola Morhun
 */
@Listeners(MockitoTestNGListener.class)
public class DockerRegistryCredentialsReaderTest {

    private static final String DOCKER_REGISTRY_CREDENTIALS_KEY = "dockerCredentials";

    @Mock
    private PreferenceDao preferenceDao;

    private DockerRegistryCredentialsReader dockerCredentials;

    @BeforeClass
    private void before() {
        dockerCredentials = new DockerRegistryCredentialsReader(preferenceDao);
    }

    @Test
    public void shouldParseCredentialsFromUserPreferences() throws ServerException {
        String base64encodedCredentials = "eyJyZWdpc3RyeS5jb206NTAwMCI6eyJ1c2VybmFtZSI6ImxvZ2luIiwicGFzc3dvcmQiOiJwYXNzIn19";
        setCredentialsIntoPreferences(base64encodedCredentials);
        String registry = "registry.com:5000";
        AuthConfig authConfig = DtoFactory.newDto(AuthConfig.class).withUsername("login").withPassword("pass");

        AuthConfigs parsedAuthConfigs = dockerCredentials.getDockerCredentialsFromUserPreferences();

        AuthConfig parsedAuthConfig = parsedAuthConfigs.getConfigs().get(registry);

        assertNotNull(parsedAuthConfig);
        assertEquals(parsedAuthConfig.getUsername(), authConfig.getUsername());
        assertEquals(parsedAuthConfig.getPassword(), authConfig.getPassword());
    }

    @Test (expectedExceptions = com.google.gson.JsonSyntaxException.class)
    public void shouldThrowJsonSyntaxExceptionIfDataFormatIsCorruptedInPreferences() throws ServerException {
        String base64encodedCredentials = "sdJfpwJwkek59kafj239lFfkHjhek5l1";
        setCredentialsIntoPreferences(base64encodedCredentials);

        AuthConfigs parsedAuthConfigs = dockerCredentials.getDockerCredentialsFromUserPreferences();

        assertNull(parsedAuthConfigs);
    }

    @Test (expectedExceptions = IllegalStateException.class)
    public void  shouldThrowIllegalStateExceptionIfDataFormatIsWrong() throws ServerException {
        String base64encodedCredentials = "eyJpbnZhbGlkIjoianNvbiJ9";
        setCredentialsIntoPreferences(base64encodedCredentials);

        dockerCredentials.getDockerCredentialsFromUserPreferences();
    }

    private void setCredentialsIntoPreferences(String base64encodedCredentials) throws ServerException {
        Map<String, String> preferences = new HashMap<>();
        preferences.put(DOCKER_REGISTRY_CREDENTIALS_KEY, base64encodedCredentials);

        EnvironmentContext.getCurrent().setSubject(new SubjectImpl("name", "id", "token1234", false));
        when(preferenceDao.getPreferences(anyObject(), anyObject())).thenReturn(preferences);
    }

}
