/*******************************************************************************
 * This file is part of Vitam Project.
 * <p>
 * Copyright Vitam (2012, 2015)
 * <p>
 * This software is governed by the CeCILL 2.1 license under French law and abiding by the rules of distribution of free
 * software. You can use, modify and/ or redistribute the software under the terms of the CeCILL license as circulated
 * by CEA, CNRS and INRIA at the following URL "http://www.cecill.info".
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
 * The fact that you are presently reading this means that you have had knowledge of the CeCILL license and that you
 * accept its terms.
 *******************************************************************************/
package fr.gouv.vitam.access.rest;

import com.jayway.restassured.RestAssured;
import com.jayway.restassured.http.ContentType;
import fr.gouv.vitam.common.GlobalDataRest;
import fr.gouv.vitam.common.PropertiesUtils;
import fr.gouv.vitam.common.exception.VitamApplicationServerException;
import fr.gouv.vitam.common.junit.JunitHelper;
import fr.gouv.vitam.common.logging.VitamLogger;
import fr.gouv.vitam.common.logging.VitamLoggerFactory;
import fr.gouv.vitam.common.server.VitamServer;
import fr.gouv.vitam.common.database.parser.request.GlobalDatasParser;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;

import javax.ws.rs.core.Response.Status;
import java.io.FileNotFoundException;

import static com.jayway.restassured.RestAssured.*;


public class AccessResourceImplTest {

    // URI
    private static final String ACCESS_CONF = "access-test.conf";
    private static final String ACCESS_RESOURCE_URI = "access/v1";
    private static final String ACCESS_STATUS_URI = "/status";
    private static final String ACCESS_UNITS_URI = "/units";
    private static final String ACCESS_UNITS_ID_URI = "/units/xyz";
    private static final String ACCESS_UPDATE_UNITS_ID_URI = "/units/xyz";
    // private static final int ASSURD_SERVER_PORT = 8187;

    private static VitamServer vitamServer;

    // QUERIES AND DSL
    // TODO
    // Create a "GET" query inspired by DSL, exemple from tech design story 76
    private static final String QUERY_TEST = "{ $query : [ { $eq : { 'title' : 'test' } } ], " +
        " $filter : { $orderby : { '#id' } }," +
        " $projection : {$fields : {#id : 1, title:2, transacdate:1}}" +
        " }";

    private static final String QUERY_SIMPLE_TEST = "{ $query : [ { $eq : { 'title' : 'test' } } ] }";

    private static final String DATA =
        "{ \"_id\": \"aeaqaaaaaeaaaaakaarp4akuuf2ldmyaaaaq\", " + "\"data\": \"data1\" }";

    private static final String DATA2 =
        "{ \"_id\": \"aeaqaaaaaeaaaaakaarp4akuuf2ldmyaaaab\"," + "\"data\": \"data2\" }";

    private static final String ID = "identifier4";
    // LOGGER
    private static final VitamLogger LOGGER = VitamLoggerFactory.getInstance(AccessResourceImplTest.class);

    private static final String BODY_TEST = "{$query: {$eq: {\"data\" : \"data2\" }}, $projection: {}, $filter: {}}";

    private static final String ID_UNIT = "identifier5";
    private static JunitHelper junitHelper;
    private static int port;


    @BeforeClass
    public static void setUpBeforeClass() throws Exception {
        junitHelper = new JunitHelper();
        port = junitHelper.findAvailablePort();
        try {
            AccessApplication.startApplication(new String[] {
                PropertiesUtils.getResourcesFile(ACCESS_CONF).getAbsolutePath(), Integer.toString(port)} );

            RestAssured.port = port;
            RestAssured.basePath = ACCESS_RESOURCE_URI;

            LOGGER.debug("Beginning tests");

        } catch (FileNotFoundException e) {
            LOGGER.error(e);
            throw new IllegalStateException(
                "Cannot start the Access Application Server", e);
        }
    }

    @AfterClass
    public static void tearDownAfterClass() throws Exception {
        LOGGER.debug("Ending tests");
        try {
            AccessApplication.stop();
            junitHelper.releasePort(port);
        } catch (final VitamApplicationServerException e) {
            LOGGER.error(e);
        }
    }

    // Status
    /**
     * Tests the state of the access service API by get
     *
     * @throws Exception
     */
    @Test
    public void givenStartedServer_WhenGetStatus_ThenReturnStatusOk() throws Exception {
        get(ACCESS_STATUS_URI).then().statusCode(Status.OK.getStatusCode());
    }

    // Error cases
    /**
     * Test if the request is inconsistent
     *
     * @throws Exception
     */
    @Ignore("To implement")
    public void givenStartedServer_WhenRequestNotCorrect_ThenReturnError() throws Exception {

    }

    /**
     * Checks if the send parameter doesn't have Json format
     *
     * @throws Exception
     */
    @Test
    public void givenStartedServer_WhenRequestNotJson_ThenReturnError_UnsupportedMediaType() throws Exception {
        given()
            .contentType(ContentType.XML).header(GlobalDataRest.X_HTTP_METHOD_OVERRIDE, "GET")
            .body(buildDSLWithOptions(QUERY_TEST, DATA2))
            .when().post(ACCESS_UNITS_URI).then().statusCode(Status.UNSUPPORTED_MEDIA_TYPE.getStatusCode());
    }

    /**
     * Checks if the send parameter is a bad request
     *
     * @throws Exception
     */
    @Test
    public void givenStartedServer_WhenBadRequest_ThenReturnError_BadRequest() throws Exception {
        given()
            .contentType(ContentType.JSON).header(GlobalDataRest.X_HTTP_METHOD_OVERRIDE, "GET")
            .body(buildDSLWithOptions(QUERY_TEST, DATA2)).
        when()
            .post(ACCESS_UNITS_URI).
        then()
            .statusCode(Status.BAD_REQUEST.getStatusCode());
    }

    @Test
    public void givenStartedServer_When_Empty_Http_Get_ThenReturnError_METHOD_NOT_ALLOWED() throws Exception {
        given()
            .contentType(ContentType.JSON).header(GlobalDataRest.X_HTTP_METHOD_OVERRIDE, "ABC")
            .body(buildDSLWithOptions(QUERY_TEST, DATA2))
            .when().post(ACCESS_UNITS_URI).then().statusCode(Status.METHOD_NOT_ALLOWED.getStatusCode());
    }

    /**
     * 
     * @param data
     * @return query DSL with Options
     */
    private static final String buildDSLWithOptions(String query, String data) {
        return "{ $roots : [ '' ], $query : [ " + query + " ], $data : " + data + " }";
    }

    /**
     * 
     * @param data
     * @return query DSL with id as Roots
     */

    private static final String buildDSLWithRoots(String data) {
        return "{ $roots : [ " + data + " ], $query : [ '' ], $data : " + data + " }";
    }

    /**
     * Checks if the send parameter doesn't have Json format
     * 
     * @throws Exception
     */
    @Test
    public void givenStartedServer_WhenRequestNotJson_ThenReturnError_SelectById_UnsupportedMediaType()
        throws Exception {
        given()
            .contentType(ContentType.XML).header(GlobalDataRest.X_HTTP_METHOD_OVERRIDE, "GET")
            .body(buildDSLWithRoots(ID))
            .when().post(ACCESS_UNITS_ID_URI).then().statusCode(Status.UNSUPPORTED_MEDIA_TYPE.getStatusCode());
    }

    /**
     * Checks if the send parameter is a bad request
     * 
     * @throws Exception
     */
    @Test
    public void givenStartedServer_WhenBadRequest_ThenReturnError_SelectById_BadRequest() throws Exception {
        given()
            .contentType(ContentType.JSON).header(GlobalDataRest.X_HTTP_METHOD_OVERRIDE, "GET")
            .body(buildDSLWithRoots(ID))
            .when().post(ACCESS_UNITS_ID_URI).then().statusCode(Status.BAD_REQUEST.getStatusCode());
    }


    @Test
    public void givenStartedServer_When_Empty_Http_Get_ThenReturnError_SelectById_METHOD_NOT_ALLOWED()
        throws Exception {
        given()
            .contentType(ContentType.JSON).header(GlobalDataRest.X_HTTP_METHOD_OVERRIDE, "ABC")
            .body(buildDSLWithRoots(ID))
            .when().post(ACCESS_UNITS_ID_URI).then().statusCode(Status.METHOD_NOT_ALLOWED.getStatusCode());
    }

    @Test
    public void given_SelectUnitById_WhenStringTooLong_Then_Throw_MethodNotAllowed() throws Exception {
        GlobalDatasParser.limitRequest = 1000;
        given()
            .contentType(ContentType.JSON).header(GlobalDataRest.X_HTTP_METHOD_OVERRIDE, "ABC")
            .body(buildDSLWithOptions(createLongString(1001), DATA2))
            .when().post(ACCESS_UNITS_ID_URI).then().statusCode(Status.METHOD_NOT_ALLOWED.getStatusCode());

    }

    @Test
    public void given_updateUnitById_WhenStringTooLong_Then_Throw_BadRequest() throws Exception {
        GlobalDatasParser.limitRequest = 1000;
        given()
                .contentType(ContentType.JSON).body(buildDSLWithOptions(createLongString(1001), DATA2))
                .when().put(ACCESS_UPDATE_UNITS_ID_URI).then().statusCode(Status.BAD_REQUEST.getStatusCode());
    }



    private static String createLongString(int size) throws Exception {
        final StringBuilder sb = new StringBuilder(size);
        for (int i = 0; i < size; i++) {
            sb.append('a');
        }
        return sb.toString();
    }

    @Ignore
    @Test
    public void given_units_insert_when_searchUnitsByID_thenReturn_Found() throws Exception {
        with()
            .contentType(ContentType.JSON)
            .header(GlobalDataRest.X_HTTP_METHOD_OVERRIDE, "GET")
            .body(buildDSLWithOptions("", DATA2)).when()
            .post("/units").then()
            .statusCode(Status.CREATED.getStatusCode());

        given()
            .contentType(ContentType.JSON)
            .header(GlobalDataRest.X_HTTP_METHOD_OVERRIDE, "GET")
            .body(BODY_TEST).when()
            .post("/units/" + ID_UNIT).then()
            .statusCode(Status.FOUND.getStatusCode());

    }


    @Test
    public void given_emptyQuery_when_SelectByID_thenReturn_Bad_Request() {

        given()
            .contentType(ContentType.JSON)
            .header(GlobalDataRest.X_HTTP_METHOD_OVERRIDE, "GET")
            .body("")
            .when()
            .post("/units/" + ID_UNIT)
            .then()
            .statusCode(Status.BAD_REQUEST.getStatusCode());
    }

    @Test
    public void given_emptyQuery_when_UpdateByID_thenReturn_Bad_Request() {

        given()
                .contentType(ContentType.JSON)
                .body("")
                .when()
                .put("/units/" + ID_UNIT)
                .then()
                .statusCode(Status.BAD_REQUEST.getStatusCode());
    }

    @Test
    public void given_queryThatThrowException_when_updateByID_thenThrowAccessException() {

        given()
                .contentType(ContentType.JSON)
                .body(buildDSLWithOptions(QUERY_SIMPLE_TEST, DATA))
                .when()
                .put("/units/" + ID)
                .then()
                .statusCode(Status.INTERNAL_SERVER_ERROR.getStatusCode());
    }

    @Test
    public void given_bad_header_when_SelectByID_thenReturn_Not_allowed() {

        given()
                .contentType(ContentType.JSON)
                .header(GlobalDataRest.X_HTTP_METHOD_OVERRIDE, "ABC")
                .body(BODY_TEST)
                .when()
                .post("/units/" + ID_UNIT)
                .then()
                .statusCode(Status.METHOD_NOT_ALLOWED.getStatusCode());
    }

    @Test
    public void given_pathWithId_when_get_SelectByID_thenReturn_MethodNotAllowed() {

        given()
            .contentType(ContentType.JSON)
            .header(GlobalDataRest.X_HTTP_METHOD_OVERRIDE, "GET")
            .body(BODY_TEST)
            .when()
            .post("/units/" + ID_UNIT)
            .then()
            .statusCode(Status.METHOD_NOT_ALLOWED.getStatusCode());
    }

    @Ignore
    @Test
    public void given_bad_header_when_updateByID_thenReturn_Not_allowed() {

        given()
                .contentType(ContentType.JSON)
                .body(BODY_TEST)
                .when()
                .put("/units/" + ID_UNIT)
                .then()
                .statusCode(Status.METHOD_NOT_ALLOWED.getStatusCode());
    }

    @Ignore
    @Test
    public void shouldReturnInternalServerError() throws Exception {
        int limitRequest = GlobalDatasParser.limitRequest;
        GlobalDatasParser.limitRequest = 99;
        given()
            .contentType(ContentType.JSON)
            .header(GlobalDataRest.X_HTTP_METHOD_OVERRIDE, "GET")
            .body(buildDSLWithOptions("", createJsonStringWithDepth(101))).when()
            .post("/units/" + ID_UNIT).then()
            .statusCode(Status.INTERNAL_SERVER_ERROR.getStatusCode());
        GlobalDatasParser.limitRequest = limitRequest;
    }

    private static String createJsonStringWithDepth(int depth) {
        final StringBuilder obj = new StringBuilder();
        if (depth == 0) {
            return " \"b\" ";
        }
        obj.append("{ \"a\": ").append(createJsonStringWithDepth(depth - 1)).append("}");
        return obj.toString();
    }
}

