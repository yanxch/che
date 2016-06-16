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

/**
 * An interface for mapping vendor specific database
 * error codes to the values of {@link DBErrorCode}.
 *
 * @author Yevhenii Voevodin
 */
public interface DBErrorCodesMapper {

    /**
     * Maps {@code vendorDbErrorCode} to the value of {@code DBErrorCode}.
     *
     * @param vendorDbErrorCode
     *         vendor specific error code
     * @return an appropriate value of {@code DBErrorCode} or {@link DBErrorCode#UNDEFINED}
     * if the {@code vendorDbErrorCode} can't be mapped to any of those values.
     */
    DBErrorCode map(int vendorDbErrorCode);
}
