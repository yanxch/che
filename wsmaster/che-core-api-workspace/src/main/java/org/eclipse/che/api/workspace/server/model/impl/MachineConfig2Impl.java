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
package org.eclipse.che.api.workspace.server.model.impl;

import org.eclipse.che.api.core.model.machine.MachineConfig2;
import org.eclipse.che.api.core.model.machine.ServerConf;
import org.eclipse.che.api.machine.server.model.impl.ServerConfImpl;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * @author Alexander Garagatyi
 */
public class MachineConfig2Impl implements MachineConfig2 {
    private List<String> agents;
    private Map<String, ServerConfImpl> servers;

    public MachineConfig2Impl(List<String> agents,
                              Map<String, ServerConf> servers) {
        this.agents = agents;
        if (servers != null) {
            this.servers = servers.entrySet()
                                  .stream()
                                  .collect(Collectors.toMap(Map.Entry::getKey,
                                                            entry -> new ServerConfImpl(entry.getValue())));
        }
    }

    public MachineConfig2Impl(MachineConfig2 config) {
        this(config.getAgents(), config.getServers()
                                       .entrySet()
                                       .stream()
                                       .collect(Collectors.toMap(Map.Entry::getKey,
                                                                 entry -> new ServerConfImpl(entry.getValue()))));
    }

    @Override
    public List<String> getAgents() {
        if (agents == null) {
            agents = new ArrayList<>();
        }

        return agents;
    }

    public void setAgents(List<String> agents) {
        this.agents = agents;
    }

    @Override
    public Map<String, ? extends ServerConf> getServers() {
        if (servers == null) {
            servers = new HashMap<>();
        }
        return servers;
    }

    public void setServers(Map<String, ServerConfImpl> servers) {
        this.servers = servers;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof MachineConfig2Impl)) return false;
        MachineConfig2Impl that = (MachineConfig2Impl)o;
        return Objects.equals(agents, that.agents) &&
               Objects.equals(servers, that.servers);
    }

    @Override
    public int hashCode() {
        return Objects.hash(agents, servers);
    }

    @Override
    public String toString() {
        return "MachineConfig2Impl{" +
               "agents=" + agents +
               ", servers=" + servers +
               '}';
    }
}
