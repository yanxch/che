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
package org.eclipse.che.api.workspace.server.env.impl.che.opencompose.model;

import java.util.List;
import java.util.Map;

/**
 * @author Alexander Garagatyi
 */
public interface Service {
    String getImage();

    BuildConfig getBuild();

    List<String> getEntrypoint();

    List<String> getCommand();

    Map<String, String> getEnvironment();

    List<String> getDependsOn();

    String getContainerName();

    List<String> getLinks();

    Map<String, String> getLabels();

    List<String> getExpose();

    List<String> getPorts();

    List<String> getVolumesFrom();

    List<String> getVolumes();

    Integer getMemLimit();
}
