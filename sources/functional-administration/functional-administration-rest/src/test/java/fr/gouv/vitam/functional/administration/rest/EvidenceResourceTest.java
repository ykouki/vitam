package fr.gouv.vitam.functional.administration.rest;


import com.fasterxml.jackson.databind.JsonNode;
import fr.gouv.vitam.common.database.builder.query.QueryHelper;
import fr.gouv.vitam.common.database.builder.request.multiple.SelectMultiQuery;
import fr.gouv.vitam.common.database.builder.request.single.Select;
import fr.gouv.vitam.common.exception.VitamClientException;
import fr.gouv.vitam.common.guid.GUID;
import fr.gouv.vitam.common.guid.GUIDFactory;
import fr.gouv.vitam.common.logging.VitamLogger;
import fr.gouv.vitam.common.logging.VitamLoggerFactory;
import fr.gouv.vitam.common.model.RequestResponseOK;
import fr.gouv.vitam.common.thread.RunWithCustomExecutor;
import fr.gouv.vitam.common.thread.RunWithCustomExecutorRule;
import fr.gouv.vitam.common.thread.VitamThreadPoolExecutor;
import fr.gouv.vitam.common.thread.VitamThreadUtils;
import fr.gouv.vitam.logbook.common.exception.LogbookClientAlreadyExistsException;
import fr.gouv.vitam.logbook.operations.client.LogbookOperationsClient;
import fr.gouv.vitam.logbook.operations.client.LogbookOperationsClientFactory;
import fr.gouv.vitam.processing.management.client.ProcessingManagementClient;
import fr.gouv.vitam.processing.management.client.ProcessingManagementClientFactory;
import fr.gouv.vitam.workspace.client.WorkspaceClient;
import fr.gouv.vitam.workspace.client.WorkspaceClientFactory;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;

import javax.ws.rs.core.Response;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.when;

/**
 * EvidenceResource Test
 */
public class EvidenceResourceTest {

    private static final VitamLogger LOGGER = VitamLoggerFactory.getInstance(EvidenceResourceTest.class);

    @Rule
    public RunWithCustomExecutorRule runInThread =
        new RunWithCustomExecutorRule(VitamThreadPoolExecutor.getDefaultExecutor());
    @Mock ProcessingManagementClientFactory processingManagementClientFactory;
    @Mock ProcessingManagementClient processingManagementClient;
    @Mock WorkspaceClientFactory workspaceClientFactory;
    @Mock WorkspaceClient workspaceClient;
    @Mock LogbookOperationsClientFactory logbookOperationsClientFactory;
    @Mock LogbookOperationsClient logbookOperationsClient;

    @Rule
    public MockitoRule rule = MockitoJUnit.rule();

    private static final int TENANT_ID = 0;

    @Before
    public void setUp() {
        when(processingManagementClientFactory.getClient()).thenReturn(processingManagementClient);
        when(workspaceClientFactory.getClient()).thenReturn(workspaceClient);
        when(logbookOperationsClientFactory.getClient()).thenReturn(logbookOperationsClient);
    }

    @Test
    @RunWithCustomExecutor
    public void testEvidenceResource() throws Exception {
        VitamThreadUtils.getVitamSession().setTenantId(TENANT_ID);

        GUID guid = GUIDFactory.newEventGUID(TENANT_ID);
        VitamThreadUtils.getVitamSession().setRequestId(guid);

        EvidenceResource evidenceResource =
            new EvidenceResource(processingManagementClientFactory, logbookOperationsClientFactory,
                workspaceClientFactory);

        Response audit = evidenceResource.audit(new Select().getFinalSelect());
        assertThat(audit.getStatus()).isEqualTo(Response.Status.FORBIDDEN.getStatusCode());

        SelectMultiQuery selectMultiQuery = new SelectMultiQuery();

        selectMultiQuery.setQuery(QueryHelper.eq("title", "test"));

        when(processingManagementClient
            .executeOperationProcess(anyString(), eq("EVIDENCE_AUDIT"), anyString(), anyString()))
            .thenReturn(new RequestResponseOK<JsonNode>(new Select().getFinalSelect()).setHttpCode(200));
        audit = evidenceResource.audit(selectMultiQuery.getFinalSelect());
        assertThat(audit.getStatus()).isEqualTo(Response.Status.OK.getStatusCode());

        willThrow(VitamClientException.class).given(workspaceClient).putObject(anyString(), any(), any());
        audit = evidenceResource.audit(selectMultiQuery.getFinalSelect());
        assertThat(audit.getStatus()).isEqualTo(Response.Status.INTERNAL_SERVER_ERROR.getStatusCode());


        willThrow(LogbookClientAlreadyExistsException.class).given(logbookOperationsClient).create( any());
        audit = evidenceResource.audit(selectMultiQuery.getFinalSelect());
        assertThat(audit.getStatus()).isEqualTo(Response.Status.INTERNAL_SERVER_ERROR.getStatusCode());


    }

}
