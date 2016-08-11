package fr.gouv.vitam.ingest.external.rest;

import static org.junit.Assert.fail;

import java.io.File;
import java.io.FileNotFoundException;

import org.junit.Test;

import fr.gouv.vitam.common.PropertiesUtils;
import fr.gouv.vitam.common.exception.VitamApplicationServerException;
import fr.gouv.vitam.common.server.BasicVitamServer;

public class IngestExternalApplcationTest {

    private static final String SHOULD_NOT_RAIZED_AN_EXCEPTION = "Should not raized an exception";

    private static final String INGEST_EXTERNEL_CONF = "ingest-external-test.conf";
    private static int serverPort;
    private static File config;

    @Test
    public final void testFictiveLaunch() throws FileNotFoundException {
        config = PropertiesUtils.findFile(INGEST_EXTERNEL_CONF);
        try {
            ((BasicVitamServer) IngestExternalApplication.startApplication(new String[] {
                config.getAbsolutePath(), "8082"
            })).stop();
        } catch (final IllegalStateException e) {
            fail(SHOULD_NOT_RAIZED_AN_EXCEPTION);
        } catch (final VitamApplicationServerException e) {
            fail(SHOULD_NOT_RAIZED_AN_EXCEPTION);
        }
        try {
            ((BasicVitamServer) IngestExternalApplication.startApplication(new String[] {
                config.getAbsolutePath(), "-1"
            })).stop();
        } catch (final IllegalStateException e) {
            fail(SHOULD_NOT_RAIZED_AN_EXCEPTION);
        } catch (final VitamApplicationServerException e) {
            fail(SHOULD_NOT_RAIZED_AN_EXCEPTION);
        }
        try {
            ((BasicVitamServer) IngestExternalApplication.startApplication(new String[] {
                config.getAbsolutePath(), "-1xx"
            })).stop();
        } catch (final IllegalStateException e) {
            fail(SHOULD_NOT_RAIZED_AN_EXCEPTION);
        } catch (final VitamApplicationServerException e) {
            fail(SHOULD_NOT_RAIZED_AN_EXCEPTION);
        }
        try {
            ((BasicVitamServer) IngestExternalApplication.startApplication(new String[] {
                config.getAbsolutePath(), Integer.toString(serverPort)
            })).stop();
        } catch (final IllegalStateException e) {
            fail(SHOULD_NOT_RAIZED_AN_EXCEPTION);
        } catch (final VitamApplicationServerException e) {
            fail(SHOULD_NOT_RAIZED_AN_EXCEPTION);
        }
        try {
            ((BasicVitamServer) IngestExternalApplication.startApplication(new String[0])).stop();
        } catch (final IllegalStateException e) {
            fail(SHOULD_NOT_RAIZED_AN_EXCEPTION);
        } catch (final VitamApplicationServerException e) {
            fail(SHOULD_NOT_RAIZED_AN_EXCEPTION);
        }
    }

    @Test
    public final void givenConfigFileNotFoundWhenStartApplicationThenStartOnDefaultPort() throws FileNotFoundException {
        try {
            ((BasicVitamServer) IngestExternalApplication.startApplication(new String[] {
                "src/test/resources/notFound.conf"})).stop();
        } catch (final IllegalStateException e) {
            fail(SHOULD_NOT_RAIZED_AN_EXCEPTION);
        } catch (final VitamApplicationServerException e) {
            fail(SHOULD_NOT_RAIZED_AN_EXCEPTION);
        }
    }

    @Test(expected = IllegalStateException.class)
    public final void givenInCorrrectConfigFileWhenStartApplicationThenStartOnDefaultPort() {
        try {
            ((BasicVitamServer) IngestExternalApplication.startApplication(new String[] {
                "src/test/resources/ingest-external-err1.conf"})).stop();
        } catch (VitamApplicationServerException e) {
            fail("Exception");
        }
    }

    @Test
    public final void givenCorrrectConfigFileWhenStartApplicationThenStartOnDefaultPort() throws FileNotFoundException {
        try {
            ((BasicVitamServer) IngestExternalApplication.startApplication(new String[] {
                "src/test/resources/ingest-external-test.conf"})).stop();
        } catch (final IllegalStateException e) {
            fail(SHOULD_NOT_RAIZED_AN_EXCEPTION);
        } catch (final VitamApplicationServerException e) {
            fail(SHOULD_NOT_RAIZED_AN_EXCEPTION);
        }
    }

}
