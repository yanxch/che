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
package org.eclipse.che.plugin.jdb.ide.debug;

import com.google.inject.Inject;
import com.google.inject.name.Named;
import com.google.web.bindery.event.shared.EventBus;

import org.eclipse.che.api.debug.shared.model.Location;
import org.eclipse.che.commons.annotation.Nullable;
import org.eclipse.che.ide.api.debug.Breakpoint;
import org.eclipse.che.ide.api.debug.BreakpointFactory;
import org.eclipse.che.ide.api.debug.BreakpointManager;
import org.eclipse.che.ide.api.debug.BreakpointRecipe;
import org.eclipse.che.ide.api.debug.DebuggerServiceClient;
import org.eclipse.che.ide.api.resources.VirtualFile;
import org.eclipse.che.ide.debug.DebuggerDescriptor;
import org.eclipse.che.ide.debug.DebuggerManager;
import org.eclipse.che.ide.dto.DtoFactory;
import org.eclipse.che.ide.ext.java.client.project.node.JavaFileNode;
import org.eclipse.che.ide.ext.java.client.project.node.jar.JarFileNode;
import org.eclipse.che.ide.util.storage.LocalStorageProvider;
import org.eclipse.che.ide.websocket.MessageBusProvider;
import org.eclipse.che.plugin.debugger.ide.debug.AbstractDebugger;

import javax.validation.constraints.NotNull;
import java.util.Map;

import static org.eclipse.che.plugin.jdb.ide.breakpoint.recipe.JavaFileBreakpointRecipe.FQN;
import static org.eclipse.che.plugin.jdb.ide.debug.JavaDebugger.ConnectionProperties.HOST;
import static org.eclipse.che.plugin.jdb.ide.debug.JavaDebugger.ConnectionProperties.PORT;


/**
 * The java debugger.
 *
 * @author Anatoliy Bazko
 */
public class JavaDebugger extends AbstractDebugger {

    public static final String ID = "jdb";

    @Inject
    public JavaDebugger(DebuggerServiceClient service,
                        DtoFactory dtoFactory,
                        LocalStorageProvider localStorageProvider,
                        MessageBusProvider messageBusProvider,
                        EventBus eventBus,
                        JavaDebuggerFileHandler javaDebuggerFileHandler,
                        DebuggerManager debuggerManager,
                        BreakpointManager breakpointManager,
                        BreakpointFactory breakpointFactory,
                        @Named("JavaFileBreakpoint") BreakpointRecipe javaBreakpointRecipe,
                        @Named("JarFileBreakpoint") BreakpointRecipe jarBreakpointRecipe) {
        super(service,
              dtoFactory,
              localStorageProvider,
              messageBusProvider,
              eventBus,
              javaDebuggerFileHandler,
              debuggerManager,
              breakpointManager,
              breakpointFactory,
              ID);
        breakpointFactory.registerBreakpointRecipe(jarBreakpointRecipe);
        breakpointFactory.registerBreakpointRecipe(javaBreakpointRecipe);
    }

    @Override
    protected String fqnToPath(@NotNull Location location) {
        String resourcePath = location.getResourcePath();
        return  resourcePath != null ? resourcePath : location.getTarget();
    }

    @Nullable
    @Override
    protected String pathToFqn(VirtualFile file) {
        if (file instanceof JavaFileNode) {
            return ((JavaFileNode)file).getFqn();
        }
        if (file instanceof JarFileNode) {
            return file.getPath();
        }
        return null;
    }

    @Override
    protected DebuggerDescriptor toDescriptor(Map<String, String> connectionProperties) {
        String address = connectionProperties.get(HOST.toString()) + ":" + connectionProperties.get(PORT.toString());
        return new DebuggerDescriptor("", address);
    }

    @Nullable
    @Override
    protected String getTarget(Breakpoint breakpoint) {
        String target = null;
        Map<String, String> attr = breakpoint.getAttr().get(ID);
        if (attr != null) {
            target = attr.get(FQN);
        }
        return target;
    }

    public enum ConnectionProperties {
        HOST,
        PORT
    }
}
