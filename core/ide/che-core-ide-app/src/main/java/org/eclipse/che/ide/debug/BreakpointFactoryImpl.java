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

import com.google.inject.Inject;
import com.google.inject.Singleton;

import org.eclipse.che.ide.api.debug.Breakpoint;
import org.eclipse.che.ide.api.debug.BreakpointFactory;
import org.eclipse.che.ide.api.debug.BreakpointRecipe;
import org.eclipse.che.ide.api.resources.VirtualFile;
import org.eclipse.che.ide.util.loging.Log;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.eclipse.che.ide.api.debug.Breakpoint.Type;

/**
 * @author Alexander Andrienko
 */
@Singleton
public class BreakpointFactoryImpl implements BreakpointFactory {

    private List<BreakpointRecipe> recipes;

    @Inject
    public BreakpointFactoryImpl() {
        recipes = new ArrayList<>();
    }

    @Override
    public void registerBreakpointRecipe(BreakpointRecipe breakpointRecipe) {
        recipes.add(breakpointRecipe);
    }

    @Override
    public Breakpoint create(Type type, int lineNumber, String path, VirtualFile file, boolean active) {
        Log.info(getClass(), "empty0");
       return create(type, lineNumber, path, file, active, new HashMap<String, Map<String, String>>());
    }

    @Override
    public Breakpoint create(Type type, int lineNumber, String path, VirtualFile file, boolean active, Map<String, Map<String, String>> attr) {
        Log.info(getClass(), "empty1");
        Breakpoint breakpoint = new Breakpoint(type, lineNumber, path, file, active, attr);

        for (BreakpointRecipe recipe: recipes) {
            recipe.addAdditionalInfo(breakpoint);
        }

        return breakpoint;
    }
}
