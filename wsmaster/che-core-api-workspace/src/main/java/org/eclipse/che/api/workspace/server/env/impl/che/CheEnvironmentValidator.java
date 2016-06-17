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
package org.eclipse.che.api.workspace.server.env.impl.che;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.google.common.base.Joiner;

import org.eclipse.che.api.core.BadRequestException;
import org.eclipse.che.api.core.ServerException;
import org.eclipse.che.api.core.model.machine.MachineConfig;
import org.eclipse.che.api.core.model.machine.MachineConfig2;
import org.eclipse.che.api.core.model.machine.ServerConf;
import org.eclipse.che.api.core.model.workspace.Environment;
import org.eclipse.che.api.core.model.workspace.EnvironmentRecipe;
import org.eclipse.che.api.machine.server.MachineInstanceProviders;
import org.eclipse.che.api.machine.server.exception.MachineException;
import org.eclipse.che.api.machine.server.model.impl.LimitsImpl;
import org.eclipse.che.api.machine.server.model.impl.MachineConfigImpl;
import org.eclipse.che.api.machine.server.model.impl.MachineSourceImpl;
import org.eclipse.che.api.workspace.server.env.impl.che.opencompose.impl.EnvironmentRecipeContentImpl;
import org.eclipse.che.api.workspace.server.env.impl.che.opencompose.impl.ServiceImpl;
import org.eclipse.che.api.workspace.server.env.spi.EnvironmentValidator;
import org.eclipse.che.api.workspace.server.model.impl.EnvironmentImpl;
import org.eclipse.che.api.workspace.server.model.impl.MachineConfig2Impl;
import org.eclipse.che.commons.env.EnvironmentContext;
import org.eclipse.che.commons.lang.IoUtil;
import org.slf4j.Logger;

import javax.inject.Inject;
import javax.inject.Named;
import javax.ws.rs.core.UriBuilder;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.URI;
import java.net.URL;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static com.google.common.base.MoreObjects.firstNonNull;
import static com.google.common.base.Strings.isNullOrEmpty;
import static java.lang.String.format;
import static java.util.Collections.emptyMap;
import static org.slf4j.LoggerFactory.getLogger;

/**
 * author Alexander Garagatyi
 */
public class CheEnvironmentValidator implements EnvironmentValidator {
    private static final Logger       LOG        = getLogger(CheEnvironmentValidator.class);
    private static final ObjectMapper jsonParser = new ObjectMapper();
    private static final ObjectMapper yamlParser = new ObjectMapper(new YAMLFactory());

    private static final Pattern SERVER_PORT     = Pattern.compile("[1-9]+[0-9]*/(?:tcp|udp)");
    private static final Pattern SERVER_PROTOCOL = Pattern.compile("[a-z][a-z0-9-+.]*");

    private final MachineInstanceProviders machineInstanceProviders;
    private final URI                      apiEndpoint;

    @Inject
    public CheEnvironmentValidator(MachineInstanceProviders machineInstanceProviders,
                                   @Named("api.endpoint") URI apiEndpoint) {
        this.machineInstanceProviders = machineInstanceProviders;
        this.apiEndpoint = apiEndpoint;
    }

    public String getType() {
        return CheEnvironmentEngine.ENVIRONMENT_TYPE;
    }

    // todo validate depends on in the same way as machine name
    // todo validate that env contains machine with name equal to dependency
    // todo use strategy to check if order is valid
    @Override
    public void validate(Environment env) throws BadRequestException, ServerException {
        EnvironmentImpl envImpl = new EnvironmentImpl(env);

        List<? extends MachineConfig> machineConfigs = validateAndReturnMachines(envImpl.getRecipe());

        for (MachineConfig machineConfig : machineConfigs) {
            MachineConfig2 machineConfig2 = env.getMachines().get(machineConfig.getName());
            if (machineConfig2 == null) {
                envImpl.getMachines().put(machineConfig.getName(), new MachineConfig2Impl(null, null));
            }
        }

        if (envImpl.getMachines().size() > machineConfigs.size()) {
            throw new BadRequestException("Environment contains machine description missing in environment recipe");
        }
    }
//todo validate depends on fields to check cyclic dependencies
    // todo should throw another exception in case it is not possible to download recipe
    public List<MachineConfig> parse(EnvironmentRecipe envRecipe) throws IllegalArgumentException, ServerException {
        String recipeContent = getContentOfRecipe(envRecipe);
        EnvironmentRecipeContentImpl environmentRecipeContent = parseEnvironmentRecipeContent(recipeContent, envRecipe.getContentType());
        List<MachineConfigImpl> machineConfigs =
                environmentRecipeContent.getServices()
                                        .entrySet()
                                        .stream()
                                        .map(entry -> {
                                            ServiceImpl service = entry.getValue();

                                            MachineSourceImpl machineSource;
                                            if (service.getImage() != null) {
                                                machineSource = new MachineSourceImpl("image").setLocation(service.getImage());
                                            } else {
                                                machineSource = new MachineSourceImpl("dockerfile").setLocation(service.getDockerfile());
                                            }

                                            return MachineConfigImpl.builder()
                                                                    .setType("docker")
                                                                    .setDev("true".equals(firstNonNull(service.getLabels(),
                                                                                                       emptyMap()).get("dev")))
                                                                    .setSource(machineSource)
                                                                    .setCommand(service.getCommand())
                                                                    .setContainerName(service.getContainerName())
                                                                    .setDependsOn(service.getDependsOn())
                                                                    .setEntrypoint(service.getEntrypoint())
                                                                    .setEnvVariables(service.getEnvironment())
                                                                    .setExpose(service.getExpose())
                                                                    .setLabels(service.getLabels())
                                                                    .setLimits(new LimitsImpl(
                                                                            service.getMemLimit() != null ? service.getMemLimit()
                                                                                                          : 0))
                                                                    .setMachineLinks(service.getLinks())
                                                                    .setName(entry.getKey())
                                                                    .setPorts(service.getPorts())
//                                                                       .setServers() todo + agents
                                                                    .build();
                                        })
                                        .collect(Collectors.toList());
        return machineConfigs.stream().collect(Collectors.toList());
    }

    private EnvironmentRecipeContentImpl parseEnvironmentRecipeContent(String recipeContent, String contentType) {
        if (contentType == null) {
            throw new IllegalArgumentException(
                    "Environment recipe content type required. Supported values are: application/json, application/x-yaml, text/yaml");
        }
        EnvironmentRecipeContentImpl envRecipeContent;
        switch (contentType) {
            case "application/json" :
                try {
                    envRecipeContent = jsonParser.readValue(recipeContent, EnvironmentRecipeContentImpl.class);
                } catch (IOException e) {
                    throw new IllegalArgumentException("Parsing of environment configuration failed. " + e.getLocalizedMessage());
                }
                break;
            case "application/x-yaml" :
            case "text/yaml":
//                Yaml yaml = new Yaml();
//                envRecipeContent = yaml.loadAs(recipeContent, EnvironmentRecipeContentImpl.class);
                try {
                    envRecipeContent = yamlParser.readValue(recipeContent, EnvironmentRecipeContentImpl.class);
                } catch (IOException e) {
                    throw new IllegalArgumentException("Parsing of environment configuration failed. " + e.getLocalizedMessage());
                }
                break;
            default:
                throw new IllegalArgumentException("Provided environment recipe content type '" +
                                                   contentType +
                                                   "' is unsupported. Supported values are: application/json, application/x-yaml");
        }
        return envRecipeContent;
    }

    private List<? extends MachineConfig> validateAndReturnMachines(EnvironmentRecipe envRecipe) throws ServerException, IllegalArgumentException {
        List<? extends MachineConfig> machines = parse(envRecipe);

        //machine configs
        checkArgument(!machines.isEmpty(), "Environment should contain at least 1 machine");

        final long devCount = machines.stream()
                                      .filter(MachineConfig::isDev)
                                      .count();
        checkArgument(devCount == 1,
                      "Environment should contain exactly 1 dev machine, but contains '%d'",
                      devCount);
        for (MachineConfig machineCfg : machines) {
            validateMachine(machineCfg);
        }

        return machines;
    }

    private void validateMachine(MachineConfig machineCfg) throws IllegalArgumentException {
        checkArgument(!isNullOrEmpty(machineCfg.getName()), "Environment contains machine with null or empty name");
        checkNotNull(machineCfg.getSource(), "Environment contains machine without source");
        checkArgument(!(machineCfg.getSource().getContent() == null && machineCfg.getSource().getLocation() == null),
                      "Environment contains machine with source but this source doesn't define a location or content");
        checkArgument(machineInstanceProviders.hasProvider(machineCfg.getType()),
                      "Type %s of machine %s is not supported. Supported values: %s.",
                      machineCfg.getType(),
                      machineCfg.getName(),
                      Joiner.on(", ").join(machineInstanceProviders.getProviderTypes()));

        for (ServerConf serverConf : machineCfg.getServers()) {
            checkArgument(serverConf.getPort() != null && SERVER_PORT.matcher(serverConf.getPort()).matches(),
                          "Machine %s contains server conf with invalid port %s",
                          machineCfg.getName(),
                          serverConf.getPort());
            checkArgument(serverConf.getProtocol() == null || SERVER_PROTOCOL.matcher(serverConf.getProtocol()).matches(),
                          "Machine %s contains server conf with invalid protocol %s",
                          machineCfg.getName(),
                          serverConf.getProtocol());
        }
        for (Map.Entry<String, String> envVariable : machineCfg.getEnvVariables().entrySet()) {
            checkArgument(!isNullOrEmpty(envVariable.getKey()), "Machine %s contains environment variable with null or empty name");
            checkNotNull(envVariable.getValue(), "Machine %s contains environment variable with null value");
        }
    }

    /**
     * Checks that object reference is not null, throws {@link BadRequestException}
     * in the case of null {@code object} with given {@code message}.
     */
    private static void checkNotNull(Object object, String message) throws IllegalArgumentException {
        if (object == null) {
            throw new IllegalArgumentException(message);
        }
    }

    /**
     * Checks that expression is true, throws {@link BadRequestException} otherwise.
     *
     * <p>Exception uses error message built from error message template and error message parameters.
     */
    private static void checkArgument(boolean expression, String errorMessage) {
        if (!expression) {
            throw new IllegalArgumentException(errorMessage);
        }
    }

    /**
     * Checks that expression is true, throws {@link BadRequestException} otherwise.
     *
     * <p>Exception uses error message built from error message template and error message parameters.
     */
    private static void checkArgument(boolean expression, String errorMessageTemplate, Object... errorMessageParams)
            throws IllegalArgumentException {
        if (!expression) {
            throw new IllegalArgumentException(format(errorMessageTemplate, errorMessageParams));
        }
    }

    private String getContentOfRecipe(EnvironmentRecipe environmentRecipe) throws ServerException {
        if (environmentRecipe.getContent() != null) {
            return environmentRecipe.getContent();
        } else {
            return getRecipe(environmentRecipe.getLocation());
        }
    }

    private String getRecipe(String location) throws ServerException {
        URL recipeUrl;
        File file = null;
        try {
            UriBuilder targetUriBuilder = UriBuilder.fromUri(location);
            // add user token to be able to download user's private recipe
            final String apiEndPointHost = apiEndpoint.getHost();
            final String host = targetUriBuilder.build().getHost();
            if (apiEndPointHost.equals(host)) {
                if (EnvironmentContext.getCurrent().getSubject() != null
                    && EnvironmentContext.getCurrent().getSubject().getToken() != null) {
                    targetUriBuilder.queryParam("token", EnvironmentContext.getCurrent().getSubject().getToken());
                }
            }
            recipeUrl = targetUriBuilder.build().toURL();
            file = IoUtil.downloadFileWithRedirect(null, "recipe", null, recipeUrl);

            return IoUtil.readAndCloseQuietly(new FileInputStream(file));
        } catch (IOException | IllegalArgumentException e) {
            throw new MachineException(format("Recipe downloading failed. Recipe url %s. Error: %s",
                                              location,
                                              e.getLocalizedMessage()));
        } finally {
            if (file != null && !file.delete()) {
                LOG.error(String.format("Removal of recipe file %s failed.", file.getAbsolutePath()));
            }
        }
    }
}
