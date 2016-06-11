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
package org.eclipse.che.ide.api.event;

import com.google.gwt.event.shared.GwtEvent;

import org.eclipse.che.ide.api.resources.VirtualFile;

/**
 * @author Alexander Andrienko
 */
public class FileContentUpdatedEvent extends GwtEvent<FileContentUpdatedEventHandler> {

    public static Type<FileContentUpdatedEventHandler> TYPE = new Type<>();
    private VirtualFile file;

    public FileContentUpdatedEvent(VirtualFile file) {
        this.file = file;
    }

    public VirtualFile getFile() {
        return file;
    }

    @Override
    public Type<FileContentUpdatedEventHandler> getAssociatedType() {
        return TYPE;
    }

    @Override
    protected void dispatch(FileContentUpdatedEventHandler handler) {
        handler.onContentUpdated(this);
    }
}
