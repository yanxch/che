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

import com.google.common.annotations.VisibleForTesting;

import org.eclipse.che.api.core.ServerException;
import org.eclipse.che.commons.annotation.Nullable;
import org.eclipse.che.inject.ConfigurationProperties;
import org.eclipse.che.plugin.docker.client.dto.AuthConfig;
import org.eclipse.che.plugin.docker.client.dto.AuthConfigs;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import static java.lang.String.format;
import static java.util.stream.Collectors.toMap;
import static org.eclipse.che.dto.server.DtoFactory.newDto;

/**
 * Collects auth configurations for private docker registries. Credential might be configured in .properties files, see details {@link
 * org.eclipse.che.inject.CheBootstrap}. Credentials configured as (key=value) pairs. Key is string that starts with prefix
 * {@code docker.registry.auth.} followed by url and credentials of docker registry server.
 * <pre>{@code
 * docker.registry.auth.url=localhost:5000
 * docker.registry.auth.username=user1
 * docker.registry.auth.password=pass
 * }</pre>
 *
 * @author Alexander Garagatyi
 * @author Alexander Andrienko
 */
@Singleton
public class InitialAuthConfig {
    private static final String URL       = "url";
    private static final String USER_NAME = "username";
    private static final String PASSWORD  = "password";

    @VisibleForTesting
    protected static final String CONFIG_PREFIX                      = "docker.registry.auth.";
    @VisibleForTesting
    protected static final String CONFIGURATION_PREFIX_PATTERN       = "docker\\.registry\\.auth\\..+";
    @VisibleForTesting
    protected static final String VALID_DOCKER_PROPERTY_NAME_EXAMPLE = CONFIG_PREFIX + "registry_name.parameter_name";

    private final Map<String, AuthConfig> configMap = new HashMap<>();

    /** For testing purposes */
    public InitialAuthConfig() {
    }

    @Inject
    public InitialAuthConfig(ConfigurationProperties properties) throws ServerException {
        Map<String, String> classifierMap = properties.getProperties(CONFIGURATION_PREFIX_PATTERN)
                                                      .entrySet()
                                                      .stream()
                                                      .filter(e -> e.getValue() != null && !e.getValue().isEmpty())
                                                      .collect(toMap(e -> e.getKey().replaceFirst(CONFIG_PREFIX, ""), Map.Entry::getValue));

        Set<String> registryNames = new HashSet<>();
        for (Map.Entry<String, String> property : classifierMap.entrySet()) {
            String registryName = getRegistryName(property.getKey());
            registryNames.add(registryName);
        }

        for (String registryName : registryNames) {
            String serverAddress = classifierMap.get(registryName + "." + URL);
            String userName = classifierMap.get(registryName + "." + USER_NAME);
            String password = classifierMap.get(registryName + "." + PASSWORD);

            AuthConfig authConfig = createConfig(serverAddress, userName, password, registryName);
            configMap.put(serverAddress, authConfig);
        }
    }

    private String getRegistryName(String classifier) throws ServerException {
        String[] parts = classifier.split("\\.");
        if (parts.length < 2) {
            throw new ServerException(format("You missed '.' for property '%s'. Valid format for docker registry property is '%s'",
                                             CONFIG_PREFIX + classifier, VALID_DOCKER_PROPERTY_NAME_EXAMPLE));
        }
        if (parts.length > 2) {
            throw new ServerException(format("You set redundant '.' for property '%s'. Valid format for docker registry property is '%s'",
                                             CONFIG_PREFIX + classifier, VALID_DOCKER_PROPERTY_NAME_EXAMPLE));
        }
        return parts[0];
    }

    @Nullable
    private static AuthConfig createConfig(String serverAddress, String username, String password, String registry) throws ServerException {
        if (serverAddress == null) {
            throw new ServerException("You missed property " + CONFIG_PREFIX + registry + "." + URL);
        }
        if (username == null) {
            throw new ServerException("You missed property " + CONFIG_PREFIX + registry + "." + USER_NAME);
        }
        if (password == null) {
            throw new ServerException("You missed property " + CONFIG_PREFIX + registry + "." + PASSWORD);
        }
        return newDto(AuthConfig.class).withServeraddress(serverAddress).withUsername(username).withPassword(password);
    }

    public AuthConfigs getAuthConfigs() {
        AuthConfigs authConfigs = newDto(AuthConfigs.class);
        authConfigs.getConfigs().putAll(configMap);
        return authConfigs;
    }

}
