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
package fr.gouv.vitam.common.client;

import java.io.IOException;

import javax.ws.rs.client.ClientRequestContext;
import javax.ws.rs.client.ClientRequestFilter;
import javax.ws.rs.client.ClientResponseContext;
import javax.ws.rs.client.ClientResponseFilter;
import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ContainerRequestFilter;
import javax.ws.rs.container.ContainerResponseContext;
import javax.ws.rs.container.ContainerResponseFilter;

import fr.gouv.vitam.common.server.HeaderIdHelper;

/**
 * Manage the headers from the client-side perspective.
 */
public class HeaderIdClientFilter implements ClientRequestFilter, ClientResponseFilter {

    /**
     * Extracts the ids from the headers to save it in the VitamSession
     *
     * @see ContainerRequestFilter#filter(ContainerRequestContext)
     * @param requestContext {@link ContainerRequestFilter#filter(ContainerRequestContext)}
     * @throws IOException {@see ContainerRequestFilter#filter(ContainerRequestContext)}
     */
    @Override
    public void filter(ClientRequestContext requestContext) throws IOException {
        HeaderIdHelper.putVitamIdFromSessionInHeader(requestContext.getHeaders(),
            HeaderIdHelper.Context.REQUEST, 0);
    }

    /**
     * Retrieves the vitam ids from the VitamSession and add a X-TENANT-ID header to the request
     *
     * @see ContainerResponseFilter#filter(ContainerRequestContext, ContainerResponseContext)
     * @param requestContext Cf.
     *        {@link ContainerResponseFilter#filter(ContainerRequestContext, ContainerResponseContext) }
     * @param responseContext Cf.
     *        {@link ContainerResponseFilter#filter(ContainerRequestContext, ContainerResponseContext) }
     * @throws IOException Cf.
     *         {@link ContainerResponseFilter#filter(ContainerRequestContext, ContainerResponseContext) }
     */
    @Override
    public void filter(ClientRequestContext requestContext, ClientResponseContext responseContext)
        throws IOException {
        HeaderIdHelper.putVitamIdFromHeaderInSession(responseContext.getHeaders(),
            HeaderIdHelper.Context.RESPONSE);
    }
}
