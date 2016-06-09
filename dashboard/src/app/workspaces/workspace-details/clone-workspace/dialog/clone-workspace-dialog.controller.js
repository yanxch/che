/*
 * Copyright (c) 2015-2016 Codenvy, S.A.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *   Codenvy, S.A. - initial API and implementation
 */
'use strict';

/**
 * @ngdoc controller
 * @name workspace.clone.controller:CloneWorkspaceDialogController
 * @description This class is handling the controller for the dialog box about the cloning of workspace
 * @author Oleksii Kurinnyi
 */
export class CloneWorkspaceDialogController {

  /**
   * Default constructor that is using resource
   * @ngInject for Dependency injection
   */
  constructor($q, cheNotification, $mdDialog, $log, cheWorkspace) {
    this.$q = $q;
    this.cheNotification = cheNotification;
    this.$mdDialog = $mdDialog;
    this.$log = $log;
    this.cheWorkspace = cheWorkspace;
    this.cloningInProgress = false;

    this.workspaceName = '';
  }

  /**
   * It will hide the dialog box.
   */
  hide() {
    this.$mdDialog.hide();
  }

  /**
   * Start the process to clone the workspace
   */
  cloneWorkspace() {
    this.cloningInProgress = true;
    this.cloningSteps = '';

    let cloneConfig = angular.copy(this.workspaceDetails.config);
    cloneConfig.name = this.workspaceName;
    let cloneAttributes = {stackId: this.workspaceDetails.attributes.stackId};

    let creationPromise = this.cheWorkspace.createWorkspaceFromConfig(null, cloneConfig, cloneAttributes);
    creationPromise.then((workspace) => {
      this.cloningSteps += 'Creating workspace <b>' + this.workspaceName + '</b>...<br>';

      if (cloneConfig.projects && cloneConfig.projects.length > 0) {
        // fetch new workspace to be able to fetch it status
        this.cheWorkspace.fetchWorkspaceDetails(workspace.id).then(() => {
          return this.cheWorkspace.startWorkspace(workspace.id, workspace.config.defaultEnv);
        }).then(() => {
          return this.cheWorkspace.fetchStatusChange(workspace.id, 'RUNNING');
        }).then(() => {
          // fetch workspace again to get runtime
          return this.cheWorkspace.fetchWorkspaceDetails(workspace.id);
        }).then(() => {
          let importProjectsPromise = this.importProjectsIntoWorkspace(workspace);
          importProjectsPromise.then((data) => {
            this.finishWorkspaceCloning(workspace);
          }, (error) => {
            this.handleError(error);
          });
        })
      } else {
        this.finishWorkspaceCloning(workspace);
      }
    }, (error) => {
      this.handleError(error);
    });

  }

  /**
   * Import all projects with a given source location into the workspace
   *
   * @param workspace
   */
  importProjectsIntoWorkspace(workspace) {
    var projectPromises = [];

    workspace.config.projects.forEach((project) => {
      if (project.source && project.source.location && project.source.location.length > 0) {
        let deferred = this.$q.defer();
        let deferredPromise = deferred.promise;
        projectPromises.push(deferredPromise);
        let importProjectPromise = this.cheWorkspace.getWorkspaceAgent(workspace.id).getProject().importProject(project.name, project.source);

        importProjectPromise.then(() => {
          this.cloningSteps += 'Importing project <b>' + project.name + '</b>...<br>';
          let updateProjectPromise = this.cheWorkspace.getWorkspaceAgent(workspace.id).getProject().updateProject(project.name, project);
          updateProjectPromise.then(() => {
            deferred.resolve(workspace);
          }, (error) => {
            deferred.reject(error);
          });
        }, (error) => {
          deferred.reject(error);
        });
      }
    });
    return this.$q.all(projectPromises);
  }

  /**
   * Finilize the Workspace cloning - show proper messages, close popup.
   *
   * @param workspace the cloned workspace
   */
  finishWorkspaceCloning(workspace) {
    this.cloningSteps += 'Cloning of workspace <b>' + workspace.config.name + '</b> finished <br>';
    this.cheNotification.showInfo('Successfully cloned the workspace to ' + workspace.config.name);

    // fetch all workspaces in purpose to update number of workspaces in left navbar
    this.cheWorkspace.fetchWorkspaces().finally(() => {
      this.hide();
    });
  }

  /**
   * Notify user about the error.
   * @param error the error message to display
   */
  handleError(error) {
    this.cloningInProgress = false;
    var message;
    if (error.data) {
      if (error.data.message) {
        message = error.data.message;
      } else {
        message = error.data;
      }
    } else {
      message = 'Unable to clone the workspace';
    }
    this.cheNotification.showError('Cloning workspace failed: ' + message);
    this.$log.error('error', message, error);
  }


}
