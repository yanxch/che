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
package org.eclipse.che.api.debugger.shared.dto.action;

import org.eclipse.che.dto.shared.DTO;

/**
 * @author Anatoliy Bazko
 */
@DTO
public interface StepOverAction extends Action {
    int getType();

    void setType(int type);

    StepOverAction withType(int type);
}
