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
package org.eclipse.che.api.factory.server;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;

import com.google.gson.JsonSyntaxException;

import org.apache.commons.fileupload.FileItem;
import org.eclipse.che.api.core.BadRequestException;
import org.eclipse.che.api.core.ConflictException;
import org.eclipse.che.api.core.ForbiddenException;
import org.eclipse.che.api.core.NotFoundException;
import org.eclipse.che.api.core.ServerException;
import org.eclipse.che.api.core.model.project.ProjectConfig;
import org.eclipse.che.api.core.rest.Service;
import org.eclipse.che.api.factory.server.builder.FactoryBuilder;
import org.eclipse.che.api.factory.server.model.impl.FactoryImpl;
import org.eclipse.che.api.factory.server.snippet.SnippetGenerator;
import org.eclipse.che.api.factory.shared.dto.AuthorDto;
import org.eclipse.che.api.factory.shared.dto.FactoryDto;
import org.eclipse.che.api.user.server.dao.UserDao;
import org.eclipse.che.api.workspace.server.WorkspaceManager;
import org.eclipse.che.api.workspace.server.model.impl.ProjectConfigImpl;
import org.eclipse.che.api.workspace.server.model.impl.WorkspaceImpl;
import org.eclipse.che.commons.env.EnvironmentContext;
import org.eclipse.che.commons.lang.NameGenerator;
import org.eclipse.che.commons.lang.Pair;
import org.eclipse.che.commons.lang.URLEncodedUtils;
import org.eclipse.che.commons.subject.Subject;
import org.eclipse.che.dto.server.DtoFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriBuilder;
import javax.ws.rs.core.UriInfo;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Predicate;

import static com.google.common.base.Strings.isNullOrEmpty;
import static java.util.stream.Collectors.toList;
import static javax.ws.rs.core.HttpHeaders.CONTENT_DISPOSITION;
import static javax.ws.rs.core.MediaType.APPLICATION_JSON;
import static javax.ws.rs.core.MediaType.MULTIPART_FORM_DATA;
import static javax.ws.rs.core.MediaType.TEXT_PLAIN;
import static org.eclipse.che.api.factory.server.FactoryLinksHelper.createLinks;

/**
 * Defines Factory REST API.
 *
 * @author Anton Korneta
 * @author Florent Benoit
 */
@Api(value = "/factory",
     description = "Factory manager")
@Path("/factory")
public class FactoryService extends Service {
    private static final Logger LOG = LoggerFactory.getLogger(FactoryService.class);

    /**
     * Error message if there is no plugged resolver.
     */
    public static final String ERROR_NO_RESOLVER_AVAILABLE = "Cannot build factory with any of the provided parameters.";

    /**
     * If there is no parameter.
     */
    public static final String ERROR_NO_PARAMETERS = "Missing parameters";

    /**
     * Validate query parameter. If true, factory will be validated
     */
    public static final String VALIDATE_QUERY_PARAMETER = "validate";

    /**
     * Set of resolvers for factories. Injected through an holder.
     */
    private final Set<FactoryParametersResolver> factoryParametersResolvers;

    private final FactoryManager         factoryManager;
    private final FactoryEditValidator   factoryEditValidator;
    private final FactoryCreateValidator createValidator;
    private final FactoryAcceptValidator acceptValidator;
    private final LinksHelper            linksHelper;
    private final FactoryBuilder         factoryBuilder;
    private final WorkspaceManager       workspaceManager;
    private final UserDao                userDao;

    @Inject
    public FactoryService(FactoryManager factoryManager,
                          FactoryCreateValidator createValidator,
                          FactoryAcceptValidator acceptValidator,
                          FactoryEditValidator factoryEditValidator,
                          LinksHelper linksHelper,
                          FactoryBuilder factoryBuilder,
                          WorkspaceManager workspaceManager,
                          FactoryParametersResolverHolder factoryParametersResolverHolder,
                          UserDao userDao) {
        this.factoryManager = factoryManager;
        this.createValidator = createValidator;
        this.acceptValidator = acceptValidator;
        this.factoryEditValidator = factoryEditValidator;
        this.linksHelper = linksHelper;
        this.factoryBuilder = factoryBuilder;
        this.workspaceManager = workspaceManager;
        this.factoryParametersResolvers = factoryParametersResolverHolder.getFactoryParametersResolvers();
        this.userDao = userDao;
    }

    /**
     * Save factory to storage and return stored data. Field 'factory' should contains factory information.
     * Fields with images should be named 'image'. Acceptable image size 100x100 pixels.
     *
     * @param formData
     *         http request form data
     * @return stored data
     * @throws ForbiddenException
     *         when the user have no access rights for saving the factory
     * @throws ConflictException
     *         when an error occurred during saving the factory
     * @throws BadRequestException
     *         when image content cannot be read or is invalid
     * @throws ServerException
     *         when any server errors occurs
     */
    @POST
    @Consumes(MULTIPART_FORM_DATA)
    @Produces(APPLICATION_JSON)
    @ApiOperation(value = "Create a Factory and return data",
                  notes = "Save factory to storage and return stored data. Field 'factory' should contains factory information.")
    @ApiResponses({@ApiResponse(code = 200, message = "OK"),
                   @ApiResponse(code = 400, message = "Missed required parameters, parameters are not valid"),
                   @ApiResponse(code = 403, message = "The user does not have appropriate rights for perform factory save"),
                   @ApiResponse(code = 409, message = "Conflict error. Some parameter is missing"),
                   @ApiResponse(code = 500, message = "Unable to identify user from context")})
    public FactoryDto saveFactory(Iterator<FileItem> formData) throws ForbiddenException,
                                                                      ConflictException,
                                                                      BadRequestException,
                                                                      ServerException,
                                                                      NotFoundException {
        try {
            final Set<FactoryImage> images = new HashSet<>();
            FactoryDto factoryDto = null;
            while (formData.hasNext()) {
                final FileItem item = formData.next();
                switch (item.getFieldName()) {
                    case ("factory"): {
                        try (InputStream factoryData = item.getInputStream()) {
                            factoryDto = factoryBuilder.build(factoryData);
                        } catch (JsonSyntaxException e) {
                            throw new BadRequestException("You have provided an invalid JSON.  For more information, please visit: " +
                                                          "http://docs.codenvy.com/user/creating-factories/factory-parameter-reference/");
                        }
                        break;
                    }
                    case ("image"): {
                        try (InputStream imageData = item.getInputStream()) {
                            final FactoryImage factoryImage = FactoryImage.createImage(imageData,
                                                                                       item.getContentType(),
                                                                                       NameGenerator.generate(null, 16));
                            if (factoryImage.hasContent()) {
                                images.add(factoryImage);
                            }
                        }
                        break;
                    }
                    default:
                        //DO NOTHING
                }
            }
            if (factoryDto == null) {
                LOG.warn("No factory information found in 'factory' section of multipart form-data.");
                throw new BadRequestException("No factory information found in 'factory' section of multipart/form-data.");
            }
            processDefaults(factoryDto);
            final FactoryImpl factory = new FactoryImpl(factoryDto, images);
            createValidator.validateOnCreate(factoryDto);
            final FactoryImpl storedFactory = factoryManager.createFactory(factory);
            return injectLinks(DtoConverter.asDto(storedFactory), images, uriInfo);
        } catch (IOException e) {
            LOG.error(e.getLocalizedMessage(), e);
            throw new ServerException(e.getLocalizedMessage(), e);
        }
    }

    /**
     * Save factory to storage and return stored data.
     *
     * @param factory
     *         instance of factory which would be stored
     * @return decorated the factory instance of which has been stored
     * @throws BadRequestException
     *         when stored the factory is invalid
     * @throws ServerException
     *         when any server errors occurs
     * @throws ForbiddenException
     *         when the user have no access rights for saving the factory
     * @throws ConflictException
     *         when stored the factory is already exist
     */
    @POST
    @Consumes(APPLICATION_JSON)
    @Produces(APPLICATION_JSON)
    @ApiOperation(value = "Stores the factory from the configuration",
                  notes = "Stores the factory without pictures and returns instance of the stored factory with links")
    @ApiResponses({@ApiResponse(code = 200, message = "OK"),
                   @ApiResponse(code = 400, message = "Missed required parameters, parameters are not valid"),
                   @ApiResponse(code = 403, message = "The user does not have appropriate rights for perform factory save"),
                   @ApiResponse(code = 409, message = "Conflict error. Some parameter is missing"),
                   @ApiResponse(code = 500, message = "Internal Server Error")})
    public FactoryDto saveFactory(FactoryDto factory) throws BadRequestException,
                                                             ServerException,
                                                             ForbiddenException,
                                                             ConflictException,
                                                             NotFoundException {
        if (factory == null) {
            throw new BadRequestException("Not null factory required");
        }
        FactoryImpl factoryImp = new FactoryImpl(factory, null);
        processDefaults(factory);
        createValidator.validateOnCreate(factory);
        FactoryDto factoryDto = DtoConverter.asDto(factoryManager.createFactory(factoryImp));
        try {
            factoryDto.withLinks(createLinks(factoryDto, uriInfo, null));
            return factoryDto;
        } catch (UnsupportedEncodingException e) {
            throw new ServerException(e);
        }
    }

    /**
     * Get factory information from storage by specified id.
     *
     * @param id
     *         id of factory
     * @param uriInfo
     *         url context
     * @return the factory instance if it's found by id
     * @throws NotFoundException
     *         when the factory with specified id doesn't not found
     * @throws ServerException
     *         when any server errors occurs
     * @throws BadRequestException
     *         when the factory is invalid e.g. is expired
     */
    @GET
    @Path("/{id}")
    @Produces(APPLICATION_JSON)
    @ApiOperation(value = "Get factory information by its id",
                  notes = "Get JSON with factory information. Factory id is passed in a path parameter")
    @ApiResponses({@ApiResponse(code = 200, message = "OK"),
                   @ApiResponse(code = 404, message = "Factory not found"),
                   @ApiResponse(code = 400, message = "Failed to validate factory e.g. if it expired"),
                   @ApiResponse(code = 500, message = "Internal server error")})
    public FactoryDto getFactory(@ApiParam(value = "Factory ID")
                                 @PathParam("id")
                                 String id,
                                 @ApiParam(value = "Whether or not to validate values like it is done when accepting a Factory",
                                           allowableValues = "true,false",
                                           defaultValue = "false")
                                 @DefaultValue("false")
                                 @QueryParam("validate")
                                 Boolean validate,
                                 @Context
                                 UriInfo uriInfo) throws NotFoundException,
                                                         ServerException,
                                                         BadRequestException {
        final FactoryImpl factory = factoryManager.getById(id);
        final FactoryDto factoryDto = DtoConverter.asDto(factory);
        if (validate) {
            acceptValidator.validateOnAccept(factoryDto);
        }
        return injectLinks(factoryDto, factoryManager.getFactoryImages(id, null), uriInfo);
    }

    /**
     * Updates specified factory with a new factory content.
     *
     * @param id
     *         id of factory
     * @param newFactory
     *         the new data for the factory
     * @return updated factory with links
     * @throws BadRequestException
     *         when the factory config is invalid
     * @throws NotFoundException
     *         when the factory with specified id doesn't not found
     * @throws ServerException
     *         when any server error occurs
     * @throws ForbiddenException
     *         when the current user is not granted to edit the factory
     * @throws ConflictException
     *         when not rewritable factory information is present in the new factory
     */
    @PUT
    @Path("/{id}")
    @Consumes(APPLICATION_JSON)
    @Produces(APPLICATION_JSON)
    @ApiOperation(value = "Updates factory information by its id",
                  notes = "Updates factory based on the factory id which is passed in a path parameter. " +
                          "For perform this operation user needs respective rights")
    @ApiResponses({@ApiResponse(code = 200, message = "OK"),
                   @ApiResponse(code = 400, message = "Missed required parameters, parameters are not valid"),
                   @ApiResponse(code = 403, message = "User not authorized to call this operation"),
                   @ApiResponse(code = 409, message = "Not rewritable factory information is present in the new factory"),
                   @ApiResponse(code = 404, message = "Factory to update not found"),
                   @ApiResponse(code = 500, message = "Internal server error")})
    public FactoryDto updateFactory(@ApiParam(value = "Factory id")
                                    @PathParam("id")
                                    String id,
                                    FactoryDto newFactory) throws BadRequestException,
                                                                  NotFoundException,
                                                                  ServerException,
                                                                  ForbiddenException,
                                                                  ConflictException {
        // forbid null update
        if (newFactory == null) {
            throw new BadRequestException("The updating factory shouldn't be null");
        }
        final FactoryImpl existingFactory = factoryManager.getById(id);

        processDefaults(newFactory);
        newFactory.getCreator().withCreated(existingFactory.getCreator().getCreated());
        newFactory.setId(existingFactory.getId());

        final FactoryImpl factory = new FactoryImpl(newFactory, null);
        // validate the new content
        createValidator.validateOnCreate(newFactory);

        factoryManager.updateFactory(factory);
        return injectLinks(DtoConverter.asDto(factory), factoryManager.getFactoryImages(id, null), uriInfo);
    }

    /**
     * Removes factory information from storage by its id.
     *
     * @param id
     *         id of factory
     * @param uriInfo
     *         url context
     * @throws NotFoundException
     *         when the factory with specified id doesn't not found
     * @throws ServerException
     *         when any server errors occurs
     * @throws ForbiddenException
     *         when user does not have permission for removal the factory
     */
    @DELETE
    @Path("/{id}")
    @ApiOperation(value = "Removes factory by its id",
                  notes = "Removes factory based on the factory id which is passed in a path parameter. " +
                          "For perform this operation user needs respective rights")
    @ApiResponses({@ApiResponse(code = 200, message = "OK"),
                   @ApiResponse(code = 403, message = "User not authorized to call this operation"),
                   @ApiResponse(code = 404, message = "Factory not found"),
                   @ApiResponse(code = 500, message = "Internal server error")})
    public void removeFactory(@ApiParam(value = "Factory id")
                              @PathParam("id")
                              String id,
                              @Context
                              UriInfo uriInfo) throws NotFoundException,
                                                      ServerException,
                                                      ForbiddenException {
        factoryManager.removeFactory(id);
    }

    /**
     * Get list of factories which conform specified attributes.
     *
     * @param maxItems
     *         max number of items in response
     * @param skipCount
     *         skip items. Must be equals or greater then {@code 0}
     * @param uriInfo
     *         url context
     * @return stored data, if specified attributes is correct
     * @throws BadRequestException
     *         when no search attributes passed
     */
    @GET
    @Path("/find")
    @Produces(APPLICATION_JSON)
    @ApiOperation(value = "Get Factory by attribute",
                  notes = "If specify more than one value for a single query parameter then will be taken first one")
    @ApiResponses({@ApiResponse(code = 200, message = "OK"),
                   @ApiResponse(code = 400, message = "Failed to validate factory e.g. if it expired"),
                   @ApiResponse(code = 500, message = "Internal server error")})
    public List<FactoryDto> getFactoryByAttribute(@DefaultValue("0")
                                                  @QueryParam("skipCount")
                                                  Integer skipCount,
                                                  @DefaultValue("30")
                                                  @QueryParam("maxItems")
                                                  Integer maxItems,
                                                  @Context
                                                  UriInfo uriInfo) throws BadRequestException,
                                                                          NotFoundException,
                                                                          ServerException {
        final List<String> skipParams = Arrays.asList("token", "skipCount", "maxItems");
        final List<Pair<String, String>> queryParams = URLEncodedUtils.parse(uriInfo.getRequestUri()).entrySet().stream()
                                                                      .filter(entry -> !skipParams.contains(entry.getKey()) &&
                                                                                       !entry.getValue().isEmpty())
                                                                      .map(entry -> Pair.of(entry.getKey(),
                                                                                            entry.getValue().iterator().next()))
                                                                      .collect(toList());
        if (queryParams.isEmpty()) {
            throw new BadRequestException("Query must contain at least one attribute.");
        }

        final List<FactoryDto> result = new ArrayList<>();
        for (FactoryImpl factory : factoryManager.getByAttribute(maxItems, skipCount, queryParams)) {
            result.add(injectLinks(DtoConverter.asDto(factory), null, uriInfo));
        }

        return result;
    }

    /**
     * Get image information by its id from specified factory.
     *
     * @param id
     *         id of factory
     * @param imageId
     *         image id
     * @return image information if ids are correct. If imageId is not set, random image of the factory will be returned,
     * if factory has no images, exception will be thrown
     * @throws NotFoundException
     *         when the factory with specified id doesn't not found
     * @throws NotFoundException
     *         when image id is not specified and there is no default image for the specified factory
     * @throws NotFoundException
     *         when image with specified id doesn't exist
     */
    @GET
    @Path("/{id}/image")
    @Produces("image/*")
    @ApiOperation(value = "Get factory image information",
                  notes = "If the factory does not have image with specified id then first found image will be returned")
    @ApiResponses({@ApiResponse(code = 200, message = "OK"),
                   @ApiResponse(code = 404, message = "Factory or image id not found")})
    public Response getImage(@ApiParam(value = "Factory id")
                             @PathParam("id")
                             String id,
                             @ApiParam(value = "Image id", required = true)
                             @QueryParam("imgId")
                             String imageId) throws NotFoundException {
        final Set<FactoryImage> factoryImages = factoryManager.getFactoryImages(id, null);
        if (isNullOrEmpty(imageId)) {
            if (factoryImages.isEmpty()) {
                LOG.warn("Default image for factory {} is not found.", id);
                throw new NotFoundException("Default image for factory " + id + " is not found.");
            }
            final FactoryImage image = factoryImages.iterator().next();
            return Response.ok(image.getImageData(), image.getMediaType()).build();
        }
        for (FactoryImage image : factoryImages) {
            if (imageId.equals(image.getName())) {
                return Response.ok(image.getImageData(), image.getMediaType()).build();
            }
        }
        LOG.warn("Image with id {} is not found.", imageId);
        throw new NotFoundException("Image with id " + imageId + " is not found.");
    }

    /**
     * Get factory snippet by factory id and snippet type. If snippet type is not set, "url" type will be used as default.
     *
     * @param id
     *         id of factory
     * @param type
     *         type of snippet
     * @param uriInfo
     *         url context
     * @return snippet content.
     * @throws NotFoundException
     *         when factory with specified id doesn't not found - with response code 400
     * @throws ServerException
     *         when any server error occurs during snippet creation
     * @throws BadRequestException
     *         when the snippet type is not supported,
     *         or if the specified factory does not contain enough information for snippet creation
     */
    @GET
    @Path("/{id}/snippet")
    @Produces(TEXT_PLAIN)
    @ApiOperation(value = "Get factory snippet by id",
                  notes = "If snippet type not set then default 'url' will be used")
    @ApiResponses({@ApiResponse(code = 200, message = "OK"),
                   @ApiResponse(code = 400, message = "Missed required parameters, parameters are not valid"),
                   @ApiResponse(code = 404, message = "Factory or factory images not found"),
                   @ApiResponse(code = 500, message = "Internal server error")})
    public String getFactorySnippet(@ApiParam(value = "Factory ID")
                                    @PathParam("id")
                                    String id,
                                    @ApiParam(value = "Snippet type",
                                              required = true,
                                              allowableValues = "url,html,iframe,markdown",
                                              defaultValue = "url")
                                    @DefaultValue("url")
                                    @QueryParam("type")
                                    String type,
                                    @Context
                                    UriInfo uriInfo) throws NotFoundException,
                                                            ServerException,
                                                            BadRequestException {
        final FactoryImpl factory = factoryManager.getById(id);
        final String baseUrl = UriBuilder.fromUri(uriInfo.getBaseUri()).replacePath("").build().toString();
        switch (type) {
            case "url":
                return UriBuilder.fromUri(uriInfo.getBaseUri()).replacePath("factory").queryParam("id", id).build().toString();
            case "html":
                return SnippetGenerator.generateHtmlSnippet(baseUrl, id);
            case "iframe":
                return SnippetGenerator.generateiFrameSnippet(baseUrl, id);
            case "markdown":
                final Set<FactoryImage> factoryImages = factoryManager.getFactoryImages(id, null);
                final String imageId = (factoryImages.size() > 0) ? factoryImages.iterator().next().getName() : null;
                try {
                    return SnippetGenerator.generateMarkdownSnippet(baseUrl, factory, imageId);
                } catch (IllegalArgumentException e) {
                    throw new BadRequestException(e.getLocalizedMessage());
                }
            default:
                LOG.warn("Snippet type {} is unsupported", type);
                throw new BadRequestException("Snippet type \"" + type + "\" is unsupported.");
        }
    }

    /**
     * Generate factory containing workspace configuration.
     * Only projects that have {@code SourceStorage} configured can be included.
     *
     * @param workspace
     *         workspace id to generate factory from
     * @param path
     *         optional project path, if set, only this project will be included into result projects set
     * @throws ServerException
     *         when any server error occurs during factory getting
     * @throws BadRequestException
     *         when it is impossible get factory from specified workspace e.g. no projects in workspace
     * @throws NotFoundException
     *         when user's workspace with specified id not found
     * @throws ForbiddenException
     *         when user have no access rights e.g. user is not owner of specified workspace
     */
    @GET
    @Path("/workspace/{ws-id}")
    @Produces(APPLICATION_JSON)
    @ApiOperation(value = "Construct factory from workspace",
                  notes = "This call returns a Factory.json that is used to create a factory.")
    @ApiResponses({@ApiResponse(code = 200, message = "OK"),
                   @ApiResponse(code = 400, message = "Missed required parameters, parameters are not valid"),
                   @ApiResponse(code = 403, message = "Access to workspace denied"),
                   @ApiResponse(code = 404, message = "Workspace not found"),
                   @ApiResponse(code = 500, message = "Internal server error")})
    public Response getFactoryJson(@ApiParam(value = "Workspace ID")
                                   @PathParam("ws-id")
                                   String workspace,
                                   @ApiParam(value = "Project path")
                                   @QueryParam("path")
                                   String path) throws ServerException,
                                                       BadRequestException,
                                                       NotFoundException,
                                                       ForbiddenException {
        final WorkspaceImpl usersWorkspace = workspaceManager.getWorkspace(workspace);
        excludeProjectsWithoutLocation(usersWorkspace, path);
        final FactoryDto factory = DtoFactory.newDto(FactoryDto.class).withWorkspace(
                org.eclipse.che.api.workspace.server.DtoConverter.asDto(usersWorkspace.getConfig()))
                                                           .withV("4.0");
        return Response.ok(factory, APPLICATION_JSON)
                       .header(CONTENT_DISPOSITION, "attachment; filename=factory.json")
                       .build();
    }


    /**
     * Resolve parameters and build a factory for the given parameters
     *
     * @param parameters
     *         map of key/values used to build factory.
     * @param uriInfo
     *         url context
     * @return a factory instance if found a matching resolver
     * @throws NotFoundException
     *         when no resolver can be used
     * @throws ServerException
     *         when any server errors occurs
     * @throws BadRequestException
     *         when the factory is invalid e.g. is expired
     */
    @POST
    @Path("/resolver")
    @Consumes(APPLICATION_JSON)
    @Produces(APPLICATION_JSON)
    @ApiOperation(value = "Create factory by providing map of parameters",
                  notes = "Get JSON with factory information.")
    @ApiResponses({@ApiResponse(code = 200, message = "OK"),
                   @ApiResponse(code = 400, message = "Failed to validate factory"),
                   @ApiResponse(code = 500, message = "Internal server error")})
    public FactoryDto resolveFactory(@ApiParam(value = "Parameters provided to create factories")
                                     Map<String, String> parameters,
                                     @ApiParam(value = "Whether or not to validate values like it is done when accepting a Factory",
                                               allowableValues = "true,false",
                                               defaultValue = "false")
                                     @DefaultValue("false")
                                     @QueryParam(VALIDATE_QUERY_PARAMETER)
                                     Boolean validate,
                                     @Context
                                     UriInfo uriInfo) throws NotFoundException,
                                                             ServerException,
                                                             BadRequestException {

        // Check parameter
        if (parameters == null) {
            throw new BadRequestException(ERROR_NO_PARAMETERS);
        }

        // search matching resolver
        Optional<FactoryParametersResolver> factoryParametersResolverOptional = this.factoryParametersResolvers.stream()
                                                                                                               .filter((resolver -> resolver
                                                                                                                       .accept(parameters)))
                                                                                                               .findFirst();

        // no match
        if (!factoryParametersResolverOptional.isPresent()) {
            throw new NotFoundException(ERROR_NO_RESOLVER_AVAILABLE);
        }

        // create factory from matching resolver
        final FactoryDto factory = factoryParametersResolverOptional.get().createFactory(parameters);

        // Apply links
        try {
            factory.setLinks(linksHelper.createLinks(factory, uriInfo, null));
        } catch (UnsupportedEncodingException e) {
            throw new ServerException(e.getLocalizedMessage(), e);
        }

        // time to validate the factory
        if (validate) {
            acceptValidator.validateOnAccept(factory);
        }

        return factory;
    }

    /**
     * Creates factory links.
     *
     * If factory is named it will be generated accept named link,
     * if images set is not null and not empty it will be generate links for them
     */
    private FactoryDto injectLinks(FactoryDto factory, Set<FactoryImage> images, UriInfo uriInfo) throws NotFoundException,
                                                                                                         ServerException {
        try {
            String username = null;
            if (!isNullOrEmpty(factory.getName())) {
                username = userDao.getById(factory.getCreator().getUserId()).getName();
            }
            factory.withLinks(images != null && !images.isEmpty()
                              ? createLinks(factory, images, uriInfo, username)
                              : createLinks(factory, uriInfo, username));
            return factory;
        } catch (UnsupportedEncodingException e) {
            throw new ServerException(e.getLocalizedMessage(), e);
        }
    }

    /**
     * Filters workspace projects, removes projects which don't have location set.
     * If all workspace projects don't have location throws {@link BadRequestException}.
     */
    private void excludeProjectsWithoutLocation(WorkspaceImpl usersWorkspace, String projectPath) throws BadRequestException {
        final boolean notEmptyPath = projectPath != null;
        //Condition for sifting valid project in user's workspace
        Predicate<ProjectConfig> predicate = projectConfig -> {
            // if project is a subproject (it's path contains another project) , then location can be null
            final boolean isSubProject = projectConfig.getPath().indexOf('/', 1) != -1;
            final boolean hasNotEmptySource = projectConfig.getSource() != null
                                              && projectConfig.getSource().getType() != null
                                              && projectConfig.getSource().getLocation() != null;

            return !(notEmptyPath && !projectPath.equals(projectConfig.getPath()))
                   && (isSubProject || hasNotEmptySource);
        };

        //Filtered out projects by path and source storage presence.
        final List<ProjectConfigImpl> filtered = usersWorkspace.getConfig()
                                                               .getProjects()
                                                               .stream()
                                                               .filter(predicate)
                                                               .collect(toList());
        if (filtered.isEmpty()) {
            throw new BadRequestException("Unable to create factory from this workspace, " +
                                          "because it does not contains projects with source storage set and/or specified path");
        }
        usersWorkspace.getConfig().setProjects(filtered);
    }

    /**
     * Adds to the factory information about creator and time of creation
     */
    private void processDefaults(FactoryDto factory) {
        final Subject currentSubject = EnvironmentContext.getCurrent().getSubject();
        final AuthorDto creator = factory.getCreator();
        if (creator == null) {
            factory.setCreator(DtoFactory.newDto(AuthorDto.class).withUserId(currentSubject.getUserId())
                                                   .withCreated(System.currentTimeMillis()));
            return;
        }
        if (isNullOrEmpty(creator.getUserId())) {
            creator.setUserId(currentSubject.getUserId());
        }
        if (creator.getCreated() == null) {
            creator.setCreated(System.currentTimeMillis());
        }
    }


    /**
     * Usage of a dedicated class to manage the optional resolvers
     */
    protected static class FactoryParametersResolverHolder {

        /**
         * Optional inject for the resolvers.
         */
        @com.google.inject.Inject(optional = true)
        private Set<FactoryParametersResolver> factoryParametersResolvers;

        /**
         * Provides the set of resolvers if there are some else return an empty set.
         * @return a non null set
         */
        public Set<FactoryParametersResolver> getFactoryParametersResolvers() {
            if (factoryParametersResolvers != null) {
                return factoryParametersResolvers;
            } else {
                return Collections.emptySet();
            }
        }
    }
}
