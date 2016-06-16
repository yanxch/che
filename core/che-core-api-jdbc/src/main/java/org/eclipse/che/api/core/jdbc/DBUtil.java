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
package org.eclipse.che.api.core.jdbc;

import org.eclipse.che.commons.annotation.Nullable;

import java.sql.SQLException;

/**
 * @author Yevhenii Voevodin
 */
public final class DBUtil {

    /**
     * Searches for {@link SQLException} in the causes chain
     * starting from the given {@code throwable} and returns the
     * {@link SQLException#getErrorCode()} of the found {@code SQLException}.
     *
     * @param throwable
     *         the base exception class for searching {@code SQLException}
     * @return the {@link SQLException#getErrorCode()} or -1 if {@code SQLException}
     * is missed in the causes chain of the given {@code throwable}
     */
    public static int extractSqlErrorCode(@Nullable Throwable throwable) {
        if (throwable == null) {
            return -1;
        }
        if (throwable instanceof SQLException) {
            return ((SQLException)throwable).getErrorCode();
        }
        return extractSqlErrorCode(throwable.getCause());
    }

    private DBUtil() {}
}
