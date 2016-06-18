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
package org.eclipse.che.ide.extension.machine.client.machine;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.google.web.bindery.event.shared.EventBus;

import org.eclipse.che.api.core.model.machine.MachineSource;
import org.eclipse.che.api.core.rest.shared.dto.LinkParameter;
import org.eclipse.che.api.machine.shared.dto.LimitsDto;
import org.eclipse.che.api.machine.shared.dto.MachineConfigDto;
import org.eclipse.che.api.machine.shared.dto.MachineDto;
import org.eclipse.che.api.machine.shared.dto.MachineLogMessageDto;
import org.eclipse.che.api.machine.shared.dto.MachineSourceDto;
import org.eclipse.che.api.machine.shared.dto.event.MachineStatusEvent;
import org.eclipse.che.api.promises.client.Operation;
import org.eclipse.che.api.promises.client.OperationException;
import org.eclipse.che.api.promises.client.Promise;
import org.eclipse.che.api.workspace.shared.Constants;
import org.eclipse.che.api.workspace.shared.dto.WorkspaceDto;
import org.eclipse.che.ide.api.app.AppContext;
import org.eclipse.che.ide.api.machine.DevMachine;
import org.eclipse.che.ide.api.machine.MachineManager;
import org.eclipse.che.ide.api.machine.MachineServiceClient;
import org.eclipse.che.ide.api.machine.OutputMessageUnmarshaller;
import org.eclipse.che.ide.api.machine.events.DevMachineStateEvent;
import org.eclipse.che.ide.api.parts.PerspectiveManager;
import org.eclipse.che.ide.api.workspace.WorkspaceServiceClient;
import org.eclipse.che.ide.api.workspace.event.WorkspaceStoppedEvent;
import org.eclipse.che.ide.api.workspace.event.WorkspaceStoppedHandler;
import org.eclipse.che.ide.dto.DtoFactory;
import org.eclipse.che.ide.extension.machine.client.machine.MachineStatusNotifier.RunningListener;
import org.eclipse.che.ide.extension.machine.client.machine.console.MachineConsolePresenter;
import org.eclipse.che.ide.rest.DtoUnmarshallerFactory;
import org.eclipse.che.ide.ui.loaders.initialization.InitialLoadingInfo;
import org.eclipse.che.ide.util.loging.Log;
import org.eclipse.che.ide.websocket.MessageBus;
import org.eclipse.che.ide.websocket.MessageBusProvider;
import org.eclipse.che.ide.websocket.WebSocketException;
import org.eclipse.che.ide.websocket.rest.SubscriptionHandler;
import org.eclipse.che.ide.websocket.rest.Unmarshallable;

import static org.eclipse.che.api.machine.shared.Constants.LINK_REL_GET_MACHINE_LOGS_CHANNEL;
import static org.eclipse.che.ide.api.machine.MachineManager.MachineOperationType.DESTROY;
import static org.eclipse.che.ide.api.machine.MachineManager.MachineOperationType.RESTART;
import static org.eclipse.che.ide.api.machine.MachineManager.MachineOperationType.START;
import static org.eclipse.che.ide.extension.machine.client.perspective.OperationsPerspective.OPERATIONS_PERSPECTIVE_ID;
import static org.eclipse.che.ide.ui.loaders.initialization.InitialLoadingInfo.Operations.MACHINE_BOOTING;
import static org.eclipse.che.ide.ui.loaders.initialization.OperationInfo.Status.ERROR;
import static org.eclipse.che.ide.ui.loaders.initialization.OperationInfo.Status.IN_PROGRESS;
import static org.eclipse.che.ide.ui.loaders.initialization.OperationInfo.Status.SUCCESS;

/**
 * Manager for machine operations.
 *
 * @author Artem Zatsarynnyi
 */
@Singleton
public class MachineManagerImpl implements MachineManager, WorkspaceStoppedHandler {

    private final DtoUnmarshallerFactory  dtoUnmarshallerFactory;
    private final MachineServiceClient    machineServiceClient;
    private final WorkspaceServiceClient  workspaceServiceClient;
    private final MachineConsolePresenter machineConsolePresenter;
    private final MachineStatusNotifier   machineStatusNotifier;
    private final InitialLoadingInfo      initialLoadingInfo;
    private final PerspectiveManager      perspectiveManager;
    private final AppContext              appContext;
    private final DtoFactory              dtoFactory;
    private final EventBus                eventBus;

    private MessageBus messageBus;
    private boolean    isMachineRestarting;

    private String                                    wsAgentLogChannel;
    private String                                    statusChannel;
    private String                                    outputChannel;
    private SubscriptionHandler<MachineStatusEvent>   statusHandler;
    private SubscriptionHandler<MachineLogMessageDto> machinesOutputHandler;
    private SubscriptionHandler<String>               wsagentOutputHandler;

    @Inject
    public MachineManagerImpl(DtoUnmarshallerFactory dtoUnmarshallerFactory,
                              MachineServiceClient machineServiceClient,
                              WorkspaceServiceClient workspaceServiceClient,
                              MachineConsolePresenter machineConsolePresenter,
                              MachineStatusNotifier machineStatusNotifier,
                              final MessageBusProvider messageBusProvider,
                              final InitialLoadingInfo initialLoadingInfo,
                              final PerspectiveManager perspectiveManager,
                              EventBus eventBus,
                              final AppContext appContext,
                              DtoFactory dtoFactory) {
        this.dtoUnmarshallerFactory = dtoUnmarshallerFactory;
        this.machineServiceClient = machineServiceClient;
        this.workspaceServiceClient = workspaceServiceClient;
        this.machineConsolePresenter = machineConsolePresenter;
        this.machineStatusNotifier = machineStatusNotifier;
        this.initialLoadingInfo = initialLoadingInfo;
        this.perspectiveManager = perspectiveManager;
        this.appContext = appContext;
        this.dtoFactory = dtoFactory;
        this.eventBus = eventBus;

        this.messageBus = messageBusProvider.getMessageBus();

        eventBus.addHandler(WorkspaceStoppedEvent.TYPE, this);

        initializeHandlers();
    }

    private void initializeHandlers() {
        final Unmarshallable<MachineStatusEvent> unmarshaller = dtoUnmarshallerFactory.newWSUnmarshaller(MachineStatusEvent.class);

        statusHandler = new SubscriptionHandler<MachineStatusEvent>(unmarshaller) {
            @Override
            protected void onMessageReceived(MachineStatusEvent event) {
                switch (event.getEventType()) {
                    case RUNNING:
                        initialLoadingInfo.setOperationStatus(MACHINE_BOOTING.getValue(), SUCCESS);

                        String machineId = event.getMachineId();
                        onMachineRunning(machineId);

                        eventBus.fireEvent(new DevMachineStateEvent(event));
                        break;
                    case ERROR:
                        initialLoadingInfo.setOperationStatus(MACHINE_BOOTING.getValue(), ERROR);
                        break;
                    default:
                }
            }

            @Override
            protected void onErrorReceived(Throwable exception) {
                Log.error(MachineManagerImpl.class, exception);
            }
        };

        machinesOutputHandler =
                new SubscriptionHandler<MachineLogMessageDto>(dtoUnmarshallerFactory.newWSUnmarshaller(MachineLogMessageDto.class)) {
                    @Override
                    protected void onMessageReceived(MachineLogMessageDto logMessage) {
                        machineConsolePresenter.print("[" + logMessage.getMachine() + "] " + logMessage.getContent());
                    }

                    @Override
                    protected void onErrorReceived(Throwable exception) {
                        Log.error(MachineManagerImpl.class, exception);
                    }
                };

        wsagentOutputHandler = new SubscriptionHandler<String>(new OutputMessageUnmarshaller()) {
            @Override
            protected void onMessageReceived(String result) {
                machineConsolePresenter.print(result);
            }

            @Override
            protected void onErrorReceived(Throwable exception) {
                Log.error(MachineManagerImpl.class, exception);
            }
        };
    }

    @Override
    public void onWorkspaceStopped(WorkspaceStoppedEvent event) {
        boolean statusChannelNotNull = statusChannel != null;
        boolean outputChannelNotNull = outputChannel != null;
        boolean wsLogChannelNotNull = wsAgentLogChannel != null;

        if (statusChannelNotNull && messageBus.isHandlerSubscribed(statusHandler, statusChannel)) {
            unsubscribeChannel(statusChannel, statusHandler);
        }

        if (outputChannelNotNull && messageBus.isHandlerSubscribed(machinesOutputHandler, outputChannel)) {
            unsubscribeChannel(outputChannel, machinesOutputHandler);
        }

        if (wsLogChannelNotNull && messageBus.isHandlerSubscribed(machinesOutputHandler, wsAgentLogChannel)) {
            unsubscribeChannel(wsAgentLogChannel, machinesOutputHandler);
        }
    }

    @Override
    public void restartMachine(final org.eclipse.che.api.core.model.machine.Machine machineState) {
        eventBus.addHandler(MachineStateEvent.TYPE, new MachineStateEvent.Handler() {

            @Override
            public void onMachineCreating(MachineStateEvent event) {
            }

            @Override
            public void onMachineRunning(MachineStateEvent event) {
            }

            @Override
            public void onMachineDestroyed(MachineStateEvent event) {
                if (isMachineRestarting) {
                    final MachineSource machineSource = machineState.getConfig().getSource();
                    final String displayName = machineState.getConfig().getName();
                    final boolean isDev = machineState.getConfig().isDev();

                    startMachine(asDto(machineSource), displayName, isDev, RESTART, "docker");

                    isMachineRestarting = false;
                }
            }
        });

        destroyMachine(machineState).then(new Operation<Void>() {
            @Override
            public void apply(Void arg) throws OperationException {
                isMachineRestarting = true;
            }
        });
    }

    /**
     * Converts {@link MachineSource} to {@link MachineSourceDto}.
     */
    public MachineSourceDto asDto(MachineSource source) {
        return this.dtoFactory.createDto(MachineSourceDto.class)
                              .withType(source.getType())
                              .withLocation(source.getLocation())
                              .withContent(source.getContent());
    }


    /** Start new machine. */
    @Override
    public void startMachine(String recipeURL, String displayName) {
        startMachine(recipeURL, displayName, false, START, "dockerfile", "docker");
    }

    /** Start new machine as dev-machine (bind workspace to running machine). */
    @Override
    public void startDevMachine(String recipeURL, String displayName) {
        startMachine(recipeURL, displayName, true, START, "dockerfile", "docker");
    }


    /**
     * @param recipeURL
     * @param displayName
     * @param isDev
     * @param operationType
     * @param sourceType
     *         "dockerfile" or "ssh-config"
     * @param machineType
     *         "docker" or "ssh"
     */
    private void startMachine(final String recipeURL,
                              final String displayName,
                              final boolean isDev,
                              final MachineOperationType operationType,
                              final String sourceType,
                              final String machineType) {
        MachineSourceDto sourceDto = dtoFactory.createDto(MachineSourceDto.class).withType(sourceType).withLocation(recipeURL);
        startMachine(sourceDto, displayName, isDev, operationType, machineType);
    }

    /**
     * @param machineSourceDto
     * @param displayName
     * @param isDev
     * @param operationType
     * @param machineType
     *         "docker" or "ssh"
     */
    private void startMachine(final MachineSourceDto machineSourceDto,
                              final String displayName,
                              final boolean isDev,
                              final MachineOperationType operationType,
                              final String machineType) {

        LimitsDto limitsDto = dtoFactory.createDto(LimitsDto.class).withRam(1024);
        if (isDev) {
            limitsDto.withRam(3072);
        }

        MachineConfigDto configDto = dtoFactory.createDto(MachineConfigDto.class)
                                               .withDev(isDev)
                                               .withName(displayName)
                                               .withSource(machineSourceDto)
                                               .withLimits(limitsDto)
                                               .withType(machineType);

        Promise<MachineDto> machinePromise = workspaceServiceClient.createMachine(appContext.getWorkspace().getId(), configDto);

        machinePromise.then(new Operation<MachineDto>() {
            @Override
            public void apply(final MachineDto machineDto) throws OperationException {
                eventBus.fireEvent(new MachineStateEvent(machineDto, MachineStateEvent.MachineAction.CREATING));

                subscribeToChannel(machineDto.getConfig()
                                             .getLink(LINK_REL_GET_MACHINE_LOGS_CHANNEL)
                                             .getParameter("channel")
                                             .getDefaultValue(),
                                   machinesOutputHandler);

                RunningListener runningListener = null;

                if (isDev) {
                    runningListener = new RunningListener() {
                        @Override
                        public void onRunning() {
                            onMachineRunning(machineDto.getId());
                        }
                    };
                }

                machineStatusNotifier.trackMachine(machineDto, runningListener, operationType);
            }
        });
    }

    @Override
    public void onMachineRunning(final String machineId) {
        machineServiceClient.getMachine(machineId).then(new Operation<MachineDto>() {
            @Override
            public void apply(MachineDto machineDto) throws OperationException {
                DevMachine devMachine = new DevMachine(machineDto);
                appContext.setDevMachine(devMachine);
                appContext.setProjectsRoot(machineDto.getRuntime().projectsRoot());
            }
        });
    }

    @Override
    public Promise<Void> destroyMachine(final org.eclipse.che.api.core.model.machine.Machine machineState) {
        return machineServiceClient.destroyMachine(machineState.getId()).then(new Operation<Void>() {
            @Override
            public void apply(Void arg) throws OperationException {
                machineStatusNotifier.trackMachine(machineState, DESTROY);

                final DevMachine devMachine = appContext.getDevMachine();
                if (devMachine != null && machineState.getId().equals(devMachine.getId())) {
                    appContext.setDevMachine(null);
                }
            }
        });
    }

    @Override
    public void onWsStarting(WorkspaceDto workspace) {
        perspectiveManager.setPerspectiveId(OPERATIONS_PERSPECTIVE_ID);
        initialLoadingInfo.setOperationStatus(MACHINE_BOOTING.getValue(), IN_PROGRESS);

        if (workspace.getLink(Constants.GET_WORKSPACE_OUTPUT_CHANNEL) != null) {
            final LinkParameter logsChannelLinkParameter = workspace.getLink(Constants.GET_WORKSPACE_OUTPUT_CHANNEL)
                                                                    .getParameter("channel");
            if (logsChannelLinkParameter != null) {
                outputChannel = logsChannelLinkParameter.getDefaultValue();
            }
        }
        if (outputChannel != null) {
            wsAgentLogChannel = "workspace:" + appContext.getWorkspaceId() + ":ext-server:output";
            statusChannel = "workspace:" + workspace.getId() + ":machines_statuses";
            subscribeToChannel(wsAgentLogChannel, wsagentOutputHandler);
            subscribeToChannel(outputChannel, machinesOutputHandler);
            subscribeToChannel(statusChannel, statusHandler);
        } else {
            initialLoadingInfo.setOperationStatus(MACHINE_BOOTING.getValue(), ERROR);
        }
    }

    private void subscribeToChannel(String chanel, SubscriptionHandler handler) {
        try {
            messageBus.subscribe(chanel, handler);
        } catch (WebSocketException exception) {
            Log.error(getClass(), exception);
        }
    }

    private void unsubscribeChannel(String chanel, SubscriptionHandler handler) {
        try {
            messageBus.unsubscribe(chanel, handler);
        } catch (WebSocketException exception) {
            Log.error(getClass(), exception);
        }
    }

}
