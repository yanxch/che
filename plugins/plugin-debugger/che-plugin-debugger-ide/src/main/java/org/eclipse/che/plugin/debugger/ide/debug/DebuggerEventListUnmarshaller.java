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

import com.google.gwt.json.client.JSONObject;
import com.google.gwt.json.client.JSONParser;

import org.eclipse.che.api.debugger.shared.dto.events.BreakpointActivatedEvent;
import org.eclipse.che.api.debugger.shared.dto.events.DebuggerEvent;
import org.eclipse.che.api.debugger.shared.dto.events.DisconnectEvent;
import org.eclipse.che.api.debugger.shared.dto.events.SuspendEvent;
import org.eclipse.che.ide.commons.exception.UnmarshallerException;
import org.eclipse.che.ide.dto.DtoFactory;
import org.eclipse.che.ide.websocket.Message;
import org.eclipse.che.ide.websocket.rest.Unmarshallable;

/**
 * Unmarshaller for deserializing debugger event which is received over WebSocket connection.
 *
 * @author Artem Zatsarynnyi
 */
public class DebuggerEventListUnmarshaller implements Unmarshallable<DebuggerEvent> {
    private DtoFactory    dtoFactory;
    private DebuggerEvent event;

    public DebuggerEventListUnmarshaller(DtoFactory dtoFactory) {
        this.dtoFactory = dtoFactory;
    }

    @Override
    public void unmarshal(Message response) throws UnmarshallerException {
        JSONObject jsonObject = JSONParser.parseStrict(response.getBody()).isObject();
        if (jsonObject == null) {
            return;
        }

        if (jsonObject.containsKey("type")) {
            final int type = (int)jsonObject.get("type").isNumber().doubleValue();
            if (DebuggerEvent.SUSPEND == type) {
                event = dtoFactory.createDtoFromJson(jsonObject.toString(), SuspendEvent.class);

            } else if (DebuggerEvent.BREAKPOINT_ACTIVATED == type) {
                event = dtoFactory.createDtoFromJson(jsonObject.toString(), BreakpointActivatedEvent.class);

            } else if (DebuggerEvent.DISCONNECTED == type) {
                event = dtoFactory.createDtoFromJson(jsonObject.toString(), DisconnectEvent.class);
            } else {
                throw new UnmarshallerException("Can't parse response.",
                                                new IllegalArgumentException("Unknown debug event type: " + type));
            }
        }
    }

    @Override
    public DebuggerEvent getPayload() {
        return event;
    }
}
