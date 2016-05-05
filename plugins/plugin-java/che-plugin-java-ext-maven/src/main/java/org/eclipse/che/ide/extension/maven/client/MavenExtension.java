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
package org.eclipse.che.ide.extension.maven.client;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.google.web.bindery.event.shared.EventBus;

import org.eclipse.che.api.workspace.shared.dto.ProjectConfigDto;
import org.eclipse.che.ide.api.action.ActionManager;
import org.eclipse.che.ide.api.action.DefaultActionGroup;
import org.eclipse.che.ide.api.action.IdeActions;
import org.eclipse.che.ide.api.constraints.Constraints;
import org.eclipse.che.ide.api.extension.Extension;
import org.eclipse.che.ide.api.filetypes.FileType;
import org.eclipse.che.ide.api.filetypes.FileTypeRegistry;
import org.eclipse.che.ide.api.project.type.wizard.PreSelectedProjectTypeManager;
import org.eclipse.che.ide.ext.java.client.dependenciesupdater.DependenciesUpdater;
import org.eclipse.che.ide.extension.maven.client.actions.UpdateDependencyAction;
import org.eclipse.che.ide.extension.maven.shared.MavenAttributes;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

import static org.eclipse.che.ide.api.action.IdeActions.GROUP_BUILD_CONTEXT_MENU;

/**
 * Maven extension entry point.
 *
 * @author Artem Zatsarynnyi
 */
@Singleton
@Extension(title = "Maven", version = "3.0.0")
public class MavenExtension {
    private static List<MavenArchetype> archetypes;
    private        ProjectConfigDto     project;

    @Inject
    public MavenExtension(PreSelectedProjectTypeManager preSelectedProjectManager) {

        preSelectedProjectManager.setProjectTypeIdToPreselect(MavenAttributes.MAVEN_ID, 100);

        archetypes =
                Arrays.asList(new MavenArchetype("org.apache.maven.archetypes", "maven-archetype-quickstart", "RELEASE", null),
                              new MavenArchetype("org.apache.maven.archetypes", "maven-archetype-webapp", "RELEASE", null),
                              new MavenArchetype("org.apache.openejb.maven", "tomee-webapp-archetype", "1.7.1", null));
    }

    public static List<MavenArchetype> getAvailableArchetypes() {
        return archetypes;
    }

    @Inject
    private void bindEvents(final EventBus eventBus,
                            final DependenciesUpdater dependenciesUpdater) {

//        projectExplorerPresenter.addBeforeNodeLoadHandler(new BeforeLoadEvent.BeforeLoadHandler() {
//            @Override
//            public void onBeforeLoad(BeforeLoadEvent event) {
//                Node node = event.getRequestedNode();
//                if (!projectExplorerPresenter.isLoaded(node) && JavaNodeManager.isJavaProject(node) && isValid(node)) {
//                    dependenciesUpdater.updateDependencies(((HasProjectConfig)node).getProjectConfig());
//                }
//            }
//        });

    }

    @Inject
    private void prepareActions(ActionManager actionManager,
                                UpdateDependencyAction updateDependencyAction) {
        // register actions
        actionManager.registerAction("updateDependency", updateDependencyAction);

        // add actions in main menu
        DefaultActionGroup assistantGroup = (DefaultActionGroup)actionManager.getAction(IdeActions.GROUP_ASSISTANT);
        assistantGroup.add(updateDependencyAction, Constraints.LAST);

        // add actions in context menu
        DefaultActionGroup buildContextMenuGroup = (DefaultActionGroup)actionManager.getAction(GROUP_BUILD_CONTEXT_MENU);
        buildContextMenuGroup.addSeparator();
        buildContextMenuGroup.addAction(updateDependencyAction);
    }

    @Inject
    private void registerFileType(FileTypeRegistry fileTypeRegistry, MavenResources mavenResources) {
        fileTypeRegistry.registerFileType(new FileType(mavenResources.maven(), "pom.xml"));
    }

    private boolean isValidForResolveDependencies(ProjectConfigDto project) {
        Map<String, List<String>> attr = project.getAttributes();
        return !(attr.containsKey(MavenAttributes.PACKAGING) && "pom".equals(attr.get(MavenAttributes.PACKAGING).get(0)));
    }
}
