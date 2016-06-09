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
 * @name workspace.clone.controller:CloneWorkspaceController
 * @description This class is handling the controller for the clone of workspace
 * @author Oleksii Kurinnyi
 */
export class CloneWorkspaceController {

  /**
   * Default constructor that is using resource
   * @ngInject for Dependency injection
   */
  constructor($mdDialog) {
    this.$mdDialog = $mdDialog;
  }

  showClone($event) {
    this.$mdDialog.show({
      targetEvent: $event,
      controller: 'CloneWorkspaceDialogController',
      controllerAs: 'cloneWorkspaceDialogController',
      bindToController: true,
      clickOutsideToClose: true,
      locals: {
        workspaceDetails: this.workspaceDetails,
        callbackController: this},
      templateUrl: 'app/workspaces/workspace-details/clone-workspace/dialog/clone-workspace-dialog.html'
    });
  }


}
