/**
 * Copyright French Prime minister Office/SGMAP/DINSIC/Vitam Program (2015-2019)
 *
 * contact.vitam@culture.gouv.fr
 *
 * This software is a computer program whose purpose is to implement a digital archiving back-office system managing
 * high volumetry securely and efficiently.
 *
 * This software is governed by the CeCILL 2.1 license under French law and abiding by the rules of distribution of free
 * software. You can use, modify and/ or redistribute the software under the terms of the CeCILL 2.1 license as
 * circulated by CEA, CNRS and INRIA at the following URL "http://www.cecill.info".
 *
 * As a counterpart to the access to the source code and rights to copy, modify and redistribute granted by the license,
 * users are provided only with a limited warranty and the software's author, the holder of the economic rights, and the
 * successive licensors have only limited liability.
 *
 * In this respect, the user's attention is drawn to the risks associated with loading, using, modifying and/or
 * developing or reproducing the software by the user in light of its specific status of free software, that may mean
 * that it is complicated to manipulate, and that also therefore means that it is reserved for developers and
 * experienced professionals having in-depth computer knowledge. Users are therefore encouraged to load and test the
 * software's suitability as regards their requirements in conditions enabling the security of their systems and/or data
 * to be ensured and, more generally, to use and operate it in the same conditions as regards security.
 *
 * The fact that you are presently reading this means that you have had knowledge of the CeCILL 2.1 license and that you
 * accept its terms.
 */
package fr.gouv.vitam.storage.engine.client;

import java.io.InputStream;
import java.time.LocalDateTime;

import org.apache.commons.io.IOUtils;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import fr.gouv.vitam.common.LocalDateUtil;
import fr.gouv.vitam.common.ServerIdentity;
import fr.gouv.vitam.common.client.SSLClientConfiguration;
import fr.gouv.vitam.common.exception.InvalidParseOperationException;
import fr.gouv.vitam.common.exception.VitamClientException;
import fr.gouv.vitam.common.json.JsonHandler;
import fr.gouv.vitam.common.model.StatusMessage;
import fr.gouv.vitam.storage.engine.client.exception.StorageAlreadyExistsClientException;
import fr.gouv.vitam.storage.engine.client.exception.StorageNotFoundClientException;
import fr.gouv.vitam.storage.engine.client.exception.StorageServerClientException;
import fr.gouv.vitam.storage.engine.common.model.request.CreateObjectDescription;
import fr.gouv.vitam.storage.engine.common.model.response.StoredInfoResult;

/**
 * Mock client implementation for storage
 */
class StorageClientMock extends StorageClientRest implements StorageClient {

    private static final ServerIdentity SERVER_IDENTITY = ServerIdentity.getInstance();

    static final String MOCK_POST_RESULT = "{\"_id\": \"{id}\",\"status\": \"OK\"}";
    static final String MOCK_INFOS_RESULT = "{\"usableSpace\": 838860800" + "}";
    static final String MOCK_GET_FILE_CONTENT = "Vitam test";

    /**
     * Constructor
     */
    StorageClientMock() {
        super(new SSLClientConfiguration("mock", 1, false, "/"), "/", false);
    }

    @Override
    public StatusMessage getStatus() throws VitamClientException {
        return new StatusMessage(SERVER_IDENTITY);
    }

    @Override
    public JsonNode getStorageInformation(String tenantId, String strategyId)
        throws StorageNotFoundClientException, StorageServerClientException {
        try {
            return JsonHandler.getFromString(MOCK_INFOS_RESULT);
        } catch (InvalidParseOperationException e) {
            throw new StorageServerClientException(e);
        }
    }

    @Override
    public StoredInfoResult storeFileFromWorkspace(String tenantId, String strategyId, StorageCollectionType type,
        String guid,
        CreateObjectDescription description)
        throws StorageAlreadyExistsClientException, StorageNotFoundClientException, StorageServerClientException {
        return generateStoredInfoResult(guid);
    }

    @Override
    public boolean deleteContainer(String tenantId, String strategyId) throws StorageServerClientException {
        return true;
    }

    @Override
    public boolean delete(String tenantId, String strategyId, StorageCollectionType type, String guid)
        throws StorageServerClientException {
        return true;
    }

    @Override
    public boolean existsContainer(String tenantId, String strategyId) throws StorageServerClientException {
        return true;
    }

    @Override
    public boolean exists(String tenantId, String strategyId, StorageCollectionType type, String guid)
        throws StorageServerClientException {
        return true;
    }

    @Override
    public InputStream getContainerObject(String tenantId, String strategyId, String guid) {
        return IOUtils.toInputStream(MOCK_GET_FILE_CONTENT);
    }

    private StoredInfoResult generateStoredInfoResult(String guid) {
        StoredInfoResult result = new StoredInfoResult();
        result.setId(guid);
        result.setInfo("Stockage de l'objet réalisé avec succès");
        result.setCreationTime(LocalDateUtil.getString(LocalDateTime.now()));
        result.setLastModifiedTime(LocalDateUtil.getString(LocalDateTime.now()));
        return result;
    }

}
