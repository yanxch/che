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
package org.eclipse.che.api.debugger.server;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import org.eclipse.che.api.debugger.shared.dto.action.Action;
import org.eclipse.che.api.debugger.shared.dto.action.ResumeAction;
import org.eclipse.che.api.debugger.shared.dto.action.StartAction;
import org.eclipse.che.api.debugger.shared.dto.action.StepIntoAction;
import org.eclipse.che.api.debugger.shared.dto.action.StepOutAction;
import org.eclipse.che.api.debugger.shared.dto.action.StepOverAction;
import org.eclipse.che.dto.server.DtoFactory;
import org.eclipse.che.dto.shared.DTO;

import javax.inject.Singleton;
import javax.ws.rs.Consumes;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.ext.MessageBodyReader;
import javax.ws.rs.ext.Provider;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;

/**
 * Implementation of {@link MessageBodyReader} needed for binding JSON content to Java Objects.
 *
 * @author Anatoliy Bazko
 * @see DTO
 * @see DtoFactory
 */
@Singleton
@Provider
@Consumes({MediaType.APPLICATION_JSON})
public class DebuggerActionProvider implements MessageBodyReader<Action> {

    @Override
    public boolean isReadable(Class<?> type, Type genericType, Annotation[] annotations, MediaType mediaType) {
        return Action.class.isAssignableFrom(type);
    }

    @Override
    public Action readFrom(Class<Action> type,
                           Type genericType,
                           Annotation[] annotations,
                           MediaType mediaType,
                           MultivaluedMap<String, String> httpHeaders,
                           InputStream entityStream) throws IOException, WebApplicationException {
        String json = readJson(entityStream);

        JsonParser jsonParser = new JsonParser();
        JsonElement jsonElement = jsonParser.parse(json);
        JsonObject jsonObject = jsonElement.getAsJsonObject();

        if (jsonObject.has("type")) {
            int actionType = jsonObject.get("type").getAsJsonPrimitive().getAsInt();
            switch (actionType) {
                case Action.RESUME:
                    return DtoFactory.getInstance().createDtoFromJson(json, ResumeAction.class);
                case Action.START:
                    return DtoFactory.getInstance().createDtoFromJson(json, StartAction.class);
                case Action.STEP_INTO:
                    return DtoFactory.getInstance().createDtoFromJson(json, StepIntoAction.class);
                case Action.STEP_OUT:
                    return DtoFactory.getInstance().createDtoFromJson(json, StepOutAction.class);
                case Action.STEP_OVER:
                    return DtoFactory.getInstance().createDtoFromJson(json, StepOverAction.class);
                default:
                    throw new IOException("Unsupported type value " + actionType);
            }
        }

        throw new IOException("Json is broken. There is not type key in json object");
    }

    private String readJson(InputStream entityStream) throws IOException {
        String line;
        StringBuilder sb = new StringBuilder();
        BufferedReader reader = new BufferedReader(new InputStreamReader(entityStream));
        while ((line = reader.readLine()) != null) {
            sb.append(line);
        }
        return sb.toString();
    }
}
