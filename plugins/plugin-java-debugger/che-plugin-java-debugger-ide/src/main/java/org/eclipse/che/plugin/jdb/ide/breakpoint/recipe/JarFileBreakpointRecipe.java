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
package org.eclipse.che.plugin.jdb.ide.breakpoint.recipe;

import com.google.inject.Singleton;

import org.eclipse.che.ide.api.debug.Breakpoint;
import org.eclipse.che.ide.api.debug.BreakpointRecipe;
import org.eclipse.che.ide.api.resources.VirtualFile;
import org.eclipse.che.ide.ext.java.client.project.node.jar.JarFileNode;

import java.util.HashMap;
import java.util.Map;

import static org.eclipse.che.plugin.jdb.ide.debug.JavaDebugger.ID;

/**
 * @author Alexander Andrienko
 */
@Singleton
public class JarFileBreakpointRecipe implements BreakpointRecipe {
    public static final String FQN = "fqn";
    public static final String BINARY = "binary";

    @Override
    public void addAdditionalInfo(Breakpoint breakpoint) {
        VirtualFile virtualFile = breakpoint.getFile();
        String fqn = null;
        if (virtualFile instanceof JarFileNode) {
            fqn = virtualFile.getPath();
        }

        if (fqn == null) {
            return;
        }

        Map<String, String> attr = new HashMap<>();
        attr.put(FQN, fqn);
        attr.put(BINARY, "binary");

        breakpoint.getAttr().put(ID, attr);
    }
}
