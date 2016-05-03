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
package org.eclipse.che.plugin.debugger.ide.debug;

import com.google.gwt.user.client.Timer;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.web.bindery.event.shared.EventBus;

import org.eclipse.che.api.debugger.gwt.client.DebuggerServiceClient;
import org.eclipse.che.api.debugger.shared.dto.DebugSession;
import org.eclipse.che.api.debugger.shared.dto.DebuggerInfo;
import org.eclipse.che.api.debugger.shared.dto.LinePosition;
import org.eclipse.che.api.debugger.shared.dto.Location;
import org.eclipse.che.api.debugger.shared.dto.StackFrameDump;
import org.eclipse.che.api.debugger.shared.dto.Value;
import org.eclipse.che.api.debugger.shared.dto.Variable;
import org.eclipse.che.api.debugger.shared.dto.action.Action;
import org.eclipse.che.api.debugger.shared.dto.action.ResumeAction;
import org.eclipse.che.api.debugger.shared.dto.action.StartAction;
import org.eclipse.che.api.debugger.shared.dto.action.StepIntoAction;
import org.eclipse.che.api.debugger.shared.dto.action.StepOutAction;
import org.eclipse.che.api.debugger.shared.dto.action.StepOverAction;
import org.eclipse.che.api.debugger.shared.dto.events.BreakpointActivatedEvent;
import org.eclipse.che.api.debugger.shared.dto.events.DebuggerEvent;
import org.eclipse.che.api.debugger.shared.dto.events.SuspendEvent;
import org.eclipse.che.api.machine.gwt.client.events.WsAgentStateEvent;
import org.eclipse.che.api.machine.gwt.client.events.WsAgentStateHandler;
import org.eclipse.che.api.promises.client.Function;
import org.eclipse.che.api.promises.client.FunctionException;
import org.eclipse.che.api.promises.client.Operation;
import org.eclipse.che.api.promises.client.OperationException;
import org.eclipse.che.api.promises.client.Promise;
import org.eclipse.che.api.promises.client.PromiseError;
import org.eclipse.che.api.promises.client.js.JsPromise;
import org.eclipse.che.api.promises.client.js.JsPromiseError;
import org.eclipse.che.api.promises.client.js.Promises;
import org.eclipse.che.commons.annotation.Nullable;
import org.eclipse.che.ide.api.app.AppContext;
import org.eclipse.che.ide.api.editor.EditorAgent;
import org.eclipse.che.ide.api.filetypes.FileTypeRegistry;
import org.eclipse.che.ide.api.project.tree.VirtualFile;
import org.eclipse.che.ide.debug.Breakpoint;
import org.eclipse.che.ide.debug.Debugger;
import org.eclipse.che.ide.debug.DebuggerDescriptor;
import org.eclipse.che.ide.debug.DebuggerManager;
import org.eclipse.che.ide.debug.DebuggerObservable;
import org.eclipse.che.ide.debug.DebuggerObserver;
import org.eclipse.che.ide.dto.DtoFactory;
import org.eclipse.che.ide.jseditor.client.text.LinearRange;
import org.eclipse.che.ide.jseditor.client.texteditor.TextEditor;
import org.eclipse.che.ide.resource.Path;
import org.eclipse.che.plugin.debugger.ide.fqn.FqnResolverFactory;
import org.eclipse.che.ide.rest.HTTPStatus;
import org.eclipse.che.ide.util.loging.Log;
import org.eclipse.che.ide.util.storage.LocalStorage;
import org.eclipse.che.ide.util.storage.LocalStorageProvider;
import org.eclipse.che.ide.websocket.MessageBus;
import org.eclipse.che.ide.websocket.MessageBusProvider;
import org.eclipse.che.ide.websocket.WebSocketException;
import org.eclipse.che.ide.websocket.rest.SubscriptionHandler;
import org.eclipse.che.ide.websocket.rest.exceptions.ServerException;

import javax.validation.constraints.NotNull;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;


/**
 * The common debugger.
 *
 * @author Anatoliy Bazko
 */
public abstract class AbstractDebugger implements Debugger, DebuggerObservable {

    public static final String LOCAL_STORAGE_DEBUGGER_SESSION_KEY = "che-debugger-session";
    public static final String LOCAL_STORAGE_DEBUGGER_STATE_KEY   = "che-debugger-state";

    protected final DtoFactory         dtoFactory;
    protected final FileTypeRegistry   fileTypeRegistry;
    protected final FqnResolverFactory fqnResolverFactory;

    private final List<DebuggerObserver> observers;
    private final DebuggerServiceClient  service;
    private final LocalStorageProvider   localStorageProvider;
    private final EventBus               eventBus;
    private final ActiveFileHandler      activeFileHandler;
    private final DebuggerManager        debuggerManager;
    private final String                 debuggerType;
    private final String                 eventChannel;
    private final EditorAgent            editorAgent;
    private final AppContext             appContext;

    private DebugSession                       debugSession;
    private Location                           currentLocation;
    private SubscriptionHandler<DebuggerEvent> debuggerEventsHandler;

    private MessageBus messageBus;

    public AbstractDebugger(DebuggerServiceClient service,
                            DtoFactory dtoFactory,
                            LocalStorageProvider localStorageProvider,
                            MessageBusProvider messageBusProvider,
                            EventBus eventBus,
                            FqnResolverFactory fqnResolverFactory,
                            ActiveFileHandler activeFileHandler,
                            DebuggerManager debuggerManager,
                            FileTypeRegistry fileTypeRegistry,
                            String type,
                            EditorAgent editorAgent,
                            AppContext appContext) {
        this.service = service;
        this.dtoFactory = dtoFactory;
        this.localStorageProvider = localStorageProvider;
        this.eventBus = eventBus;
        this.fqnResolverFactory = fqnResolverFactory;
        this.activeFileHandler = activeFileHandler;
        this.debuggerManager = debuggerManager;
        this.observers = new ArrayList<>();
        this.fileTypeRegistry = fileTypeRegistry;
        this.debuggerType = type;
        this.eventChannel = debuggerType + ":events:";
        this.editorAgent = editorAgent;
        this.appContext = appContext;

        restoreDebuggerState();
        addHandlers(messageBusProvider);
    }


    private void addHandlers(final MessageBusProvider messageBusProvider) {
        eventBus.addHandler(WsAgentStateEvent.TYPE, new WsAgentStateHandler() {
            @Override
            public void onWsAgentStarted(WsAgentStateEvent event) {
                messageBus = messageBusProvider.getMachineMessageBus();

                if (isConnected()) {
                    Promise<DebugSession> promise = service.getSessionInfo(debugSession.getId());
                    promise.then(new Operation<DebugSession>() {
                        @Override
                        public void apply(DebugSession arg) throws OperationException {
                            debuggerManager.setActiveDebugger(AbstractDebugger.this);
                            setDebugSession(arg);

                            DebuggerInfo debuggerInfo = arg.getDebuggerInfo();
                            String info = debuggerInfo.getName() + " " + debuggerInfo.getVersion();
                            String address = debuggerInfo.getHost() + ":" + debuggerInfo.getPort();
                            DebuggerDescriptor debuggerDescriptor = new DebuggerDescriptor(info, address);
                            JsPromise<Void> promise = Promises.resolve(null);

                            for (DebuggerObserver observer : observers) {
                                observer.onDebuggerAttached(debuggerDescriptor, promise);
                            }

                            startCheckingEvents();
                        }
                    }).catchError(new Operation<PromiseError>() {
                        @Override
                        public void apply(PromiseError arg) throws OperationException {
                            invalidateDebugSession();
                            preserveDebuggerState();
                        }
                    });
                }
            }

            @Override
            public void onWsAgentStopped(WsAgentStateEvent event) {}
        });

        this.debuggerEventsHandler = new SubscriptionHandler<DebuggerEvent>(new DebuggerEventListUnmarshaller(dtoFactory)) {
            @Override
            public void onMessageReceived(DebuggerEvent result) {
                if (isConnected()) {
                    onEventListReceived(result);
                }
            }

            @Override
            public void onErrorReceived(Throwable exception) {
                if (isConnected()) {
                    try {
                        messageBus.unsubscribe(eventChannel, this);
                    } catch (WebSocketException e) {
                        Log.error(AbstractDebugger.class, e);
                    }

                    if (exception instanceof ServerException) {
                        ServerException serverException = (ServerException)exception;
                        if (HTTPStatus.INTERNAL_ERROR == serverException.getHTTPStatus()
                            && serverException.getMessage() != null
                            && serverException.getMessage().contains("not found")) {

                            disconnect();
                        }
                    }
                }
            }
        };
    }

    private void onEventListReceived(@NotNull DebuggerEvent event) {
        Location newLocation;

        switch (event.getType()) {
            case DebuggerEvent.SUSPEND:
                newLocation = ((SuspendEvent)event).getLocation();
                break;
            case DebuggerEvent.BREAKPOINT_ACTIVATED:
                org.eclipse.che.api.debugger.shared.dto.Breakpoint breakpoint = ((BreakpointActivatedEvent)event).getBreakpoint();
                onBreakpointActivated(breakpoint.getLocation());
                return;
            case DebuggerEvent.DISCONNECTED:
                disconnect();
                return;
            default:
                Log.error(AbstractDebugger.class, "Unknown debuggerType of debugger event: " + event.getType());
                return;
        }

        if (newLocation != null) {
            currentLocation = newLocation;
            openCurrentFile();
        }

        preserveDebuggerState();
    }

    private void openCurrentFile() {
        activeFileHandler.openFile(fqnToPath(currentLocation),
                                   currentLocation.getTarget(),
                                   currentLocation.getLineNumber(),
                                   new AsyncCallback<VirtualFile>() {
                                       @Override
                                       public void onFailure(Throwable caught) {
                                           for (DebuggerObserver observer : observers) {
                                               observer.onBreakpointStopped(currentLocation.getTarget(),
                                                                            currentLocation.getTarget(),
                                                                            currentLocation.getLineNumber());
                                           }
                                       }

                                       @Override
                                       public void onSuccess(VirtualFile result) {
                                           for (DebuggerObserver observer : observers) {
                                               observer.onBreakpointStopped(result.getPath(),
                                                                            currentLocation.getTarget(),
                                                                            currentLocation.getLineNumber());
                                           }
                                       }
                                   });
    }

    /**
     * Breakpoint became active. It might happens because of different reasons:
     * <li>breakpoint was deferred and VM eventually loaded class and added it</li>
     * <li>condition triggered</li>
     * <li>etc</li>
     */
    private void onBreakpointActivated(Location location) {
        List<String> filePaths = fqnToPath(location);
        for (String filePath : filePaths) {
            for (DebuggerObserver observer : observers) {
                observer.onBreakpointActivated(filePath, location.getLineNumber() - 1);
            }
        }
    }

    private void startCheckingEvents() {
        try {
            messageBus.subscribe(eventChannel, debuggerEventsHandler);
        } catch (WebSocketException e) {
            Log.error(DebuggerPresenter.class, e);
        }
    }

    private void stopCheckingDebugEvents() {
        try {
            if (messageBus.isHandlerSubscribed(debuggerEventsHandler, eventChannel)) {
                messageBus.unsubscribe(eventChannel, debuggerEventsHandler);
            }
        } catch (WebSocketException e) {
            Log.error(AbstractDebugger.class, e);
        }
    }

    @Override
    public Promise<Value> getValue(Variable variable) {
        if (!isConnected()) {
            return Promises.reject(JsPromiseError.create("Debugger is not connected"));
        }

        return service.getValue(debugSession.getId(), variable);
    }

    @Override
    public Promise<StackFrameDump> dumpStackFrame() {
        if (!isConnected()) {
            return Promises.reject(JsPromiseError.create("Debugger is not connected"));
        }

        return service.getStackFrameDump(debugSession.getId());
    }

    @Override
    public void addBreakpoint(final VirtualFile file, final int lineNumber) {
        if (isConnected()) {
            Location location = dtoFactory.createDto(Location.class);
            location.setLineNumber(lineNumber + 1);

            TextEditor textEditor = (TextEditor)editorAgent.getOpenedEditor(Path.valueOf(file.getPath()));

            LinearRange range = textEditor.getDocument().getLinearRangeForLine(lineNumber);
            int startCharacterCoordinate = range.getStartOffset();
            int endCharacterCoordinate = startCharacterCoordinate + range.getLength();
            Log.info(getClass(), startCharacterCoordinate + " " + endCharacterCoordinate);

            LinePosition linePosition = dtoFactory.createDto(org.eclipse.che.api.debugger.shared.dto.LinePosition.class);
            location.setLinePosition(linePosition);
            location.getLinePosition().withStartCharOffset(startCharacterCoordinate).withEndCharOffset(endCharacterCoordinate);

            String fqn = pathToFqn(file);//todo get fqn from file
            if (fqn == null) {
                return;
            }
            location.withTarget(fqn).withProjectName(appContext.getCurrentProject().getRootProject().getPath());

            org.eclipse.che.api.debugger.shared.dto.Breakpoint breakpoint =
                    dtoFactory.createDto(org.eclipse.che.api.debugger.shared.dto.Breakpoint.class);
            breakpoint.setLocation(location);
            breakpoint.setEnabled(true);

            Promise<Void> promise = service.addBreakpoint(debugSession.getId(), breakpoint);
            promise.then(new Operation<Void>() {
                @Override
                public void apply(Void arg) throws OperationException {
                    Breakpoint breakpoint = new Breakpoint(Breakpoint.Type.BREAKPOINT, lineNumber, file.getPath(), file, true);
                    for (DebuggerObserver observer : observers) {
                        observer.onBreakpointAdded(breakpoint);
                    }
                }
            }).catchError(new Operation<PromiseError>() {
                @Override
                public void apply(PromiseError arg) throws OperationException {
                    Log.error(AbstractDebugger.class, arg.getMessage());
                }
            });
        } else {
            Breakpoint breakpoint = new Breakpoint(Breakpoint.Type.BREAKPOINT, lineNumber, file.getPath(), file, false);
            for (DebuggerObserver observer : observers) {
                observer.onBreakpointAdded(breakpoint);
            }
        }
    }

    @Override
    public void deleteBreakpoint(final VirtualFile file, final int lineNumber) {
        if (isConnected()) {
            Location location = dtoFactory.createDto(Location.class);
            location.setLineNumber(lineNumber + 1);

            String fqn = pathToFqn(file);
            if (fqn == null) {
                return;
            }
            location.setTarget(fqn);

            Promise<Void> promise = service.deleteBreakpoint(debugSession.getId(), location);
            promise.then(new Operation<Void>() {
                @Override
                public void apply(Void arg) throws OperationException {
                    for (DebuggerObserver observer : observers) {
                        Breakpoint breakpoint = new Breakpoint(Breakpoint.Type.BREAKPOINT, lineNumber, file.getPath(), file, false);
                        observer.onBreakpointDeleted(breakpoint);
                    }
                }
            }).catchError(new Operation<PromiseError>() {
                @Override
                public void apply(PromiseError arg) throws OperationException {
                    Log.error(AbstractDebugger.class, arg.getMessage());
                }
            });
        }
    }

    @Override
    public void deleteAllBreakpoints() {
        if (isConnected()) {
            Promise<Void> promise = service.deleteAllBreakpoints(debugSession.getId());

            promise.then(new Operation<Void>() {
                @Override
                public void apply(Void arg) throws OperationException {
                    for (DebuggerObserver observer : observers) {
                        observer.onAllBreakpointsDeleted();
                    }
                }
            }).catchError(new Operation<PromiseError>() {
                @Override
                public void apply(PromiseError arg) throws OperationException {
                    Log.error(AbstractDebugger.class, arg.getMessage());
                }
            });
        }
    }

    @Override
    public Promise<Void> connect(Map<String, String> connectionProperties) {
        if (isConnected()) {
            return Promises.reject(JsPromiseError.create("Debugger already connected"));
        }

        Promise<DebugSession> connect = service.connect(debuggerType, connectionProperties);
        final DebuggerDescriptor debuggerDescriptor = toDescriptor(connectionProperties);

        Promise<Void> promise = connect.then(new Function<DebugSession, Void>() {
            @Override
            public Void apply(final DebugSession arg) throws FunctionException {
                DebuggerInfo debuggerInfo = arg.getDebuggerInfo();
                debuggerDescriptor.setInfo(debuggerInfo.getName() + " " + debuggerInfo.getVersion());

                setDebugSession(arg);
                preserveDebuggerState();
                startCheckingEvents();
                startDebuggerWithDelay(arg);

                return null;
            }
        }).catchError(new Operation<PromiseError>() {
            @Override
            public void apply(PromiseError arg) throws OperationException {
                Log.error(AbstractDebugger.class, arg.getMessage());
                throw new OperationException(arg.getCause());
            }
        });

        for (DebuggerObserver observer : observers) {
            observer.onDebuggerAttached(debuggerDescriptor, promise);
        }

        return promise;
    }

    protected void startDebuggerWithDelay(final DebugSession debugSession) {
        new Timer() {
            @Override
            public void run() {
                StartAction action = dtoFactory.createDto(StartAction.class).withType(Action.START);
                service.start(debugSession.getId(), action);
            }
        }.schedule(2000);
    }

    @Override
    public void disconnect() {
        stopCheckingDebugEvents();

        Promise<Void> disconnect;
        if (isConnected()) {
            disconnect = service.disconnect(debugSession.getId());
        } else {
            disconnect = Promises.resolve(null);
        }

        invalidateDebugSession();
        preserveDebuggerState();

        disconnect.then(new Operation<Void>() {
            @Override
            public void apply(Void arg) throws OperationException {
                for (DebuggerObserver observer : observers) {
                    observer.onDebuggerDisconnected();
                }
                debuggerManager.setActiveDebugger(null);
            }
        }).catchError(new Operation<PromiseError>() {
            @Override
            public void apply(PromiseError arg) throws OperationException {
                for (DebuggerObserver observer : observers) {
                    observer.onDebuggerDisconnected();
                }
                debuggerManager.setActiveDebugger(null);
            }
        });
    }

    @Override
    public void stepInto() {
        if (isConnected()) {
            for (DebuggerObserver observer : observers) {
                observer.onPreStepInto();
            }
            currentLocation = null;

            StepIntoAction action = dtoFactory.createDto(StepIntoAction.class);
            action.setType(Action.STEP_INTO);

            Promise<Void> promise = service.stepInto(debugSession.getId(), action);
            promise.catchError(new Operation<PromiseError>() {
                @Override
                public void apply(PromiseError arg) throws OperationException {
                    Log.error(AbstractDebugger.class, arg.getCause());
                }
            });
        }
    }

    @Override
    public void stepOver() {
        if (isConnected()) {
            for (DebuggerObserver observer : observers) {
                observer.onPreStepOver();
            }
            currentLocation = null;

            StepOverAction action = dtoFactory.createDto(StepOverAction.class);
            action.setType(Action.STEP_OVER);

            Promise<Void> promise = service.stepOver(debugSession.getId(), action);
            promise.catchError(new Operation<PromiseError>() {
                @Override
                public void apply(PromiseError arg) throws OperationException {
                    Log.error(AbstractDebugger.class, arg.getCause());
                }
            });
        }
    }

    @Override
    public void stepOut() {
        if (isConnected()) {
            for (DebuggerObserver observer : observers) {
                observer.onPreStepOut();
            }
            currentLocation = null;

            StepOutAction action = dtoFactory.createDto(StepOutAction.class);
            action.setType(Action.STEP_OUT);

            Promise<Void> promise = service.stepOut(debugSession.getId(), action);
            promise.catchError(new Operation<PromiseError>() {
                @Override
                public void apply(PromiseError arg) throws OperationException {
                    Log.error(AbstractDebugger.class, arg.getCause());
                }
            });
        }
    }

    @Override
    public void resume() {
        if (isConnected()) {
            for (DebuggerObserver observer : observers) {
                observer.onPreResume();
            }
            currentLocation = null;

            ResumeAction action = dtoFactory.createDto(ResumeAction.class);
            action.setType(Action.RESUME);

            Promise<Void> promise = service.resume(debugSession.getId(), action);
            promise.catchError(new Operation<PromiseError>() {
                @Override
                public void apply(PromiseError arg) throws OperationException {
                    Log.error(AbstractDebugger.class, arg.getCause());
                }
            });
        }
    }

    @Override
    public Promise<String> evaluate(String expression) {
        if (isConnected()) {
            return service.evaluate(debugSession.getId(), expression);
        }

        return Promises.reject(JsPromiseError.create("Debugger is not connected"));
    }

    @Override
    public void setValue(final Variable variable) {
        if (isConnected()) {
            Promise<Void> promise = service.setValue(debugSession.getId(), variable);

            promise.then(new Operation<Void>() {
                @Override
                public void apply(Void arg) throws OperationException {
                    for (DebuggerObserver observer : observers) {
                        observer.onValueChanged(variable.getVariablePath().getPath(), variable.getValue());
                    }
                }
            }).catchError(new Operation<PromiseError>() {
                @Override
                public void apply(PromiseError arg) throws OperationException {
                    Log.error(AbstractDebugger.class, arg.getMessage());
                }
            });
        }
    }

    @Override
    public boolean isConnected() {
        return debugSession != null;
    }

    @Override
    public boolean isSuspended() {
        return isConnected() && currentLocation != null;
    }

    public String getDebuggerType() {
        return debuggerType;
    }

    @Override
    public void addObserver(DebuggerObserver observer) {
        observers.add(observer);
    }

    @Override
    public void removeObserver(DebuggerObserver observer) {
        observers.remove(observer);
    }

    protected void setDebugSession(DebugSession debugSession) {
        this.debugSession = debugSession;
    }

    private void invalidateDebugSession() {
        this.debugSession = null;
        this.currentLocation = null;
    }

    /**
     * Preserves debugger information into the local storage.
     */
    protected void preserveDebuggerState() {
        LocalStorage localStorage = localStorageProvider.get();

        if (localStorage == null) {
            return;
        }

        if (!isConnected()) {
            localStorage.setItem(LOCAL_STORAGE_DEBUGGER_SESSION_KEY, "");
            localStorage.setItem(LOCAL_STORAGE_DEBUGGER_STATE_KEY, "");
        } else {
            localStorage.setItem(LOCAL_STORAGE_DEBUGGER_SESSION_KEY, dtoFactory.toJson(debugSession));
            if (currentLocation == null) {
                localStorage.setItem(LOCAL_STORAGE_DEBUGGER_STATE_KEY, "");
            } else {
                localStorage.setItem(LOCAL_STORAGE_DEBUGGER_STATE_KEY, dtoFactory.toJson(currentLocation));
            }
        }
    }

    /**
     * Loads debugger information from the local storage.
     */
    protected void restoreDebuggerState() {
        invalidateDebugSession();

        LocalStorage localStorage = localStorageProvider.get();
        if (localStorage == null) {
            return;
        }

        String data = localStorage.getItem(LOCAL_STORAGE_DEBUGGER_SESSION_KEY);
        if (data != null && !data.isEmpty()) {
            DebugSession debugSession = dtoFactory.createDtoFromJson(data, DebugSession.class);
            if (!debugSession.getType().equals(getDebuggerType())) {
                return;
            }

            setDebugSession(debugSession);
        }

        data = localStorage.getItem(LOCAL_STORAGE_DEBUGGER_STATE_KEY);
        if (data != null && !data.isEmpty()) {
            currentLocation = dtoFactory.createDtoFromJson(data, Location.class);
        }
    }

    /**
     * Transforms FQN to file path.
     */
    abstract protected List<String> fqnToPath(@NotNull Location location);

    /**
     * Transforms file path to FQN>
     */
    @Nullable
    abstract protected String pathToFqn(VirtualFile file);

    abstract protected DebuggerDescriptor toDescriptor(Map<String, String> connectionProperties);
}
