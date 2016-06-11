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
package org.eclipse.che.ide.api.debug;

import org.eclipse.che.ide.api.resources.VirtualFile;

import java.util.Map;

import static org.eclipse.che.ide.api.debug.Breakpoint.Type;

/**
 * @author Alexander Andrienko
 */
public interface BreakpointFactory {

    void registerBreakpointRecipe(BreakpointRecipe breakpointRecipe);

    Breakpoint create(Type type, int lineNumber, String path, VirtualFile file, boolean active);

    Breakpoint create(Type type, int lineNumber, String path, VirtualFile file, boolean active, Map<String, Map<String, String>> attr);
}
