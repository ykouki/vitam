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
 */
package fr.gouv.vitam.functional.administration.counter;


import fr.gouv.vitam.functional.administration.common.server.FunctionalAdminCollections;

public enum SequenceType {


    /**
     * Rules Collection
     */
    RULES_SEQUENCE(FunctionalAdminCollections.RULES, "RULE"),

    /**
     * Ingest contract collection
     */
    INGEST_CONTRACT_SEQUENCE(FunctionalAdminCollections.INGEST_CONTRACT, "IC"),

    /**
     * Access contract collection
     */
    ACCESS_CONTRACT_SEQUENCE(FunctionalAdminCollections.ACCESS_CONTRACT, "AC"),


    /**
     * Profile collection
     */
    PROFILE_SEQUENCE(FunctionalAdminCollections.PROFILE, "PR"),

    /**
     * Context collection
     */
    CONTEXT_SEQUENCE(FunctionalAdminCollections.CONTEXT, "CT"),

    /**
     * Security profile collection
     */
    SECURITY_PROFILE_SEQUENCE(FunctionalAdminCollections.SECURITY_PROFILE, "SEC_PROFILE");

    SequenceType(FunctionalAdminCollections collection, String prefix) {
        this.collection = collection;
        this.name = prefix;
    }

    private String name;

    private FunctionalAdminCollections collection;

    /**
     * Getter for   name;
     */
    public String getName() {
        return name;
    }

    /**
     * Getter for   collection;
     */
    public FunctionalAdminCollections getCollection() {
        return collection;
    }
}
