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
package org.eclipse.che.api.workspace.server.env.impl.che.opencompose.impl;

import org.eclipse.che.api.workspace.server.env.impl.che.opencompose.model.EnvironmentRecipeContent;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * @author Alexander Garagatyi
 */
public class EnvironmentRecipeContentImpl implements EnvironmentRecipeContent {
    private String                   version;
    private Map<String, ServiceImpl> services;

    public EnvironmentRecipeContentImpl() {}

    public EnvironmentRecipeContentImpl(EnvironmentRecipeContent recipeContent) {
        version = recipeContent.getVersion();
        services = recipeContent.getServices()
                                .entrySet()
                                .stream()
                                .collect(Collectors.toMap(Map.Entry::getKey,
                                                          entry -> new ServiceImpl(entry.getValue())));
    }

    @Override
    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        this.version = version;
    }

    @Override
    public Map<String, ServiceImpl> getServices() {
        if (services == null) {
            services = new HashMap<>();
        }
        return services;
    }

    public void setServices(Map<String, ServiceImpl> services) {
        this.services = services;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof EnvironmentRecipeContentImpl)) return false;
        EnvironmentRecipeContentImpl that = (EnvironmentRecipeContentImpl)o;
        return Objects.equals(version, that.version) &&
               Objects.equals(services, that.services);
    }

    @Override
    public int hashCode() {
        return Objects.hash(version, services);
    }

    @Override
    public String toString() {
        return "EnvironmentRecipeContentImpl{" +
               "version='" + version + '\'' +
               ", services=" + services +
               '}';
    }
}
