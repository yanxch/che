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
package org.eclipse.che.ide.part.explorer.project;

import com.google.gwt.user.client.rpc.AsyncCallback;

import org.eclipse.che.api.promises.client.Promise;
import org.eclipse.che.api.promises.client.callback.AsyncPromiseHelper;
import org.eclipse.che.ide.api.data.tree.Node;
import org.eclipse.che.ide.resource.Path;
import org.eclipse.che.ide.resources.tree.ResourceNode;
import org.eclipse.che.ide.ui.smartTree.Tree;
import org.eclipse.che.ide.ui.smartTree.event.BeforeExpandNodeEvent;
import org.eclipse.che.ide.ui.smartTree.event.BeforeExpandNodeEvent.BeforeExpandNodeHandler;
import org.eclipse.che.ide.ui.smartTree.event.ExpandNodeEvent;
import org.eclipse.che.ide.ui.smartTree.event.ExpandNodeEvent.ExpandNodeHandler;
import org.eclipse.che.ide.ui.smartTree.event.NodeAddedEvent;
import org.eclipse.che.ide.ui.smartTree.event.NodeAddedEvent.NodeAddedEventHandler;
import org.eclipse.che.ide.ui.smartTree.event.PostLoadEvent;
import org.eclipse.che.ide.ui.smartTree.event.PostLoadEvent.PostLoadHandler;

import java.util.List;

/**
 * Search node handler, perform searching specified node in the tree by storable value.
 * For example if user passes "/project/path/to/file" then this node handler will check
 * opened root nodes and if it contains project node with path "/project" then it will
 * search children by path "path/to/file".
 *
 * @author Vlad Zhukovskiy
 */
public class SearchNodeHandler implements ExpandNodeHandler, BeforeExpandNodeHandler, NodeAddedEventHandler, PostLoadHandler {

    private Tree tree;

    private boolean inSearchMode = false;

    private Path path;

    private AsyncCallback<Node> callback;

    private boolean forceUpdate = false;
    private boolean closeMissingFiles = true;

    public SearchNodeHandler(Tree tree) {
        this.tree = tree;

        tree.addExpandHandler(this);
        tree.addBeforeExpandHandler(this);
        tree.addNodeAddedHandler(this);
        tree.getNodeLoader().addPostLoadHandler(this);
    }

    /**
     * Search node in the project explorer tree by storable path.
     *
     * @param path
     *         path to node
     * @param forceUpdate
     *         force children reload
     * @param closeMissingFiles
     *         allow editor to close removed files if they were opened
     * @return promise object with found node or promise error if node wasn't found
     */
    public Promise<Node> getNodeByPath(final Path path, boolean forceUpdate, boolean closeMissingFiles) {
        this.forceUpdate = forceUpdate;
        this.closeMissingFiles = closeMissingFiles;
        return AsyncPromiseHelper.createFromAsyncRequest(new AsyncPromiseHelper.RequestCall<Node>() {
            @Override
            public void makeCall(AsyncCallback<Node> callback) {
                getNodeByPath(path, callback);
            }
        });
    }

    protected void getNodeByPath(Path path, AsyncCallback<Node> callback) {
        if (path == null) {
            callback.onFailure(new IllegalArgumentException("Invalid search path"));
        }

        if (inSearchMode) {
            callback.onFailure(new IllegalStateException("Project explorer has been already activated in search mode"));
        }

        this.callback = callback;
        this.path = path;
        this.inSearchMode = true;

        ResourceNode rootNode = getRootNode(path);

        if (rootNode == null) {
            inSearchMode = false;
            return;
        }

        if (rootNode.getData().getLocation().equals(path)) {
            //maybe we searched root node, so just return it back
            inSearchMode = false;
            callback.onSuccess(rootNode);
            return;
        }

        tree.setExpanded(rootNode, true);
    }

    @Override
    public void onBeforeExpand(BeforeExpandNodeEvent event) {
        if (!isInSearchMode()) {
            return;
        }

        Node node = event.getNode();

        if (tree.isExpanded(node)) {

            if (forceUpdate) {
                tree.getNodeLoader().loadChildren(node);
                return;
            }

            List<Node> children = tree.getNodeStorage().getChildren(node);

            for (Node child : children) {
                if (!(child instanceof ResourceNode)) {
                    continue;
                }

//                String childPath = ((HasStorablePath)child).getStorablePath();
                if (path.equals(((ResourceNode)child).getData().getLocation())) {
                    callback.onSuccess(child);
                    inSearchMode = false;
                    return;
                } else if (((ResourceNode)child).getData().getLocation().isPrefixOf(path)/*path.getStorablePath().startsWith(childPath + (child.isLeaf() ? "" : "/"))*/) {
                    event.setCancelled(true); //disallow to continue expanding current node
                    tree.setExpanded(child, true);
                    return;
                }
            }

            //node wasn't found, try to make request to load the same children, may be there is a new nodes on server were created
            tree.getNodeLoader().loadChildren(node);
        }
    }

    @Override
    public void onExpand(ExpandNodeEvent event) {
        if (!isInSearchMode()) {
            return;
        }

        List<Node> children = tree.getNodeStorage().getChildren(event.getNode());

        for (Node child : children) {
            if (!(child instanceof ResourceNode)) {
                continue;
            }

//            String childPath = ((HasStorablePath)child).getStorablePath();
            if (path.equals(((ResourceNode)child).getData().getLocation())) {
                callback.onSuccess(child);
                inSearchMode = false;
                return;
            } else if (((ResourceNode)child).getData().getLocation().isPrefixOf(path)/*path.getStorablePath().startsWith(childPath + (child.isLeaf() ? "" : "/"))*/) {
                tree.setExpanded(child, true);
                return;
            }
        }

        //node wasn't found, try to make request to load the same children, may be there is a new nodes on server were created
        tree.getNodeLoader().loadChildren(event.getNode());
    }

    @Override
    public void onNodeAdded(NodeAddedEvent event) {
        if (!isInSearchMode()) {
            return;
        }

        List<Node> addedNodes = event.getNodes();

        for (Node node : addedNodes) {
            if (!(node instanceof ResourceNode)) {
                continue;
            }

//            String childPath = ((HasStorablePath)node).getStorablePath();

            if (path.equals(((ResourceNode)node).getData().getLocation())) {
                callback.onSuccess(node);
                inSearchMode = false;
                break;
            } else if (((ResourceNode)node).getData().getLocation().isPrefixOf(path)/*path.getStorablePath().startsWith(childPath + (node.isLeaf() ? "" : "/"))*/) {
                tree.setExpanded(node, true);
                return;
            }
        }
    }

    @Override
    public void onPostLoad(PostLoadEvent event) {
        if (!isInSearchMode()) {
            return;
        }

        List<Node> receivedNodes = event.getReceivedNodes();

        for (Node receivedNode : receivedNodes) {
            if (!(receivedNode instanceof ResourceNode)) {
                continue;
            }

//            String childPath = ((HasStorablePath)receivedNode).getStorablePath();
            if (path.equals(((ResourceNode)receivedNode).getData().getLocation())) {
                callback.onSuccess(receivedNode);
                inSearchMode = false;
                return;
            } else if (((ResourceNode)receivedNode).getData().getLocation().isPrefixOf(path)/*path.getStorablePath().startsWith(childPath + (receivedNode.isLeaf() ? "" : "/"))*/) {
                tree.setExpanded(receivedNode, true);
                return;
            }
        }

        callback.onFailure(new IllegalStateException("Node '" + path + "' not found"));
        inSearchMode = false;
    }

    public boolean isInSearchMode() {
        return inSearchMode;
    }

    /**
     * Indicates that during node search removed nodes need to be checked
     * if they were opened in editor parts and need to be closed.
     *
     * @return true if opened nodes in editor part should be closed
     */
    public boolean isCloseMissingFiles() {
        return closeMissingFiles;
    }

    private ResourceNode getRootNode(Path path) {
        for (Node root : tree.getRootNodes()) {
            if (!(root instanceof ResourceNode)) {
                continue;
            }

            final Path rootPath = ((ResourceNode)root).getData().getLocation();

            if (!rootPath.isPrefixOf(path)) {
                continue;
            }

            return (ResourceNode)root;
        }

        return null;
    }
}
