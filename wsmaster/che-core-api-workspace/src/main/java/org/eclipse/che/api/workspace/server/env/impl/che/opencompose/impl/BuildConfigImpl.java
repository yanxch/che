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

import org.eclipse.che.api.workspace.server.env.impl.che.opencompose.model.BuildConfig;

import java.util.Objects;

/**
 * @author Alexander Garagatyi
 */
public class BuildConfigImpl implements BuildConfig {
    private String context;
    private String dockerfile;

    public BuildConfigImpl() {}

    public BuildConfigImpl(BuildConfig buildConfig) {
        this(buildConfig.getContext(), buildConfig.getDockerfile());
    }

    public BuildConfigImpl(String context, String dockerfile) {
        this.context = context;
        this.dockerfile = dockerfile;
    }

    @Override
    public String getContext() {
        return context;
    }

    public void setContext(String context) {
        this.context = context;
    }

    @Override
    public String getDockerfile() {
        return dockerfile;
    }

    public void setDockerfile(String dockerfile) {
        this.dockerfile = dockerfile;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof BuildConfigImpl)) return false;
        BuildConfigImpl that = (BuildConfigImpl)o;
        return Objects.equals(context, that.context) &&
               Objects.equals(dockerfile, that.dockerfile);
    }

    @Override
    public int hashCode() {
        return Objects.hash(context, dockerfile);
    }

    @Override
    public String toString() {
        return "BuildConfigImpl{" +
               "context='" + context + '\'' +
               ", dockerfile='" + dockerfile + '\'' +
               '}';
    }
}
