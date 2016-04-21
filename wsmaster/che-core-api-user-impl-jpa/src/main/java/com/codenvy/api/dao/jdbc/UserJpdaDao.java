package com.codenvy.api.dao.jdbc;

import com.google.common.collect.ImmutableList;

import org.eclipse.che.api.core.ConflictException;
import org.eclipse.che.api.core.NotFoundException;
import org.eclipse.che.api.core.ServerException;
import org.eclipse.che.api.core.UnauthorizedException;
import org.eclipse.che.api.user.server.dao.Profile;
import org.eclipse.che.api.user.server.dao.User;
import org.eclipse.che.api.user.server.dao.UserDao;

import javax.inject.Inject;
import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.PersistenceException;
import javax.persistence.RollbackException;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Predicate;
import javax.persistence.criteria.Root;
import java.util.ArrayList;

import static java.lang.String.format;

public class UserJpdaDao implements UserDao {

    private final EntityManagerFactory entityManagerFactory;

    @Inject
    public UserJpdaDao(EntityManagerFactory entityManagerFactory) {
        this.entityManagerFactory = entityManagerFactory;
    }

    @Override
    public String authenticate(String alias, String password) throws UnauthorizedException, ServerException {
        return null;
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
            Profile profile = em.find(Profile.class, id);
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
        CriteriaBuilder builder = em.getCriteriaBuilder();
        CriteriaQuery<User> criteriaQuery = builder.createQuery(User.class);
        Root<User> userQuery = criteriaQuery.from(User.class);
        Predicate where = builder.conjunction();
        where = builder.and(where, userQuery.get("aliases").in(ImmutableList.of(alias)));
        criteriaQuery.where(where);
        User user = em.createQuery(criteriaQuery).getSingleResult();



//        User user = (User)em.createQuery("SELECT U FROM User U WHERE U.aliases in :alias").setParameter("alias", ImmutableList.of(alias))
//                            .getSingleResult();

        return user == null ? null : new User().withId(user.getId())
                                               .withName(user.getName())
                                               .withEmail(user.getEmail())
                                               .withPassword(null)
                                               .withAliases(new ArrayList<>(user.getAliases()));

    }

    @Override
    public User getById(String id) throws NotFoundException, ServerException {
        EntityManager em = entityManagerFactory.createEntityManager();
        try {
            User user = em.find(User.class, id);
            if (user == null) {
                throw new NotFoundException(String.format("User not found %s", id));
            }
            return new User().withId(user.getId())
                             .withName(user.getName())
                             .withEmail(user.getEmail())
                             .withPassword(null)
                             .withAliases(new ArrayList<>(user.getAliases()));
        } finally {
            em.close();
        }
    }

    @Override
    public User getByName(String userName) throws NotFoundException, ServerException {
        return null;
    }
}
