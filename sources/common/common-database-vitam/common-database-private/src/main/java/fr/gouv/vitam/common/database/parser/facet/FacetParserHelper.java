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
package fr.gouv.vitam.common.database.parser.facet;

import com.fasterxml.jackson.databind.JsonNode;

import fr.gouv.vitam.common.database.builder.facet.Facet;
import fr.gouv.vitam.common.database.builder.facet.FacetHelper;
import fr.gouv.vitam.common.database.builder.request.configuration.BuilderToken.FACET;
import fr.gouv.vitam.common.database.builder.request.configuration.BuilderToken.FACETARGS;
import fr.gouv.vitam.common.database.builder.request.exception.InvalidCreateOperationException;
import fr.gouv.vitam.common.database.parser.request.adapter.VarNameAdapter;
import fr.gouv.vitam.common.exception.InvalidParseOperationException;

/**
 * Facet from Parser Helper
 *
 */
public class FacetParserHelper extends FacetHelper {

    /**
     * Construction
     */
    protected FacetParserHelper() {}

    /**
     * Transform facet jsonNode in terms Facet object
     * 
     * @param facet facet node
     * @param adapter adapter
     * @return terms Facet object
     * @throws InvalidCreateOperationException error while creating terms Facet
     * @throws InvalidParseOperationException error in adapater
     */
    public static final Facet terms(final JsonNode facet, VarNameAdapter adapter)
        throws InvalidCreateOperationException, InvalidParseOperationException {
        final String name = facet.get(FACETARGS.NAME.exactToken()).asText();
        JsonNode terms = facet.get(FACET.TERMS.exactToken());

        String field = terms.get(FACETARGS.FIELD.exactToken()).asText();
        String translatedField = adapter.getVariableName(field);
        if (translatedField == null) {
            translatedField = field;
        }

        if (terms.has(FACETARGS.SIZE.exactToken())) {
            Integer size = terms.get(FACETARGS.SIZE.exactToken()).asInt();
            return FacetHelper.terms(name, translatedField, size);
        } else {
            return FacetHelper.terms(name, translatedField);
        }

    }
}