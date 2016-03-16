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

import org.eclipse.che.ide.api.resources.Project.ProblemProjectMarker;
import org.eclipse.che.ide.api.resources.Resource;

import java.util.Map;

/**
 * Markers are a general mechanism for associating notes and meta-data with resources.
 * <p/>
 * Each marker has a type string, specifying its unique id. The resources plugin defines only one standard
 * marker (at this moment): {@link ProblemProjectMarker#PROBLEM_PROJECT}.
 * <p/>
 * Marker, by nature is only runtime attribute and doesn't store on the server side.
 * <p/>
 * To implement own type of marker, abstract class {@link AbstractMarker} should be extended.
 * This interface is not intended to be implemented by third part components.
 *
 * @author Vlad Zhukovskiy
 * @see Resource#getMarker(String)
 * @see Resource#getMarkers()
 * @see Resource#addMarker(Marker)
 * @see Resource#deleteMarker(String)
 * @see AbstractMarker
 * @since 4.0.0-RC14
 */
@Beta
public interface Marker {

    /* -- Base marker types -- */

    /**
     * Problem marker type.
     * <p/>
     * TODO implement default marker for the problem resource
     *
     * @see #getType()
     * @since 4.0.0-RC14
     */
    String PROBLEM = "problem.marker";

    /**
     * Text marker type.
     * <p/>
     * TODO implement default simple text marker
     *
     * @see #getType()
     * @since 4.0.0-RC14
     */
    String TEXT = "text.marker";

    /* -- Marker attributes -- */

    /**
     * Severity marker attribute. A number from the set of error, warning and info
     * severities defined by the platform.
     *
     * @see #SEVERITY_ERROR
     * @see #SEVERITY_WARNING
     * @see #SEVERITY_INFO
     * @see #getAttribute(String, int)
     * @since 4.0.0-RC14
     */
    String SEVERITY = "severity";

    /**
     * Message marker attribute. A localized string describing the nature of
     * the marker. The content and form of this attribute is not specified or
     * interpreted by the platform.
     *
     * @see #getAttribute(String, String)
     * @since 4.0.0-RC14
     */
    String MESSAGE = "message";

    /* -- Marker values -- */

    /**
     * Error severity constant (value 2) indicating an error state.
     *
     * @see #getAttribute(String, int)
     * @since 4.0.0-RC14
     */
    int SEVERITY_ERROR = 2;

    /**
     * Warning severity constant (value 1) indicating a warning.
     *
     * @see #getAttribute(String, int)
     * @since 4.0.0-RC14
     */
    int SEVERITY_WARNING = 1;

    /**
     * Info severity constant (value 0) indicating information only.
     *
     * @see #getAttribute(String, int)
     * @since 4.0.0-RC14
     */
    int SEVERITY_INFO = 0;

    /**
     * Returns the type of this marker. The returned marker type will not be {@code null}.
     *
     * @return the type of this marker
     * @since 4.0.0-RC14
     */
    String getType();

    /**
     * Returns the attribute with the given name.  The result is an instance of one of the following classes:
     * {@code String}, {@code Integer} or {@code Boolean}. Returns {@code null} if the attribute is undefined.
     *
     * @param attributeName
     *         the name of the attribute
     * @return the value, or {@code null} if the attribute is undefined
     * @since 4.0.0-RC14
     */
    Object getAttribute(String attributeName);

    /**
     * Returns the integer-valued attribute with the given name. Returns the given default value if the attribute.
     * is undefined.
     *
     * @param attributeName
     *         the name of the attribute
     * @param defaultValue
     *         the value to use if no value is found
     * @return the value or the default value if no value was found
     * @since 4.0.0-RC14
     */
    int getAttribute(String attributeName, int defaultValue);

    /**
     * Returns the string-valued attribute with the given name. Returns the given default value if the attribute is
     * undefined.
     *
     * @param attributeName
     *         the name of the attribute
     * @param defaultValue
     *         the value to use if no value is found
     * @return the value or the default value if no value was found
     * @since 4.0.0-RC14
     */
    String getAttribute(String attributeName, String defaultValue);

    /**
     * Returns the boolean-valued attribute with the given name. Returns the given default value if the attribute is
     * undefined.
     *
     * @param attributeName
     *         the name of the attribute
     * @param defaultValue
     *         the value to use if no value is found
     * @return the value or the default value if no value was found
     * @since 4.0.0-RC14
     */
    boolean getAttribute(String attributeName, boolean defaultValue);

    /**
     * Returns a map with all the attributes for the marker.If the marker has no attributes then {@code null} is returned.
     *
     * @return a map of attribute keys and values (key type : {@code String}, value type : {@code String}, {@code Integer},
     * or {@code Boolean}) or {@code null}
     * @since 4.0.0-RC14
     */
    Map<String, Object> getAttributes();

    /**
     * Sets the integer-valued attribute with the given name.
     * <p/>
     * This method doesn't change the resource.
     *
     * @param attributeName
     *         the name of the attribute
     * @param value
     *         the value
     * @since 4.0.0-RC14
     */
    void setAttribute(String attributeName, int value);

    /**
     * Sets the attribute with the given name. The value must be {@code null} or an instance of one of the following classes:
     * {@code String}, {@code Integer}, or {@code Boolean}. If the value is {@code null}, the attribute is considered to be
     * undefined.
     * <p/>
     * This method doesn't change the resource.
     *
     * @param attributeName
     *         the name of the attribute
     * @param value
     *         the value, or {@code null} if the attribute is to be undefined
     * @since 4.0.0-RC14
     */
    void setAttribute(String attributeName, Object value);

    /**
     * Sets the boolean-valued attribute with the given name.
     * <p/>
     * This method doesn't change the resource.
     *
     * @param attributeName
     *         the name of the attribute
     * @param value
     *         the value
     * @since 4.0.0-RC14
     */
    void setAttribute(String attributeName, boolean value);
}
