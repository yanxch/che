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

/**
 * @author Yevhenii Voevodin
 */
public class H2DBErrorCodesMapper implements DBErrorCodesMapper {

    @Override
    public DBErrorCode map(int vendorDbErrorCode) {
        switch (vendorDbErrorCode) {
            case 23505:
                return DBErrorCode.DUPLICATE_KEY;
            default:
                return DBErrorCode.UNDEFINED;
        }
    }
}
