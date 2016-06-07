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
package org.eclipse.che.api.debug.shared.dto;

import org.eclipse.che.api.debug.shared.model.Location;
import org.eclipse.che.dto.shared.DTO;

/** @author andrew00x */
@DTO
public interface LocationDto extends Location {
    void setTarget(String target);

    LocationDto withTarget(String target);

    void setLineNumber(int lineNumber);

    LocationDto withLineNumber(int lineNumber);

    void setExternalResource(boolean externalResource);

    void setResourcePath(String resourcePath);

    LocationDto withResourcePath(String resourcePath);

    LocationDto withExternalResource(boolean externalResource);

    void setExternalResourceId(int externalResourceId);//todo maybe String here? it'll be more universally

    LocationDto withExternalResourceId(int externalResourceId);

    void setProjectPath(String projectPath);

    LocationDto withProjectPath(String projectPath);
}
