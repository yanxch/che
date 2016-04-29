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
package org.eclipse.che.api.debugger.server;

import com.google.inject.Inject;

import org.eclipse.che.api.debugger.server.exceptions.DebuggerException;
import org.eclipse.che.api.debugger.server.exceptions.DebuggerNotFoundException;
import org.eclipse.che.api.debugger.shared.dto.Breakpoint;
import org.eclipse.che.api.debugger.shared.dto.Location;
import org.eclipse.che.api.debugger.shared.dto.DebugSession;
import org.eclipse.che.api.debugger.shared.dto.StackFrameDump;
import org.eclipse.che.api.debugger.shared.dto.Value;
import org.eclipse.che.api.debugger.shared.dto.Variable;
import org.eclipse.che.api.debugger.shared.dto.VariablePath;
import org.eclipse.che.api.debugger.shared.dto.action.Action;
import org.eclipse.che.api.debugger.shared.dto.action.ResumeAction;
import org.eclipse.che.api.debugger.shared.dto.action.StartAction;
import org.eclipse.che.api.debugger.shared.dto.action.StepIntoAction;
import org.eclipse.che.api.debugger.shared.dto.action.StepOutAction;
import org.eclipse.che.api.debugger.shared.dto.action.StepOverAction;

import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.UriInfo;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.eclipse.che.api.debugger.shared.dto.action.Action.RESUME;
import static org.eclipse.che.api.debugger.shared.dto.action.Action.START;
import static org.eclipse.che.api.debugger.shared.dto.action.Action.STEP_INTO;
import static org.eclipse.che.api.debugger.shared.dto.action.Action.STEP_OUT;
import static org.eclipse.che.api.debugger.shared.dto.action.Action.STEP_OVER;
import static org.eclipse.che.dto.server.DtoFactory.newDto;

/**
 * Debugger REST API.
 *
 * @author Anatoliy Bazko
 */
@Path("debugger/{ws-id}")
public class DebuggerService {
    private final DebuggerManager debuggerManager;

    @Inject
    public DebuggerService(DebuggerManager debuggerManager) {
        this.debuggerManager = debuggerManager;
    }

    @POST
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    public DebugSession connect(final @QueryParam("type") String debuggerType,
                                final Map<String, String> properties) throws DebuggerException {
        String sessionId = debuggerManager.create(debuggerType, properties);
        return getDebugSession(sessionId);
    }

    @DELETE
    @Path("{id}")
    public void disconnect(@PathParam("id") String sessionId) throws DebuggerException {
        Debugger debugger;
        try {
            debugger = debuggerManager.getDebugger(sessionId);
        } catch (DebuggerNotFoundException e) {
            // probably already disconnected
            return;
        }

        debugger.disconnect();
    }

    @GET
    @Path("{id}")
    @Produces(MediaType.APPLICATION_JSON)
    public DebugSession getDebugSession(@PathParam("id") String sessionId) throws DebuggerException {
        DebugSession debugSession = newDto(DebugSession.class);
        debugSession.setDebuggerInfo(debuggerManager.getDebugger(sessionId).getInfo());
        debugSession.setId(sessionId);
        debugSession.setType(debuggerManager.getDebuggerType(sessionId));

        return debugSession;
    }

    @POST
    @Path("{id}")
    public void performAction(@PathParam("id") String sessionId, Action action) throws DebuggerException {
        Debugger debugger = debuggerManager.getDebugger(sessionId);
        switch (action.getType()) {
            case START:
                debugger.start((StartAction)action);
                break;
            case RESUME:
                debugger.resume((ResumeAction)action);
                break;
            case STEP_INTO:
                debugger.stepInto((StepIntoAction)action);
                break;
            case STEP_OUT:
                debugger.stepOut((StepOutAction)action);
                break;
            case STEP_OVER:
                debugger.stepOver((StepOverAction)action);
                break;
            default:
                throw new DebuggerException("Unknown debugger action type " + action.getType());
        }
    }

    @POST
    @Path("{id}/breakpoint")
    @Consumes(MediaType.APPLICATION_JSON)
    public void addBreakpoint(@PathParam("id") String sessionId, Breakpoint breakpoint) throws DebuggerException {
        debuggerManager.getDebugger(sessionId).addBreakpoint(breakpoint);
    }

    @GET
    @Path("{id}/breakpoint")
    @Produces(MediaType.APPLICATION_JSON)
    public List<Breakpoint> getBreakpoints(@PathParam("id") String sessionId) throws DebuggerException {
        return debuggerManager.getDebugger(sessionId).getAllBreakpoints();
    }

    @DELETE
    @Path("{id}/breakpoint")
    public void deleteBreakpoint(@PathParam("id") String sessionId,
                                 @QueryParam("target") String target,
                                 @QueryParam("line") @DefaultValue("0") int lineNumber) throws DebuggerException {
        if (target == null) {
            debuggerManager.getDebugger(sessionId).deleteAllBreakpoints();
        } else {
            Location location = newDto(Location.class);
            location.setTarget(target);
            location.setLineNumber(lineNumber);
            debuggerManager.getDebugger(sessionId).deleteBreakpoint(location);
        }
    }

    @GET
    @Path("{id}/dump")
    @Produces(MediaType.APPLICATION_JSON)
    public StackFrameDump getStackFrameDump(@PathParam("id") String sessionId) throws DebuggerException {
        return debuggerManager.getDebugger(sessionId).dumpStackFrame();
    }

    @GET
    @Path("{id}/value")
    @Produces(MediaType.APPLICATION_JSON)
    public Value getValue(@PathParam("id") String sessionId, @Context UriInfo uriInfo) throws DebuggerException {
        List<String> path = new ArrayList<>();

        MultivaluedMap<String, String> parameters = uriInfo.getPathParameters();

        int i = 0;
        String item;
        while ((item = parameters.getFirst("path" + (i++))) != null) {
            path.add(item);
        }

        VariablePath variablePath = newDto(VariablePath.class);
        variablePath.setPath(path);

        return debuggerManager.getDebugger(sessionId).getValue(variablePath);
    }

    @PUT
    @Path("{id}/value")
    @Consumes(MediaType.APPLICATION_JSON)
    public void setValue(@PathParam("id") String sessionId, Variable variable) throws DebuggerException {
        debuggerManager.getDebugger(sessionId).setValue(variable);
    }

    @GET
    @Path("{id}/evaluation")
    @Produces(MediaType.TEXT_PLAIN)
    public String expression(@PathParam("id") String sessionId,
                             @QueryParam("expression") String expression) throws DebuggerException {
        return debuggerManager.getDebugger(sessionId).evaluate(expression);
    }
}
