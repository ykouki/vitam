/*******************************************************************************
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
 *******************************************************************************/
package fr.gouv.vitam.metadata.core.database.collections;

import fr.gouv.vitam.common.database.collections.VitamCollection;
import fr.gouv.vitam.common.database.server.elasticsearch.ElasticsearchNode;
import fr.gouv.vitam.common.exception.InvalidParseOperationException;
import fr.gouv.vitam.common.exception.VitamApplicationServerException;
import fr.gouv.vitam.common.guid.GUIDFactory;
import fr.gouv.vitam.common.json.JsonHandler;
import fr.gouv.vitam.common.junit.JunitHelper;
import fr.gouv.vitam.common.junit.JunitHelper.ElasticsearchTestConfiguration;
import fr.gouv.vitam.common.logging.VitamLogger;
import fr.gouv.vitam.common.logging.VitamLoggerFactory;
import fr.gouv.vitam.common.thread.RunWithCustomExecutor;
import fr.gouv.vitam.common.thread.RunWithCustomExecutorRule;
import fr.gouv.vitam.common.thread.VitamThreadPoolExecutor;
import fr.gouv.vitam.common.thread.VitamThreadUtils;
import fr.gouv.vitam.metadata.api.exception.MetaDataExecutionException;
import fr.gouv.vitam.metadata.api.exception.MetaDataNotFoundException;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;
import static org.junit.Assume.assumeTrue;

public class ElasticsearchAccessMetadataTest {

    private static final VitamLogger LOGGER = VitamLoggerFactory.getInstance(DbRequestTest.class);

    @Rule
    public RunWithCustomExecutorRule runInThread =
        new RunWithCustomExecutorRule(VitamThreadPoolExecutor.getDefaultExecutor());

    @ClassRule
    public static TemporaryFolder tempFolder = new TemporaryFolder();

    private final static String CLUSTER_NAME = "vitam-cluster";
    private final static String HOST_NAME = "127.0.0.1";
    private static ElasticsearchAccessMetadata esClient;

    private static final int TENANT_ID = 0;
    private static final Integer TENANT_ID_0 = new Integer(0);

    private static final String S1 = "{ \"title\":\"title1\", \"_max\": \"5\", \"_min\": \"2\"}";
    private static final String S1_OG =
        "{ \"Filename\":\"Vitam-Sensibilisation-API-V1.0.odp\", \"_max\": \"5\", \"_min\": \"2\"}";
    private static final String S3 =
        "{\"$roots\":[\"id2\"],\"$query\":[],\"$filter\":{},\"$action\":[{\"$set\":{\"title\":\"Archive2\"}}]}";
    private final int IntTest = 12345;
    private final String groupGUID = GUIDFactory.newObjectGUID(IntTest).toString();
    private final String go = "{\"_id\":\"" + groupGUID +
        "\", \"_qualifiers\" :{\"Physique Master\" : {\"PhysiqueOId\" : \"abceff\"}}, \"title\":\"title1\"}";
    private static ElasticsearchTestConfiguration config = null;

    @BeforeClass
    public static void setUpBeforeClass() throws Exception {
        // ES
        try {
            config = JunitHelper.startElasticsearchForTest(tempFolder, CLUSTER_NAME);
        } catch (final VitamApplicationServerException e1) {
            assumeTrue(false);
        }

        final List<ElasticsearchNode> nodes = new ArrayList<>();
        nodes.add(new ElasticsearchNode(HOST_NAME, config.getTcpPort()));

        esClient = new ElasticsearchAccessMetadata(CLUSTER_NAME, nodes);
    }

    @AfterClass
    public static void tearDownAfterClass() {
        if (config == null) {
            return;
        }
        JunitHelper.stopElasticsearchForTest(config);
        esClient.close();
    }


    @Test
    public void testElasticsearchAccessMetadatas() throws InvalidParseOperationException {
        // add index
        assertEquals(true, esClient.addIndex(MetadataCollections.UNIT, TENANT_ID_0));
        // add unit
        final String id = GUIDFactory.newUnitGUID(TENANT_ID).toString();
        assertEquals(true, esClient.addEntryIndex(MetadataCollections.UNIT, TENANT_ID_0, id, S1));
        // delete unit
        try {
            esClient.deleteEntryIndex(MetadataCollections.UNIT, TENANT_ID_0, VitamCollection.getTypeunique(), id);
        } catch (final MetaDataExecutionException e) {
            fail(e.getMessage());
        } catch (final MetaDataNotFoundException e) {
            fail(e.getMessage());
        }

        try {
            esClient.deleteEntryIndex(MetadataCollections.UNIT, TENANT_ID_0, VitamCollection.getTypeunique(),
                GUIDFactory.newUnitGUID(TENANT_ID).toString());
            fail("Unit not found");

        } catch (final MetaDataExecutionException e) {
        } catch (final MetaDataNotFoundException e) {
        }

        // delete index
        assertEquals(true, esClient.deleteIndex(MetadataCollections.UNIT, TENANT_ID_0));

        @SuppressWarnings("unchecked") final Map<String, String> targetMap =
            (Map<String, String>) (Object) JsonHandler.getMapFromString(S1);
        // add entries
        esClient.addEntryIndexes(MetadataCollections.UNIT, TENANT_ID_0, targetMap);
        esClient.addEntryIndexesBlocking(MetadataCollections.UNIT, TENANT_ID_0, targetMap);

        final Unit unit = new Unit(S1);
        esClient.addBulkEntryIndex(targetMap, TENANT_ID_0, unit);

    }

    @Test
    public void testElasticsearchUpdateAccessMetadatas() throws Exception {

        // add index
        assertEquals(true, esClient.addIndex(MetadataCollections.UNIT, TENANT_ID_0));
        // add unit
        final String id = GUIDFactory.newUnitGUID(TENANT_ID).toString();
        assertEquals(true, esClient.addEntryIndex(MetadataCollections.UNIT, TENANT_ID_0, id, S1));

        // update index
        assertEquals(true, esClient.updateEntryIndex(MetadataCollections.UNIT, TENANT_ID_0, id, S3));

    }

    @Test
    public void testElasticsearchAccessOGMetadatas() throws MetaDataExecutionException, MetaDataNotFoundException {

        esClient.refreshIndex(MetadataCollections.OBJECTGROUP, TENANT_ID_0);
        // add index
        assertEquals(true, esClient.addIndex(MetadataCollections.OBJECTGROUP, TENANT_ID_0));
        // add OG
        final String id = GUIDFactory.newUnitGUID(TENANT_ID).toString();
        assertEquals(true, esClient.addEntryIndex(MetadataCollections.OBJECTGROUP, TENANT_ID_0, id, S1_OG));
        // delete OG

        esClient.deleteEntryIndex(MetadataCollections.OBJECTGROUP, TENANT_ID_0, VitamCollection.getTypeunique(), id);

        // delete index
        assertEquals(true, esClient.deleteIndex(MetadataCollections.OBJECTGROUP, TENANT_ID_0));

    }

    @Test(expected = MetaDataNotFoundException.class)
    public void testElasticSearchDeleteOGAccessMetadatsNotFoundThenThrowException()
        throws MetaDataExecutionException, MetaDataNotFoundException {
        esClient.deleteEntryIndex(MetadataCollections.OBJECTGROUP, TENANT_ID_0, VitamCollection.getTypeunique(),
            GUIDFactory.newObjectGroupGUID(TENANT_ID).toString());
    }

    @Test
    public void testElasticsearchUpdateOGAccessMetadatas() throws Exception {

        // add index
        assertEquals(true, esClient.addIndex(MetadataCollections.OBJECTGROUP, TENANT_ID_0));
        // add OG
        final String id = GUIDFactory.newObjectGroupGUID(TENANT_ID).toString();
        assertEquals(true, esClient.addEntryIndex(MetadataCollections.OBJECTGROUP, TENANT_ID_0, id, S1_OG));

        // update index
        assertEquals(true, esClient.updateEntryIndex(MetadataCollections.OBJECTGROUP, TENANT_ID_0, id, S3));

    }

    @Test
    @RunWithCustomExecutor
    public void testElasticsearchOGAccessMetadatas() throws Exception {
        VitamThreadUtils.getVitamSession().setTenantId(0);
        // add index
        assertEquals(true, esClient.addIndex(MetadataCollections.OBJECTGROUP, TENANT_ID_0));
        // add OG
        final String id = GUIDFactory.newObjectGroupGUID(TENANT_ID).toString();
        assertEquals(true, esClient.addEntryIndex(MetadataCollections.OBJECTGROUP, TENANT_ID_0, id, S1_OG));

        // update index
        assertEquals(true, esClient.updateEntryIndex(MetadataCollections.OBJECTGROUP, TENANT_ID_0, id, S3));

        final MetadataDocument<?> doc = new ObjectGroup(go);
        assertEquals(true, esClient.addEntryIndex(doc, TENANT_ID_0));

    }
}
