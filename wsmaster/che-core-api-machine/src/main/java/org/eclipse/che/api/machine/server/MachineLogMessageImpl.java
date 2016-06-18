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
package org.eclipse.che.api.machine.server;

import org.eclipse.che.api.core.model.machine.MachineLogMessage;

import java.util.Objects;

/**
 * author Alexander Garagatyi
 */
public class MachineLogMessageImpl implements MachineLogMessage {
    private String machine;
    private String content;

    public MachineLogMessageImpl() {}

    public MachineLogMessageImpl(String machine, String content) {
        this.machine = machine;
        this.content = content;
    }

    @Override
    public String getMachine() {
        return machine;
    }

    public void setMachine(String machine) {
        this.machine = machine;
    }


    @Override
    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof MachineLogMessageImpl)) return false;
        MachineLogMessageImpl that = (MachineLogMessageImpl)o;
        return Objects.equals(machine, that.machine) &&
               Objects.equals(content, that.content);
    }

    @Override
    public int hashCode() {
        return Objects.hash(machine, content);
    }

    @Override
    public String toString() {
        return "MachineLogMessageImpl{" +
               "machine='" + machine + '\'' +
               ", content='" + content + '\'' +
               '}';
    }
}
