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

import org.eclipse.che.api.user.server.model.impl.UserImpl;
import org.eclipse.che.api.user.server.spi.tck.UserDaoTest;
import org.testng.ITestContext;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.RollbackException;

import static java.util.Arrays.asList;

/**
 * Tests specific use-cases of {@link JpaUserDao} while
 * usual use-cases are covered by {@link UserDaoTest}.
 *
 * @author Yevhenii Voevodin
 */
public class JpaUserDaoTest {

    private EntityManagerFactory factory;

    @BeforeClass
    public void setUp(ITestContext context) {
        factory = (EntityManagerFactory)context.getAttribute(H2DBServerListener.ENTITY_MANAGER_FACTORY_ATTR_NAME);
    }

    @AfterMethod
    public void cleanUp() {
        final EntityManager manager = factory.createEntityManager();
        manager.getTransaction().begin();
        manager.createQuery("DELETE FROM User").executeUpdate();
        manager.getTransaction().commit();
    }

    @Test
    public void shouldThrowConflictExceptionWithAppropriateMessageWhenIdIsDuplicated() throws Exception {
//        final EntityManager manager = factory.createEntityManager();
//        final UserImpl user = new UserImpl("id", "email", "name", "password", asList("alias1", "alias2"));
//        final UserImpl user2 = new UserImpl("id2", "email2", "name2", "password", asList("alias3", "alias4"));
//        try {
//            manager.getTransaction().begin();
//            manager.persist(user);
//            manager.persist(user2);
//            manager.getTransaction().commit();
//        } catch (RollbackException x) {
//            x.printStackTrace();
//        } catch (RuntimeException x) {
//            System.out.printf("");
//        }
    }
}
