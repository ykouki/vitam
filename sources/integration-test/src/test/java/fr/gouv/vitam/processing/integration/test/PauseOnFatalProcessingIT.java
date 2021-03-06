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

package fr.gouv.vitam.processing.integration.test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.InputStream;
import java.util.List;

import javax.ws.rs.core.Response;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import de.flapdoodle.embed.mongo.MongodExecutable;
import de.flapdoodle.embed.mongo.MongodProcess;
import de.flapdoodle.embed.mongo.MongodStarter;
import de.flapdoodle.embed.mongo.config.MongodConfigBuilder;
import de.flapdoodle.embed.mongo.config.Net;
import de.flapdoodle.embed.mongo.distribution.Version;
import de.flapdoodle.embed.process.runtime.Network;
import fr.gouv.vitam.common.CommonMediaType;
import fr.gouv.vitam.common.PropertiesUtils;
import fr.gouv.vitam.common.SystemPropertyUtil;
import fr.gouv.vitam.common.VitamConfiguration;
import fr.gouv.vitam.common.client.MockOrRestClient;
import fr.gouv.vitam.common.client.configuration.ClientConfigurationImpl;
import fr.gouv.vitam.common.database.builder.request.single.Select;
import fr.gouv.vitam.common.format.identification.FormatIdentifierFactory;
import fr.gouv.vitam.common.guid.GUID;
import fr.gouv.vitam.common.guid.GUIDFactory;
import fr.gouv.vitam.common.json.JsonHandler;
import fr.gouv.vitam.common.junit.JunitHelper;
import fr.gouv.vitam.common.logging.SysErrLogger;
import fr.gouv.vitam.common.logging.VitamLogger;
import fr.gouv.vitam.common.logging.VitamLoggerFactory;
import fr.gouv.vitam.common.model.ItemStatus;
import fr.gouv.vitam.common.model.ProcessAction;
import fr.gouv.vitam.common.model.ProcessState;
import fr.gouv.vitam.common.model.RequestResponse;
import fr.gouv.vitam.common.model.RequestResponseOK;
import fr.gouv.vitam.common.model.StatusCode;
import fr.gouv.vitam.common.model.administration.ContextModel;
import fr.gouv.vitam.common.model.administration.IngestContractModel;
import fr.gouv.vitam.common.model.administration.ProfileModel;
import fr.gouv.vitam.common.model.administration.SecurityProfileModel;
import fr.gouv.vitam.common.storage.StorageConfiguration;
import fr.gouv.vitam.common.thread.RunWithCustomExecutor;
import fr.gouv.vitam.common.thread.RunWithCustomExecutorRule;
import fr.gouv.vitam.common.thread.VitamThreadPoolExecutor;
import fr.gouv.vitam.common.thread.VitamThreadUtils;
import fr.gouv.vitam.functional.administration.client.AdminManagementClient;
import fr.gouv.vitam.functional.administration.client.AdminManagementClientFactory;
import fr.gouv.vitam.functional.administration.rest.AdminManagementMain;
import fr.gouv.vitam.logbook.common.exception.LogbookClientAlreadyExistsException;
import fr.gouv.vitam.logbook.common.exception.LogbookClientBadRequestException;
import fr.gouv.vitam.logbook.common.exception.LogbookClientServerException;
import fr.gouv.vitam.logbook.common.parameters.Contexts;
import fr.gouv.vitam.logbook.common.parameters.LogbookOperationParameters;
import fr.gouv.vitam.logbook.common.parameters.LogbookParametersFactory;
import fr.gouv.vitam.logbook.common.parameters.LogbookTypeProcess;
import fr.gouv.vitam.logbook.lifecycles.client.LogbookLifeCyclesClientFactory;
import fr.gouv.vitam.logbook.operations.client.LogbookOperationsClient;
import fr.gouv.vitam.logbook.operations.client.LogbookOperationsClientFactory;
import fr.gouv.vitam.logbook.rest.LogbookMain;
import fr.gouv.vitam.metadata.client.MetaDataClientFactory;
import fr.gouv.vitam.metadata.rest.MetadataMain;
import fr.gouv.vitam.processing.common.model.PauseRecover;
import fr.gouv.vitam.processing.common.model.ProcessStep;
import fr.gouv.vitam.processing.common.model.ProcessWorkflow;
import fr.gouv.vitam.processing.engine.core.monitoring.ProcessMonitoringImpl;
import fr.gouv.vitam.processing.management.client.ProcessingManagementClient;
import fr.gouv.vitam.processing.management.client.ProcessingManagementClientFactory;
import fr.gouv.vitam.processing.management.rest.ProcessManagementMain;
import fr.gouv.vitam.worker.server.rest.WorkerMain;
import fr.gouv.vitam.workspace.client.WorkspaceClient;
import fr.gouv.vitam.workspace.client.WorkspaceClientFactory;
import fr.gouv.vitam.workspace.rest.WorkspaceMain;
import org.assertj.core.api.Assertions;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

public class PauseOnFatalProcessingIT {

    private static final VitamLogger LOGGER = VitamLoggerFactory.getInstance(PausedProcessingIT.class);

    private final static String CLUSTER_NAME = "vitam-cluster";
    private static final Integer TENANT_ID = 0;
    private static final int DATABASE_PORT = 12346;

    private static final long SLEEP_TIME = 100l;
    private static final long NB_TRY = 4800; // equivalent to 4 minute

    private static final String WORFKLOW_NAME = "PROCESS_SIP_UNITARY";

    private static int TCP_PORT = 54321;
    private static int HTTP_PORT = 54320;

    private static final int PORT_SERVICE_WORKER = 8098;
    private static final int PORT_SERVICE_WORKSPACE = 8094;
    private static final int PORT_SERVICE_METADATA = 8096;
    private static final int PORT_SERVICE_PROCESSING = 8097;
    private static final int PORT_SERVICE_FUNCTIONAL_ADMIN = 8093;
    private static final int PORT_SERVICE_LOGBOOK = 8099;

    private static final String SIP_FOLDER = "SIP";

    @ClassRule
    public static TemporaryFolder tempFolder = new TemporaryFolder();

    @Rule
    public RunWithCustomExecutorRule runInThread =
        new RunWithCustomExecutorRule(VitamThreadPoolExecutor.getDefaultExecutor());

    private static String CONFIG_WORKER_PATH;
    private static String CONFIG_WORKSPACE_PATH;
    private static String CONFIG_METADATA_PATH;
    private static String CONFIG_PROCESSING_PATH;
    private static String CONFIG_FUNCTIONAL_ADMIN_PATH;
    private static String CONFIG_LOGBOOK_PATH;
    private static String CONFIG_SIEGFRIED_PATH;
    private static final String WORKSPACE_URL = "http://localhost:" + PORT_SERVICE_WORKSPACE;
    private static final String PROCESSING_URL = "http://localhost:" + PORT_SERVICE_PROCESSING;

    private static MetadataMain metadataApplication;
    private static WorkerMain workerApplication;
    private static AdminManagementMain adminManagementApplication;
    private static LogbookMain logbookApplication;
    private static ProcessManagementMain processManagementMain;
    private static WorkspaceMain workspaceMain;

    private static JunitHelper.ElasticsearchTestConfiguration configES;
    private static MongodExecutable mongodExecutable;
    private static MongodProcess mongod;

    private static String SIP_FILE_OK_NAME = "integration-processing/OK_ARBO_complexe.zip";

    private WorkspaceClient workspaceClient;
    private ProcessingManagementClient processingClient;

    private boolean imported = false;
    // used to check processed elements per step
    private int[] elementCountPerStep = {1, 2, 5, 1, 2, 5, 2, 5, 0, 1, 1};


    @BeforeClass
    public static void setUpBeforeClass() throws Exception {

        // set bulk size to 1 for tests
        VitamConfiguration.setWorkerBulkSize(1);

        File vitamTempFolder = tempFolder.newFolder();
        SystemPropertyUtil.set("vitam.tmp.folder", vitamTempFolder.getAbsolutePath());

        VitamConfiguration.getConfiguration()
            .setData(PropertiesUtils.getResourcePath("integration-processing/").toString());

        CONFIG_METADATA_PATH = PropertiesUtils.getResourcePath("integration-processing/metadata.conf").toString();
        CONFIG_WORKER_PATH = PropertiesUtils.getResourcePath("integration-processing/worker.conf").toString();
        CONFIG_WORKSPACE_PATH = PropertiesUtils.getResourcePath("integration-processing/workspace.conf").toString();
        CONFIG_PROCESSING_PATH = PropertiesUtils.getResourcePath("integration-processing/processing.conf").toString();
        CONFIG_SIEGFRIED_PATH =
            PropertiesUtils.getResourcePath("integration-processing/format-identifiers.conf").toString();
        CONFIG_FUNCTIONAL_ADMIN_PATH =
            PropertiesUtils.getResourcePath("integration-processing/functional-administration.conf").toString();
        CONFIG_LOGBOOK_PATH = PropertiesUtils.getResourcePath("integration-processing/logbook.conf").toString();
        CONFIG_SIEGFRIED_PATH =
            PropertiesUtils.getResourcePath("integration-processing/format-identifiers.conf").toString();

        configES = JunitHelper.startElasticsearchForTest(tempFolder, CLUSTER_NAME, TCP_PORT, HTTP_PORT);
        final MongodStarter starter = MongodStarter.getDefaultInstance();
        mongodExecutable = starter.prepare(new MongodConfigBuilder()
            .withLaunchArgument("--enableMajorityReadConcern")
            .version(Version.Main.PRODUCTION)
            .net(new Net(DATABASE_PORT, Network.localhostIsIPv6()))
            .build());
        mongod = mongodExecutable.start();

        // launch metadata
        SystemPropertyUtil.set(MetadataMain.PARAMETER_JETTY_SERVER_PORT,
            Integer.toString(PORT_SERVICE_METADATA));
        metadataApplication = new MetadataMain(CONFIG_METADATA_PATH);
        metadataApplication.start();
        SystemPropertyUtil.clear(MetadataMain.PARAMETER_JETTY_SERVER_PORT);

        MetaDataClientFactory.changeMode(new ClientConfigurationImpl("localhost", PORT_SERVICE_METADATA));

        // launch workspace
        File workspaceConfigurationFile = PropertiesUtils.findFile(CONFIG_WORKSPACE_PATH);
        final StorageConfiguration workspaceConfiguration =
            PropertiesUtils.readYaml(workspaceConfigurationFile, StorageConfiguration.class);
        workspaceConfiguration.setStoragePath(vitamTempFolder.getAbsolutePath());
        PropertiesUtils.writeYaml(workspaceConfigurationFile, workspaceConfiguration);

        SystemPropertyUtil.set(WorkspaceMain.PARAMETER_JETTY_SERVER_PORT,
            Integer.toString(PORT_SERVICE_WORKSPACE));
        workspaceMain = new WorkspaceMain(CONFIG_WORKSPACE_PATH);
        workspaceMain.start();
        SystemPropertyUtil.clear(WorkspaceMain.PARAMETER_JETTY_SERVER_PORT);

        WorkspaceClientFactory.changeMode(WORKSPACE_URL);

        // launch logbook
        SystemPropertyUtil
            .set(LogbookMain.PARAMETER_JETTY_SERVER_PORT, Integer.toString(PORT_SERVICE_LOGBOOK));
        logbookApplication = new LogbookMain(CONFIG_LOGBOOK_PATH);
        logbookApplication.start();
        SystemPropertyUtil.clear(LogbookMain.PARAMETER_JETTY_SERVER_PORT);

        LogbookOperationsClientFactory.changeMode(new ClientConfigurationImpl("localhost", PORT_SERVICE_LOGBOOK));
        LogbookLifeCyclesClientFactory.changeMode(new ClientConfigurationImpl("localhost", PORT_SERVICE_LOGBOOK));

        // launch processing
        SystemPropertyUtil.set(ProcessManagementMain.PARAMETER_JETTY_SERVER_PORT,
            Integer.toString(PORT_SERVICE_PROCESSING));
        processManagementMain = new ProcessManagementMain(CONFIG_PROCESSING_PATH);
        processManagementMain.start();
        SystemPropertyUtil.clear(ProcessManagementMain.PARAMETER_JETTY_SERVER_PORT);

        // launch worker
        SystemPropertyUtil.set("jetty.worker.port", Integer.toString(PORT_SERVICE_WORKER));
        workerApplication = new WorkerMain(CONFIG_WORKER_PATH);
        workerApplication.start();
        SystemPropertyUtil.clear("jetty.worker.port");

        FormatIdentifierFactory.getInstance().changeConfigurationFile(CONFIG_SIEGFRIED_PATH);

        // launch functional Admin server
        adminManagementApplication = new AdminManagementMain(CONFIG_FUNCTIONAL_ADMIN_PATH);
        adminManagementApplication.start();

        AdminManagementClientFactory
            .changeMode(new ClientConfigurationImpl("localhost", PORT_SERVICE_FUNCTIONAL_ADMIN));
    }

    @AfterClass
    public static void tearDownAfterClass() throws Exception {
        VitamConfiguration.setWorkerBulkSize(10);
        try (WorkspaceClient workspaceClient = WorkspaceClientFactory.getInstance().getClient()) {
            workspaceClient.deleteContainer("process", true);
        } catch (final Exception e) {
            LOGGER.error(e);
        }
        if (configES != null) {
            JunitHelper.stopElasticsearchForTest(configES);
        }
        if (mongod != null) {
            mongod.stop();
        }
        if (mongodExecutable != null) {
            mongodExecutable.stop();
        }
        if (workspaceMain != null) {
            workspaceMain.stop();
        }
        if (adminManagementApplication != null) {
            adminManagementApplication.stop();
        }
        if (workerApplication != null) {
            workerApplication.stop();
        }
        if (logbookApplication != null) {
            logbookApplication.stop();
        }
        if (processManagementMain != null) {
            processManagementMain.stop();
        }
        if (metadataApplication != null) {
            metadataApplication.stop();
        }
    }

    private void wait(String operationId) {
        int nbTry = 0;
        while (!processingClient.isOperationCompleted(operationId)) {
            try {
                Thread.sleep(SLEEP_TIME);
            } catch (InterruptedException e) {
                SysErrLogger.FAKE_LOGGER.ignoreLog(e);
            }
            if (nbTry == NB_TRY)
                break;
            nbTry++;
        }
    }

    private void tryImportFile() {
        VitamThreadUtils.getVitamSession().setContextId("Context_IT");

        if (!imported) {
            try (AdminManagementClient client = AdminManagementClientFactory.getInstance().getClient()) {
                VitamThreadUtils.getVitamSession().setRequestId(GUIDFactory.newOperationLogbookGUID(TENANT_ID));
                client.importFormat(
                    PropertiesUtils.getResourceAsStream("integration-processing/DROID_SignatureFile_V88.xml"),
                    "DROID_SignatureFile_V88.xml");

                // Import Rules
                VitamThreadUtils.getVitamSession().setRequestId(GUIDFactory.newOperationLogbookGUID(TENANT_ID));
                client.importRulesFile(
                    PropertiesUtils.getResourceAsStream("integration-processing/jeu_donnees_OK_regles_CSV_regles.csv"),
                    "jeu_donnees_OK_regles_CSV_regles.csv");

                VitamThreadUtils.getVitamSession().setRequestId(GUIDFactory.newOperationLogbookGUID(TENANT_ID));
                client.importAgenciesFile(PropertiesUtils.getResourceAsStream("agencies.csv"), "agencies.csv");

                File fileProfiles = PropertiesUtils.getResourceFile("integration-processing/OK_profil.json");
                List<ProfileModel> profileModelList =
                    JsonHandler.getFromFileAsTypeRefence(fileProfiles, new TypeReference<List<ProfileModel>>() {
                    });
                VitamThreadUtils.getVitamSession().setRequestId(GUIDFactory.newOperationLogbookGUID(TENANT_ID));
                RequestResponse improrResponse = client.createProfiles(profileModelList);

                RequestResponseOK<ProfileModel> response =
                    (RequestResponseOK<ProfileModel>) client.findProfiles(new Select().getFinalSelect());
                VitamThreadUtils.getVitamSession().setRequestId(GUIDFactory.newOperationLogbookGUID(TENANT_ID));
                client.importProfileFile(response.getResults().get(0).getIdentifier(),
                    PropertiesUtils.getResourceAsStream("integration-processing/profil_ok.rng"));

                // import contract
                File fileContracts =
                    PropertiesUtils.getResourceFile("integration-processing/referential_contracts_ok.json");
                List<IngestContractModel> IngestContractModelList = JsonHandler
                    .getFromFileAsTypeRefence(fileContracts, new TypeReference<List<IngestContractModel>>() {
                    });

                VitamThreadUtils.getVitamSession().setRequestId(GUIDFactory.newOperationLogbookGUID(TENANT_ID));
                client.importIngestContracts(IngestContractModelList);

                // Import Security Profile
                VitamThreadUtils.getVitamSession().setRequestId(GUIDFactory.newOperationLogbookGUID(TENANT_ID));
                client.importSecurityProfiles(JsonHandler
                    .getFromFileAsTypeRefence(
                        PropertiesUtils.getResourceFile("integration-processing/security_profile_ok.json"),
                        new TypeReference<List<SecurityProfileModel>>() {
                        }));

                // Import Context
                VitamThreadUtils.getVitamSession().setRequestId(GUIDFactory.newOperationLogbookGUID(TENANT_ID));
                client.importContexts(JsonHandler
                    .getFromFileAsTypeRefence(PropertiesUtils.getResourceFile("integration-processing/contexts.json"),
                        new TypeReference<List<ContextModel>>() {
                        }));
            } catch (final Exception e) {
                LOGGER.error(e);
            }
            imported = true;
        }
    }

    private void createLogbookOperation(GUID operationId, GUID objectId)
        throws LogbookClientBadRequestException, LogbookClientAlreadyExistsException, LogbookClientServerException {

        final LogbookOperationsClient logbookClient = LogbookOperationsClientFactory.getInstance().getClient();

        final LogbookOperationParameters initParameters = LogbookParametersFactory.newLogbookOperationParameters(
            operationId, "Process_SIP_unitary", objectId,
            LogbookTypeProcess.INGEST, StatusCode.STARTED,
            operationId != null ? operationId.toString() : "outcomeDetailMessage",
            operationId);
        logbookClient.create(initParameters);
    }

    @RunWithCustomExecutor
    @Test
    public void testPauseProcessWorkflowOnFatalThenRecoverAndResume() throws Exception {
        /**
         * start process, go to first step (pause)
         * stop metadata application
         * check process in pause with status Fatal
         * restart metadata application
         * resume process
         * check process complete with status warning
         */
        testPauseOnFatal(true, false);
    }

    @RunWithCustomExecutor
    @Test
    public void testPauseProcessWorkflowOnFatalThenRecoverThenRestartProcessingAndResume() throws Exception {
        /**
         * start process, go to first step (pause)
         * stop metadata application 
         * check process in pause with status Fatal
         * restart metadata application
         * stop and restart Processing
         * resume process
         * check process complete with status warning
         */
        testPauseOnFatal(true, true);
    }

    @RunWithCustomExecutor
    @Test
    public void testPauseProcessWorkflowOnFatalThenResumeWithoutRecover() throws Exception {
        /**
         * start process, go to first step
         * stop metadata application
         * check process in pause with status Fatal
         * resume process
         * check process still in pause with status Fatal
         */
        testPauseOnFatal(false, false);
    }

    /**
     * test pause on fatal then resume
     *
     * @param restartMDServerAfterFatal                if true MD server will be started after pause on Fatal
     * @param stopAndRestartProcessingServerAfterFatal if true Processing Server wil be stopped and restarted after pause on Fatal
     * @throws Exception
     */
    public void testPauseOnFatal(boolean restartMDServerAfterFatal, boolean stopAndRestartProcessingServerAfterFatal)
        throws Exception {
        try {
            // prepare
            VitamThreadUtils.getVitamSession().setTenantId(TENANT_ID);
            tryImportFile();
            final GUID operationGuid = GUIDFactory.newOperationLogbookGUID(TENANT_ID);
            VitamThreadUtils.getVitamSession().setRequestId(operationGuid);
            final GUID objectGuid = GUIDFactory.newManifestGUID(TENANT_ID);
            final String containerName = objectGuid.getId();
            createLogbookOperation(operationGuid, objectGuid);

            // workspace client dezip SIP in workspace
            final InputStream zipInputStreamSipObject = PropertiesUtils.getResourceAsStream(SIP_FILE_OK_NAME);
            workspaceClient = WorkspaceClientFactory.getInstance().getClient();
            workspaceClient.createContainer(containerName);
            workspaceClient.uncompressObject(containerName, SIP_FOLDER, CommonMediaType.ZIP, zipInputStreamSipObject);

            processingClient = ProcessingManagementClientFactory.getInstance().getClient();
            processingClient.initVitamProcess(Contexts.DEFAULT_WORKFLOW.name(), containerName,
                WORFKLOW_NAME);

            // process execute
            RequestResponse<JsonNode> resp = processingClient.executeOperationProcess(containerName, WORFKLOW_NAME,
                LogbookTypeProcess.INGEST.toString(), ProcessAction.NEXT.getValue());
            assertNotNull(resp);
            assertEquals(Response.Status.ACCEPTED.getStatusCode(), resp.getStatus());

            // check process
            wait(containerName);
            ProcessWorkflow processWorkflow =
                ProcessMonitoringImpl.getInstance().findOneProcessWorkflow(containerName, TENANT_ID);
            assertNotNull(processWorkflow);
            assertEquals(ProcessState.PAUSE, processWorkflow.getState());
            assertEquals(StatusCode.OK, processWorkflow.getStatus());

            // stop MD server while process in pause
            // shutdown metadata, this should generate FATAl status
            metadataApplication.stop();
            waitServer(true, MetaDataClientFactory.getInstance().getClient());

            // resume process
            RequestResponse<ItemStatus> ret =
                processingClient.updateOperationActionProcess(ProcessAction.RESUME.getValue(), containerName);
            assertNotNull(ret);
            assertEquals(Response.Status.ACCEPTED.getStatusCode(), ret.getStatus());

            // check process status
            wait(containerName);
            processWorkflow = ProcessMonitoringImpl.getInstance().findOneProcessWorkflow(containerName, TENANT_ID);
            assertNotNull(processWorkflow);
            assertEquals(ProcessState.PAUSE, processWorkflow.getState());
            assertEquals(StatusCode.FATAL, processWorkflow.getStatus());
            assertEquals(PauseRecover.RECOVER_FROM_API_PAUSE, processWorkflow.getPauseRecover());

            // restart metadata 
            if (restartMDServerAfterFatal) {
                SystemPropertyUtil.set(MetadataMain.PARAMETER_JETTY_SERVER_PORT,
                    Integer.toString(PORT_SERVICE_METADATA));
                metadataApplication = new MetadataMain(CONFIG_METADATA_PATH);
                metadataApplication.start();
                SystemPropertyUtil.clear(MetadataMain.PARAMETER_JETTY_SERVER_PORT);
                waitServer(false, MetaDataClientFactory.getInstance().getClient());
            }

            // stop and restart processing
            if (stopAndRestartProcessingServerAfterFatal) {
                // shutdown processing
                processManagementMain.stop();
                // wait a little bit
                waitServer(true, processingClient);
                // restart processing
                SystemPropertyUtil.set(ProcessManagementMain.PARAMETER_JETTY_SERVER_PORT,
                    Integer.toString(PORT_SERVICE_PROCESSING));
                processManagementMain = new ProcessManagementMain(CONFIG_PROCESSING_PATH);
                processManagementMain.start();
                SystemPropertyUtil.clear(ProcessManagementMain.PARAMETER_JETTY_SERVER_PORT);
                waitServer(false, processingClient);
            }

            // resume process
            ret = processingClient.updateOperationActionProcess(ProcessAction.RESUME.getValue(), containerName);
            assertNotNull(ret);
            assertEquals(Response.Status.ACCEPTED.getStatusCode(), ret.getStatus());

            // check process status
            wait(containerName);
            processWorkflow = ProcessMonitoringImpl.getInstance().findOneProcessWorkflow(containerName, TENANT_ID);
            assertNotNull(processWorkflow);
            // if MD server restarted process should complete with status Warning, otherwise it must still in pause with status Fatal
            if (restartMDServerAfterFatal) {
                assertEquals(restartMDServerAfterFatal ? ProcessState.COMPLETED : ProcessState.PAUSE,
                    processWorkflow.getState());
                assertEquals(restartMDServerAfterFatal ? StatusCode.WARNING : StatusCode.FATAL,
                    processWorkflow.getStatus());

                // check if all steps are OK 
                checkAllSteps(processWorkflow);
            } else {
                assertEquals(ProcessState.PAUSE, processWorkflow.getState());
                assertEquals(StatusCode.FATAL, processWorkflow.getStatus());
            }
        } finally {
            // restart metadata if not already done 
            if (!restartMDServerAfterFatal) {
                SystemPropertyUtil.set(MetadataMain.PARAMETER_JETTY_SERVER_PORT,
                    Integer.toString(PORT_SERVICE_METADATA));
                metadataApplication = new MetadataMain(CONFIG_METADATA_PATH);
                metadataApplication.start();
                SystemPropertyUtil.clear(MetadataMain.PARAMETER_JETTY_SERVER_PORT);
                waitServer(false, MetaDataClientFactory.getInstance().getClient());
            }
        }
    }

    private void checkAllSteps(ProcessWorkflow processWorkflow) {
        int stepIndex = 0;
        for (ProcessStep step : processWorkflow.getSteps()) {
            // check status
            assertTrue(step.getStepStatusCode().equals(StatusCode.OK) ||
                step.getStepStatusCode().equals(StatusCode.WARNING));

            // check processed elements
            Assertions.assertThat(step.getElementProcessed()).isEqualTo(step.getElementToProcess());
            Assertions.assertThat(step.getElementProcessed()).isEqualTo(elementCountPerStep[stepIndex++]);
        }
    }

    private void waitServer(boolean checkStop, MockOrRestClient client) {
        int nbTry = 30;
        while (checkStatus(client, checkStop)) {
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                SysErrLogger.FAKE_LOGGER.ignoreLog(e);
            }
            nbTry--;

            if (nbTry < 0) {
                LOGGER.error("SERVER ALREADY {} {}", checkStop ? "UP" : "DOWN",
                    client.getServiceUrl());
            }
        }
        if (nbTry >= 0) {
            LOGGER.debug(checkStop ? "SERVER DOWN" : "SERVER UP");
        }
    }

    private boolean checkStatus(MockOrRestClient client, boolean checkStop) {
        try {
            client.checkStatus();
            return checkStop;
        } catch (Exception e) {
            LOGGER.error("Server is not active.", e);
            return !checkStop;
        }
    }

}
