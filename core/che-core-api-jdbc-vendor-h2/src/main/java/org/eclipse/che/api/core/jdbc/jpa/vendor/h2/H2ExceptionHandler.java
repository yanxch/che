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
package org.eclipse.che.api.core.jdbc.jpa.vendor.h2;

import org.eclipse.che.api.core.jdbc.DBErrorCode;
import org.eclipse.che.api.core.jdbc.DBErrorCodesMapper;
import org.eclipse.persistence.exceptions.ExceptionHandler;

import javax.persistence.RollbackException;

import static org.eclipse.che.api.core.jdbc.DBUtil.extractSqlErrorCode;

/**
 * @author Yevhenii Voevodin
 */
public class H2ExceptionHandler implements ExceptionHandler {

    // TODO move it from here
    private final DBErrorCodesMapper mapper = new H2DBErrorCodesMapper();

    @Override
    public Object handleException(RuntimeException exception) {
        if (exception instanceof RollbackException) {
            final DBErrorCode code = mapper.map(extractSqlErrorCode(exception));
        }
        throw exception;
    }
}
