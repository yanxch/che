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
package com.codenvy.api.dao.jdbc;

import org.eclipse.che.api.core.NotFoundException;
import org.eclipse.che.api.core.ServerException;
import org.eclipse.che.api.user.server.dao.PreferenceDao;

import java.util.Map;

/**
 * Created by sj on 15.04.16.
 */
public class PreferenceDaoJdbcDao implements PreferenceDao {
    @Override
    public void setPreferences(String userId, Map<String, String> preferences) throws ServerException, NotFoundException {

    }

    @Override
    public Map<String, String> getPreferences(String userId) throws ServerException {
        return null;
    }

    @Override
    public Map<String, String> getPreferences(String userId, String filter) throws ServerException {
        return null;
    }

    @Override
    public void remove(String userId) throws ServerException {

    }
}
