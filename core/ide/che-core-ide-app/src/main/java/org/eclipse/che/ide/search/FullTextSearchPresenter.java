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
package org.eclipse.che.ide.search;

import com.google.common.base.Optional;
import com.google.inject.Inject;
import com.google.inject.Singleton;

import org.eclipse.che.api.promises.client.Operation;
import org.eclipse.che.api.promises.client.OperationException;
import org.eclipse.che.ide.api.resources.Container;
import org.eclipse.che.ide.api.resources.Resource;
import org.eclipse.che.ide.api.workspace.Workspace;
import org.eclipse.che.ide.resource.Path;
import org.eclipse.che.ide.search.presentation.FindResultPresenter;

import static com.google.common.base.Strings.isNullOrEmpty;

/**
 * Presenter for full text search.
 *
 * @author Valeriy Svydenko
 * @author Vlad Zhukovskyi
 */
@Singleton
public class FullTextSearchPresenter implements FullTextSearchView.ActionDelegate {

    private final FullTextSearchView  view;
    private final FindResultPresenter findResultPresenter;
    private final Workspace           workspace;
    private       Path                defaultStartPoint;

    @Inject
    public FullTextSearchPresenter(FullTextSearchView view,
                                   FindResultPresenter findResultPresenter,
                                   Workspace workspace) {
        this.view = view;
        this.findResultPresenter = findResultPresenter;
        this.workspace = workspace;

        this.view.setDelegate(this);
    }

    /** Show dialog with view for searching. */
    public void showDialog(Path path) {
        this.defaultStartPoint = path;

        view.showDialog();
        view.clearInput();
        view.setPathDirectory(path.toString());
    }

    @Override
    public void search(final String text) {
        final Path startPoint = isNullOrEmpty(view.getPathToSearch()) ? defaultStartPoint : Path.valueOf(view.getPathToSearch());

        workspace.getWorkspaceRoot().getContainer(startPoint).then(new Operation<Optional<Container>>() {
            @Override
            public void apply(Optional<Container> optionalContainer) throws OperationException {
                if (!optionalContainer.isPresent()) {
                    view.showErrorMessage("Path '" + startPoint + "' doesn't exists");
                    return;
                }

                final Container container = optionalContainer.get();
                container.search(view.getFileMask(), text).then(new Operation<Resource[]>() {
                    @Override
                    public void apply(Resource[] result) throws OperationException {
                        view.close();
                        findResultPresenter.handleResponse(result, text);
                    }
                });
            }
        });
    }

    @Override
    public void setPathDirectory(String path) {
        view.setPathDirectory(path);
    }

    @Override
    public void setFocus() {
        view.setFocus();
    }

    @Override
    public void onEnterClicked() {
        if (view.isAcceptButtonInFocus()) {
            String searchText = view.getSearchText();
            if (!searchText.isEmpty()) {
                search(searchText);
            }
            return;
        }

        if (view.isCancelButtonInFocus()) {
            view.close();
            return;
        }

        if (view.isSelectPathButtonInFocus()) {
            view.showSelectPathDialog();
        }
    }
}
