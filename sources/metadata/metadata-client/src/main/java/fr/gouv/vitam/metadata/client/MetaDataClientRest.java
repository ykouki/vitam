/**
 * Copyright French Prime minister Office/SGMAP/DINSIC/Vitam Program (2015-2019)
 * <p>
 * contact.vitam@culture.gouv.fr
 * <p>
 * This software is a computer program whose purpose is to implement a digital archiving back-office system managing
 * high volumetry securely and efficiently.
 * <p>
 * This software is governed by the CeCILL 2.1 license under French law and abiding by the rules of distribution of free
 * software. You can use, modify and/ or redistribute the software under the terms of the CeCILL 2.1 license as
 * circulated by CEA, CNRS and INRIA at the following URL "http://www.cecill.info".
 * <p>
 * As a counterpart to the access to the source code and rights to copy, modify and redistribute granted by the license,
 * users are provided only with a limited warranty and the software's author, the holder of the economic rights, and the
 * successive licensors have only limited liability.
 * <p>
 * In this respect, the user's attention is drawn to the risks associated with loading, using, modifying and/or
 * developing or reproducing the software by the user in light of its specific status of free software, that may mean
 * that it is complicated to manipulate, and that also therefore means that it is reserved for developers and
 * experienced professionals having in-depth computer knowledge. Users are therefore encouraged to load and test the
 * software's suitability as regards their requirements in conditions enabling the security of their systems and/or data
 * to be ensured and, more generally, to use and operate it in the same conditions as regards security.
 * <p>
 * The fact that you are presently reading this means that you have had knowledge of the CeCILL 2.1 license and that you
 * accept its terms.
 **/

package fr.gouv.vitam.metadata.client;

import java.util.ArrayList;
import java.util.List;

import javax.ws.rs.HttpMethod;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import com.fasterxml.jackson.databind.JsonNode;
import com.google.common.base.Strings;
import fr.gouv.vitam.common.ParametersChecker;
import fr.gouv.vitam.common.client.DefaultClient;
import fr.gouv.vitam.common.client.VitamClientFactoryInterface;
import fr.gouv.vitam.common.database.parameter.IndexParameters;
import fr.gouv.vitam.common.database.parameter.SwitchIndexParameters;
import fr.gouv.vitam.common.error.VitamError;
import fr.gouv.vitam.common.exception.InvalidParseOperationException;
import fr.gouv.vitam.common.exception.VitamClientException;
import fr.gouv.vitam.common.exception.VitamClientInternalException;
import fr.gouv.vitam.common.exception.VitamDBException;
import fr.gouv.vitam.common.json.JsonHandler;
import fr.gouv.vitam.common.logging.VitamLogger;
import fr.gouv.vitam.common.logging.VitamLoggerFactory;
import fr.gouv.vitam.common.model.RequestResponse;
import fr.gouv.vitam.common.model.RequestResponseOK;
import fr.gouv.vitam.metadata.api.exception.MetaDataAlreadyExistException;
import fr.gouv.vitam.metadata.api.exception.MetaDataClientServerException;
import fr.gouv.vitam.metadata.api.exception.MetaDataDocumentSizeException;
import fr.gouv.vitam.metadata.api.exception.MetaDataExecutionException;
import fr.gouv.vitam.metadata.api.exception.MetaDataNotFoundException;
import fr.gouv.vitam.metadata.api.exception.MetadataInvalidSelectException;
import fr.gouv.vitam.metadata.api.model.ObjectGroupPerOriginatingAgency;
import fr.gouv.vitam.metadata.api.model.UnitPerOriginatingAgency;

/**
 * Rest client for metadata
 */
public class MetaDataClientRest extends DefaultClient implements MetaDataClient {

    private static final VitamLogger LOGGER = VitamLoggerFactory.getInstance(MetaDataClientRest.class);

    private static final String REINDEX_URI = "/reindex";
    private static final String ALIASES_URI = "/alias";
    public static final String DESCRIPTION = "description";
    public static final String CONSISTENCY_ERROR_AN_INTERNAL_DATA_CONSISTENCY_ERROR_HAS_BEEN_DETECTED =
        "[Consistency ERROR] : An internal data consistency error has been detected !";

    /**
     * Constructor using given scheme (http)
     *
     * @param factory The client factory
     */
    public MetaDataClientRest(VitamClientFactoryInterface factory) {
        super(factory);
    }

    @Override
    public JsonNode insertUnit(JsonNode insertQuery)
        throws InvalidParseOperationException, MetaDataExecutionException, MetaDataNotFoundException,
        MetaDataAlreadyExistException, MetaDataDocumentSizeException, MetaDataClientServerException {
        try {
            ParametersChecker.checkParameter(ErrorMessage.INSERT_UNITS_QUERY_NULL.getMessage(), insertQuery);
        } catch (final IllegalArgumentException e) {
            throw new InvalidParseOperationException(e);
        }
        Response response = null;
        try {
            response = performRequest(HttpMethod.POST, "/units", null, insertQuery, MediaType.APPLICATION_JSON_TYPE,
                MediaType.APPLICATION_JSON_TYPE);
            if (response.getStatus() == Response.Status.INTERNAL_SERVER_ERROR.getStatusCode()) {
                throw new MetaDataExecutionException(INTERNAL_SERVER_ERROR);
            } else if (response.getStatus() == Response.Status.NOT_FOUND.getStatusCode()) {
                throw new MetaDataNotFoundException(ErrorMessage.NOT_FOUND.getMessage());
            } else if (response.getStatus() == Response.Status.CONFLICT.getStatusCode()) {
                throw new MetaDataAlreadyExistException(ErrorMessage.DATA_ALREADY_EXISTS.getMessage());
            } else if (response.getStatus() == Response.Status.REQUEST_ENTITY_TOO_LARGE.getStatusCode()) {
                throw new MetaDataDocumentSizeException(ErrorMessage.SIZE_TOO_LARGE.getMessage());
            } else if (response.getStatus() == Response.Status.BAD_REQUEST.getStatusCode()) {
                throw new InvalidParseOperationException(ErrorMessage.INVALID_PARSE_OPERATION.getMessage());
            } else if (response.getStatus() == Response.Status.PRECONDITION_FAILED.getStatusCode()) {
                throw new IllegalArgumentException(ErrorMessage.INVALID_METADATA_VALUE.getMessage());
            }
            return JsonHandler.getFromString(response.readEntity(String.class));
        } catch (final VitamClientInternalException e) {
            LOGGER.error(INTERNAL_SERVER_ERROR, e);
            throw new MetaDataClientServerException(INTERNAL_SERVER_ERROR, e);
        } finally {
            consumeAnyEntityAndClose(response);
        }
    }

    @Override
    public JsonNode selectUnits(JsonNode selectQuery)
        throws MetaDataExecutionException, MetaDataDocumentSizeException, InvalidParseOperationException,
        MetaDataClientServerException, VitamDBException {
        try {
            ParametersChecker.checkParameter(ErrorMessage.SELECT_UNITS_QUERY_NULL.getMessage(), selectQuery);
        } catch (final IllegalArgumentException e) {
            throw new InvalidParseOperationException(e);
        }
        Response response = null;
        try {
            response = performRequest(HttpMethod.GET, "/units", null, selectQuery, MediaType.APPLICATION_JSON_TYPE,
                MediaType.APPLICATION_JSON_TYPE);
            if (response.getStatus() == Response.Status.INTERNAL_SERVER_ERROR.getStatusCode()) {
                throw new VitamDBException(CONSISTENCY_ERROR_AN_INTERNAL_DATA_CONSISTENCY_ERROR_HAS_BEEN_DETECTED);
            } else if (response.getStatus() == Response.Status.REQUEST_ENTITY_TOO_LARGE.getStatusCode()) {
                throw new MetaDataDocumentSizeException(ErrorMessage.SIZE_TOO_LARGE.getMessage());
            } else if (response.getStatus() == Response.Status.BAD_REQUEST.getStatusCode()) {
                throw new InvalidParseOperationException(ErrorMessage.INVALID_PARSE_OPERATION.getMessage());
            } else if (response.getStatus() == Response.Status.PRECONDITION_FAILED.getStatusCode()) {
                throw new InvalidParseOperationException(ErrorMessage.INVALID_PARSE_OPERATION.getMessage());
            }
            return response.readEntity(JsonNode.class);
        } catch (final VitamClientInternalException e) {
            LOGGER.error(INTERNAL_SERVER_ERROR, e);
            throw new MetaDataClientServerException(INTERNAL_SERVER_ERROR, e);
        } finally {
            consumeAnyEntityAndClose(response);
        }
    }

    @Override
    public JsonNode selectObjectGroups(JsonNode selectQuery)
        throws MetaDataExecutionException, MetaDataDocumentSizeException, InvalidParseOperationException,
        MetaDataClientServerException {
        try {
            ParametersChecker.checkParameter(ErrorMessage.SELECT_OBJECT_GROUP_QUERY_NULL.getMessage(), selectQuery);
        } catch (final IllegalArgumentException e) {
            throw new InvalidParseOperationException(e);
        }
        Response response = null;
        try {
            response =
                performRequest(HttpMethod.GET, "/objectgroups", null, selectQuery, MediaType.APPLICATION_JSON_TYPE,
                    MediaType.APPLICATION_JSON_TYPE);
            if (response.getStatus() == Response.Status.INTERNAL_SERVER_ERROR.getStatusCode()) {
                throw new MetaDataExecutionException(INTERNAL_SERVER_ERROR);
            } else if (response.getStatus() == Response.Status.REQUEST_ENTITY_TOO_LARGE.getStatusCode()) {
                throw new MetaDataDocumentSizeException(ErrorMessage.SIZE_TOO_LARGE.getMessage());
            } else if (response.getStatus() == Response.Status.BAD_REQUEST.getStatusCode()) {
                throw new InvalidParseOperationException(ErrorMessage.INVALID_PARSE_OPERATION.getMessage());
            } else if (response.getStatus() == Response.Status.PRECONDITION_FAILED.getStatusCode()) {
                throw new InvalidParseOperationException(ErrorMessage.INVALID_PARSE_OPERATION.getMessage());
            }
            return response.readEntity(JsonNode.class);
        } catch (final VitamClientInternalException e) {
            LOGGER.error(INTERNAL_SERVER_ERROR, e);
            throw new MetaDataClientServerException(INTERNAL_SERVER_ERROR, e);
        } finally {
            consumeAnyEntityAndClose(response);
        }
    }

    @Override
    public JsonNode selectUnitbyId(JsonNode selectQuery, String unitId)
        throws MetaDataExecutionException,
        MetaDataDocumentSizeException, InvalidParseOperationException, MetaDataClientServerException {
        try {
            ParametersChecker.checkParameter("One parameter is empty", selectQuery, unitId);
        } catch (final IllegalArgumentException e) {
            throw new InvalidParseOperationException(e);
        }
        if (Strings.isNullOrEmpty(unitId)) {
            throw new InvalidParseOperationException("unitId may not be empty");
        }
        Response response = null;
        try {
            response = performRequest(HttpMethod.GET, "/units/" + unitId, null, selectQuery,
                MediaType.APPLICATION_JSON_TYPE, MediaType.APPLICATION_JSON_TYPE);
            if (response.getStatus() == Response.Status.INTERNAL_SERVER_ERROR.getStatusCode()) {
                throw new MetaDataExecutionException(INTERNAL_SERVER_ERROR);
            } else if (response.getStatus() == Response.Status.REQUEST_ENTITY_TOO_LARGE.getStatusCode()) {
                throw new MetaDataDocumentSizeException(ErrorMessage.SIZE_TOO_LARGE.getMessage());
            } else if (response.getStatus() == Response.Status.BAD_REQUEST.getStatusCode()) {
                throw new InvalidParseOperationException(ErrorMessage.INVALID_PARSE_OPERATION.getMessage());
            } else if (response.getStatus() == Response.Status.PRECONDITION_FAILED.getStatusCode()) {
                throw new InvalidParseOperationException(ErrorMessage.INVALID_PARSE_OPERATION.getMessage());
            }
            return response.readEntity(JsonNode.class);
        } catch (final VitamClientInternalException e) {
            LOGGER.error(INTERNAL_SERVER_ERROR, e);
            throw new MetaDataClientServerException(INTERNAL_SERVER_ERROR, e);
        } finally {
            consumeAnyEntityAndClose(response);
        }
    }

    @Override
    public JsonNode selectObjectGrouptbyId(JsonNode selectQuery, String objectGroupId)
        throws MetaDataExecutionException, MetaDataDocumentSizeException, InvalidParseOperationException,
        MetadataInvalidSelectException, MetaDataClientServerException {
        try {
            ParametersChecker.checkParameter(ErrorMessage.SELECT_OBJECT_GROUP_QUERY_NULL.getMessage(), selectQuery);
            ParametersChecker.checkParameter(ErrorMessage.BLANK_PARAM.getMessage(), objectGroupId);
        } catch (final IllegalArgumentException e) {
            throw new InvalidParseOperationException(e);
        }
        if (Strings.isNullOrEmpty(objectGroupId)) {
            throw new InvalidParseOperationException("objectGroupId may not be empty");
        }
        Response response = null;
        try {
            response = performRequest(HttpMethod.GET, "/objectgroups/" + objectGroupId, null, selectQuery,
                MediaType.APPLICATION_JSON_TYPE, MediaType.APPLICATION_JSON_TYPE);
            if (response.getStatus() == Response.Status.INTERNAL_SERVER_ERROR.getStatusCode()) {
                throw new MetaDataExecutionException(INTERNAL_SERVER_ERROR);
            } else if (response.getStatus() == Response.Status.REQUEST_ENTITY_TOO_LARGE.getStatusCode()) {
                throw new MetaDataDocumentSizeException(ErrorMessage.SIZE_TOO_LARGE.getMessage());
            } else if (response.getStatus() == Response.Status.BAD_REQUEST.getStatusCode()) {
                throw new InvalidParseOperationException(ErrorMessage.INVALID_PARSE_OPERATION.getMessage());
            } else if (response.getStatus() == Response.Status.PRECONDITION_FAILED.getStatusCode()) {
                throw new InvalidParseOperationException(ErrorMessage.INVALID_PARSE_OPERATION.getMessage());
            }
            return response.readEntity(JsonNode.class);
        } catch (final VitamClientInternalException e) {
            LOGGER.error(INTERNAL_SERVER_ERROR, e);
            throw new MetaDataClientServerException(INTERNAL_SERVER_ERROR, e);
        } finally {
            consumeAnyEntityAndClose(response);
        }
    }

    @Override
    public JsonNode updateUnitbyId(JsonNode updateQuery, String unitId)
        throws MetaDataExecutionException,
        MetaDataDocumentSizeException, InvalidParseOperationException, MetaDataClientServerException,
        MetaDataNotFoundException {
        try {
            ParametersChecker.checkParameter(ErrorMessage.UPDATE_UNITS_QUERY_NULL.getMessage(), updateQuery, unitId);
        } catch (final IllegalArgumentException e) {
            throw new InvalidParseOperationException(e);
        }
        if (Strings.isNullOrEmpty(unitId)) {
            throw new InvalidParseOperationException("unitId may not be empty");
        }
        Response response = null;
        try {
            response = performRequest(HttpMethod.PUT, "/units/" + unitId, null, updateQuery,
                MediaType.APPLICATION_JSON_TYPE, MediaType.APPLICATION_JSON_TYPE);
            if (response.getStatus() == Response.Status.INTERNAL_SERVER_ERROR.getStatusCode()) {
                throw new MetaDataExecutionException(INTERNAL_SERVER_ERROR);
            } else if (response.getStatus() == Response.Status.REQUEST_ENTITY_TOO_LARGE.getStatusCode()) {
                throw new MetaDataDocumentSizeException(ErrorMessage.SIZE_TOO_LARGE.getMessage());
            } else if (response.getStatus() == Response.Status.BAD_REQUEST.getStatusCode()) {
                JsonNode resp = response.readEntity(JsonNode.class);
                if (null != resp) {
                    JsonNode errNode = resp.get(DESCRIPTION);
                    if (null != errNode) {
                        throw new InvalidParseOperationException(JsonHandler.unprettyPrint(errNode));
                    }
                }
                throw new InvalidParseOperationException(ErrorMessage.INVALID_PARSE_OPERATION.getMessage());
            } else if (response.getStatus() == Response.Status.PRECONDITION_FAILED.getStatusCode()) {
                throw new InvalidParseOperationException(ErrorMessage.INVALID_PARSE_OPERATION.getMessage());
            } else if (response.getStatus() == Response.Status.NOT_FOUND.getStatusCode()) {
                throw new MetaDataNotFoundException(ErrorMessage.NOT_FOUND.getMessage());
            }
            return response.readEntity(JsonNode.class);
        } catch (final VitamClientInternalException e) {
            LOGGER.error(INTERNAL_SERVER_ERROR, e);
            throw new MetaDataClientServerException(INTERNAL_SERVER_ERROR, e);
        } finally {
            consumeAnyEntityAndClose(response);
        }
    }

    @Override
    public JsonNode insertObjectGroup(JsonNode insertQuery)
        throws InvalidParseOperationException, MetaDataExecutionException, MetaDataNotFoundException,
        MetaDataAlreadyExistException, MetaDataDocumentSizeException, MetaDataClientServerException {
        ParametersChecker.checkParameter("Insert Request is a mandatory parameter", insertQuery);
        Response response = null;
        try {
            response =
                performRequest(HttpMethod.POST, "/objectgroups", null, insertQuery, MediaType.APPLICATION_JSON_TYPE,
                    MediaType.APPLICATION_JSON_TYPE);
            if (response.getStatus() == Response.Status.INTERNAL_SERVER_ERROR.getStatusCode()) {
                throw new MetaDataExecutionException(INTERNAL_SERVER_ERROR);
            } else if (response.getStatus() == Response.Status.NOT_FOUND.getStatusCode()) {
                throw new MetaDataNotFoundException(ErrorMessage.NOT_FOUND.getMessage());
            } else if (response.getStatus() == Response.Status.CONFLICT.getStatusCode()) {
                throw new MetaDataAlreadyExistException(ErrorMessage.DATA_ALREADY_EXISTS.getMessage());
            } else if (response.getStatus() == Response.Status.REQUEST_ENTITY_TOO_LARGE.getStatusCode()) {
                throw new MetaDataDocumentSizeException(ErrorMessage.SIZE_TOO_LARGE.getMessage());
            } else if (response.getStatus() == Response.Status.BAD_REQUEST.getStatusCode()) {
                throw new InvalidParseOperationException(ErrorMessage.INVALID_PARSE_OPERATION.getMessage());
            } else if (response.getStatus() == Response.Status.PRECONDITION_FAILED.getStatusCode()) {
                throw new InvalidParseOperationException(ErrorMessage.INVALID_PARSE_OPERATION.getMessage());
            }
            return JsonHandler.getFromString(response.readEntity(String.class));
        } catch (final VitamClientInternalException e) {
            LOGGER.error(INTERNAL_SERVER_ERROR, e);
            throw new MetaDataClientServerException(INTERNAL_SERVER_ERROR, e);
        } finally {
            consumeAnyEntityAndClose(response);
        }
    }


    @Override
    public void updateObjectGroupById(JsonNode queryUpdate, String objectGroupId)
        throws InvalidParseOperationException, MetaDataClientServerException, MetaDataExecutionException {
        try {
            ParametersChecker
                .checkParameter(ErrorMessage.UPDATE_UNITS_QUERY_NULL.getMessage(), queryUpdate, objectGroupId);
        } catch (final IllegalArgumentException e) {
            throw new InvalidParseOperationException(e);
        }
        if (Strings.isNullOrEmpty(objectGroupId)) {
            throw new InvalidParseOperationException("objectGroupId may not be empty");
        }
        Response response = null;
        try {
            response = performRequest(HttpMethod.PUT, "/objectgroups/" + objectGroupId, null, queryUpdate,
                MediaType.APPLICATION_JSON_TYPE, MediaType.APPLICATION_JSON_TYPE);
            if (response.getStatus() == Response.Status.INTERNAL_SERVER_ERROR.getStatusCode()) {
                throw new MetaDataExecutionException(INTERNAL_SERVER_ERROR);
            } else if (response.getStatus() == Response.Status.EXPECTATION_FAILED.getStatusCode()) {
                throw new MetaDataExecutionException(Status.EXPECTATION_FAILED.getReasonPhrase());
            } else if (response.getStatus() == Response.Status.BAD_REQUEST.getStatusCode()) {
                throw new InvalidParseOperationException(ErrorMessage.INVALID_PARSE_OPERATION.getMessage());
            } else if (response.getStatus() != Status.CREATED.getStatusCode()) {
                throw new InvalidParseOperationException(ErrorMessage.INVALID_PARSE_OPERATION.getMessage());
            } else if (response.getStatus() == Response.Status.PRECONDITION_FAILED.getStatusCode()) {
                throw new InvalidParseOperationException(ErrorMessage.INVALID_PARSE_OPERATION.getMessage());
            }
        } catch (final VitamClientInternalException e) {
            LOGGER.error(INTERNAL_SERVER_ERROR, e);
            throw new MetaDataClientServerException(INTERNAL_SERVER_ERROR, e);
        } finally {
            consumeAnyEntityAndClose(response);
        }

    }

    @Override
    public List<UnitPerOriginatingAgency> selectAccessionRegisterOnUnitByOperationId(String operationId)
        throws MetaDataClientServerException {
        Response response = null;

        try {
            response =
                performRequest(HttpMethod.GET, "/accession-registers/units/" + operationId, null, null,
                    MediaType.APPLICATION_JSON_TYPE,
                    MediaType.APPLICATION_JSON_TYPE);

            RequestResponse<JsonNode> requestResponse = RequestResponse.parseFromResponse(response);
            if (requestResponse.isOk()) {
                RequestResponseOK<JsonNode> requestResponseOK = (RequestResponseOK<JsonNode>) requestResponse;

                List<UnitPerOriginatingAgency> unitPerOriginatingAgencies = new ArrayList<>();
                for (JsonNode jsonNode : requestResponseOK.getResults()) {
                    unitPerOriginatingAgencies
                        .add(JsonHandler.getFromJsonNode(jsonNode, UnitPerOriginatingAgency.class));
                }

                return unitPerOriginatingAgencies;
            } else {
                VitamError vitamError = (VitamError) requestResponse;
                LOGGER
                    .error("find accession register for unit failed, http code is {}, error is {}",
                        vitamError.getCode(),
                        vitamError.getErrors());
                throw new MetaDataClientServerException(vitamError.getDescription());
            }

        } catch (VitamClientInternalException | InvalidParseOperationException e) {
            LOGGER.error(INTERNAL_SERVER_ERROR, e);
            throw new MetaDataClientServerException(INTERNAL_SERVER_ERROR, e);
        } finally {
            consumeAnyEntityAndClose(response);
        }
    }

    @Override
    public List<ObjectGroupPerOriginatingAgency> selectAccessionRegisterOnObjectByOperationId(String operationId)
        throws MetaDataClientServerException {
        Response response = null;

        try {
            response =
                performRequest(HttpMethod.GET, "/accession-registers/objects/" + operationId, null, null,
                    MediaType.APPLICATION_JSON_TYPE,
                    MediaType.APPLICATION_JSON_TYPE);

            RequestResponse<ObjectGroupPerOriginatingAgency> requestResponse =
                RequestResponse.parseFromResponse(response, ObjectGroupPerOriginatingAgency.class);
            if (requestResponse.isOk()) {
                RequestResponseOK<ObjectGroupPerOriginatingAgency> requestResponseOK =
                    (RequestResponseOK<ObjectGroupPerOriginatingAgency>) requestResponse;

                return requestResponseOK.getResults();
            } else {
                VitamError vitamError = (VitamError) requestResponse;
                LOGGER
                    .error("find accession register for object group failed, http code is {}, error is {}",
                        vitamError.getCode(),
                        vitamError.getErrors());
                throw new MetaDataClientServerException(vitamError.getDescription());
            }

        } catch (VitamClientInternalException e) {
            LOGGER.error(INTERNAL_SERVER_ERROR, e);
            throw new MetaDataClientServerException(INTERNAL_SERVER_ERROR, e);
        } finally {
            consumeAnyEntityAndClose(response);
        }
    }

    @Override
    public boolean flushUnits() throws MetaDataClientServerException {
        Response response = null;
        try {
            response = performRequest(HttpMethod.PUT, "/units", null, MediaType.APPLICATION_JSON_TYPE);
            return response.getStatus() == Response.Status.OK.getStatusCode();
        } catch (final VitamClientInternalException e) {
            LOGGER.error(INTERNAL_SERVER_ERROR, e);
            throw new MetaDataClientServerException(INTERNAL_SERVER_ERROR, e);
        } finally {
            consumeAnyEntityAndClose(response);
        }
    }

    @Override
    public boolean flushObjectGroups() throws MetaDataClientServerException {
        Response response = null;
        try {
            response = performRequest(HttpMethod.PUT, "/objectgroups", null, MediaType.APPLICATION_JSON_TYPE);
            return response.getStatus() == Response.Status.OK.getStatusCode();
        } catch (final VitamClientInternalException e) {
            LOGGER.error(INTERNAL_SERVER_ERROR, e);
            throw new MetaDataClientServerException(INTERNAL_SERVER_ERROR, e);
        } finally {
            consumeAnyEntityAndClose(response);
        }
    }

    @Override
    public JsonNode reindex(IndexParameters indexParam)
        throws InvalidParseOperationException, MetaDataClientServerException, MetaDataNotFoundException {
        ParametersChecker.checkParameter("The options are mandatory", indexParam);
        Response response = null;
        try {
            response = performRequest(HttpMethod.POST, REINDEX_URI, null, indexParam,
                MediaType.APPLICATION_JSON_TYPE, MediaType.APPLICATION_JSON_TYPE);

            return response.readEntity(JsonNode.class);

        } catch (VitamClientInternalException e) {
            LOGGER.error("Internal Server Error", e);
            throw new MetaDataClientServerException("Internal Server Error", e);
        } finally {
            consumeAnyEntityAndClose(response);
        }
    }

    @Override
    public JsonNode switchIndexes(SwitchIndexParameters switchIndexParam)
        throws InvalidParseOperationException, MetaDataClientServerException, MetaDataNotFoundException {
        ParametersChecker.checkParameter("The options are mandatory", switchIndexParam);
        Response response = null;
        try {
            response = performRequest(HttpMethod.POST, ALIASES_URI, null, switchIndexParam,
                MediaType.APPLICATION_JSON_TYPE, MediaType.APPLICATION_JSON_TYPE);

            return response.readEntity(JsonNode.class);

        } catch (VitamClientInternalException e) {
            LOGGER.error("Internal Server Error", e);
            throw new MetaDataClientServerException("Internal Server Error", e);
        } finally {
            consumeAnyEntityAndClose(response);
        }
    }

    @Override
    public RequestResponse<JsonNode> getUnitByIdRaw(String unitId) throws VitamClientException {
        ParametersChecker.checkParameter("The unit id is mandatory", unitId);
        Response response = null;
        try {
            response = performRequest(HttpMethod.GET, "/raw/units/" + unitId, null, null,
                MediaType.APPLICATION_JSON_TYPE, MediaType.APPLICATION_JSON_TYPE);
            return RequestResponse.parseFromResponse(response, JsonNode.class);

        } catch (IllegalStateException e) {
            LOGGER.error("Could not parse server response ", e);
            throw createExceptionFromResponse(response);
        } catch (final VitamClientInternalException e) {
            LOGGER.error(INTERNAL_SERVER_ERROR, e);
            throw new VitamClientException(INTERNAL_SERVER_ERROR, e);
        } finally {
            consumeAnyEntityAndClose(response);
        }
    }

    @Override
    public RequestResponse<JsonNode> getObjectGroupByIdRaw(String objectGroupId) throws VitamClientException {
        ParametersChecker.checkParameter("The unit id is mandatory", objectGroupId);
        Response response = null;
        try {
            response = performRequest(HttpMethod.GET, "/raw/objectgroups/" + objectGroupId, null, null,
                MediaType.APPLICATION_JSON_TYPE, MediaType.APPLICATION_JSON_TYPE);
            return RequestResponse.parseFromResponse(response, JsonNode.class);

        } catch (IllegalStateException e) {
            LOGGER.error("Could not parse server response ", e);
            throw createExceptionFromResponse(response);
        } catch (final VitamClientInternalException e) {
            LOGGER.error(INTERNAL_SERVER_ERROR, e);
            throw new VitamClientException(INTERNAL_SERVER_ERROR, e);
        } finally {
            consumeAnyEntityAndClose(response);
        }
    }

}
