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

import org.eclipse.che.api.core.ConflictException;
import org.eclipse.che.api.core.NotFoundException;
import org.eclipse.che.api.core.ServerException;
import org.eclipse.che.api.user.server.dao.Profile;
import org.eclipse.che.api.user.server.dao.UserProfileDao;

import javax.inject.Inject;
import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import java.util.LinkedHashMap;

public class UserProfileJpaDao implements UserProfileDao {

    private final EntityManagerFactory entityManagerFactory;

    @Inject
    public UserProfileJpaDao(EntityManagerFactory entityManagerFactory) {
        this.entityManagerFactory = entityManagerFactory;
    }

    @Override
    public void create(Profile profile) throws ConflictException, ServerException {
        EntityManager em = entityManagerFactory.createEntityManager();
        try {
            em.getTransaction().begin();
            em.persist(profile);
            em.getTransaction().commit();
        } finally {
            if (em.getTransaction().isActive())
                em.getTransaction().rollback();
            em.close();
        }
    }

    @Override
    public void update(Profile profile) throws NotFoundException, ServerException {
        EntityManager em = entityManagerFactory.createEntityManager();
        try {
            em.getTransaction().begin();
            em.merge(profile);
            em.getTransaction().commit();
        } finally {
            if (em.getTransaction().isActive())
                em.getTransaction().rollback();
            em.close();
        }
    }

    @Override
    public void remove(String id) throws NotFoundException, ServerException {
        EntityManager em = entityManagerFactory.createEntityManager();
        try {
            Profile profile = em.find(Profile.class, id);
            if (profile != null) {
                em.getTransaction().begin();
                em.remove(profile);
                em.getTransaction().commit();
            } else {
                throw new NotFoundException(String.format("Profile not found %s", id));
            }
        } finally {
            if (em.getTransaction().isActive())
                em.getTransaction().rollback();
            em.close();
        }
    }

    @Override
    public Profile getById(String id) throws NotFoundException, ServerException {
        EntityManager em = entityManagerFactory.createEntityManager();
        try {
            Profile profile = em.find(Profile.class, id);
            if (profile == null) {
                throw new NotFoundException(String.format("Profile not found %s", id));
            }
            return new Profile().withId(profile.getId()).withUserId(profile.getUserId())
                                .withAttributes(new LinkedHashMap<>(profile.getAttributes()));
        } finally {
            em.close();
        }
    }
}
