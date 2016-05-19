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
package org.eclipse.che.plugin.jdb.server.utils;

import org.eclipse.che.api.debug.shared.model.impl.LocationImpl;
import org.eclipse.che.api.debugger.server.exceptions.DebuggerException;
import org.eclipse.che.api.debug.shared.model.Location;
import org.eclipse.che.commons.lang.Pair;
import org.eclipse.jdt.core.IClassFile;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IPackageFragmentRoot;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.search.IJavaSearchConstants;
import org.eclipse.jdt.core.search.SearchEngine;
import org.eclipse.jdt.core.search.TypeNameMatch;
import org.eclipse.jdt.core.search.TypeNameMatchRequestor;
import org.eclipse.jdt.internal.ui.search.JavaSearchScopeFactory;

import java.util.ArrayList;
import java.util.List;

/**
 * Java DebuggerUtil
 *
 * @author Alexander Andrienko
 */
public class JavaDebuggerUtils {

    public Location getLocation(com.sun.jdi.Location location) throws DebuggerException {
        String fqn = location.declaringType().name();

        List<IType> types;
        try {
            Pair<char[][], char[][]> fqnPair = prepareFqnToSearch(fqn);
            types = findByFqn(fqnPair.first, fqnPair.second);
        } catch (JavaModelException e) {
            throw new DebuggerException("Can't find class models by fqn: " + fqn, e);
        }

        if (types.isEmpty()) {
            throw new DebuggerException("Type with fully qualified name: " + fqn + " was not found");
        }
        IType type = types.get(0);//we need handle few result!!!!
        if (type.isBinary()) {
            IClassFile classFile = type.getClassFile();
            int libId = classFile.getAncestor(IPackageFragmentRoot.PACKAGE_FRAGMENT_ROOT).hashCode();
            String projectPath = type.getJavaProject().getPath().toOSString();
            return new LocationImpl(classFile.getType().getFullyQualifiedName(), location.lineNumber(), true, libId, projectPath);
        } else {
            ICompilationUnit compilationUnit = type.getCompilationUnit();
            String projectPath = type.getJavaProject().getPath().toOSString();
            return new LocationImpl(compilationUnit.getPath().toOSString(), location.lineNumber(), false, -1, projectPath);
        }
    }

    private Pair<char[][], char[][]> prepareFqnToSearch(String fqn) {
        int lastDotIndex = fqn.trim().lastIndexOf('.');

        char[][] packages;
        if (lastDotIndex == -1) {
            packages = new char[0][];
        } else {
            String packageLine = fqn.substring(0, lastDotIndex);
            packages = new char[][] {packageLine.toCharArray()};
        }

        char[][] names;
        int nestedIndex = fqn.indexOf("$");
        String name;
        if (nestedIndex == -1) {
            name = fqn.substring(lastDotIndex + 1, fqn.length());
            names = new char[][] {name.toCharArray()};
        } else {
            name = fqn.substring(lastDotIndex + 1, nestedIndex);
            names = new char[][] {name.toCharArray()};
        }
        return new Pair<>(packages, names);
    }

    private List<IType> findByFqn(char[][] packages, char[][] names) throws JavaModelException {
        List<IType> result = new ArrayList<>();

        SearchEngine searchEngine = new SearchEngine();

        searchEngine.searchAllTypeNames(packages,
                                        names,
                                        JavaSearchScopeFactory.getInstance().createWorkspaceScope(true),//todo check we can set scope JAVA
                                        new TypeNameMatchRequestor() {
                                            @Override
                                            public void acceptTypeNameMatch(TypeNameMatch typeNameMatch) {
                                                result.add(typeNameMatch.getType());
                                            }
                                        },
                                        IJavaSearchConstants.WAIT_UNTIL_READY_TO_SEARCH,
                                        new org.eclipse.core.runtime.NullProgressMonitor());
        return result;
    }
}
