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
package org.eclipse.che.ide.api.resources.marker;

import com.google.common.annotations.Beta;
import com.google.common.base.Objects;

import java.util.HashMap;
import java.util.Map;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Base class for implementing {@link Marker}. Third party component should extends this class to create own implementation
 * of markers.
 *
 * TODO create observable mechanism to notify it if marker has changed own state
 *
 * @author Vlad Zhukovskiy
 * @see Marker
 * @since 4.0.0-RC14
 */
@Beta
public abstract class AbstractMarker implements Marker {

    protected Map<String, Object> attributes = null;

    /** {@inheritDoc} */
    @Override
    public Object getAttribute(String attributeName) {
        return attributes == null ? null : attributes.get(attributeName);
    }

    /** {@inheritDoc} */
    @Override
    public int getAttribute(String attributeName, int defaultValue) {
        if (attributes == null) {
            return defaultValue;
        }

        final Object value = attributes.get(attributeName);

        if (value instanceof Integer) {
            return (Integer)value;
        }

        return defaultValue;
    }

    /** {@inheritDoc} */
    @Override
    public String getAttribute(String attributeName, String defaultValue) {
        if (attributes == null) {
            return defaultValue;
        }

        final Object value = attributes.get(attributeName);

        if (value instanceof String) {
            return (String)value;
        }

        return defaultValue;
    }

    /** {@inheritDoc} */
    @Override
    public boolean getAttribute(String attributeName, boolean defaultValue) {
        if (attributes == null) {
            return defaultValue;
        }

        final Object value = attributes.get(attributeName);

        if (value instanceof Boolean) {
            return (Boolean)value;
        }

        return defaultValue;
    }

    /** {@inheritDoc} */
    @Override
    public Map<String, Object> getAttributes() {
        return attributes;
    }

    /** {@inheritDoc} */
    @Override
    public void setAttribute(String attributeName, int value) {
        setAttribute(attributeName, new Integer(value));
    }

    /** {@inheritDoc} */
    @Override
    public void setAttribute(String attributeName, Object value) {
        checkNotNull(attributeName, "Null attribute provided");

        if (attributes == null) {
            attributes = new HashMap<>();
        }

        attributes.put(attributeName, value);
    }

    /** {@inheritDoc} */
    @Override
    public void setAttribute(String attributeName, boolean value) {
        setAttribute(attributeName, value ? Boolean.TRUE : Boolean.FALSE);
    }

    /** {@inheritDoc} */
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof AbstractMarker)) return false;
        AbstractMarker that = (AbstractMarker)o;
        return Objects.equal(attributes, that.attributes) && Objects.equal(getType(), that.getType());
    }

    /** {@inheritDoc} */
    @Override
    public int hashCode() {
        return Objects.hashCode(attributes, getType());
    }
}
