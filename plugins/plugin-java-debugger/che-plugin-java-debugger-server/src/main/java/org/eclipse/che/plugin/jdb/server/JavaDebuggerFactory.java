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
package org.eclipse.che.plugin.jdb.server;

import org.eclipse.che.api.debugger.server.Debugger;
import org.eclipse.che.api.debugger.server.DebuggerFactory;
import org.eclipse.che.api.debugger.server.exceptions.DebuggerException;

import java.util.HashMap;
import java.util.Map;

/**
 * @author Anatoliy Bazko
 */
public class JavaDebuggerFactory implements DebuggerFactory {

    private static final String TYPE = "jdb";

    @Override
    public String getType() {
        return TYPE;
    }

    @Override
    public Debugger create(Map<String, String> properties, Debugger.DebuggerCallBack debuggerCallBack) throws DebuggerException {
        Map<String, String> normalizedProps = new HashMap<>(properties.size());
        properties.keySet().stream().forEach(key -> normalizedProps.put(key.toLowerCase(), properties.get(key)));

        String host = normalizedProps.get("host");
        if (host == null) {
            throw new DebuggerException("Can't establish connection: host property is unknown.");
        }

        String portProp = normalizedProps.get("port");
        if (portProp == null) {
            throw new DebuggerException("Can't establish connection: port property is unknown.");
        }

        int port;
        try {
            port = Integer.parseInt(portProp);
        } catch (NumberFormatException e) {
            throw new DebuggerException("Unknown port property format: " + portProp);
        }

        return new JavaDebugger(host, port, debuggerCallBack);
    }
}
