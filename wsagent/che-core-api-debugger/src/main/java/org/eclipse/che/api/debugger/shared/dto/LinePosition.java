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
package org.eclipse.che.api.debugger.shared.dto;

import org.eclipse.che.dto.shared.DTO;

/**
 * //
 *
 * @author Alexander Andrienko
 */
//todo need check do we need this dto
@DTO
public interface LinePosition {
    int getStartCharOffset();

    void setStartCharOffset(int startOffset);

    LinePosition withStartCharOffset(int startOffset);

    int getEndCharOffset();

    void setEndCharOffset(int endOffset);

    LinePosition withEndCharOffset(int endOffset);
}

