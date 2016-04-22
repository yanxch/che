package com.codenvy.api.dao.jdbc;

import com.google.common.collect.ImmutableList;

import org.eclipse.che.api.core.ConflictException;
import org.eclipse.che.api.core.NotFoundException;
import org.eclipse.che.api.core.ServerException;
import org.eclipse.che.api.core.UnauthorizedException;
import org.eclipse.che.api.user.server.dao.User;
import org.eclipse.che.api.user.server.dao.UserDao;

import javax.inject.Inject;
import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.NoResultException;
import javax.persistence.PersistenceException;
import javax.persistence.RollbackException;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Predicate;
import javax.persistence.criteria.Root;
import java.util.ArrayList;

import static com.google.common.base.Strings.isNullOrEmpty;
import static java.lang.String.format;

public class UserJpaDao implements UserDao {

    private final EntityManagerFactory entityManagerFactory;

    @Inject
    public UserJpaDao(EntityManagerFactory entityManagerFactory) {
        this.entityManagerFactory = entityManagerFactory;
    }

    @Override
    public String authenticate(String alias, String password) throws UnauthorizedException, ServerException {
        if (isNullOrEmpty(alias) || isNullOrEmpty(password)) {
            throw new UnauthorizedException(
                    String.format("Can't perform authentication for user '%s'. Username or password is empty", alias));
        }
        User user;
        try {
            user = getByAlias(alias);
        } catch (NotFoundException e) {
            try {
                user = getByName(alias);
            } catch (NotFoundException e2) {
                try {
                    user = getById(alias);
                } catch (NotFoundException e3) {
                    throw new UnauthorizedException(format("Authentication failed for user '%s'", alias));
                }
            }
        }


        if (!user.getPassword().equals(password)) {
            throw new UnauthorizedException(format("Authentication failed for user '%s'", alias));
        }
        return user.getId();
    }

    @Override
    public void create(User user) throws ConflictException, ServerException {
        EntityManager em = entityManagerFactory.createEntityManager();
        try {
            em.getTransaction().begin();
            em.persist(user);
            em.getTransaction().commit();
        } catch (RollbackException e) {
            if (e.getLocalizedMessage().contains("Unique index or primary key violation: \"PRIMARY_KEY_2 ")) {
                throw new ConflictException(format("Unable create new user '%s'. User already exists", user.getId()));
            } else if (e.getLocalizedMessage().contains("Unique index or primary key violation: \"CONSTRAINT_INDEX_A ")) {
                throw new ConflictException(format("User with alias .* already exists", user.getId()));
            } else if (e.getLocalizedMessage().contains("Unique index or primary key violation: \"CONSTRAINT_INDEX_2 ")) {
                throw new ConflictException(format("User with name .* already exists", user.getId()));
            }
            throw new ConflictException(e.getLocalizedMessage());
        } catch (PersistenceException e) {
            throw new ConflictException(e.getLocalizedMessage());
        } catch (Exception e) {
            throw new ServerException(e);
        } finally {
            if (em.getTransaction().isActive())
                em.getTransaction().rollback();
            em.close();
        }
    }

    @Override
    public void update(User user) throws NotFoundException, ServerException, ConflictException {
        EntityManager em = entityManagerFactory.createEntityManager();
        try {
            em.getTransaction().begin();
            em.merge(user);
            em.getTransaction().commit();
        } catch (RollbackException e) {
            if (e.getLocalizedMessage().contains("Unique index or primary key violation: \"CONSTRAINT_INDEX_A ")) {
                throw new NotFoundException(format("User with alias .* doesn't exists", user.getId()));
            } else if (e.getLocalizedMessage().contains("Unique index or primary key violation: \"CONSTRAINT_INDEX_2 ")) {
                throw new ConflictException(format("Unable update user '%s', alias %s is already in use", user.getId(), user.getAliases()));
            }
            throw new ConflictException(e.getLocalizedMessage());

        } finally {
            if (em.getTransaction().isActive())
                em.getTransaction().rollback();
            em.close();
        }
    }

    @Override
    public void remove(String id) throws NotFoundException, ServerException, ConflictException {
        EntityManager em = entityManagerFactory.createEntityManager();
        try {
            User profile = em.find(User.class, id);
            if (profile != null) {
                em.getTransaction().begin();
                em.remove(profile);
                em.getTransaction().commit();
            } else {
                throw new NotFoundException(String.format("User with id %s is not found ", id));
            }
        } finally {
            if (em.getTransaction().isActive())
                em.getTransaction().rollback();
            em.close();
        }
    }

    @Override
    public User getByAlias(String alias) throws NotFoundException, ServerException {
        EntityManager em = entityManagerFactory.createEntityManager();
        try {
            CriteriaBuilder builder = em.getCriteriaBuilder();
            CriteriaQuery<User> criteriaQuery = builder.createQuery(User.class);
            Root<User> userQuery = criteriaQuery.from(User.class);
            Predicate where = builder.conjunction();
            where = builder.and(where, userQuery.join("aliases").in(ImmutableList.of(alias)));
            criteriaQuery.where(where);
            User user = em.createQuery(criteriaQuery).getSingleResult();

            return doClone(user);
        } catch (NoResultException e) {
            throw new NotFoundException(format("User with alias %s is not found", alias));
        } finally {
            em.close();
        }

    }

    @Override
    public User getById(String id) throws NotFoundException, ServerException {
        EntityManager em = entityManagerFactory.createEntityManager();
        try {
            User user = em.find(User.class, id);
            if (user == null) {
                throw new NotFoundException(String.format("User %s is not found ", id));
            }
            return doClone(user);
        } finally {
            em.close();
        }
    }

    @Override
    public User getByName(String userName) throws NotFoundException, ServerException {
        EntityManager em = entityManagerFactory.createEntityManager();
        try {
            CriteriaBuilder builder = em.getCriteriaBuilder();
            CriteriaQuery<User> criteriaQuery = builder.createQuery(User.class);
            Root<User> userQuery = criteriaQuery.from(User.class);
            Predicate where = builder.conjunction();
            where = builder.and(where, builder.equal(userQuery.<String>get("name"), userName));
            criteriaQuery.where(where);
            User user = em.createQuery(criteriaQuery).getSingleResult();

            if (user == null) {
                throw new NotFoundException(String.format("User %s is not found", userName));
            }

            return doClone(user);
        } finally {
            em.close();
        }
    }

    private User doClone(User user) {
        return new User().withId(user.getId())
                         .withName(user.getName())
                         .withEmail(user.getEmail())
                         .withPassword(user.getPassword())
                         .withAliases(new ArrayList<>(user.getAliases()));
    }
}
