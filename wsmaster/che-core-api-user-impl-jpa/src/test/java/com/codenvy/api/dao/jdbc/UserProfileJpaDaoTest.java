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

import com.beust.jcommander.internal.Maps;

import org.eclipse.che.api.core.NotFoundException;
import org.eclipse.che.api.user.server.dao.Profile;
import org.eclipse.che.api.user.server.dao.UserProfileDao;
import org.eclipse.persistence.config.TargetServer;
import org.testng.annotations.BeforeTest;
import org.testng.annotations.Test;

import javax.persistence.EntityManagerFactory;
import javax.persistence.Persistence;
import javax.persistence.spi.PersistenceUnitTransactionType;
import java.util.HashMap;
import java.util.Map;

import static org.eclipse.persistence.config.PersistenceUnitProperties.JDBC_DRIVER;
import static org.eclipse.persistence.config.PersistenceUnitProperties.JDBC_PASSWORD;
import static org.eclipse.persistence.config.PersistenceUnitProperties.JDBC_URL;
import static org.eclipse.persistence.config.PersistenceUnitProperties.JDBC_USER;
import static org.eclipse.persistence.config.PersistenceUnitProperties.TARGET_SERVER;
import static org.eclipse.persistence.config.PersistenceUnitProperties.TRANSACTION_TYPE;
import static org.testng.Assert.assertEquals;


public class UserProfileJpaDaoTest {


    EntityManagerFactory factory;

    @BeforeTest
    public void setUp() {
        Map<String, String> properties = new HashMap();

        // Ensure RESOURCE_LOCAL transactions is used.
        properties.put(TRANSACTION_TYPE,
                       PersistenceUnitTransactionType.RESOURCE_LOCAL.name());

        // Configure the internal connection pool
        properties.put(JDBC_DRIVER, "org.h2.Driver");
        properties.put(JDBC_URL, "jdbc:h2:mem:db1;DB_CLOSE_DELAY=0;MVCC=true;TRACE_LEVEL_FILE=4;TRACE_LEVEL_SYSTEM_OUT=4");
        properties.put(JDBC_USER, "");
        properties.put(JDBC_PASSWORD, "");


        // Ensure that no server-platform is configured
        properties.put(TARGET_SERVER, TargetServer.None);
        factory = Persistence.createEntityManagerFactory("unitName", properties);

    }


    @Test
    public void shouldBeAbleToSaveAndGet() throws Exception {
        //given
        Profile expected =
                new Profile("id2222").withUserId("user234234").withAttributes(Maps.newHashMap("key1", "value2", "key3", "value4"));
        UserProfileDao dao = new UserProfileJpaDao(factory);
        //when
        dao.create(expected);
        Profile actual = dao.getById("id2222");
        //then
        assertEquals(actual, expected);
    }

    @Test
    public void shouldBeAbleToSaveAndGetMultiple() throws Exception {
        //given
        Profile p1 = new Profile("id11").withUserId("user1").withAttributes(Maps.newHashMap("key1", "value2", "key3", "value4"));
        Profile p2 = new Profile("id22").withUserId("user2").withAttributes(Maps.newHashMap("key12", "value22", "key32", "value42"));
        UserProfileDao dao = new UserProfileJpaDao(factory);
        //when
        dao.create(p1);
        dao.create(p2);
        Profile ap1 = dao.getById("id11");
        Profile ap2 = dao.getById("id22");
        //then
        assertEquals(ap1, p1);
        assertEquals(ap2, p2);
    }

    @Test
    public void shouldBeAbleToUpdateExisted() throws Exception {
        //given
        Profile expected =
                new Profile("id2222").withUserId("user234234").withAttributes(Maps.newHashMap("key1", "value2", "key3", "value4"));
        UserProfileDao dao = new UserProfileJpaDao(factory);
        dao.create(expected);
        expected = new Profile("id2222").withUserId("user234234").withAttributes(Maps.newHashMap("key1", "v33", "key4", "value5"));
        //when
        dao.update(expected);
        Profile actual = dao.getById("id2222");
        //then
        assertEquals(actual, expected);
    }


    @Test(expectedExceptions = NotFoundException.class)
    public void shouldBeAbleToDeleteExisted() throws Exception {
        //given
        Profile expected =
                new Profile("id2222").withUserId("user234234").withAttributes(Maps.newHashMap("key1", "value2", "key3", "value4"));
        UserProfileDao dao = new UserProfileJpaDao(factory);
        dao.create(expected);
        //when
        dao.remove("id2222");
        //then
        dao.getById("id2222");

    }
}
