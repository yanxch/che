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

import {DockerRegistryList} from './prefs/docker-registry-list.directive';
import {DockerRegistryListController} from './prefs/docker-registry-list.controller';
import {AddRegistryController} from './prefs/add-registry/add-registry.controller';


export class ProfileConfig {

  constructor(register) {
    register.directive('dockerRegistryList', DockerRegistryList);
    register.controller('DockerRegistryListController', DockerRegistryListController);
    register.controller('AddRegistryController', AddRegistryController);
  }
}
