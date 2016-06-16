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
package org.eclipse.che.api.user.server.dao.jpa;

import org.eclipse.che.api.user.server.model.impl.UserImpl;
import org.eclipse.persistence.config.TargetServer;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.Persistence;
import javax.persistence.RollbackException;
import java.util.HashMap;
import java.util.Map;

import static java.util.Arrays.asList;
import static javax.persistence.spi.PersistenceUnitTransactionType.RESOURCE_LOCAL;
import static org.eclipse.persistence.config.PersistenceUnitProperties.JDBC_DRIVER;
import static org.eclipse.persistence.config.PersistenceUnitProperties.JDBC_PASSWORD;
import static org.eclipse.persistence.config.PersistenceUnitProperties.JDBC_URL;
import static org.eclipse.persistence.config.PersistenceUnitProperties.JDBC_USER;
import static org.eclipse.persistence.config.PersistenceUnitProperties.TARGET_SERVER;
import static org.eclipse.persistence.config.PersistenceUnitProperties.TRANSACTION_TYPE;

public class JpaDaoTest {

    private EntityManagerFactory factory;

    @BeforeMethod
    public void setUp() {
        final Map<String, String> properties = new HashMap<>();
        properties.put(TRANSACTION_TYPE, RESOURCE_LOCAL.name());
        properties.put(JDBC_DRIVER, "org.h2.Driver");
        properties.put(JDBC_URL, "jdbc:h2:mem:;DB_CLOSE_DELAY=0;MVCC=true;TRACE_LEVEL_FILE=4;TRACE_LEVEL_SYSTEM_OUT=4");
        properties.put(JDBC_USER, "");
        properties.put(JDBC_PASSWORD, "");
        properties.put(TARGET_SERVER, TargetServer.None);

        factory = Persistence.createEntityManagerFactory("main", properties);
    }

    @Test
    public void test() throws Exception {
        final EntityManager manager = factory.createEntityManager();
        final UserImpl user = new UserImpl("id", "email", "name", "password", asList("alias1", "alias2"));
        final UserImpl user2 = new UserImpl("id2", "email2", "name2", "password", asList("alias1", "alias4"));
        try {
            manager.getTransaction().begin();
            manager.persist(user);
            manager.persist(user2);
            manager.getTransaction().commit();
        } catch (RollbackException x) {
            x.printStackTrace();
        } catch (RuntimeException x) {
            System.out.printf("");
        }
    }
}
