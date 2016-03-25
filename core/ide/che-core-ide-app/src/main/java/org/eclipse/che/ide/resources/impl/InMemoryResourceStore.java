package org.eclipse.che.ide.resources.impl;

import com.google.common.annotations.Beta;
import com.google.common.base.Optional;
import com.google.common.collect.Maps;

import org.eclipse.che.ide.api.resources.Container;
import org.eclipse.che.ide.api.resources.Resource;
import org.eclipse.che.ide.resource.Path;

import java.util.Map;
import java.util.Set;

import static com.google.common.base.Optional.absent;
import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.collect.Sets.newHashSet;

/**
 * In memory implementation of {@link ResourceStore}.
 *
 * @author Vlad Zhukovskiy
 * @see ResourceStore
 * @since 4.0.0-RC14
 */
@Beta
class InMemoryResourceStore implements ResourceStore {

    Map<Path, Set<Resource>> memoryCache;

    public InMemoryResourceStore() {
        memoryCache = Maps.newHashMap();
    }

    /** {@inheritDoc} */
    @Override
    public boolean register(Path parent, Resource resource) {
        checkArgument(parent != null, "Null parent occurred");
        checkArgument(resource != null, "Null resource occurred");

        final Set<Resource> container;

        if (!memoryCache.containsKey(parent)) {
            container = newHashSet();
            memoryCache.put(parent, container);
        } else {
            container = memoryCache.get(parent);
        }

        return container.add(resource);
    }

    /** {@inheritDoc} */
    @Override
    public void dispose(Path path, boolean withChildren) {
        checkArgument(path != null, "Null path occurred");

        if (!memoryCache.containsKey(path)) {
            return;
        }

        final Set<Resource> container = memoryCache.remove(path);

        if (withChildren) {
            for (Resource resource : container) {
                if (resource instanceof Container) {
                    dispose(resource.getLocation(), true);
                }
            }
        }
    }

    /** {@inheritDoc} */
    @Override
    public Optional<Resource> getResource(Path path) {
        checkArgument(path != null, "Null path occurred");

        final Path parent = path.parent();

        if (!memoryCache.containsKey(parent)) {
            return absent();
        }

        final Set<Resource> container = memoryCache.get(parent);

        for (Resource resource : container) {
            if (resource.getLocation().equals(path)) {
                return Optional.of(resource);
            }
        }

        return absent();
    }

    /** {@inheritDoc} */
    @Override
    public Optional<Resource[]> get(Path parent) {
        checkArgument(parent != null, "Null path occurred");

        if (!memoryCache.containsKey(parent)) {
            return absent();
        }

        final Set<Resource> container = memoryCache.get(parent);

        return Optional.of(container.toArray(new Resource[container.size()]));
    }

    /** {@inheritDoc} */
    @Override
    public Optional<Resource[]> getAll(Path parent) {
        checkArgument(parent != null, "Null path occurred");

        if (!memoryCache.containsKey(parent)) {
            return absent();
        }

        Set<Resource> all = newHashSet();

        for (Map.Entry<Path, Set<Resource>> setEntry : memoryCache.entrySet()) {
            if (!parent.isPrefixOf(setEntry.getKey())) {
                continue;
            }

            all.addAll(setEntry.getValue());
        }

        return Optional.of(all.toArray(new Resource[all.size()]));
    }

    /** {@inheritDoc} */
    @Override
    public void clear() {
        memoryCache.clear();
    }
}
