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

import org.eclipse.che.api.core.ConflictException;
import org.eclipse.che.api.core.NotFoundException;
import org.eclipse.che.api.core.ServerException;
import org.eclipse.che.api.core.UnauthorizedException;
import org.eclipse.che.api.user.server.model.impl.UserImpl;
import org.eclipse.che.api.user.server.spi.UserDao;

import javax.inject.Inject;
import javax.inject.Singleton;
import javax.persistence.EntityExistsException;
import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Root;

import static java.lang.String.format;
import static java.util.Objects.requireNonNull;

/**
 * @author Yevhenii Voevodin
 */
@Singleton
public class JpaUserDao implements UserDao {

    private final EntityManagerFactory factory;

    @Inject
    public JpaUserDao(EntityManagerFactory factory) {
        this.factory = factory;
    }

    @Override
    public String authenticate(String emailOrAliasOrName, String password) throws UnauthorizedException, ServerException {
        return null;
    }

    @Override
    public void create(UserImpl user) throws ConflictException, ServerException {
        final EntityManager manager = factory.createEntityManager();
        try {
            manager.getTransaction().begin();
            manager.persist(user);
            manager.getTransaction().commit();
        } catch (EntityExistsException x) {
            // TODO make more concrete
            throw new ConflictException("User with such id/name/email/alias already exists");
        } catch (RuntimeException x) {
            throw new ServerException("TODO");
        } finally {
            manager.close();
        }
    }

    @Override
    public void update(UserImpl user) throws NotFoundException, ServerException, ConflictException {

    }

    @Override
    public void remove(String id) throws ServerException, ConflictException {

    }

    @Override
    public UserImpl getByAlias(String alias) throws NotFoundException, ServerException {
        return null;
    }

    @Override
    public UserImpl getById(String id) throws NotFoundException, ServerException {
        requireNonNull(id, "Required non-null id");
        final EntityManager manager = factory.createEntityManager();
        try {
            final UserImpl user = manager.find(UserImpl.class, id);
            if (user == null) {
                throw new NotFoundException(format("User with id '%s' doesn't exist", id));
            }
            return user;
        } catch (RuntimeException x) {
            throw new ServerException(x.getMessage(), x);
        } finally {
            manager.close();
        }
    }

    @Override
    public UserImpl getByName(String name) throws NotFoundException, ServerException {
        requireNonNull(name, "Required non-null name");
        final EntityManager manager = factory.createEntityManager();
        try {
            final CriteriaBuilder cb = manager.getCriteriaBuilder();
            final CriteriaQuery<UserImpl> query = cb.createQuery(UserImpl.class);
            final Root<UserImpl> root = query.from(UserImpl.class);
            query.select(root).where(cb.equal(root.get("name"), name));
            final UserImpl user = manager.createQuery(query).getSingleResult();
            if (user == null) {
                throw new NotFoundException(format("User with name '%s' doesn't exist", name));
            }
            return user;
        } catch (RuntimeException x) {
            throw new ServerException(x.getMessage(), x);
        } finally {
            manager.close();
        }
    }

    @Override
    public UserImpl getByEmail(String email) throws NotFoundException, ServerException {
        requireNonNull(email, "Required non-null email");
        final EntityManager manager = factory.createEntityManager();
        try {
            final CriteriaBuilder cb = manager.getCriteriaBuilder();
            final CriteriaQuery<UserImpl> query = cb.createQuery(UserImpl.class);
            final Root<UserImpl> root = query.from(UserImpl.class);
            query.select(root).where(cb.equal(root.get("email"), email));
            final UserImpl user = manager.createQuery(query).getSingleResult();
            if (user == null) {
                throw new NotFoundException(format("User with email '%s' doesn't exist", email));
            }
            return user;
        } catch (RuntimeException x) {
            throw new ServerException(x.getMessage(), x);
        } finally {
            manager.close();
        }
    }
}
