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
package org.eclipse.che.api.user.server.jpa;

import org.eclipse.che.api.core.ConflictException;
import org.eclipse.che.api.core.NotFoundException;
import org.eclipse.che.api.core.ServerException;
import org.eclipse.che.api.user.server.model.impl.ProfileImpl;
import org.eclipse.che.api.user.server.spi.ProfileDao;

public class JpaProfileDao implements ProfileDao {
    @Override
    public void create(ProfileImpl profile) throws ServerException, ConflictException {

    }

    @Override
    public void update(ProfileImpl profile) throws NotFoundException, ServerException {

    }

    @Override
    public void remove(String id) throws ServerException {

    }

    @Override
    public ProfileImpl getById(String id) throws NotFoundException, ServerException {
        return null;
    }
}
