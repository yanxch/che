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

import com.google.common.base.Strings;
import com.google.common.collect.Maps;
import com.google.common.collect.ObjectArrays;
import com.google.common.collect.Sets;
import com.google.inject.Inject;

import org.eclipse.che.api.core.NotFoundException;
import org.eclipse.che.api.core.model.machine.Machine;
import org.eclipse.che.api.core.model.machine.MachineConfig;
import org.eclipse.che.api.core.model.machine.MachineSource;
import org.eclipse.che.api.core.model.machine.Recipe;
import org.eclipse.che.api.core.model.machine.ServerConf;
import org.eclipse.che.api.core.util.FileCleaner;
import org.eclipse.che.api.core.util.LineConsumer;
import org.eclipse.che.api.core.util.SystemInfo;
import org.eclipse.che.api.machine.server.exception.InvalidRecipeException;
import org.eclipse.che.api.machine.server.exception.MachineException;
import org.eclipse.che.api.machine.server.exception.SnapshotException;
import org.eclipse.che.api.machine.server.exception.UnsupportedRecipeException;
import org.eclipse.che.api.machine.server.spi.Instance;
import org.eclipse.che.api.machine.server.spi.InstanceProvider;
import org.eclipse.che.api.machine.server.util.RecipeRetriever;
import org.eclipse.che.commons.annotation.Nullable;
import org.eclipse.che.commons.env.EnvironmentContext;
import org.eclipse.che.commons.lang.IoUtil;
import org.eclipse.che.plugin.docker.client.DockerConnector;
import org.eclipse.che.plugin.docker.client.DockerConnectorConfiguration;
import org.eclipse.che.plugin.docker.client.DockerFileException;
import org.eclipse.che.plugin.docker.client.Dockerfile;
import org.eclipse.che.plugin.docker.client.DockerfileParser;
import org.eclipse.che.plugin.docker.client.ProgressLineFormatterImpl;
import org.eclipse.che.plugin.docker.client.ProgressMonitor;
import org.eclipse.che.plugin.docker.client.json.ContainerConfig;
import org.eclipse.che.plugin.docker.client.json.Filters;
import org.eclipse.che.plugin.docker.client.json.HostConfig;
import org.eclipse.che.plugin.docker.client.json.container.NetworkingConfig;
import org.eclipse.che.plugin.docker.client.json.network.EndpointConfig;
import org.eclipse.che.plugin.docker.client.json.network.Network;
import org.eclipse.che.plugin.docker.client.json.network.NewNetwork;
import org.eclipse.che.plugin.docker.client.params.BuildImageParams;
import org.eclipse.che.plugin.docker.client.params.CreateContainerParams;
import org.eclipse.che.plugin.docker.client.params.PullParams;
import org.eclipse.che.plugin.docker.client.params.RemoveImageParams;
import org.eclipse.che.plugin.docker.client.params.StartContainerParams;
import org.eclipse.che.plugin.docker.client.params.TagParams;
import org.eclipse.che.plugin.docker.client.params.network.CreateNetworkParams;
import org.eclipse.che.plugin.docker.client.params.network.GetNetworksParams;
import org.eclipse.che.plugin.docker.machine.node.DockerNode;
import org.eclipse.che.plugin.docker.machine.node.WorkspaceFolderPathProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Named;
import javax.ws.rs.core.UriBuilder;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import static com.google.common.base.Strings.isNullOrEmpty;
import static java.lang.String.format;
import static java.util.Collections.singletonList;
import static java.util.Collections.singletonMap;
import static org.eclipse.che.plugin.docker.machine.DockerInstance.LATEST_TAG;

/**
 * Docker implementation of {@link InstanceProvider}
 *
 * @author andrew00x
 * @author Alexander Garagatyi
 * @author Roman Iuvshyn
 */
public class DockerInstanceProvider implements InstanceProvider {
    private static final Logger LOG = LoggerFactory.getLogger(DockerInstanceProvider.class);

    /**
     * dockerfile type support with recipe being a content of Dockerfile
     */
    public static final String DOCKER_FILE_TYPE = "dockerfile";

    /**
     * image type support with recipe script being the name of the repository + image name
     */
    public static final String DOCKER_IMAGE_TYPE = "image";

    private final DockerConnector                  docker;
    private final DockerInstanceStopDetector       dockerInstanceStopDetector;
    private final DockerContainerNameGenerator     containerNameGenerator;
    private final WorkspaceFolderPathProvider      workspaceFolderPathProvider;
    private final boolean                          doForcePullOnBuild;
    private final boolean                          privilegeMode;
    private final Set<String>                      supportedRecipeTypes;
    private final DockerMachineFactory             dockerMachineFactory;
    private final Map<String, Map<String, String>> devMachinePortsToExpose;
    private final Map<String, Map<String, String>> commonMachinePortsToExpose;
    private final String[]                         devMachineSystemVolumes;
    private final String[]                         commonMachineSystemVolumes;
    private final Set<String>                      devMachineEnvVariables;
    private final Set<String>                      commonMachineEnvVariables;
    private final String[]                         allMachinesExtraHosts;
    private final String                           projectFolderPath;
    private final boolean                          snapshotUseRegistry;
    private final RecipeRetriever                  recipeRetriever;
    private final double                           memorySwapMultiplier;

    @Inject
    public DockerInstanceProvider(DockerConnector docker,
                                  DockerConnectorConfiguration dockerConnectorConfiguration,
                                  DockerMachineFactory dockerMachineFactory,
                                  DockerInstanceStopDetector dockerInstanceStopDetector,
                                  DockerContainerNameGenerator containerNameGenerator,
                                  RecipeRetriever recipeRetriever,
                                  @Named("machine.docker.dev_machine.machine_servers") Set<ServerConf> devMachineServers,
                                  @Named("machine.docker.machine_servers") Set<ServerConf> allMachinesServers,
                                  @Named("machine.docker.dev_machine.machine_volumes") Set<String> devMachineSystemVolumes,
                                  @Named("machine.docker.machine_volumes") Set<String> allMachinesSystemVolumes,
                                  @Nullable @Named("machine.docker.machine_extra_hosts") String allMachinesExtraHosts,
                                  WorkspaceFolderPathProvider workspaceFolderPathProvider,
                                  @Named("che.machine.projects.internal.storage") String projectFolderPath,
                                  @Named("machine.docker.pull_image") boolean doForcePullOnBuild,
                                  @Named("machine.docker.privilege_mode") boolean privilegeMode,
                                  @Named("machine.docker.dev_machine.machine_env") Set<String> devMachineEnvVariables,
                                  @Named("machine.docker.machine_env") Set<String> allMachinesEnvVariables,
                                  @Named("machine.docker.snapshot_use_registry") boolean snapshotUseRegistry,
                                  @Named("machine.docker.memory_swap_multiplier") double memorySwapMultiplier) throws IOException {
        this.docker = docker;
        this.dockerMachineFactory = dockerMachineFactory;
        this.dockerInstanceStopDetector = dockerInstanceStopDetector;
        this.containerNameGenerator = containerNameGenerator;
        this.recipeRetriever = recipeRetriever;
        this.workspaceFolderPathProvider = workspaceFolderPathProvider;
        this.doForcePullOnBuild = doForcePullOnBuild;
        this.privilegeMode = privilegeMode;
        this.supportedRecipeTypes = Sets.newHashSet(DOCKER_FILE_TYPE, DOCKER_IMAGE_TYPE);
        this.projectFolderPath = projectFolderPath;
        this.snapshotUseRegistry = snapshotUseRegistry;
        // usecases:
        //  -1  enable unlimited swap
        //  0   disable swap
        //  0.5 enable swap with size equal to half of current memory size
        //  1   enable swap with size equal to current memory size
        //
        //  according to docker docs field  memorySwap should be equal to memory+swap
        //  we calculate this field as memorySwap=memory * (1+ multiplier) so we just add +1 to multiplier
        this.memorySwapMultiplier = memorySwapMultiplier == -1 ? -1 : memorySwapMultiplier + 1;

        allMachinesSystemVolumes = removeEmptyAndNullValues(allMachinesSystemVolumes);
        devMachineSystemVolumes = removeEmptyAndNullValues(devMachineSystemVolumes);
        if (SystemInfo.isWindows()) {
            allMachinesSystemVolumes = escapePaths(allMachinesSystemVolumes);
            devMachineSystemVolumes = escapePaths(devMachineSystemVolumes);
        }
        this.commonMachineSystemVolumes = allMachinesSystemVolumes.toArray(new String[allMachinesSystemVolumes.size()]);
        final Set<String> devMachineVolumes = Sets.newHashSetWithExpectedSize(allMachinesSystemVolumes.size()
                                                                              + devMachineSystemVolumes.size());
        devMachineVolumes.addAll(allMachinesSystemVolumes);
        devMachineVolumes.addAll(devMachineSystemVolumes);
        this.devMachineSystemVolumes = devMachineVolumes.toArray(new String[devMachineVolumes.size()]);

        this.devMachinePortsToExpose = Maps.newHashMapWithExpectedSize(allMachinesServers.size() + devMachineServers.size());
        this.commonMachinePortsToExpose = Maps.newHashMapWithExpectedSize(allMachinesServers.size());
        for (ServerConf serverConf : devMachineServers) {
            devMachinePortsToExpose.put(serverConf.getPort(), Collections.emptyMap());
        }
        for (ServerConf serverConf : allMachinesServers) {
            commonMachinePortsToExpose.put(serverConf.getPort(), Collections.emptyMap());
            devMachinePortsToExpose.put(serverConf.getPort(), Collections.emptyMap());
        }

        allMachinesEnvVariables = removeEmptyAndNullValues(allMachinesEnvVariables);
        devMachineEnvVariables = removeEmptyAndNullValues(devMachineEnvVariables);
        this.commonMachineEnvVariables = allMachinesEnvVariables;
        final HashSet<String> envVariablesForDevMachine = Sets.newHashSetWithExpectedSize(allMachinesEnvVariables.size() +
                                                                                          devMachineEnvVariables.size());
        envVariablesForDevMachine.addAll(allMachinesEnvVariables);
        envVariablesForDevMachine.addAll(devMachineEnvVariables);
        this.devMachineEnvVariables = envVariablesForDevMachine;

        // always add the docker host
        String dockerHost = DockerInstanceRuntimeInfo.CHE_HOST.concat(":").concat(dockerConnectorConfiguration.getDockerHostIp());
        if (isNullOrEmpty(allMachinesExtraHosts)) {
            this.allMachinesExtraHosts = new String[] {dockerHost};
        } else {
            this.allMachinesExtraHosts = ObjectArrays.concat(allMachinesExtraHosts.split(","), dockerHost);
        }
    }

    /**
     * Escape paths for Windows system with boot@docker according to rules given here :
     * https://github.com/boot2docker/boot2docker/blob/master/README.md#virtualbox-guest-additions
     *
     * @param paths
     *         set of paths to escape
     * @return set of escaped path
     */
    protected Set<String> escapePaths(Set<String> paths) {
        return paths.stream().map(this::escapePath).collect(Collectors.toSet());
    }

    /**
     * Escape path for Windows system with boot@docker according to rules given here :
     * https://github.com/boot2docker/boot2docker/blob/master/README.md#virtualbox-guest-additions
     *
     * @param path
     *         path to escape
     * @return escaped path
     */
    protected String escapePath(String path) {
        String esc;
        if (path.indexOf(":") == 1) {
            //check and replace only occurrence of ":" after disk label on Windows host (e.g. C:/)
            // but keep other occurrences it can be marker for docker mount volumes
            // (e.g. /path/dir/from/host:/name/of/dir/in/container                                               )
            esc = path.replaceFirst(":", "").replace('\\', '/');
            esc = Character.toLowerCase(esc.charAt(0)) + esc.substring(1); //letter of disk mark must be lower case
        } else {
            esc = path.replace('\\', '/');
        }
        if (!esc.startsWith("/")) {
            esc = "/" + esc;
        }
        return esc;
    }


    @Override
    public String getType() {
        return "docker";
    }

    @Override
    public Set<String> getRecipeTypes() {
        return supportedRecipeTypes;
    }



    /**
     * Creates instance from scratch or by reusing a previously one by using specified {@link MachineSource}
     * data in {@link MachineConfig}.
     *
     * @param machine
     *         machine description
     * @param creationLogsOutput
     *         output for instance creation logs
     * @return newly created {@link Instance}
     * @throws UnsupportedRecipeException
     *         if specified {@code recipe} is not supported
     * @throws InvalidRecipeException
     *         if {@code recipe} is invalid
     * @throws NotFoundException
     *         if instance described by {@link MachineSource} doesn't exists
     * @throws MachineException
     *         if other error occurs
     */
    @Override
    public Instance createInstance(final Machine machine, final LineConsumer creationLogsOutput)
            throws UnsupportedRecipeException, InvalidRecipeException, NotFoundException, MachineException {

        // based on machine source, do the right steps
        MachineConfig machineConfig = machine.getConfig();
        MachineSource machineSource = machineConfig.getSource();
        String type = machineSource.getType();

        // create container machine name
        final String userName = EnvironmentContext.getCurrent().getSubject().getUserName();
        final String machineContainerName = containerNameGenerator.generateContainerName(machine.getWorkspaceId(),
                                                                                         machine.getId(),
                                                                                         userName,
                                                                                         machine.getConfig().getName());
        // get recipe
        // - it's a dockerfile type:
        //    - location defined : download this location and get script as recipe
        //    - content defined  : use this content as recipe script
        // - it's an image:
        //    - use location of image ([registry:port]/<repository-image>[:tag][@digest])
        final Recipe recipe;
        if (DOCKER_FILE_TYPE.equals(type)) {
            recipe = this.recipeRetriever.getRecipe(machineConfig);
        } else if (DOCKER_IMAGE_TYPE.equals(type)) {
            if (isNullOrEmpty(machineSource.getLocation())) {
                throw new InvalidRecipeException(String.format("The type '%s' needs to be used with a location, not with any other parameter. Found '%s'.", type, machineSource));
            }
            return createInstanceFromImage(machine, machineContainerName, creationLogsOutput);
        } else {
            // not supported
            throw new UnsupportedRecipeException("The type '" + type + "' is not supported");
        }
        final Dockerfile dockerfile = parseRecipe(recipe);

        final String machineImageName = "eclipse-che/" + machineContainerName;
        final long memoryLimit = (long)machine.getConfig().getLimits().getRam() * 1024 * 1024;

        buildImage(dockerfile, creationLogsOutput, machineImageName, doForcePullOnBuild, memoryLimit, -1);

        return createInstance(machineContainerName,
                              machine,
                              machineImageName,
                              creationLogsOutput);
    }
//todo do not publish all exposed ports to host, publish only ports from ports field
    protected Instance createInstanceFromImage(final Machine machine, String machineContainerName,
                                               final LineConsumer creationLogsOutput) throws NotFoundException,
                                                                                             MachineException {
        final DockerMachineSource dockerMachineSource = new DockerMachineSource(machine.getConfig().getSource());

        // todo bug is here; impossible to create machines from image (not snapshot case).
        // todo Comment snapshot workaround to fix it for demo
//        if (snapshotUseRegistry) {
        pullImage(dockerMachineSource, creationLogsOutput);
//        }

        final String machineImageName = "eclipse-che/" + machineContainerName;
        final String fullNameOfPulledImage = dockerMachineSource.getLocation(false);
        try {
            // tag image with generated name to allow sysadmin recognize it
            docker.tag(TagParams.create(fullNameOfPulledImage, machineImageName));
        } catch (IOException e) {
            LOG.error(e.getLocalizedMessage(), e);
            throw new MachineException("Can't create machine from image.");
        }
        try {
            // remove unneeded tag
            docker.removeImage(fullNameOfPulledImage);
        } catch (IOException e) {
            LOG.error(e.getLocalizedMessage(), e);
        }

        return createInstance(machineContainerName,
                              machine,
                              machineImageName,
                              creationLogsOutput);
    }

    private Dockerfile parseRecipe(final Recipe recipe) throws InvalidRecipeException {
        final Dockerfile dockerfile = getDockerFile(recipe);
        if (dockerfile.getImages().isEmpty()) {
            throw new InvalidRecipeException("Unable build docker based machine, Dockerfile found but it doesn't contain base image.");
        }
        if (dockerfile.getImages().size() > 1) {
            throw new InvalidRecipeException(
                    "Unable build docker based machine, Dockerfile found but it contains more than one instruction 'FROM'.");
        }
        return dockerfile;
    }

    private Dockerfile getDockerFile(final Recipe recipe) throws InvalidRecipeException {
        if (recipe.getScript() == null) {
            throw new InvalidRecipeException("Unable build docker based machine, recipe isn't set or doesn't provide Dockerfile and " +
                                             "no Dockerfile found in the list of files attached to this builder.");
        }
        try {
            return DockerfileParser.parse(recipe.getScript());
        } catch (DockerFileException e) {
            LOG.debug(e.getLocalizedMessage(), e);
            throw new InvalidRecipeException("Unable build docker based machine. " + e.getMessage());
        }
    }

    protected void buildImage(final Dockerfile dockerfile,
                              final LineConsumer creationLogsOutput,
                              final String imageName,
                              final boolean doForcePullOnBuild,
                              final long memoryLimit,
                              final long memorySwapLimit)
            throws MachineException {

        File workDir = null;
        try {
            // build docker image
            workDir = Files.createTempDirectory(null).toFile();
            final File dockerfileFile = new File(workDir, "Dockerfile");
            dockerfile.writeDockerfile(dockerfileFile);
            final List<File> files = new LinkedList<>();
            //noinspection ConstantConditions
            Collections.addAll(files, workDir.listFiles());
            final ProgressLineFormatterImpl progressLineFormatter = new ProgressLineFormatterImpl();
            final ProgressMonitor progressMonitor = currentProgressStatus -> {
                try {
                    creationLogsOutput.writeLine(progressLineFormatter.format(currentProgressStatus));
                } catch (IOException e) {
                    LOG.error(e.getLocalizedMessage(), e);
                }
            };
            docker.buildImage(BuildImageParams.create(files.toArray(new File[files.size()]))
                                              .withRepository(imageName)
                                              .withDoForcePull(doForcePullOnBuild)
                                              .withMemoryLimit(memoryLimit)
                                              .withMemorySwapLimit(memorySwapLimit),
                              progressMonitor);
        } catch (IOException | InterruptedException e) {
            throw new MachineException(e.getMessage(), e);
        } finally {
            if (workDir != null) {
                FileCleaner.addFile(workDir);
            }
        }
    }

    private void pullImage(final DockerMachineSource dockerMachineSource, final LineConsumer creationLogsOutput) throws MachineException {
        if (dockerMachineSource.getRepository() == null) {
            throw new MachineException(String.format("Machine creation failed. Machine source is invalid. No repository is defined. Found %s.", dockerMachineSource));
        }

        final String tag;
        if (isNullOrEmpty(dockerMachineSource.getTag())) {
            tag = LATEST_TAG;
        } else {
            tag = dockerMachineSource.getTag();
        }
        PullParams pullParams = PullParams.create(dockerMachineSource.getRepository())
                                          .withTag(tag)
                                          .withRegistry(dockerMachineSource.getRegistry());
        try {
            final ProgressLineFormatterImpl progressLineFormatter = new ProgressLineFormatterImpl();
            docker.pull(pullParams,
                        currentProgressStatus -> {
                            try {
                                creationLogsOutput.writeLine(progressLineFormatter.format(currentProgressStatus));
                            } catch (IOException e) {
                                LOG.error(e.getLocalizedMessage(), e);
                            }
                        });
        } catch (IOException | InterruptedException e) {
            throw new MachineException(e.getLocalizedMessage(), e);
        }
    }

    /**
     * Removes snapshot of the instance in implementation specific way.
     *
     * @param machineSource
     *         contains implementation specific key of the snapshot of the instance that should be removed
     * @throws SnapshotException
     *         if exception occurs on instance snapshot removal
     */
    @Override
    public void removeInstanceSnapshot(final MachineSource machineSource) throws SnapshotException {
        // use registry API directly because docker doesn't have such API yet
        // https://github.com/docker/docker-registry/issues/45
        final DockerMachineSource dockerMachineSource;
        try {
            dockerMachineSource = new DockerMachineSource(machineSource);
        } catch (MachineException e) {
            throw new SnapshotException(e);
        }

        if (!snapshotUseRegistry) {
            try {
                docker.removeImage(RemoveImageParams.create(dockerMachineSource.getLocation(false)));
            } catch (IOException ignore) {
            }
            return;
        }

        final String registry = dockerMachineSource.getRegistry();
        final String repository = dockerMachineSource.getRepository();
        if (registry == null || repository == null) {
            LOG.error("Failed to remove instance snapshot: invalid machine source: {}", dockerMachineSource);
            throw new SnapshotException("Snapshot removing failed. Snapshot attributes are not valid");
        }

        try {
            URL url = UriBuilder.fromUri("http://" + registry) // TODO make possible to use https here
                                .path("/v2/{repository}/manifests/{digest}")
                                .build(repository, dockerMachineSource.getDigest())
                                .toURL();
            final HttpURLConnection conn = (HttpURLConnection)url.openConnection();
            try {
                conn.setConnectTimeout(30 * 1000);
                conn.setRequestMethod("DELETE");
                // TODO add auth header for secured registry
                // conn.setRequestProperty("Authorization", authHeader);
                final int responseCode = conn.getResponseCode();
                if ((responseCode / 100) != 2) {
                    InputStream in = conn.getErrorStream();
                    if (in == null) {
                        in = conn.getInputStream();
                    }
                    LOG.error("An error occurred while deleting snapshot with url: {}\nError stream: {}",
                              url,
                              IoUtil.readAndCloseQuietly(in));
                    throw new SnapshotException("Internal server error occurs. Can't remove snapshot");
                }
            } finally {
                conn.disconnect();
            }
        } catch (IOException e) {
            LOG.error(e.getLocalizedMessage(), e);
        }
    }

    private Instance createInstance(final String containerName,
                                    final Machine machine,
                                    final String imageName,
                                    final LineConsumer outputConsumer)
            throws MachineException {
        try {
            // todo
            // do not create network for 1 container env
            // support overlay + default network + bridge

            String networkName = machine.getWorkspaceId();
            createNetwork(networkName);

            MachineConfig config = machine.getConfig();

            final Map<String, Map<String, String>> portsToExpose;
            final String[] volumes;
            final List<String> env;
            if (config.isDev()) {
                portsToExpose = new HashMap<>(devMachinePortsToExpose);

                final String projectFolderVolume = format("%s:%s:Z",
                                                          workspaceFolderPathProvider.getPath(machine.getWorkspaceId()),
                                                          projectFolderPath);
                volumes = ObjectArrays.concat(devMachineSystemVolumes,
                                              SystemInfo.isWindows() ? escapePath(projectFolderVolume) : projectFolderVolume);

                env = new ArrayList<>(devMachineEnvVariables);
                env.add(DockerInstanceRuntimeInfo.CHE_WORKSPACE_ID + '=' + machine.getWorkspaceId());
                env.add(DockerInstanceRuntimeInfo.USER_TOKEN + '=' + getUserToken(machine.getWorkspaceId()));
            } else {
                portsToExpose = new HashMap<>(commonMachinePortsToExpose);
                volumes = commonMachineSystemVolumes;
                env = new ArrayList<>(commonMachineEnvVariables);
            }

            final long machineMemory = machine.getConfig().getLimits().getRam() * 1024L * 1024L;
            final long machineMemorySwap = memorySwapMultiplier == -1 ? -1 : (long)(machineMemory * memorySwapMultiplier);
            config.getServers()
                  .stream()
                  .forEach(serverConf -> portsToExpose.put(serverConf.getPort(), Collections.emptyMap()));

            config.getExpose()
                  .stream()
                  .forEach(expose -> portsToExpose.put(expose, Collections.emptyMap()));

            config.getEnvVariables()
                  .entrySet()
                  .stream()
                  .map(entry -> entry.getKey() + "=" + entry.getValue())
                  .forEach(env::add);

            final EndpointConfig endpointConfig = new EndpointConfig().withAliases(singletonList(config.getName()));
            final NetworkingConfig networkingConfig = new NetworkingConfig().withEndpointsConfig(singletonMap(networkName,
                                                                                                              endpointConfig));
            // todo find ports that should not be exposed
            // set publish all ports = true
            // set port bindings for private ports to null
            // Done
            // todo or set something to ports that should be published only - servers + field ports

            // todo respect machine links
            // todo respect volumesFrom, volumes

            final HostConfig hostConfig = new HostConfig();
            hostConfig.withBinds(volumes)
                      .withExtraHosts(allMachinesExtraHosts)
                      .withPublishAllPorts(true)
                      .withMemorySwap(machineMemorySwap)
                      .withMemory(machineMemory)
                      .withPrivileged(privilegeMode)
                      .withNetworkMode(networkName);
            final ContainerConfig containerConfig = new ContainerConfig();
            containerConfig.withImage(imageName)
                           .withExposedPorts(portsToExpose)
                           .withHostConfig(hostConfig)
                           .withEnv(env.toArray(new String[env.size()]))
                           .withNetworkingConfig(networkingConfig)
                           .withLabels(config.getLabels())
                           .withEntrypoint(config.getEntrypoint().size() < 1 ? null :
                                           config.getEntrypoint().toArray(new String[config.getEntrypoint().size()]))
                           .withCmd(config.getCommand().size() < 1 ? null :
                                    config.getCommand().toArray(new String[config.getCommand().size()]));

            final String containerId = docker.createContainer(CreateContainerParams.create(containerConfig)
                                                                                   .withContainerName(containerName))
                                             .getId();

            docker.startContainer(StartContainerParams.create(containerId));

            final DockerNode node = dockerMachineFactory.createNode(machine.getWorkspaceId(), containerId);
            if (config.isDev()) {
                node.bindWorkspace();
                LOG.info("Machine with id '{}' backed by container '{}' has been deployed on node '{}'",
                         machine.getId(), containerId, node.getHost());
            }

            dockerInstanceStopDetector.startDetection(containerId, machine.getId());

            return dockerMachineFactory.createInstance(machine,
                                                       containerId,
                                                       imageName,
                                                       node,
                                                       outputConsumer);
        } catch (IOException e) {
            throw new MachineException(e.getLocalizedMessage(), e);
        }
    }

    private void createNetwork(String networkName) throws IOException {
        List<Network> networks = docker.getNetworks(GetNetworksParams.create().withFilters(new Filters().withFilter("name", networkName)));
        if (networks.size() == 0) {
            docker.createNetwork(CreateNetworkParams.create(new NewNetwork().withName(networkName)
                                                                            .withCheckDuplicate(true)));
        }
    }

    // workspaceId parameter is required, because in case of separate storage for tokens
    // you need to know exactly which workspace and which user to apply the token.
    protected String getUserToken(String wsId) {
        return EnvironmentContext.getCurrent().getSubject().getToken();
    }

    /**
     * Returns set that contains all non empty and non nullable values from specified set
     */
    protected Set<String> removeEmptyAndNullValues(Set<String> paths) {
        return paths.stream()
                    .filter(path -> !Strings.isNullOrEmpty(path))
                    .collect(Collectors.toSet());
    }
}
