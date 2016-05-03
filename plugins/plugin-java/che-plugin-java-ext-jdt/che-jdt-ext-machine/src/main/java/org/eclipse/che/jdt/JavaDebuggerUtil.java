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
//todo we need discuss about this licence, we need exclude it and add eclipse licence
package org.eclipse.che.jdt;

import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IMember;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTParser;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.internal.core.SourceMethod;
import org.eclipse.jdt.internal.core.JavaModelManager;

public class JavaDebuggerUtil {

    public static IType findTypeByPosition(String projectPath, String fqn, int lineNumber) throws JavaModelException {//todo rename or change return type
        IType outerClass = findType(projectPath, fqn);
        ASTParser parser = ASTParser.newParser(AST.JLS8);
        parser.setKind(ASTParser.K_COMPILATION_UNIT);
        parser.setSource(outerClass.getClassFile());
        parser.setResolveBindings(true);

        CompilationUnit compilationUnit = (CompilationUnit)parser.createAST(null);
        int start = compilationUnit.getPosition(lineNumber, 0);



        return null;
    }

    public static IType findTypeByPosition(String projectPath, String fqn, int start, int end) throws JavaModelException {//todo rename or change return type
        IType outerClass = findType(projectPath, fqn);
        IMember iMember = binSearch(outerClass, start, end);
        if (iMember == null) {
            return null;
        }

        if (iMember instanceof SourceMethod) {
            return (IType)((SourceMethod)iMember).getDeclaringType();
        } else if (iMember instanceof IType) {
            return (IType)iMember;
        }

        return outerClass;
    }

    public static IType findType(String projectPath, String fqn) throws JavaModelException {
        IJavaProject project = JavaModelManager.getJavaModelManager().getJavaModel().getJavaProject(projectPath);
        //todo check null
        return project.findType(fqn);
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
    private static IMember binSearch(IType type, int start, int end) throws JavaModelException {
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
    protected static IJavaElement getElementAt(IType type, int pos) throws JavaModelException {
        if (type.isBinary()) {
            return type.getClassFile().getElementAt(pos);
        }
        return type.getCompilationUnit().getElementAt(pos);
    }
}
