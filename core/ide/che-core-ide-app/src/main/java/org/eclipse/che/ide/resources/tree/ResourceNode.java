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
package org.eclipse.che.ide.resources.tree;

import com.google.common.annotations.Beta;
import com.google.common.base.MoreObjects;
import com.google.common.base.Objects;

import org.eclipse.che.api.promises.client.Function;
import org.eclipse.che.api.promises.client.FunctionException;
import org.eclipse.che.api.promises.client.Promise;
import org.eclipse.che.ide.api.data.HasDataObject;
import org.eclipse.che.ide.api.data.tree.AbstractTreeNode;
import org.eclipse.che.ide.api.data.tree.Node;
import org.eclipse.che.ide.api.data.tree.settings.HasSettings;
import org.eclipse.che.ide.api.data.tree.settings.NodeSettings;
import org.eclipse.che.ide.api.resources.Container;
import org.eclipse.che.ide.api.resources.File;
import org.eclipse.che.ide.api.resources.Folder;
import org.eclipse.che.ide.api.resources.Project;
import org.eclipse.che.ide.api.resources.Resource;
import org.eclipse.che.ide.project.shared.NodesResources;
import org.eclipse.che.ide.ui.smartTree.presentation.HasPresentation;
import org.eclipse.che.ide.ui.smartTree.presentation.NodePresentation;

import javax.validation.constraints.NotNull;
import java.util.List;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkState;
import static com.google.common.collect.Lists.newArrayListWithExpectedSize;
import static java.util.Collections.emptyList;
import static java.util.Collections.unmodifiableList;
import static org.eclipse.che.ide.api.resources.Resource.FILE;
import static org.eclipse.che.ide.api.resources.Resource.FOLDER;
import static org.eclipse.che.ide.api.resources.Resource.PROJECT;

/**
 * Abstract based implementation for all resource based nodes in the IDE.
 *
 * @author Vlad Zhukovskiy
 * @since 4.0.0-RC14
 */
@Beta
public abstract class ResourceNode<R extends Resource> extends AbstractTreeNode implements HasDataObject<R>, HasPresentation, HasSettings {

    public static final String CUSTOM_BACKGROUND_FILL = "background";

    private static final List<Node> NO_CHILDREN = emptyList();

    private       R                resource;
    private       NodeSettings     nodeSettings;
    private       NodePresentation nodePresentation;
    private final NodeFactory      nodeFactory;
    protected final NodesResources nodesResources;

    protected ResourceNode(R resource,
                           NodeSettings nodeSettings,
                           NodeFactory nodeFactory,
                           NodesResources nodesResources) {
        this.resource = resource;
        this.nodeSettings = nodeSettings;
        this.nodeFactory = nodeFactory;
        this.nodesResources = nodesResources;
    }

    @Override
    public NodeSettings getSettings() {
        return nodeSettings;
    }

    @Override
    public R getData() {
        return resource;
    }

    @Override
    public void setData(R data) {
        this.resource = data;
    }

    @Override
    protected Promise<List<Node>> getChildrenImpl() {
        checkState(getData() instanceof Container, "Not a container");

        return ((Container)getData()).getChildren().then(new Function<Resource[], List<Node>>() {
            @Override
            public List<Node> apply(Resource[] children) throws FunctionException {
                if (children == null || children.length == 0) {
                    return NO_CHILDREN;
                }

                final List<Node> nodes = newArrayListWithExpectedSize(children.length);

                for (Resource child : children) {
                    nodes.add(createNode(child));
                }

                return unmodifiableList(nodes);
            }
        });
    }

    @Override
    public final NodePresentation getPresentation(boolean update) {
        if (nodePresentation == null) {
            nodePresentation = new NodePresentation();
            updatePresentation(nodePresentation);
        }

        if (update) {
            updatePresentation(nodePresentation);
        }
        return nodePresentation;
    }

    @Override
    public void updatePresentation(@NotNull NodePresentation presentation) {
        presentation.setPresentableText(getData().getName());
    }

    @Override
    public String getName() {
        return getData().getName();
    }

    @Override
    public boolean isLeaf() {
        return getData().getResourceType() == Resource.FILE;
    }

    @Override
    public boolean supportGoInto() {
        return getData() instanceof Container;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof ResourceNode)) return false;
        ResourceNode<?> that = (ResourceNode<?>)o;
        return Objects.equal(resource, that.resource);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(resource);
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this)
                          .add("resource", resource)
                          .toString();
    }

    public interface NodeFactory {
        FileNode newFileNode(File resource, NodeSettings nodeSettings);

        FolderNode newFolderNode(Folder resource, NodeSettings nodeSettings);

        ProjectNode newProjectNode(Project resource, NodeSettings nodeSettings);
    }

    protected Node createNode(Resource resource) {
        checkArgument(resource != null, "Not a resource");

        switch (resource.getResourceType()) {
            case PROJECT:
                return nodeFactory.newProjectNode((Project)resource, getSettings());
            case FOLDER:
                return nodeFactory.newFolderNode((Folder)resource, getSettings());
            case FILE:
                return nodeFactory.newFileNode((File)resource, getSettings());
            default:
                throw new IllegalArgumentException("Resource type was not recognized");
        }
    }
}
