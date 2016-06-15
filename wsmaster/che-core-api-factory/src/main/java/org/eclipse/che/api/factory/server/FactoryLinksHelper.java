/*******************************************************************************
 * Copyright (c) 2012-2016 Codenvy, S.A.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 * Codenvy, S.A. - initial API and implementation
 *******************************************************************************/
package org.eclipse.che.api.factory.server;

import com.google.common.base.Strings;

import org.eclipse.che.api.core.rest.shared.dto.Link;
import org.eclipse.che.api.core.util.LinksHelper;
import org.eclipse.che.api.factory.shared.dto.FactoryDto;

import javax.ws.rs.HttpMethod;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.UriBuilder;
import javax.ws.rs.core.UriInfo;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import static java.util.stream.Collectors.toList;
import static org.eclipse.che.api.factory.shared.FactoryConstants.ACCEPTED_REL_ATT;
import static org.eclipse.che.api.factory.shared.FactoryConstants.FACTORY_ACCEPTANCE_REL_ATT;
import static org.eclipse.che.api.factory.shared.FactoryConstants.IMAGE_REL_ATT;
import static org.eclipse.che.api.factory.shared.FactoryConstants.NAMED_FACTORY_ACCEPTANCE_REL_ATT;
import static org.eclipse.che.api.factory.shared.FactoryConstants.RETRIEVE_FACTORY_REL_ATT;
import static org.eclipse.che.api.factory.shared.FactoryConstants.SNIPPET_REL_ATT;

/** Helper class for creation links. */
public class FactoryLinksHelper {

    private static List<String> snippetTypes = Collections.unmodifiableList(Arrays.asList("markdown", "url", "html", "iframe"));

    private FactoryLinksHelper() {}

    /**
     * Creates factory links and links on factory images.
     *
     * @param images
     *         a set of factory images
     * @param uriInfo
     *         URI information about relative URIs are relative to the base URI
     * @return list of factory links
     * @throws UnsupportedEncodingException
     *         occurs when impossible to encode URL
     */
    public static List<Link> createLinks(FactoryDto factory, Set<FactoryImage> images, UriInfo uriInfo, String userName)
            throws UnsupportedEncodingException {
        final List<Link> links = new LinkedList<>(createLinks(factory, uriInfo, userName));
        final UriBuilder baseUriBuilder = uriInfo != null ? UriBuilder.fromUri(uriInfo.getBaseUri()) : UriBuilder.fromUri("/");

        // add path to factory service
        final UriBuilder factoryUriBuilder = baseUriBuilder.clone().path(FactoryService.class);
        final String factoryId = factory.getId();

        // uri's to retrieve images
        links.addAll(images.stream()
                           .map(image -> LinksHelper.createLink(HttpMethod.GET,
                                                                factoryUriBuilder.clone()
                                                                                 .path(FactoryService.class, "getImage")
                                                                                 .queryParam("imgId", image.getName())
                                                                                 .build(factoryId)
                                                                                 .toString(),
                                                                null,
                                                                image.getMediaType(),
                                                                IMAGE_REL_ATT))
                           .collect(toList()));
        return links;
    }

    /**
     * Creates factory links.
     *
     * @param uriInfo
     *         URI information about relative URIs are relative to the base URI
     * @return list of factory links
     * @throws UnsupportedEncodingException
     *         occurs when impossible to encode URL
     */
    public static List<Link> createLinks(FactoryDto factory, UriInfo uriInfo, String userName) throws UnsupportedEncodingException {
        final List<Link> links = new LinkedList<>();
        final UriBuilder baseUriBuilder = uriInfo != null ? UriBuilder.fromUri(uriInfo.getBaseUri()) : UriBuilder.fromUri("/");

        // add path to factory service
        final UriBuilder factoryUriBuilder = baseUriBuilder.clone().path(FactoryService.class);
        final String factoryId = factory.getId();

        // uri to retrieve factory
        links.add(LinksHelper.createLink(HttpMethod.GET,
                                         factoryUriBuilder.clone()
                                                          .path(FactoryService.class, "getFactory")
                                                          .build(factoryId)
                                                          .toString(),
                                         null,
                                         MediaType.APPLICATION_JSON,
                                         RETRIEVE_FACTORY_REL_ATT));

        // uri's of snippets
        links.addAll(snippetTypes.stream()
                                 .map(snippet -> LinksHelper.createLink(HttpMethod.GET,
                                                                        factoryUriBuilder.clone()
                                                                                         .path(FactoryService.class, "getFactorySnippet")
                                                                                         .queryParam("type", snippet)
                                                                                         .build(factoryId)
                                                                                         .toString(),
                                                                        null,
                                                                        MediaType.TEXT_PLAIN,
                                                                        SNIPPET_REL_ATT + snippet))
                                 .collect(toList()));

        // uri to accept factory
        final Link createWorkspace = LinksHelper.createLink(HttpMethod.GET,
                                                            baseUriBuilder.clone()
                                                                          .replacePath("f")
                                                                          .queryParam("id", factoryId)
                                                                          .build()
                                                                          .toString(),
                                                            null,
                                                            MediaType.TEXT_HTML,
                                                            FACTORY_ACCEPTANCE_REL_ATT);
        links.add(createWorkspace);

        if (!Strings.isNullOrEmpty(factory.getName()) && !Strings.isNullOrEmpty(userName)) {
            // uri to accept factory by name and creator
            final Link createWorkspaceFromNamedFactory = LinksHelper.createLink(HttpMethod.GET,
                                                                                baseUriBuilder.clone()
                                                                                              .replacePath("f")
                                                                                              .queryParam("name", factory.getName())
                                                                                              .queryParam("user", userName)
                                                                                              .build()
                                                                                              .toString(),
                                                                                null,
                                                                                MediaType.TEXT_HTML,
                                                                                NAMED_FACTORY_ACCEPTANCE_REL_ATT);
            links.add(createWorkspaceFromNamedFactory);
        }

        // links of analytics
        links.add(LinksHelper.createLink(HttpMethod.GET,
                                         baseUriBuilder.clone()
                                                       .path("analytics")
                                                       .path("public-metric/factory_used")
                                                       .queryParam("factory", URLEncoder.encode(createWorkspace.getHref(), "UTF-8"))
                                                       .build()
                                                       .toString(),
                                         null,
                                         MediaType.TEXT_PLAIN,
                                         ACCEPTED_REL_ATT));
        return links;
    }
}
