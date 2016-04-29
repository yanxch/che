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
package org.eclipse.che.plugin.gdb.server;

import com.google.common.collect.ImmutableMap;

import org.eclipse.che.api.debugger.server.Debugger;
import org.eclipse.che.api.debugger.server.exceptions.DebuggerException;
import org.eclipse.che.api.debugger.shared.dto.Breakpoint;
import org.eclipse.che.api.debugger.shared.dto.DebuggerInfo;
import org.eclipse.che.api.debugger.shared.dto.Location;
import org.eclipse.che.api.debugger.shared.dto.StackFrameDump;
import org.eclipse.che.api.debugger.shared.dto.Value;
import org.eclipse.che.api.debugger.shared.dto.Variable;
import org.eclipse.che.api.debugger.shared.dto.VariablePath;
import org.eclipse.che.api.debugger.shared.dto.action.ResumeAction;
import org.eclipse.che.api.debugger.shared.dto.action.StartAction;
import org.eclipse.che.api.debugger.shared.dto.action.StepOutAction;
import org.eclipse.che.api.debugger.shared.dto.action.StepOverAction;
import org.eclipse.che.api.debugger.shared.dto.events.BreakpointActivatedEvent;
import org.eclipse.che.api.debugger.shared.dto.events.DebuggerEvent;
import org.eclipse.che.api.debugger.shared.dto.events.DisconnectEvent;
import org.eclipse.che.api.debugger.shared.dto.events.SuspendEvent;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;

import static org.eclipse.che.dto.server.DtoFactory.newDto;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertNotNull;
import static org.testng.Assert.assertTrue;

/**
 * @author Anatoliy Bazko
 */
public class GdbDebuggerTest {

    private String                       file;
    private Path                         sourceDirectory;
    private GdbServer                    gdbServer;
    private Debugger                     gdbDebugger;
    private BlockingQueue<DebuggerEvent> events;

    @BeforeClass
    public void beforeClass() throws Exception {
        file = GdbTest.class.getResource("/hello").getFile();
        sourceDirectory = Paths.get(GdbTest.class.getResource("/h.cpp").getFile());
        events = new ArrayBlockingQueue<>(10);
    }

    @BeforeMethod
    public void setUp() throws Exception {
        gdbServer = GdbServer.start("localhost", 1111, file);
    }

    @AfterMethod
    public void tearDown() throws Exception {
        gdbServer.stop();
    }

    @Test
    public void testDebugger() throws Exception {
        initializeDebugger();
        addBreakpoint();
        startDebugger();
        doSetAndGetValues();
//        stepInto();
        stepOver();
        stepOut();
        resume();
        deleteAllBreakpoints();
        disconnect();
    }

    private void deleteAllBreakpoints() throws DebuggerException {
        List<Breakpoint> breakpoints = gdbDebugger.getAllBreakpoints();
        assertEquals(breakpoints.size(), 1);

        gdbDebugger.deleteAllBreakpoints();

        breakpoints = gdbDebugger.getAllBreakpoints();
        assertTrue(breakpoints.isEmpty());
    }

    private void resume() throws DebuggerException, InterruptedException {
        gdbDebugger.resume(newDto(ResumeAction.class));

        DebuggerEvent debuggerEvent = events.take();
        assertTrue(debuggerEvent instanceof SuspendEvent);

        SuspendEvent suspendEvent = (SuspendEvent)debuggerEvent;
        assertEquals(suspendEvent.getLocation().getTarget(), "h.cpp");
        assertEquals(suspendEvent.getLocation().getLineNumber(), 7);
    }

    private void stepOut() throws DebuggerException {
        try {
            gdbDebugger.stepOut(newDto(StepOutAction.class));
        } catch (DebuggerException e) {
            // ignore
        }

        assertTrue(events.isEmpty());
    }

    private void stepOver() throws DebuggerException, InterruptedException {
        gdbDebugger.stepOver(newDto(StepOverAction.class));

        DebuggerEvent debuggerEvent = events.take();
        assertTrue(debuggerEvent instanceof SuspendEvent);

        SuspendEvent suspendEvent = (SuspendEvent)debuggerEvent;
        assertEquals(suspendEvent.getLocation().getTarget(), "h.cpp");
        assertEquals(suspendEvent.getLocation().getLineNumber(), 5);

        gdbDebugger.stepOver(newDto(StepOverAction.class));

        debuggerEvent = events.take();
        assertTrue(debuggerEvent instanceof SuspendEvent);

        suspendEvent = (SuspendEvent)debuggerEvent;
        assertEquals(suspendEvent.getLocation().getTarget(), "h.cpp");
        assertEquals(suspendEvent.getLocation().getLineNumber(), 6);

        gdbDebugger.stepOver(newDto(StepOverAction.class));

        debuggerEvent = events.take();
        assertTrue(debuggerEvent instanceof SuspendEvent);

        suspendEvent = (SuspendEvent)debuggerEvent;
        assertEquals(suspendEvent.getLocation().getTarget(), "h.cpp");
        assertEquals(suspendEvent.getLocation().getLineNumber(), 7);
    }

    private void doSetAndGetValues() throws DebuggerException {
        VariablePath variablePath = newDto(VariablePath.class);
        variablePath.setPath(Collections.singletonList("i"));

        Variable variable = newDto(Variable.class);
        variable.setValue("2");
        variable.setVariablePath(variablePath);

        Value value = gdbDebugger.getValue(variablePath);
        assertEquals(value.getValue(), "0");

        gdbDebugger.setValue(variable);

        value = gdbDebugger.getValue(variablePath);

        assertEquals(value.getValue(), "2");

        String expression = gdbDebugger.evaluate("i");
        assertEquals(expression, "2");

        expression = gdbDebugger.evaluate("10 + 10");
        assertEquals(expression, "20");

        StackFrameDump stackFrameDump = gdbDebugger.dumpStackFrame();
        assertTrue(stackFrameDump.getFields().isEmpty());
        assertEquals(stackFrameDump.getLocalVariables().size(), 1);
        assertEquals(stackFrameDump.getLocalVariables().get(0).getName(), "i");
        assertEquals(stackFrameDump.getLocalVariables().get(0).getValue(), "2");
        assertEquals(stackFrameDump.getLocalVariables().get(0).getType(), "int");
    }

    private void startDebugger() throws DebuggerException, InterruptedException {
        gdbDebugger.start(newDto(StartAction.class));

        assertEquals(events.size(), 1);

        DebuggerEvent debuggerEvent = events.take();
        assertTrue(debuggerEvent instanceof SuspendEvent);

        SuspendEvent suspendEvent = (SuspendEvent)debuggerEvent;
        assertEquals(suspendEvent.getLocation().getTarget(), "h.cpp");
        assertEquals(suspendEvent.getLocation().getLineNumber(), 7);
    }

    private void disconnect() throws DebuggerException, InterruptedException {
        gdbDebugger.disconnect();

        assertEquals(events.size(), 1);

        DebuggerEvent debuggerEvent = events.take();
        assertTrue(debuggerEvent instanceof DisconnectEvent);
    }

    private void addBreakpoint() throws DebuggerException, InterruptedException {
        Location location = newDto(Location.class);
        location.setTarget("h.cpp");
        location.setLineNumber(7);

        Breakpoint breakpoint = newDto(Breakpoint.class);
        breakpoint.setLocation(location);
        gdbDebugger.addBreakpoint(breakpoint);

        assertEquals(events.size(), 1);

        DebuggerEvent debuggerEvent = events.take();
        assertTrue(debuggerEvent instanceof BreakpointActivatedEvent);

        BreakpointActivatedEvent breakpointActivatedEvent = (BreakpointActivatedEvent)debuggerEvent;
        assertEquals(breakpointActivatedEvent.getBreakpoint().getLocation().getTarget(), "h.cpp");
        assertEquals(breakpointActivatedEvent.getBreakpoint().getLocation().getLineNumber(), 7);
    }

    private void initializeDebugger() throws DebuggerException {
        Map<String, String> properties = ImmutableMap.of("host", "localhost",
                                                         "port", "1111",
                                                         "file", file,
                                                         "sources", sourceDirectory.getParent().toString());

        GdbDebuggerFactory gdbDebuggerFactory = new GdbDebuggerFactory();
        gdbDebugger = gdbDebuggerFactory.create(properties, events::add);


        DebuggerInfo debuggerInfo = gdbDebugger.getInfo();

        assertEquals(debuggerInfo.getFile(), file);
        assertEquals(debuggerInfo.getHost(), "localhost");
        assertEquals(debuggerInfo.getPort(), 1111);
        assertNotNull(debuggerInfo.getName());
        assertNotNull(debuggerInfo.getVersion());
    }
}
