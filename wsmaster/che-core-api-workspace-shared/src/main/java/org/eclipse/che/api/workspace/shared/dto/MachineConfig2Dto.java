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
package org.eclipse.che.api.workspace.shared.dto;

import org.eclipse.che.api.core.model.machine.MachineConfig2;
import org.eclipse.che.api.core.rest.shared.dto.Hyperlinks;
import org.eclipse.che.api.core.rest.shared.dto.Link;
import org.eclipse.che.api.machine.shared.dto.ServerConfDto;
import org.eclipse.che.dto.shared.DTO;

import java.util.List;
import java.util.Map;

/**
 * @author Alexander Garagatyi
 */
@DTO
public interface MachineConfig2Dto extends MachineConfig2, Hyperlinks {
    @Override
    List<String> getAgents();

    void setAgents(List<String> agents);

    MachineConfig2Dto withAgents(List<String> agents);

    @Override
    Map<String, ServerConfDto> getServers();

    void setServers(Map<String, ServerConfDto>  servers);

    MachineConfig2Dto withServers(Map<String, ServerConfDto>  servers);

    @Override
    MachineConfig2Dto withLinks(List<Link> links);
}
