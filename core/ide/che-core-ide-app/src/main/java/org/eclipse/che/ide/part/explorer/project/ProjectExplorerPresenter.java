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

import com.google.common.base.Optional;
import com.google.gwt.event.shared.HandlerRegistration;
import com.google.gwt.user.client.ui.AcceptsOneWidget;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.google.web.bindery.event.shared.EventBus;

import org.eclipse.che.commons.annotation.Nullable;
import org.eclipse.che.ide.CoreLocalizationConstant;
import org.eclipse.che.ide.Resources;
import org.eclipse.che.ide.api.data.tree.Node;
import org.eclipse.che.ide.api.data.tree.settings.NodeSettings;
import org.eclipse.che.ide.api.data.tree.settings.SettingsProvider;
import org.eclipse.che.ide.api.mvp.View;
import org.eclipse.che.ide.api.parts.HasView;
import org.eclipse.che.ide.api.parts.ProjectExplorerPart;
import org.eclipse.che.ide.api.parts.base.BasePresenter;
import org.eclipse.che.ide.api.resources.Container;
import org.eclipse.che.ide.api.resources.Resource;
import org.eclipse.che.ide.api.resources.ResourceChangedEvent;
import org.eclipse.che.ide.api.resources.ResourceChangedEvent.ResourceChangedHandler;
import org.eclipse.che.ide.api.resources.ResourceDelta;
import org.eclipse.che.ide.api.resources.marker.MarkerChangedEvent;
import org.eclipse.che.ide.api.selection.Selection;
import org.eclipse.che.ide.part.explorer.project.ProjectExplorerView.ActionDelegate;
import org.eclipse.che.ide.resources.reveal.RevealResourceEvent;
import org.eclipse.che.ide.resources.tree.ContainerNode;
import org.eclipse.che.ide.resources.tree.ResourceNode;
import org.eclipse.che.ide.ui.smartTree.Tree;
import org.eclipse.che.ide.ui.smartTree.event.SelectionChangedEvent;
import org.eclipse.che.ide.ui.smartTree.event.SelectionChangedEvent.SelectionChangedHandler;
import org.eclipse.che.ide.ui.smartTree.event.StoreRemoveEvent;
import org.vectomatic.dom.svg.ui.SVGResource;

import javax.validation.constraints.NotNull;
import java.util.List;

import static org.eclipse.che.ide.api.resources.Resource.PROJECT;
import static org.eclipse.che.ide.api.resources.ResourceDelta.ADDED;
import static org.eclipse.che.ide.api.resources.ResourceDelta.DERIVED;
import static org.eclipse.che.ide.api.resources.ResourceDelta.MOVED_FROM;
import static org.eclipse.che.ide.api.resources.ResourceDelta.MOVED_TO;
import static org.eclipse.che.ide.api.resources.ResourceDelta.REMOVED;
import static org.eclipse.che.ide.api.resources.ResourceDelta.UPDATED;

/**
 * Project explorer presenter. Handle basic logic to control project tree display.
 *
 * @author Vlad Zhukovskiy
 * @author Dmitry Shnurenko
 */
@Singleton
public class ProjectExplorerPresenter extends BasePresenter implements ActionDelegate,
                                                                       ProjectExplorerPart,
                                                                       HasView,
                                                                       ResourceChangedHandler,
                                                                       MarkerChangedEvent.MarkerChangedHandler {
    private final ProjectExplorerView      view;
    private final EventBus                 eventBus;
    private final ResourceNode.NodeFactory nodeFactory;
    private final SettingsProvider         settingsProvider;
    private final CoreLocalizationConstant locale;
    private final Resources                resources;

    public static final int PART_SIZE = 500;

    private boolean hiddenFilesAreShown;

    @Inject
    public ProjectExplorerPresenter(ProjectExplorerView view,
                                    EventBus eventBus,
                                    CoreLocalizationConstant locale,
                                    Resources resources,
                                    ResourceNode.NodeFactory nodeFactory,
                                    SettingsProvider settingsProvider) {
        this.view = view;
        this.eventBus = eventBus;
        this.nodeFactory = nodeFactory;
        this.settingsProvider = settingsProvider;
        this.locale = locale;
        this.resources = resources;
        this.view.setDelegate(this);

        eventBus.addHandler(ResourceChangedEvent.getType(), this);
        eventBus.addHandler(MarkerChangedEvent.getType(), this);

        view.getTree().getSelectionModel().addSelectionChangedHandler(new SelectionChangedHandler() {
            @Override
            public void onSelectionChanged(SelectionChangedEvent event) {
                setSelection(new Selection<>(event.getSelection()));
            }
        });
    }

    @Override
    public void onResourceChanged(ResourceChangedEvent event) {
        final ResourceDelta delta = event.getDelta();

        switch (delta.getKind()) {
            case ADDED:
                onResourceAdded(delta);
                break;
            case REMOVED:
                onResourceRemoved(delta);
                break;
            case UPDATED:
                onResourceUpdated(delta);
        }
    }

    @SuppressWarnings("unchecked")
    protected void onResourceAdded(ResourceDelta delta) {

        if ((delta.getFlags() & DERIVED) == 0) {
            return;
        }

        final Tree tree = view.getTree();
        final NodeSettings nodeSettings = settingsProvider.getSettings();

        final Resource resource = delta.getResource();

        if ((delta.getFlags() & (MOVED_FROM | MOVED_TO)) != 0) {
            for (Node checkNode : tree.getNodeStorage().getAll()) {
                if (checkNode instanceof ResourceNode && ((ResourceNode)checkNode).getData().getLocation().equals(delta.getFromPath())) {
                    tree.getNodeStorage().remove(checkNode);
                    break;
                }
            }
        } else {
            //process root project
            if (resource.getLocation().segmentCount() == 1 && resource.getResourceType() == PROJECT) {
                final ContainerNode node = nodeFactory.newContainerNode((Container)resource, nodeSettings);
                tree.getNodeStorage().add(node);

                return;
            }
        }

        eventBus.fireEvent(new RevealResourceEvent(resource));
    }

    @SuppressWarnings("unchecked")
    protected void onResourceRemoved(final ResourceDelta delta) {

        final Tree tree = view.getTree();
        final Node[] node = new Node[1];

        //look for removed node from existing
        for (final Node exist : tree.getNodeStorage().getAll()) {
            if (isNodeServesResource(exist, delta.getResource())) {
                node[0] = exist;
                break;
            }
        }

        final HandlerRegistration[] handler = new HandlerRegistration[1];

        if (node[0] == null) {
            return;
        }

        //handle node removal and reveal to sibling node or parent
        handler[0] = tree.getNodeStorage().addStoreRemoveHandler(new StoreRemoveEvent.StoreRemoveHandler() {
            @Override
            public void onRemove(final StoreRemoveEvent event) {
                //filter only our removed node
                if (!event.getNode().equals(node[0])) {
                    return;
                }

                //if found, de-register handler
                if (handler[0] != null) {
                    handler[0].removeHandler();
                }

                //if "/a/b/c" - removed node, we should get "/a/b" - parent
                final Node nodeParent = node[0].getParent();

                if (nodeParent == null) {
                    return;
                }

                if (nodeParent instanceof ResourceNode) {
                    final Optional<Container> resourceParent = delta.getResource().getParent(); //resource's, not node

                    //resource's parent may differs from node's parent
                    if (resourceParent.isPresent() &&
                        !((ResourceNode)nodeParent).getData().getLocation().equals(resourceParent.get().getLocation())) {
                        eventBus.fireEvent(new RevealResourceEvent((resourceParent.get())));
                    } else {

                        ResourceNode siblingToReveal = null;

                        final List<Node> siblingNodes = tree.getNodeStorage().getChildren(nodeParent);

                        if (siblingNodes != null && !siblingNodes.isEmpty()) {
                            if (event.getIndex() == 0) {
                                //if removed first node, then select first resource based
                                for (int i = 0; i < siblingNodes.size(); i++) {
                                    if (siblingNodes.get(i) instanceof ResourceNode) {
                                        siblingToReveal = (ResourceNode)siblingNodes.get(i);
                                        break;
                                    }
                                }
                            } else {
                                //or first previous resource based
                                for (int i = event.getIndex() - 1; i >= 0; i--) {
                                    if (siblingNodes.get(i) instanceof ResourceNode) {
                                        siblingToReveal = (ResourceNode)siblingNodes.get(i);
                                        break;
                                    }
                                }
                            }

                            if (siblingToReveal != null) {
                                eventBus.fireEvent(new RevealResourceEvent(siblingToReveal.getData()));
                                return;
                            }
                        }

                        eventBus.fireEvent(new RevealResourceEvent(((ResourceNode)nodeParent).getData()));
                    }
                }
            }
        });

        tree.getNodeStorage().remove(node[0]);
    }

    protected boolean isNodeServesResource(Node node, Resource resource) {
        return node instanceof ResourceNode && ((ResourceNode)node).getData().getLocation().equals(resource.getLocation());
    }

    @SuppressWarnings("unchecked")
    protected void onResourceUpdated(ResourceDelta delta) {
        final Tree tree = view.getTree();

        for (Node node : tree.getNodeStorage().getAll()) {
            if (node instanceof ResourceNode && ((ResourceNode)node).getData().getLocation().equals(delta.getResource().getLocation())) {
                ((ResourceNode)node).setData(delta.getResource());
                tree.refresh(node);
                return;
            }
        }
    }

    @Override
    public void onMarkerChanged(MarkerChangedEvent event) {

    }

    public Tree getTree() {
        return view.getTree();
    }

    /** {@inheritDoc} */
    @Override
    public View getView() {
        return view;
    }

    /** {@inheritDoc} */
    @Override
    public void setVisible(boolean visible) {
        view.setVisible(visible);
    }

    /** {@inheritDoc} */
    @NotNull
    @Override
    public String getTitle() {
        return locale.projectExplorerButtonTitle();
    }

    /** {@inheritDoc} */
    @Override
    public SVGResource getTitleSVGImage() {
        return resources.projectExplorerPartIcon();
    }

    /** {@inheritDoc} */
    @Nullable
    @Override
    public String getTitleToolTip() {
        return locale.projectExplorerPartTooltip();
    }

    /** {@inheritDoc} */
    @Override
    public int getSize() {
        return PART_SIZE;
    }

    /** {@inheritDoc} */
    @Override
    public void go(AcceptsOneWidget container) {
        container.setWidget(view);
    }

    /**
     * Activate "Go Into" mode on specified node if.
     * Node should support this mode. See {@link Node#supportGoInto()}.
     *
     * @param node
     *         node which should be activated in "Go Into" mode
     */
    @Deprecated
    public void goInto(Node node) {
        view.setGoIntoModeOn(node);
    }

    @Deprecated
    public void reloadChildren() {
        view.reloadChildren(null, true);
    }

    @Deprecated
    public void reloadChildren(Node node) {
        view.reloadChildren(node);
    }

    /**
     * Reload children by node type.
     * Useful method if you want to reload specified nodes, e.g. External Libraries.
     *
     * @param type
     *         node type to update
     */
    @Deprecated
    public void reloadChildrenByType(Class<?> type) {
        view.reloadChildrenByType(type);
    }

    /**
     * Get "Go Into" state on current tree.
     *
     * @return true - if "Go Into" mode has been activated.
     */
    @Deprecated
    public boolean isGoIntoActivated() {
        return view.isGoIntoActivated();
    }

    /**
     * Collapse all non-leaf nodes.
     */
    @Deprecated
    public void collapseAll() {
        view.collapseAll();
    }

    /**
     * Configure tree to show or hide files that starts with ".", e.g. hidden files.
     *
     * @param show
     *         true - if those files should be shown, otherwise - false
     */
    @Deprecated
    public void showHiddenFiles(boolean show) {
        hiddenFilesAreShown = show;
        view.showHiddenFilesForAllExpandedNodes(show);
    }

    /**
     * Retrieve status of showing hidden files.
     *
     * @return true - if hidden files are shown, otherwise - false
     */
    @Deprecated
    public boolean isShowHiddenFiles() {
        return hiddenFilesAreShown;
    }

}
