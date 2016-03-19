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

import org.eclipse.che.api.core.model.project.SourceStorage;
import org.eclipse.che.api.promises.client.Promise;
import org.eclipse.che.ide.api.resources.Project;
import org.eclipse.che.ide.api.resources.Project.UpdateRequest;
import org.eclipse.che.ide.resource.Path;

import java.util.List;
import java.util.Map;

/**
 * Base class for update project request.
 *
 * @author Vlad Zhukovskiy
 * @see UpdateRequest
 */
abstract class UpdateRequestImpl implements UpdateRequest {

    private Project                   project;
    private Path                      path;
    private String                    description;
    private String                    type;
    private List<String>              mixins;
    private Map<String, List<String>> attributes;
    private SourceStorage             sourceStorage;

    public UpdateRequestImpl(Project project) {
        this.project = project;
    }

    public UpdateRequestImpl(Path path, String type) {
        this.path = path;
        this.type = type;
    }

    /** {@inheritDoc} */
    @Override
    public abstract Promise<Project> send();

    /** {@inheritDoc} */
    @Override
    public UpdateRequest withAttributes(Map<String, List<String>> attributes) {
        this.attributes = attributes;

        return this;
    }

    /** {@inheritDoc} */
    @Override
    public Map<String, List<String>> getAttributes() {
        if (attributes == null) {
            if (project != null) {
                return project.getAttributes();
            }
        }

        return attributes;
    }

    /** {@inheritDoc} */
    @Override
    public UpdateRequest withMixins(List<String> mixins) {
        this.mixins = mixins;

        return this;
    }

    /** {@inheritDoc} */
    @Override
    public List<String> getMixins() {
        if (mixins == null) {
            if (project != null) {
                return project.getMixins();
            }
        }

        return mixins;
    }

    /** {@inheritDoc} */
    @Override
    public UpdateRequest withType(String type) {
        this.type = type;

        return this;
    }

    /** {@inheritDoc} */
    @Override
    public String getType() {
        if (type == null) {
            if (project != null) {
                return project.getType();
            }
        }

        return type;
    }

    /** {@inheritDoc} */
    @Override
    public UpdateRequest withDescription(String description) {
        this.description = description;

        return this;
    }

    /** {@inheritDoc} */
    @Override
    public String getDescription() {
        if (description == null) {
            if (project != null) {
                return project.getDescription();
            }
        }

        return description;
    }

    /** {@inheritDoc} */
    @Override
    public Path getPath() {
        if (path == null) {
            if (project != null) {
                return project.getLocation();
            }
        }

        return path;
    }

    /** {@inheritDoc} */
    @Override
    public UpdateRequest withPath(Path path) {
        this.path = path;

        return this;
    }

    /** {@inheritDoc} */
    @Override
    public Project getProject() {
        return project;
    }

    /** {@inheritDoc} */
    @Override
    public SourceStorage getSourceStorage() {
        if (sourceStorage == null) {
            if (project != null) {
                return project.getSource();
            }
        }

        return sourceStorage;
    }

    /** {@inheritDoc} */
    @Override
    public UpdateRequest withSourceStorage(SourceStorage sourceStorage) {
        this.sourceStorage = sourceStorage;

        return this;
    }
}
