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
package org.eclipse.che.ide.resources.impl;

import com.google.common.annotations.Beta;
import com.google.common.base.MoreObjects;
import com.google.inject.Inject;
import com.google.inject.assistedinject.Assisted;

import org.eclipse.che.api.core.model.project.ProjectConfig;
import org.eclipse.che.api.core.model.project.SourceStorage;
import org.eclipse.che.api.promises.client.Promise;
import org.eclipse.che.api.promises.client.PromiseProvider;
import org.eclipse.che.ide.api.resources.Project;
import org.eclipse.che.ide.resource.Path;

import java.util.List;
import java.util.Map;

import static java.util.Collections.unmodifiableList;
import static java.util.Collections.unmodifiableMap;

/**
 * Default implementation of the {@code Project}.
 *
 * @author Vlad Zhukovskyi
 * @see ContainerImpl
 * @see Project
 * @since 4.0.0-RC14
 */
@Beta
class ProjectImpl extends ContainerImpl implements Project {

    private final ProjectConfig reference;

    @Inject
    protected ProjectImpl(@Assisted ProjectConfig reference,
                          @Assisted ResourceManager resourceManager,
                          PromiseProvider promiseProvider) {
        super(Path.valueOf(reference.getPath()), resourceManager, promiseProvider);

        this.reference = reference;
    }

    /** {@inheritDoc} */
    @Override
    public final int getResourceType() {
        return PROJECT;
    }

    /** {@inheritDoc} */
    @Override
    public String getPath() {
        return getLocation().toString();
    }

    /** {@inheritDoc} */
    @Override
    public String getDescription() {
        return reference.getDescription();
    }

    /** {@inheritDoc} */
    @Override
    public String getType() {
        return reference.getType();
    }

    /** {@inheritDoc} */
    @Override
    public List<String> getMixins() {
        return unmodifiableList(reference.getMixins());
    }

    /** {@inheritDoc} */
    @Override
    public Map<String, List<String>> getAttributes() {
        return unmodifiableMap(reference.getAttributes());
    }

    /** {@inheritDoc} */
    @Override
    public SourceStorage getSource() {
        return reference.getSource();
    }

    /** {@inheritDoc} */
    @Override
    public UpdateRequest update() {
        return new UpdateRequestImpl(this) {
            @Override
            public Promise<Project> send() {
                return resourceManager.update(this);
            }
        };
    }

    /** {@inheritDoc} */
    @Override
    public boolean isProblem() {
        return getMarker(ProblemProjectMarker.PROBLEM_PROJECT).isPresent();
    }

    /** {@inheritDoc} */
    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this)
                          .add("path", getLocation())
                          .add("resource", getResourceType())
                          .add("type", reference.getType())
                          .add("description", reference.getDescription())
                          .add("mixins", reference.getMixins())
                          .add("attributes", reference.getAttributes())
                          .toString();
    }
}
