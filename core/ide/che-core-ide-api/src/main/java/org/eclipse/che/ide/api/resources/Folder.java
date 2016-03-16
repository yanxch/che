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
 * Folders may be leaf or non-leaf resources and may contain files and/or other folders. A folder resource is stored as
 * a directory in the local file system.
 * <p/>
 * Folder instance can be obtained by calling {@link Container#getContainer(Path)} or by {@link Container#getChildren(boolean)}.
 * <p/>
 * Note. This interface is not intended to be implemented by clients.
 *
 * @author Vlad Zhukovskyi
 * @since 4.0.0-RC14
 */
@Beta
public interface Folder extends Container {
}
