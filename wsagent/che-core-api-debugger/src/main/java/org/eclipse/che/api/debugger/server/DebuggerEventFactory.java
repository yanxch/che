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

import org.eclipse.che.api.debugger.shared.dto.Location;
import org.eclipse.che.api.debugger.shared.dto.events.BreakpointActivatedEvent;
import org.eclipse.che.api.debugger.shared.dto.events.DebuggerEvent;
import org.eclipse.che.api.debugger.shared.dto.events.DisconnectEvent;
import org.eclipse.che.api.debugger.shared.dto.events.SuspendEvent;

import static org.eclipse.che.dto.server.DtoFactory.newDto;

/**
 * Factory creates specific {@link DebuggerEvent} and sets corresponding type.

 * @author Anatoliy Bazko
 */
public class DebuggerEventFactory {

    public static SuspendEvent newSuspendEvent(Location location) {
        SuspendEvent suspendEvent = newDto(SuspendEvent.class);
        suspendEvent.setType(DebuggerEvent.SUSPEND);
        suspendEvent.setLocation(location);
        return suspendEvent;
    }

    public static DisconnectEvent newDisconnectEvent() {
        DisconnectEvent disconnectEvent = newDto(DisconnectEvent.class);
        disconnectEvent.setType(DebuggerEvent.DISCONNECTED);
        return disconnectEvent;
    }

    public static BreakpointActivatedEvent newBreakpointActivatedEvent() {
        BreakpointActivatedEvent breakpointActivatedEvent = newDto(BreakpointActivatedEvent.class);
        breakpointActivatedEvent.setType(DebuggerEvent.BREAKPOINT_ACTIVATED);
        return breakpointActivatedEvent;
    }
}
