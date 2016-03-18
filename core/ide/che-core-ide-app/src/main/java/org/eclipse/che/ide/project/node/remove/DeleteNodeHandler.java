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
package org.eclipse.che.ide.project.node.remove;

import com.google.gwt.core.client.JsArrayMixed;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.inject.Inject;
import com.google.inject.Singleton;

import org.eclipse.che.api.promises.client.Function;
import org.eclipse.che.api.promises.client.FunctionException;
import org.eclipse.che.api.promises.client.Operation;
import org.eclipse.che.api.promises.client.OperationException;
import org.eclipse.che.api.promises.client.Promise;
import org.eclipse.che.api.promises.client.callback.AsyncPromiseHelper.RequestCall;
import org.eclipse.che.api.promises.client.js.JsPromiseError;
import org.eclipse.che.api.promises.client.js.Promises;
import org.eclipse.che.ide.CoreLocalizationConstant;
import org.eclipse.che.ide.api.resources.Folder;
import org.eclipse.che.ide.api.resources.Resource;
import org.eclipse.che.ide.ui.dialogs.CancelCallback;
import org.eclipse.che.ide.ui.dialogs.ConfirmCallback;
import org.eclipse.che.ide.ui.dialogs.DialogFactory;

import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.collect.Lists.newArrayList;
import static org.eclipse.che.api.promises.client.callback.AsyncPromiseHelper.createFromAsyncRequest;
import static org.eclipse.che.ide.api.resources.Resource.FILE;
import static org.eclipse.che.ide.api.resources.Resource.FOLDER;
import static org.eclipse.che.ide.api.resources.Resource.PROJECT;

/**
 * Helper class which allow to delete multiple nodes with user prompt.
 *
 * @author Vlad Zhukovskiy
 */
@Singleton
public class DeleteNodeHandler {

    private final CoreLocalizationConstant localization;
    private final DialogFactory            dialogFactory;

    @Inject
    public DeleteNodeHandler(CoreLocalizationConstant localization, DialogFactory dialogFactory) {
        this.localization = localization;
        this.dialogFactory = dialogFactory;
    }

    public Promise<Void> deleteAll(Resource... resources) {
        return deleteAll(false, resources);
    }

    public Promise<Void> deleteAll(boolean needConfirmation, Resource... resources) {
        checkArgument(resources != null, "Null resource occurred");
        checkArgument(resources.length > 0, "No resources were provided to remove");

        final Resource[] filtered = filterDescendants(resources);

        if (!needConfirmation) {
            Promise<?>[] deleteAll = new Promise<?>[resources.length];
            for (int i = 0; i < resources.length; i++) {
                deleteAll[i] = resources[i].delete();
            }

            return Promises.all(deleteAll).then(new Function<JsArrayMixed, Void>() {
                @Override
                public Void apply(JsArrayMixed arg) throws FunctionException {
                    return null;
                }
            });
        }

        List<Resource> projectsList = newArrayList(/*Iterables.filter(filtered, isProjectNode())*/);

        for (Resource resource : filtered) {
            if (resource.getResourceType() == PROJECT) {
                projectsList.add(resource);
            }
        }

        Resource[] projects = projectsList.toArray(new Resource[projectsList.size()]);

        if (projectsList.isEmpty()) {
            //if no project were found in nodes list
            return promptUserToDelete(filtered);
        } else if (projects.length < filtered.length) {
            //inform user that we can't delete mixed list of the nodes
            return Promises.reject(JsPromiseError.create(localization.mixedProjectDeleteMessage()));
        } else {
            //delete only project nodes
            return promptUserToDelete(projects);
        }
    }

    private Promise<Void> promptUserToDelete(final Resource[] resources) {
        return createFromAsyncRequest(new RequestCall<Void>() {
            @Override
            public void makeCall(AsyncCallback<Void> callback) {
                String warningMessage = generateWarningMessage(resources);

                boolean anyDirectories = false;

                String directoryName = null;
                for (Resource resource : resources) {
                    if (resource instanceof Folder) {
                        anyDirectories = true;
                        directoryName = resource.getName();
                        break;
                    }
                }

                if (anyDirectories) {
                    warningMessage += resources.length == 1 ? localization.deleteAllFilesAndSubdirectories(directoryName)
                                                            : localization.deleteFilesAndSubdirectoriesInTheSelectedDirectory();
                }

                dialogFactory.createConfirmDialog(localization.deleteDialogTitle(),
                                                  warningMessage,
                                                  onConfirm(resources, callback),
                                                  onCancel(callback)).show();
            }
        });
    }

    private String generateWarningMessage(Resource[] resources) {
        if (resources.length == 1) {
            String name = resources[0].getName();
            String type = getDisplayType(resources[0]);

            return "Delete " + type + " \"" + name + "\"?";
        }

        Map<String, Integer> pluralToSingular = new HashMap<>();
        for (Resource resource : resources) {
            final String type = getDisplayType(resource);

            if (!pluralToSingular.containsKey(type)) {
                pluralToSingular.put(type, 1);
            } else {
                Integer count = pluralToSingular.get(type);
                count++;
                pluralToSingular.put(type, count);
            }
        }

        StringBuilder buffer = new StringBuilder("Delete ");


        Iterator<Map.Entry<String, Integer>> iterator = pluralToSingular.entrySet().iterator();
        if (iterator.hasNext()) {
            Map.Entry<String, Integer> entry = iterator.next();
            buffer.append(entry.getValue())
                  .append(" ")
                  .append(entry.getKey());

            if (entry.getValue() > 1) {
                buffer.append("s");
            }

            while (iterator.hasNext()) {
                Map.Entry<String, Integer> e = iterator.next();

                buffer.append(" and ")
                      .append(e.getValue())
                      .append(" ")
                      .append(e.getKey());

                if (e.getValue() > 1) {
                    buffer.append("s");
                }
            }
        }

        buffer.append("?");

        return buffer.toString();
    }

    private String getDisplayType(Resource resource) {
        if (resource.getResourceType() == PROJECT) {
            return "project";
        } else if (resource.getResourceType() == FOLDER) {
            return "folder";
        } else if (resource.getResourceType() == FILE) {
            return "file";
        } else {
            return "resource";
        }
    }

    private Resource[] filterDescendants(Resource[] resources) {
        List<Resource> filteredElements = newArrayList(resources);

        int previousSize;

        do {
            previousSize = filteredElements.size();
            outer:
            for (Resource element : filteredElements) {
                for (Resource element2 : filteredElements) {
                    if (element == element2) {
                        continue;
                    }
                    //compare only paths to increase performance, don't operation in this case with parents
                    if (element.getLocation().isPrefixOf(element2.getLocation())) {
                        filteredElements.remove(element2);
                        break outer;
                    }
                }
            }
        }
        while (filteredElements.size() != previousSize);

        return filteredElements.toArray(new Resource[filteredElements.size()]);
    }

    private ConfirmCallback onConfirm(final Resource[] resources,
                                      final AsyncCallback<Void> callback) {
        return new ConfirmCallback() {
            @Override
            public void accepted() {
                Promise<?>[] deleteAll = new Promise<?>[resources.length];
                for (int i = 0; i < resources.length; i++) {
                    deleteAll[i] = resources[i].delete();
                }

                Promises.all(deleteAll).then(new Operation<JsArrayMixed>() {
                    @Override
                    public void apply(JsArrayMixed arg) throws OperationException {
                        callback.onSuccess(null);
                    }
                });
            }
        };
    }

    private CancelCallback onCancel(final AsyncCallback<Void> callback) {
        return new CancelCallback() {
            @Override
            public void cancelled() {
                callback.onFailure(new Exception("Cancelled"));
            }
        };
    }
}
