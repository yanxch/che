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

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

/**
 * Immutable object represents a breakpoint. It isn't designed to be preserved.
 * {@link org.eclipse.che.ide.debug.dto.BreakpointDto} should be used then.
 *
 * @author Evgen Vidolob
 * @author Anatoliy Bazko
 */
public class Breakpoint {
    protected int                              lineNumber;
    protected VirtualFile                      file;
    private   Type                             type;
    private   String                           path;
    private   Map<String, Map<String, String>> attr;

    /**
     * Breakpoint becomes active if is added to a JVM, otherwise it is just a user mark.
     */
    private boolean active;

    //todo unmodificable map?
    public Breakpoint(Type type, int lineNumber, String path, VirtualFile file, boolean active, Map<String, Map<String, String>> attr) {
        this.type = type;
        this.lineNumber = lineNumber;
        this.path = path;
        this.file = file;
        this.active = active;
        this.attr = attr;
    }

    /**
     * Getter for {@link #active}
     */
    public boolean isActive() {
        return active;
    }

    /** @return the type */
    public Type getType() {
        return type;
    }

    /** @return the lineNumber */
    public int getLineNumber() {
        return lineNumber;
    }

    /** @return file path */
    public String getPath() {
        return path;
    }

    /**
     * Returns the file with which this breakpoint is associated.
     *
     * @return file with which this breakpoint is associated
     */
    public VirtualFile getFile() {
        return file;
    }

    /**
     * Return map with additional information about breakpoint. Key of this map this is type of debugger which need additional information.
     * Value this is the additional information map. //todo more detail why we do it
     */
    public Map<String, Map<String, String>> getAttr() {
        if (attr == null) {
            return new HashMap<>();
        }
        return attr;
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append("Breakpoint [lineNumber=").append(lineNumber)
               .append(", type=").append(type)
               .append(", active=").append(active)
               .append(", path=").append(path)
               .append(", attr=").append(attr)
               .append("]");
        return builder.toString();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Breakpoint)) return false;

        Breakpoint that = (Breakpoint)o;

        return lineNumber == that.lineNumber && Objects.equals(path, that.path);
    }

    @Override
    public int hashCode() {
        int result = lineNumber;
        result = 31 * result + (path != null ? path.hashCode() : 0);
        return result;
    }

    public enum Type {
        BREAKPOINT, CURRENT
    }
}
