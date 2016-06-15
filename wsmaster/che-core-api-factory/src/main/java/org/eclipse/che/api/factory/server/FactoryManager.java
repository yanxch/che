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
package org.eclipse.che.api.factory.server;


import org.eclipse.che.api.factory.server.model.impl.FactoryImpl;
import org.eclipse.che.commons.lang.Pair;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.List;
import java.util.Set;

/**
 *  // TODO : write doc
 *
 * @author Anton Korneta
 */
@Singleton
public class FactoryManager {

    private final FactoryDao factoryDao;

    @Inject
    public FactoryManager(FactoryDao factoryDao) {
        this.factoryDao = factoryDao;
    }

    public FactoryImpl createFactory(FactoryImpl factory) {
        return null;
    }

    public FactoryImpl updateFactory(FactoryImpl factory) {
        return null;
    }

    public FactoryImpl removeFactory(String id) {
        return null;
    }

    public FactoryImpl getById(String id) {
        return null;
    }

    public Set<FactoryImage> getFactoryImages(String id, String qwe) {
        return null;
    }

    public List<FactoryImpl> getByAttribute(int maxItems,
                                      int skipCount,
                                      List<Pair<String, String>> attributes) {
        return null;
    }
}
