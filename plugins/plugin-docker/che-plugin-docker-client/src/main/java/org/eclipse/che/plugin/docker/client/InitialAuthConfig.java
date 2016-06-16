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

import org.eclipse.che.commons.annotation.Nullable;
import org.eclipse.che.inject.ConfigurationProperties;
import org.eclipse.che.plugin.docker.client.dto.AuthConfig;
import org.eclipse.che.plugin.docker.client.dto.AuthConfigs;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.List;
import java.util.Map;

import static com.google.common.base.Strings.isNullOrEmpty;
import static java.util.stream.Collectors.toList;
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
    @VisibleForTesting
    protected static final String CONFIG_PREFIX                = "docker.registry.auth.";
    @VisibleForTesting
    protected static final String CONFIGURATION_PREFIX_PATTERN = "docker\\.registry\\.auth\\..+\\..+";

    private Map<String, AuthConfig> configMap;

    /** For testing purposes */
    public InitialAuthConfig() {
    }

    @Inject
    public InitialAuthConfig(ConfigurationProperties configProperties) {
        Map<String, String> properties = configProperties.getProperties(CONFIGURATION_PREFIX_PATTERN)
                                                         .entrySet()
                                                         .stream()
                                                         .collect(toMap(e -> e.getKey().replaceFirst(CONFIG_PREFIX, ""),
                                                                        Map.Entry::getValue));

        List<String> registriesName = properties.entrySet()
                                                .stream()
                                                .filter(elem -> elem.getKey().endsWith(".url"))
                                                .map(prop -> {
                                                    String[] keyPart = prop.getKey().split("\\.url");
                                                    return keyPart.length == 0 ? "" : keyPart[0];
                                                })
                                                .filter(elem -> !elem.isEmpty())
                                                .collect(toList());

        configMap = registriesName.stream()
                                  .map(registry -> createConfig(properties.get(registry + ".url"),
                                                                properties.get(registry + ".username"),
                                                                properties.get(registry + ".password")))
                                  .collect(toMap(AuthConfig::getServeraddress, elem -> elem));
    }

    @Nullable
    private static AuthConfig createConfig(String serverAddress, String username, String password) {
        if (isNullOrEmpty(serverAddress) || isNullOrEmpty(username) && isNullOrEmpty(password)) {
            return null;
        }
        return newDto(AuthConfig.class).withServeraddress(serverAddress).withUsername(username).withPassword(password);
    }

    public AuthConfigs getAuthConfigs() {
        AuthConfigs authConfigs = newDto(AuthConfigs.class);
        authConfigs.getConfigs().putAll(configMap);
        return authConfigs;
    }

}
