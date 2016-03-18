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
package org.eclipse.che.ide.api.resources;

import com.google.common.annotations.Beta;

import org.eclipse.che.ide.resource.Path;

/**
 * A resource delta represents changes in the state of concrete resource.
 *
 * @author Vlad Zhukovskiy
 * @since 4.0.0-RC14
 */
@Beta
public interface ResourceDelta {

    /* -- Delta kind -- */

    /**
     * Delta kind constant (bit mask) indicating that the resource has been physically created in its parent.
     *
     * @see ResourceDelta#getKind()
     * @since 4.0.0-RC14
     */
    int CREATED = 0x1;

    /**
     * Delta kind constant (bit mask) indicating that the resource has been physically removed from its parent.
     *
     * @see ResourceDelta#getKind()
     * @since 4.0.0-RC14
     */
    int REMOVED = 0x2;

    /**
     * Delta kind constant (bit mask) indicating that the resource has been changed.
     *
     * @see ResourceDelta#getKind()
     * @since 4.0.0-RC14
     */
    int CHANGED = 0x4;

    /**
     * Delta kind constant (bit mask) indicating that the resource has been loaded into client side cache.
     *
     * @see ResourceDelta#getKind()
     * @since 4.0.0-RC14
     */
    int LOADED_INTO_CACHE = 0x8;

    /**
     * Delta kind constant (bit mask) indicating that the resource has been unloaded from client side cache.
     *
     * @see ResourceDelta#getKind()
     * @since 4.0.0-RC14
     */
    int UNLOADED_FROM_CACHE = 0x10;

    /* -- Constants which describe resource changes -- */

    /**
     * Change constant (bit mask) indicating that the content of the resource has changed.
     *
     * @see ResourceDelta#getFlags()
     * @since 4.0.0-RC14
     */
    int CONTENT = 0x100;

    /**
     * Change constant (bit mask) indicating that the resource was moved from another location. The location can be retrieved
     * using {@link ResourceDelta#getFromPath()}
     *
     * @see ResourceDelta#getFlags()
     * @since 4.0.0-RC14
     */
    int MOVED_FROM = 0x200;

    /**
     * Change constant (bit mask) indicating that the resource was moved to another location. The location can be retrieved
     * using {@link ResourceDelta#getToPath()}
     *
     * @see ResourceDelta#getFlags()
     * @since 4.0.0-RC14
     */
    int MOVED_TO = 0x400;

    /**
     * Change constant (bit mask) indicating that the resource was copied from another location. The location can be retrieved
     * using {@link ResourceDelta#getFromPath()}
     *
     * @see ResourceDelta#getFlags()
     * @since 4.0.0-RC14
     */
    int COPIED_FROM = 0x800;

    /**
     * Returns the kind of this resource delta. Normally, one of {@code CREATED}, {@code REMOVED}, {@code CHANGED},
     * {@code LOADED_INTO_CACHE}, {@code UNLOADED_FROM_CACHE}.
     *
     * @return the kind of this resource delta.
     * @see #CREATED
     * @see #REMOVED
     * @see #CHANGED
     * @since 4.0.0-RC14
     */
    int getKind();

    /**
     * Returns flags which describe in more detail how a resource has been affected.
     * <p/>
     * The following codes (bit masks) are used when kind is {@code CHANGED}, and also when the resource is involved in a move:
     * <ul>
     * <li>{@code CONTENT} - The bytes contained by the resource have been altered, or <code>IResource.touch</code> has been
     * called on the resource.</li>
     * <li>{@code MOVED_FROM} - The resource has moved. {@link #getFromPath()} will return the path of where it was moved from.</li>
     * <li>{@code MOVED_TO} - The resource has moved. {@link #getToPath()} ()} will return the path of where it was moved to.</li>
     * <li>{@code COPIED_FROM} - Change constant (bit mask) indicating that the resource was copied from another location.
     * The location can be retrieved using {@link #getFromPath()}.</li>
     * </ul>
     * <p/>
     * A simple move operation would result in the following delta information. If a resource is moved from A to B (with no other
     * changes to A or B), then A will have kind {@code REMOVED}, with flag {@code MOVED_TO}, and {@link #getToPath()} on A will
     * return the path for B. B will have kind {@code ADDED}, with flag {@code MOVED_FROM}, and {@link #getFromPath()} on B will
     * return the path for A.
     *
     * @return the flags
     * @see #CONTENT
     * @see #MOVED_FROM
     * @see #MOVED_TO
     * @see #COPIED_FROM
     * @see #getKind()
     * @see #getFromPath()
     * @see #getToPath()
     * @since 4.0.0-RC14
     */
    int getFlags();

    /**
     * Returns the path from which resource was moved. This value is valid if the {@code MOVED_FROM} or {@code COPIED_FROM} change
     * flag is set, otherwise, {@code null} is returned.
     *
     * @return instance of {@link Path} or {@code null}
     * @see #getToPath()
     * @see #getFlags()
     * @since 4.0.0-RC14
     */
    Path getFromPath();

    /**
     * Returns the path to which resource was moved. This value is valid if the {@code MOVED_TO} change flag is set, otherwise,
     * {@code null} is returned.
     *
     * @return instance of {@link Path} or {@code null}
     * @see #getFromPath()
     * @see #getFlags()
     * @since 4.0.0-RC14
     */
    Path getToPath();

    /**
     * Returns a handle for the affected resource.
     *
     * @return the affected resource
     * @since 4.0.0-RC14
     */
    Resource getResource();
}
