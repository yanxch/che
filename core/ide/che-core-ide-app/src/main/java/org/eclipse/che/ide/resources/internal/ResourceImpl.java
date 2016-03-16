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
package org.eclipse.che.ide.resources.internal;

import com.google.common.annotations.Beta;
import com.google.common.base.MoreObjects;
import com.google.common.base.Objects;
import com.google.common.base.Optional;

import org.eclipse.che.api.promises.client.Promise;
import org.eclipse.che.ide.api.resources.Container;
import org.eclipse.che.ide.api.resources.marker.Marker;
import org.eclipse.che.ide.api.resources.Project;
import org.eclipse.che.ide.api.resources.Resource;
import org.eclipse.che.ide.resource.Path;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Default implementation of the {@code Resource}.
 *
 * @author Vlad Zhukovskyi
 * @see Resource
 * @since 4.0.0-RC14
 */
@Beta
abstract class ResourceImpl implements Resource {

    protected ResourceManager resourceManager;
    protected Path            path;

    protected ResourceImpl(Path path, ResourceManager resourceManager) {
        this.path = checkNotNull(path.removeTrailingSeparator(), "Null path occurred");
        this.resourceManager = checkNotNull(resourceManager, "Null project manager occurred");
    }

    /** {@inheritDoc} */
    @Override
    public Promise<Resource> copy(Path destination) {
        return copy(destination, false);
    }

    /** {@inheritDoc} */
    @Override
    public Promise<Resource> copy(Path destination, boolean force) {
        return resourceManager.copy(this, destination, force);
    }

    /** {@inheritDoc} */
    @Override
    public Promise<Resource> move(Path destination) {
        return move(destination, false);
    }

    /** {@inheritDoc} */
    @Override
    public Promise<Resource> move(Path destination, boolean force) {
        return resourceManager.move(this, destination, force);
    }

    /** {@inheritDoc} */
    @Override
    public Promise<Void> delete() {
        return resourceManager.delete(this);
    }

    /** {@inheritDoc} */
    @Override
    public Path getLocation() {
        return path;
    }

    /** {@inheritDoc} */
    @Override
    public Optional<Container> getParent() {
        return resourceManager.parentOf(this);
    }

    /** {@inheritDoc} */
    @Override
    public Project getRelatedProject() {
        if (this instanceof Project) {
            return (Project)this;
        }

        Optional<Container> optionalParent = getParent();

        if (!optionalParent.isPresent()) {
            throw new IllegalStateException("Related project wasn't found");
        }

        Container parent = optionalParent.get();

        while (!(parent instanceof Project)) {
            optionalParent = parent.getParent();

            if (!optionalParent.isPresent()) {
                throw new IllegalStateException("Related project wasn't found");
            }

            parent = optionalParent.get();
        }

        return (Project)parent;
    }

    /** {@inheritDoc} */
    @Override
    public abstract int getResourceType();

    /** {@inheritDoc} */
    @Override
    public String getName() {
        return path.lastSegment();
    }

    /** {@inheritDoc} */
    @Override
    public String getURL() {
        return resourceManager.getUrl(this);
    }

    /** {@inheritDoc} */
    @Override
    public Optional<Marker> getMarker(String type) {
        return resourceManager.getMarker(this, type);
    }

    /** {@inheritDoc} */
    @Override
    public Marker[] getMarkers() {
        return resourceManager.getMarkers(this);
    }

    /** {@inheritDoc} */
    @Override
    public void addMarker(Marker marker) {
        resourceManager.addMarker(this, marker);
    }

    /** {@inheritDoc} */
    @Override
    public boolean deleteMarker(String type) {
        return resourceManager.deleteMarker(this, type);
    }

    /** {@inheritDoc} */
    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }

        if (!(o instanceof Resource)) {
            return false;
        }

        Resource resource = (Resource)o;
        return getResourceType() == resource.getResourceType() && getLocation().equals(resource.getLocation());
    }

    /** {@inheritDoc} */
    @Override
    public int hashCode() {
        return Objects.hashCode(getResourceType(), getLocation());
    }

    /** {@inheritDoc} */
    @Override
    public int compareTo(Resource o) {
        return getLocation().toString().compareTo(o.getLocation().toString());
    }

    /** {@inheritDoc} */
    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this)
                          .add("path", path)
                          .add("resource", getResourceType())
                          .toString();
    }
}
