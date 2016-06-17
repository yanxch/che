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
package org.eclipse.che.commons.xml;

import org.eclipse.che.api.core.rest.shared.dto.ServiceError;
import org.eclipse.che.dto.server.DtoFactory;

import javax.inject.Singleton;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.ext.ExceptionMapper;
import javax.ws.rs.ext.Provider;

/**
 * @author Roman Nikitenko
 */
@Provider
@Singleton
public class XMLTreeExceptionMapper implements ExceptionMapper<XMLTreeException> {
    @Override
    public Response toResponse(XMLTreeException exception) {
        ServiceError serviceError = DtoFactory.getInstance().createDto(ServiceError.class).withMessage(exception.getMessage());
        return Response.serverError()
                       .entity(DtoFactory.getInstance().toJson(serviceError))
                       .type(MediaType.APPLICATION_JSON)
                       .build();
    }
}
