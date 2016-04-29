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
package org.eclipse.che.plugin.gdb.server.parser;

import org.eclipse.che.api.debugger.shared.dto.Breakpoint;
import org.eclipse.che.api.debugger.shared.dto.Location;
import org.eclipse.che.dto.server.DtoFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 'info b' command parser.
 *
 * @author Anatoliy Bazko
 */
public class GdbInfoBreak {

    private static final Pattern GDB_INFO_B = Pattern.compile("([0-9]*)\\s*breakpoint.*at\\s*(.*):([0-9]*).*");

    private final List<Breakpoint> breakpoints;

    private GdbInfoBreak(List<Breakpoint> breakpoints) {
        this.breakpoints = breakpoints;
    }

    public List<Breakpoint> getBreakpoints() {
        return breakpoints;
    }

    /**
     * Factory method.
     */
    public static GdbInfoBreak parse(GdbOutput gdbOutput) throws GdbParseException {
        String output = gdbOutput.getOutput();

        List<Breakpoint> breakpoints = new ArrayList<>();

        for (String line : output.split("\n")) {
            Matcher matcher = GDB_INFO_B.matcher(line);
            if (matcher.find()) {
                String file = matcher.group(2);
                String lineNumber = matcher.group(3);

                Location location = DtoFactory.newDto(Location.class);
                location.setTarget(file);
                location.setLineNumber(Integer.parseInt(lineNumber));

                Breakpoint breakpoint = DtoFactory.newDto(Breakpoint.class);
                breakpoint.setLocation(location);

                breakpoints.add(breakpoint);
            }
        }

        return new GdbInfoBreak(breakpoints);
    }
}
