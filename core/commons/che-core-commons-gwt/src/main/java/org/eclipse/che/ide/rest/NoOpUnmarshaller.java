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
package org.eclipse.che.ide.rest;

import com.google.gwt.http.client.Response;

import org.eclipse.che.ide.commons.exception.UnmarshallerException;

/**
 * @author Vlad Zhukovskiy
 */
public class NoOpUnmarshaller implements Unmarshallable<Void> {
    @Override
    public void unmarshal(Response response) throws UnmarshallerException {

    }

    @Override
    public Void getPayload() {
        return null;
    }
}
