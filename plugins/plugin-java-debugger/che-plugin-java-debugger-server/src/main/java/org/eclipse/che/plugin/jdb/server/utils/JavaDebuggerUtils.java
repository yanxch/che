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

import com.google.common.annotations.VisibleForTesting;
import com.sun.istack.internal.NotNull;

import org.eclipse.che.api.debug.shared.model.impl.LocationImpl;
import org.eclipse.che.api.debugger.server.exceptions.DebuggerException;
import org.eclipse.che.api.debug.shared.model.Location;
import org.eclipse.che.commons.annotation.Nullable;
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
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IMember;
import org.eclipse.jdt.internal.core.JavaModel;
import org.eclipse.jdt.internal.core.SourceMethod;
import org.eclipse.jdt.internal.core.JavaModelManager;

import java.util.ArrayList;
import java.util.List;

/**
 * Class uses for find and handle important information from the Java Model.
 *
 * @author Alexander Andrienko
 */
public class JavaDebuggerUtils {

    private static final JavaModel JAVE_MODEL = JavaModelManager.getJavaModelManager().getJavaModel();

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
        IType type = types.get(0);//TODO we need handle few result! It's temporary solution.
        if (type.isBinary()) {
            IClassFile classFile = type.getClassFile();
            int libId = classFile.getAncestor(IPackageFragmentRoot.PACKAGE_FRAGMENT_ROOT).hashCode();
            String projectPath = type.getJavaProject().getPath().toOSString();
            return new LocationImpl(classFile.getType().getFullyQualifiedName(), location.lineNumber(), true, libId, projectPath, null);
        } else {
            ICompilationUnit compilationUnit = type.getCompilationUnit();
            String projectPath = type.getJavaProject().getPath().toOSString();
            return new LocationImpl(compilationUnit.getPath().toOSString(), location.lineNumber(), false, -1, projectPath, null);
        }
    }

    @VisibleForTesting
    protected Pair<char[][], char[][]> prepareFqnToSearch(@NotNull String fqn) {
        String outerClassFqn = extractOuterClassFqn(fqn);
        int lastDotIndex = outerClassFqn.trim().lastIndexOf('.');

        char[][] packages;
        char[][] names;
        if (lastDotIndex == -1) {
            packages = new char[0][];
            names = new char[][]{outerClassFqn.toCharArray()};
        } else {
            String packageLine = fqn.substring(0, lastDotIndex);
            packages = new char[][]{packageLine.toCharArray()};

            String nameLine = fqn.substring(lastDotIndex + 1, outerClassFqn.length());
            names = new char[][]{nameLine.toCharArray()};
        }
        return new Pair<>(packages, names);
    }

    private String extractOuterClassFqn(String fqn) {
        //handle fqn in case nested classes
        if (fqn.contains("$")) {
            return fqn.substring(0, fqn.indexOf("$"));
        }
        //handle fqn in case lambda expressions
        if (fqn.contains("$$")) {
            return fqn.substring(0, fqn.indexOf("$$"));
        }
        return fqn;
    }

    private List<IType> findByFqn(char[][] packages, char[][] names) throws JavaModelException {
        List<IType> result = new ArrayList<>();

        SearchEngine searchEngine = new SearchEngine();

        searchEngine.searchAllTypeNames(packages,
                                        names,
                                        SearchEngine.createWorkspaceScope(),
                                        new TypeNameMatchRequestor() {
                                            @Override
                                            public void acceptTypeNameMatch(TypeNameMatch typeNameMatch) {
                                                result.add(typeNameMatch.getType());
                                            }
                                        },
                                        IJavaSearchConstants.WAIT_UNTIL_READY_TO_SEARCH,
                                        new NullProgressMonitor());
        return result;
    }

    public String findFQNByPosition(String projectPath, String parentFqn, int start, int end) throws DebuggerException {
        try {
            IType iType = findTypeByPosition(projectPath, parentFqn, start, end);
            if (iType != null) {
                return iType.getFullyQualifiedName();//what about lyambda ? This will be work for them?
            }
        } catch (JavaModelException e) {
            throw new DebuggerException(e.getMessage(), e);
        }
        throw new DebuggerException("Fully Qualified Name not found");//todo improve this message
    }


    private IType findTypeByPosition(String projectPath, String fqn, int start, int end) throws JavaModelException {//todo think about it
        IType outerClass = findType(projectPath, fqn);
        IMember iMember = binSearch(outerClass, start, end);
        if (iMember == null) {
            return null;
        }
        if (iMember instanceof SourceMethod) {
            return iMember.getDeclaringType();//todo strange moment
        } else if (iMember instanceof IType) {
            return (IType)iMember;
        }
        return outerClass;
    }

    @Nullable
    private IType findType(String projectPath, String fqn) throws JavaModelException {
        IJavaProject project = JAVE_MODEL.getJavaProject(projectPath);
        if (project != null) {
            return project.findType(fqn);
        }
        return null;
    }

    /**
     * Searches the given source range of the container for a member that is
     * not the same as the given type.
     * @param type the {@link IType}
     * @param start the starting position
     * @param end the ending position
     * @return the {@link IMember} from the given start-end range
     * @throws JavaModelException if there is a problem with the backing Java model
     */
    @Nullable
    private IMember binSearch(IType type, int start, int end) throws JavaModelException {
        IJavaElement je = getElementAt(type, start);
        if (je != null && !je.equals(type)) {
            return asMember(je);
        }
        if (end > start) {
            je = getElementAt(type, end);
            if (je != null && !je.equals(type)) {
                return asMember(je);
            }
            int mid = ((end - start) / 2) + start;
            if (mid > start) {
                je = binSearch(type, start + 1, mid);
                if (je == null) {
                    je = binSearch(type, mid + 1, end - 1);
                }
                return asMember(je);
            }
        }
        return null;
    }

    /**
     * Returns the given Java element if it is an
     * <code>IMember</code>, otherwise <code>null</code>.
     *
     * @param element Java element
     * @return the given element if it is a type member,
     * 	otherwise <code>null</code>
     */
    @Nullable
    private static IMember asMember(IJavaElement element) {
        if (element instanceof IMember) {
            return (IMember)element;
        }
        return null;
    }

    /**
     * Returns the element at the given position in the given type
     * @param type the {@link IType}
     * @param pos the position
     * @return the {@link IJavaElement} at the given position
     * @throws JavaModelException if there is a problem with the backing Java model
     */
    private static IJavaElement getElementAt(IType type, int pos) throws JavaModelException {
        if (type.isBinary()) {
            return type.getClassFile().getElementAt(pos);
        }
        return type.getCompilationUnit().getElementAt(pos);
    }
}
