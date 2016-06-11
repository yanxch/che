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
package org.eclipse.che.ide.debug;

import org.eclipse.che.ide.api.debug.Breakpoint;
import org.eclipse.che.ide.api.debug.BreakpointFactory;
import org.eclipse.che.ide.api.resources.VirtualFile;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.HashMap;
import java.util.Map;

/**
 * @author Alexander Andrienko
 */
@Singleton
public class BreakpointFactoryImpl implements BreakpointFactory {

    @Inject
    public BreakpointFactoryImpl() {

    }

    @Override
    public void registerBreakpointRecipe() {

    }

    @Override
    public Breakpoint create(Breakpoint.Type type, int lineNumber, String path, VirtualFile file, boolean active) {
        return new Breakpoint(type, lineNumber, path, file, active, new HashMap<String, Map<String, String>>());
    }
}
