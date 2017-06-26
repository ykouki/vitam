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
/**
 *
 */
package fr.gouv.vitam.metadata.core.database.collections;

import static com.mongodb.client.model.Accumulators.addToSet;
import static com.mongodb.client.model.Aggregates.group;
import static com.mongodb.client.model.Aggregates.match;
import static com.mongodb.client.model.Filters.and;
import static com.mongodb.client.model.Filters.eq;
import static com.mongodb.client.model.Filters.gte;
import static com.mongodb.client.model.Filters.in;
import static com.mongodb.client.model.Filters.lte;
import static com.mongodb.client.model.Updates.combine;
import static fr.gouv.vitam.metadata.core.database.collections.MetadataDocument.ID;

import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.bson.Document;
import org.bson.conversions.Bson;
import org.elasticsearch.index.query.QueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.search.sort.SortBuilder;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.IntNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.github.fge.jsonschema.core.exceptions.ProcessingException;
import com.mongodb.BasicDBObject;
import com.mongodb.MongoException;
import com.mongodb.MongoWriteException;
import com.mongodb.client.AggregateIterable;
import com.mongodb.client.FindIterable;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoCursor;
import com.mongodb.client.model.UpdateOptions;
import com.mongodb.client.model.Updates;
import com.mongodb.client.result.DeleteResult;
import com.mongodb.client.result.UpdateResult;

import fr.gouv.vitam.common.database.builder.query.Query;
import fr.gouv.vitam.common.database.builder.request.configuration.BuilderToken;
import fr.gouv.vitam.common.database.builder.request.configuration.BuilderToken.FILTERARGS;
import fr.gouv.vitam.common.database.builder.request.configuration.BuilderToken.QUERY;
import fr.gouv.vitam.common.database.builder.request.multiple.DeleteMultiQuery;
import fr.gouv.vitam.common.database.builder.request.multiple.InsertMultiQuery;
import fr.gouv.vitam.common.database.builder.request.multiple.RequestMultiple;
import fr.gouv.vitam.common.database.builder.request.multiple.UpdateMultiQuery;
import fr.gouv.vitam.common.database.parser.query.PathQuery;
import fr.gouv.vitam.common.database.parser.query.helper.QueryDepthHelper;
import fr.gouv.vitam.common.database.parser.request.GlobalDatasParser;
import fr.gouv.vitam.common.database.parser.request.multiple.RequestParserMultiple;
import fr.gouv.vitam.common.database.server.MongoDbInMemory;
import fr.gouv.vitam.common.database.server.mongodb.VitamDocument;
import fr.gouv.vitam.common.database.translators.RequestToAbstract;
import fr.gouv.vitam.common.database.translators.elasticsearch.QueryToElasticsearch;
import fr.gouv.vitam.common.database.translators.mongodb.DeleteToMongodb;
import fr.gouv.vitam.common.database.translators.mongodb.InsertToMongodb;
import fr.gouv.vitam.common.database.translators.mongodb.MongoDbHelper;
import fr.gouv.vitam.common.database.translators.mongodb.QueryToMongodb;
import fr.gouv.vitam.common.database.translators.mongodb.RequestToMongodb;
import fr.gouv.vitam.common.database.translators.mongodb.SelectToMongodb;
import fr.gouv.vitam.common.database.translators.mongodb.UpdateToMongodb;
import fr.gouv.vitam.common.exception.InvalidParseOperationException;
import fr.gouv.vitam.common.json.JsonHandler;
import fr.gouv.vitam.common.json.SchemaValidationStatus;
import fr.gouv.vitam.common.json.SchemaValidationStatus.SchemaValidationStatusEnum;
import fr.gouv.vitam.common.json.SchemaValidationUtils;
import fr.gouv.vitam.common.logging.VitamLogger;
import fr.gouv.vitam.common.logging.VitamLoggerFactory;
import fr.gouv.vitam.common.parameter.ParameterHelper;
import fr.gouv.vitam.common.thread.VitamThreadUtils;
import fr.gouv.vitam.metadata.api.exception.MetaDataAlreadyExistException;
import fr.gouv.vitam.metadata.api.exception.MetaDataExecutionException;
import fr.gouv.vitam.metadata.api.exception.MetaDataNotFoundException;
import fr.gouv.vitam.metadata.core.database.configuration.GlobalDatasDb;

/**
 * DB Request using MongoDB only
 */
public class DbRequest {
    private static final String QUERY2 = "query: ";

    private static final String WHERE_PREVIOUS_RESULT_WAS = "where_previous_result_was: ";

    private static final String FROM2 = "from: ";

    private static final String NO_RESULT_AT_RANK2 = "no_result_at_rank: ";

    private static final String NO_RESULT_TRUE = "no_result: true";

    private static final String WHERE_PREVIOUS_IS = " \n\twhere previous is ";

    private static final String FROM = " from ";

    private static final String NO_RESULT_AT_RANK = "No result at rank: ";

    private static final VitamLogger LOGGER = VitamLoggerFactory.getInstance(DbRequest.class);

    /**
     * Constructor
     */
    public DbRequest() {}

    /**
     * The request should be already analyzed.
     *
     * @param requestParser the RequestParserMultiple to execute
     * @param defaultStartSet the set of id from which the request should start, whatever the roots set
     * @return the Result
     * @throws MetaDataExecutionException when select/insert/update/delete on metadata collection exception occurred
     * @throws InvalidParseOperationException when json data exception occurred
     * @throws MetaDataAlreadyExistException when insert metadata exception
     * @throws MetaDataNotFoundException when metadata not found exception
     */
    public Result execRequest(final RequestParserMultiple requestParser, final Result defaultStartSet)
        throws InstantiationException, IllegalAccessException, MetaDataExecutionException,
        InvalidParseOperationException, MetaDataAlreadyExistException, MetaDataNotFoundException {
        final RequestMultiple request = requestParser.getRequest();
        final RequestToAbstract requestToMongodb = RequestToMongodb.getRequestToMongoDb(requestParser);
        final int maxQuery = request.getNbQueries();
        Result roots;
        if (requestParser.model() == FILTERARGS.UNITS) {
            roots = checkUnitStartupRoots(requestParser, defaultStartSet);
        } else {
            // OBJECTGROUPS:
            LOGGER.info(String.format("OBJECTGROUPS DbRequest: %s", requestParser.toString()));
            roots = checkObjectGroupStartupRoots(requestParser, defaultStartSet);
        }
        Result result = roots;
        int rank = 0;
        // if roots is empty, check if first query gives a non empty roots (empty query allowed for insert)
        if (result.getCurrentIds().isEmpty() && maxQuery > 0) {
            final Result newResult = executeQuery(requestToMongodb, rank, result);
            if (newResult != null && !newResult.getCurrentIds().isEmpty() && !newResult.isError()) {
                result = newResult;
            } else {
                LOGGER.error(
                    NO_RESULT_AT_RANK + rank + FROM + requestParser + WHERE_PREVIOUS_IS + result);
                // XXX TODO P1 should be adapted to have a correct error feedback
                result = new ResultError(requestParser.model())
                    .addError(newResult != null ? newResult.getCurrentIds().toString() : NO_RESULT_TRUE)
                    .addError(NO_RESULT_AT_RANK2 + rank).addError(FROM2 + requestParser)
                    .addError(WHERE_PREVIOUS_RESULT_WAS + result);

                return result;
            }
            LOGGER.debug("Query: {}\n\tResult: {}", requestParser, result);
            rank++;
        }
        // Stops if no result (empty)
        for (; !result.getCurrentIds().isEmpty() && rank < maxQuery; rank++) {
            final Result newResult = executeQuery(requestToMongodb, rank, result);
            if (newResult == null) {
                LOGGER.error(
                    NO_RESULT_AT_RANK + rank + FROM + requestParser + WHERE_PREVIOUS_IS + result);
                // XXX TODO P1 should be adapted to have a correct error feedback
                result = new ResultError(result.type)
                    .addError(result.getCurrentIds().toString())
                    .addError(NO_RESULT_AT_RANK2 + rank).addError(FROM2 + requestParser)
                    .addError(WHERE_PREVIOUS_RESULT_WAS + result);
                return result;
            }
            if (!newResult.getCurrentIds().isEmpty() && !newResult.isError()) {
                result = newResult;
            } else {
                LOGGER.error(
                    NO_RESULT_AT_RANK + rank + FROM + requestParser + WHERE_PREVIOUS_IS + result);
                // XXX TODO P1 should be adapted to have a correct error feedback
                result = new ResultError(newResult.type)
                    .addError(newResult != null ? newResult.getCurrentIds().toString() : NO_RESULT_TRUE)
                    .addError(NO_RESULT_AT_RANK2 + rank).addError(FROM2 + requestParser)
                    .addError(WHERE_PREVIOUS_RESULT_WAS + result);
                return result;
            }
            LOGGER.debug("Query: {}\n\tResult: {}", requestParser, result);
        }
        // Result contains the selection on which to act
        // Insert allow to have no result
        if (request instanceof InsertMultiQuery) {
            final Result newResult = lastInsertFilterProjection((InsertToMongodb) requestToMongodb, result);
            if (newResult != null) {
                result = newResult;
            }
            if (GlobalDatasDb.PRINT_REQUEST) {
                LOGGER.debug("Results: " + result);
            }
            return result;
        }
        // others do not allow empty result
        if (result.getCurrentIds().isEmpty()) {
            LOGGER.error(NO_RESULT_AT_RANK + rank + FROM + requestParser + WHERE_PREVIOUS_IS + result);
            // XXX TODO P1 should be adapted to have a correct error feedback
            result = new ResultError(result.type)
                .addError(result != null ? result.getCurrentIds().toString() : NO_RESULT_TRUE)
                .addError(NO_RESULT_AT_RANK2 + rank).addError(FROM2 + requestParser)
                .addError(WHERE_PREVIOUS_RESULT_WAS + result);
            return result;
        }
        if (request instanceof UpdateMultiQuery) {
            final Result newResult =
                lastUpdateFilterProjection((UpdateToMongodb) requestToMongodb, result, requestParser);
            if (newResult != null) {
                result = newResult;
            }
        } else if (request instanceof DeleteMultiQuery) {
            final Result newResult = lastDeleteFilterProjection((DeleteToMongodb) requestToMongodb, result);
            if (newResult != null) {
                result = newResult;
            }
        } else {
            // Select part
            final Result newResult = lastSelectFilterProjection((SelectToMongodb) requestToMongodb, result);
            if (newResult != null) {
                result = newResult;
            }
        }
        if (GlobalDatasDb.PRINT_REQUEST) {
            LOGGER.debug("Results: " + result);
        }
        return result;
    }

    /**
     * Check Unit at startup against Roots
     *
     * @param request
     * @param defaultStartSet
     * @return the valid root ids
     * @throws InvalidParseOperationException
     */
    protected Result checkUnitStartupRoots(final RequestParserMultiple request, final Result defaultStartSet)
        throws InvalidParseOperationException {
        final Set<String> roots = request.getRequest().getRoots();
        final Set<String> newRoots = checkUnitAgainstRoots(roots, defaultStartSet);
        if (newRoots.isEmpty()) {
            return MongoDbMetadataHelper.createOneResult(FILTERARGS.UNITS);
        }
        if (!newRoots.containsAll(roots)) {
            LOGGER.debug("Not all roots are preserved");
        }
        return MongoDbMetadataHelper.createOneResult(FILTERARGS.UNITS, newRoots);
    }

    /**
     * Check ObjectGroup at startup against Roots
     *
     * @param request
     * @param defaultStartSet
     * @return the valid root ids
     * @throws InvalidParseOperationException
     */
    protected Result checkObjectGroupStartupRoots(final RequestParserMultiple request, final Result defaultStartSet)
        throws InvalidParseOperationException {
        // TODO P1 add unit tests
        final Set<String> roots = request.getRequest().getRoots();
        if (defaultStartSet == null || defaultStartSet.getCurrentIds().isEmpty()) {
            // no limitation: using roots
            return MongoDbMetadataHelper.createOneResult(FILTERARGS.OBJECTGROUPS, roots);
        }
        if (roots.isEmpty()) {
            return MongoDbMetadataHelper.createOneResult(FILTERARGS.OBJECTGROUPS);
        }
        @SuppressWarnings("unchecked")
        final FindIterable<ObjectGroup> iterable =
            (FindIterable<ObjectGroup>) MongoDbMetadataHelper.select(MetadataCollections.C_OBJECTGROUP,
                MongoDbMetadataHelper.queryForAncestorsOrSame(roots, defaultStartSet.getCurrentIds()),
                ObjectGroup.OBJECTGROUP_VITAM_PROJECTION);
        final Set<String> newRoots = new HashSet<>();
        try (final MongoCursor<ObjectGroup> cursor = iterable.iterator()) {
            while (cursor.hasNext()) {
                final ObjectGroup og = cursor.next();
                newRoots.add(og.getId());
            }
        }
        if (newRoots.isEmpty()) {
            return MongoDbMetadataHelper.createOneResult(FILTERARGS.OBJECTGROUPS);
        }
        if (!newRoots.containsAll(roots)) {
            LOGGER.debug("Not all roots are preserved");
        }
        return MongoDbMetadataHelper.createOneResult(FILTERARGS.OBJECTGROUPS, newRoots);
    }

    /**
     * Check Unit parents against Roots
     *
     * @param current set of result id
     * @param defaultStartSet
     * @return the valid root ids set
     * @throws InvalidParseOperationException
     */
    protected Set<String> checkUnitAgainstRoots(final Set<String> current, final Result defaultStartSet)
        throws InvalidParseOperationException {
        // roots
        if (defaultStartSet == null || defaultStartSet.getCurrentIds().isEmpty()) {
            // no limitation: using roots
            return current;
        }
        // TODO P1 add unit tests
        @SuppressWarnings("unchecked")
        final FindIterable<Unit> iterable =
            (FindIterable<Unit>) MongoDbMetadataHelper.select(MetadataCollections.C_UNIT,
                MongoDbMetadataHelper.queryForAncestorsOrSame(current, defaultStartSet.getCurrentIds()),
                MongoDbMetadataHelper.ID_PROJECTION);
        final Set<String> newRoots = new HashSet<>();
        try (final MongoCursor<Unit> cursor = iterable.iterator()) {
            while (cursor.hasNext()) {
                final Unit unit = cursor.next();
                newRoots.add(unit.getId());
            }
        }
        return newRoots;
    }

    /**
     * Execute one request
     *
     * @param requestToMongodb
     * @param rank current rank query
     * @param previous previous Result from previous level (except in level == 0 where it is the subset of valid roots)
     * @return the new Result from this request
     * @throws MetaDataExecutionException
     * @throws InvalidParseOperationException
     */
    protected Result executeQuery(final RequestToAbstract requestToMongodb, final int rank, final Result previous)
        throws MetaDataExecutionException, InvalidParseOperationException {
        final Query realQuery = requestToMongodb.getNthQuery(rank);
        final boolean isLastQuery = (requestToMongodb.getNbQueries() == rank + 1);
        Bson orderBy = null;
        Integer tenantId = ParameterHelper.getTenantParameter();
        if (requestToMongodb instanceof SelectToMongodb && isLastQuery) {
            orderBy = ((SelectToMongodb) requestToMongodb).getFinalOrderBy();
        }

        if (GlobalDatasDb.PRINT_REQUEST) {
            LOGGER.debug("Rank: " + rank + "\n\tPrevious: " + previous + "\n\tRequest: " + realQuery.getCurrentQuery());
        }
        final QUERY type = realQuery.getQUERY();
        final FILTERARGS collectionType = requestToMongodb.model();
        if (type == QUERY.PATH) {
            // Check if path is compatible with previous
            if (previous.getCurrentIds().isEmpty()) {
                previous.clear();
                return MongoDbMetadataHelper.createOneResult(collectionType, ((PathQuery) realQuery).getPaths());
            }
            final Set<String> newRoots = checkUnitAgainstRoots(((PathQuery) realQuery).getPaths(), previous);
            previous.clear();
            if (newRoots.isEmpty()) {
                return MongoDbMetadataHelper.createOneResult(collectionType);
            }
            return MongoDbMetadataHelper.createOneResult(collectionType, newRoots);
        }
        // Not PATH
        int exactDepth = QueryDepthHelper.HELPER.getExactDepth(realQuery);
        if (exactDepth < 0) {
            exactDepth = GlobalDatasParser.MAXDEPTH;
        }
        final int relativeDepth = QueryDepthHelper.HELPER.getRelativeDepth(realQuery);
        Result result;
        try {
            if (collectionType == FILTERARGS.UNITS) {
                if (exactDepth > 0) {
                    // Exact Depth request (descending)
                    LOGGER.debug("Unit Exact Depth request (descending)");
                    result = exactDepthUnitQuery(realQuery, previous, exactDepth, tenantId);
                } else if (relativeDepth != 0) {
                    // Relative Depth request (ascending or descending)
                    LOGGER.debug("Unit Relative Depth request (ascending or descending)");
                    result = relativeDepthUnitQuery(realQuery, previous, relativeDepth, tenantId, orderBy);
                } else {
                    // Current sub level request
                    LOGGER.debug("Unit Current sub level request");
                    result = sameDepthUnitQuery(realQuery, previous, tenantId, orderBy);
                }
            } else {
                // OBJECTGROUPS
                // No depth at all
                LOGGER.debug("ObjectGroup No depth at all");
                result = objectGroupQuery(realQuery, previous, tenantId);
            }
        } finally {
            previous.clear();
        }
        return result;
    }

    /**
     * Execute one Unit Query using exact Depth
     *
     * @param realQuery
     * @param previous
     * @param exactDepth
     * @param tenantId
     * @return the associated Result
     * @throws InvalidParseOperationException
     */
    protected Result exactDepthUnitQuery(Query realQuery, Result previous, int exactDepth, Integer tenantId)
        throws InvalidParseOperationException {

        // TODO P1 add unit tests
        final Result result = MongoDbMetadataHelper.createOneResult(FILTERARGS.UNITS);
        final Bson query = QueryToMongodb.getCommand(realQuery);
        final Bson roots = QueryToMongodb.getRoots(MetadataDocument.UP, previous.getCurrentIds());
        Bson finalQuery = and(query, roots, lte(Unit.MINDEPTH, exactDepth), gte(Unit.MAXDEPTH, exactDepth));
        if (tenantId != null) {
            finalQuery = and(query, roots, lte(Unit.MINDEPTH, exactDepth), gte(Unit.MAXDEPTH, exactDepth),
                eq(MetadataDocument.TENANT_ID, tenantId));
        }

        previous.clear();
        LOGGER.debug(QUERY2 + MongoDbHelper.bsonToString(finalQuery, false));
        @SuppressWarnings("unchecked")
        final FindIterable<Unit> iterable = (FindIterable<Unit>) MongoDbMetadataHelper.select(
            MetadataCollections.C_UNIT, finalQuery, Unit.UNIT_VITAM_PROJECTION);
        try (final MongoCursor<Unit> cursor = iterable.iterator()) {
            while (cursor.hasNext()) {
                final Unit unit = cursor.next();
                final String id = unit.getId();
                result.addId(id);
            }
        }
        result.setNbResult(result.getCurrentIds().size());
        if (GlobalDatasDb.PRINT_REQUEST) {
            LOGGER.warn("UnitExact: {}", result);
        }
        return result;
    }

    /**
     * Execute one relative Depth Unit Query
     *
     * @param realQuery
     * @param previous
     * @param relativeDepth
     * @param tenantId
     * @return the associated Result
     * @throws InvalidParseOperationException
     * @throws MetaDataExecutionException
     */
    protected Result relativeDepthUnitQuery(Query realQuery, Result previous, int relativeDepth, Integer tenantId,
        final Bson orderBy)
        throws InvalidParseOperationException, MetaDataExecutionException {

        if (realQuery.isFullText()) {
            // ES
            QueryBuilder roots = null;

            if (previous.getCurrentIds().isEmpty()) {
                if (relativeDepth < 0) {
                    roots = QueryBuilders.rangeQuery(Unit.MAXDEPTH).lte(1);
                } else {
                    roots = QueryBuilders.rangeQuery(Unit.MAXDEPTH).lte(relativeDepth + 1);
                }
            } else {
                if (relativeDepth == 1) {
                    roots = QueryToElasticsearch.getRoots(MetadataDocument.UP,
                        previous.getCurrentIds());
                } else if (relativeDepth >= 1) {
                    roots = QueryToElasticsearch.getRoots(Unit.UNITUPS, previous.getCurrentIds());
                }

            }

            QueryBuilder query = QueryToElasticsearch.getCommand(realQuery);
            if (roots != null) {
                query = QueryToElasticsearch.getFullCommand(query, roots);
            }
            LOGGER.debug(QUERY2 + query.toString());
            if (GlobalDatasDb.PRINT_REQUEST) {
                LOGGER.debug("Req1LevelMD: {}", query);
            }
            previous.clear();

            List<SortBuilder> sorts = QueryToElasticsearch.getSorts(orderBy);

            return MetadataCollections.C_UNIT.getEsClient().search(MetadataCollections.C_UNIT, tenantId,
                Unit.TYPEUNIQUE, query, null, sorts);

        } else {
            // MongoDB
            Bson roots = null;
            boolean tocheck = false;
            if (previous.getCurrentIds().isEmpty()) {
                if (relativeDepth == 1) {
                    roots = lte(Unit.MAXDEPTH, 1);
                } else {
                    roots = lte(Unit.MAXDEPTH, relativeDepth + 1);
                }

            } else {
                if (relativeDepth < 0) {
                    // Relative parent: previous has future result in their _up
                    // so future result ids are in previous UNITDEPTHS
                    final Set<String> fathers = aggregateUnitDepths(previous.getCurrentIds(), relativeDepth);
                    roots = QueryToMongodb.getRoots(MetadataDocument.ID, fathers);
                } else if (relativeDepth == 1) {
                    // immediate step: previous is in UNIT_TO_UNIT of result
                    roots = QueryToMongodb.getRoots(MetadataDocument.UP,
                        previous.getCurrentIds());
                } else {
                    // relative depth: previous is in UNITUPS of result
                    // Will need an extra test on result
                    roots = QueryToMongodb.getRoots(Unit.UNITUPS, previous.getCurrentIds());
                    tocheck = true;
                }
            }

            Result result = null;
            Bson query = QueryToMongodb.getCommand(realQuery);
            if (roots != null) {
                query = QueryToMongodb.getFullCommand(query, roots);
            }
            if (tenantId != null) {
                // lets add the query on the tenant
                query = and(query, eq(MetadataDocument.TENANT_ID, tenantId));
            }

            LOGGER.debug(QUERY2 + MongoDbHelper.bsonToString(query, false));
            result = MongoDbMetadataHelper.createOneResult(FILTERARGS.UNITS);
            if (GlobalDatasDb.PRINT_REQUEST) {
                LOGGER.debug("Req1LevelMD: {}", realQuery);
            }
            @SuppressWarnings("unchecked")
            final FindIterable<Unit> iterable =
                (FindIterable<Unit>) MongoDbMetadataHelper.select(MetadataCollections.C_UNIT, query,
                    Unit.UNIT_VITAM_PROJECTION);
            try (final MongoCursor<Unit> cursor = iterable.iterator()) {
                while (cursor.hasNext()) {
                    final Unit unit = cursor.next();
                    if (tocheck) {
                        // now check for relativeDepth > 1
                        final Map<String, Integer> depths = unit.getDepths();
                        boolean check = false;
                        for (final String pid : previous.getCurrentIds()) {
                            final Integer depth = depths.get(pid);
                            if (depth != null && depth <= relativeDepth) {
                                check = true;
                                break;
                            }
                        }
                        if (!check) {
                            // ignore since false positive
                            continue;
                        }
                    }
                    final String id = unit.getId();
                    result.addId(id);
                }
            } finally {
                previous.clear();
            }
            result.setNbResult(result.getCurrentIds().size());
            if (GlobalDatasDb.PRINT_REQUEST) {
                LOGGER.debug("UnitRelative: {}", result);
            }
            return result;
        }
    }

    /**
     * Aggregate Unit Depths according to parent relative Depth
     *
     * @param ids
     * @param relativeDepth
     * @return the aggregate set of multi level parents for this relativeDepth
     */
    protected Set<String> aggregateUnitDepths(Set<String> ids, int relativeDepth) {
        // TODO P1 add unit tests
        // Select all items from ids
        final Bson match = match(in(MetadataDocument.ID, ids));
        // aggregate all UNITDEPTH in one (ignoring depth value)
        final Bson group = group(new BasicDBObject(MetadataDocument.ID, "all"),
            addToSet("deptharray", BuilderToken.DEFAULT_PREFIX + Unit.UNITDEPTHS));
        LOGGER.debug("Depth: " + MongoDbHelper.bsonToString(match, false) + " " +
            MongoDbHelper.bsonToString(group, false));
        final List<Bson> pipeline = Arrays.asList(match, group);
        @SuppressWarnings("unchecked")
        final AggregateIterable<Unit> aggregateIterable =
            MetadataCollections.C_UNIT.getCollection().aggregate(pipeline);
        final Unit aggregate = aggregateIterable.first();
        final Set<String> set = new HashSet<>();
        if (aggregate != null) {
            @SuppressWarnings("unchecked")
            final List<Map<String, Integer>> array = (List<Map<String, Integer>>) aggregate.get("deptharray");
            for (final Map<String, Integer> map : array) {
                for (final String key : map.keySet()) {
                    if (map.get(key) <= relativeDepth) {
                        set.add(key);
                    }
                }
                map.clear();
            }
            array.clear();
        }
        return set;
    }

    /**
     * Execute one relative Depth Unit Query
     *
     * @param realQuery
     * @param previous
     * @return the associated Result
     * @throws InvalidParseOperationException
     * @throws MetaDataExecutionException
     */
    protected Result sameDepthUnitQuery(Query realQuery, Result previous, Integer tenantId, final Bson orderBy)
        throws InvalidParseOperationException, MetaDataExecutionException {

        final Result result = MongoDbMetadataHelper.createOneResult(FILTERARGS.UNITS);

        if (realQuery.isFullText()) {

            // ES
            final QueryBuilder query = QueryToElasticsearch.getCommand(realQuery);
            QueryBuilder finalQuery;
            if (previous.getCurrentIds().isEmpty()) {
                finalQuery = query;
            } else {
                final QueryBuilder roots = QueryToElasticsearch.getRoots(MetadataDocument.ID, previous.getCurrentIds());
                finalQuery = QueryBuilders.boolQuery().must(query).must(roots);
            }

            List<SortBuilder> sorts = QueryToElasticsearch.getSorts(orderBy);

            previous.clear();
            LOGGER.debug(QUERY2 + finalQuery.toString());
            return MetadataCollections.C_UNIT.getEsClient().search(MetadataCollections.C_UNIT, tenantId,
                Unit.TYPEUNIQUE, finalQuery, null, sorts);

        } else {

            // Mongo
            // TODO P1 add unit tests
            final Bson query = QueryToMongodb.getCommand(realQuery);
            Bson finalQuery;
            if (previous.getCurrentIds().isEmpty()) {
                finalQuery = query;
            } else {
                final Bson roots = QueryToMongodb.getRoots(MetadataDocument.ID, previous.getCurrentIds());
                finalQuery = and(query, roots);
            }
            if (tenantId != null) {
                // lets add tenant query
                finalQuery = and(finalQuery, eq(MetadataDocument.TENANT_ID, tenantId));
            }
            previous.clear();
            LOGGER.debug(QUERY2 + MongoDbHelper.bsonToString(finalQuery, false));
            @SuppressWarnings("unchecked")
            final FindIterable<Unit> iterable = (FindIterable<Unit>) MongoDbMetadataHelper
                .select(MetadataCollections.C_UNIT, finalQuery, Unit.UNIT_VITAM_PROJECTION);
            try (final MongoCursor<Unit> cursor = iterable.iterator()) {
                while (cursor.hasNext()) {
                    final Unit unit = cursor.next();
                    final String id = unit.getId();
                    result.addId(id);
                }
            }
            result.setNbResult(result.getCurrentIds().size());
            if (GlobalDatasDb.PRINT_REQUEST) {
                LOGGER.warn("UnitSameDepth: {}", result);
            }
            return result;
        }
    }

    /**
     * Execute one relative Depth ObjectGroup Query
     *
     * @param realQuery
     * @param previous units, Note: only immediate Unit parents are allowed
     * @param tenantId
     * @return the associated Result
     * @throws InvalidParseOperationException
     */
    protected Result objectGroupQuery(Query realQuery, Result previous, Integer tenantId)
        throws InvalidParseOperationException {
        final Result result = MongoDbMetadataHelper.createOneResult(FILTERARGS.OBJECTGROUPS);
        final Bson query = QueryToMongodb.getCommand(realQuery);
        Bson finalQuery;
        if (previous.getCurrentIds().isEmpty()) {
            finalQuery = query;
        } else {
            if (FILTERARGS.UNITS.equals(previous.getType())) {
                final Bson roots = QueryToMongodb.getRoots(MetadataDocument.UP, previous.getCurrentIds());
                finalQuery = and(query, roots);
            } else {
                final Bson roots = QueryToMongodb.getRoots(MetadataDocument.ID, previous.getCurrentIds());
                finalQuery = and(query, roots);
            }
        }
        if (tenantId != null) {
            // lets add tenant query
            finalQuery = and(finalQuery, eq(MetadataDocument.TENANT_ID, tenantId));
        }

        previous.clear();
        LOGGER.debug(QUERY2 + MongoDbHelper.bsonToString(finalQuery, false));
        @SuppressWarnings("unchecked")
        final FindIterable<ObjectGroup> iterable = (FindIterable<ObjectGroup>) MongoDbMetadataHelper.select(
            MetadataCollections.C_OBJECTGROUP, finalQuery,
            ObjectGroup.OBJECTGROUP_VITAM_PROJECTION);
        try (final MongoCursor<ObjectGroup> cursor = iterable.iterator()) {
            while (cursor.hasNext()) {
                result.addId(cursor.next().getId());
            }
        }
        result.setNbResult(result.getCurrentIds().size());
        return result;
    }

    /**
     * Finalize the queries with last True Select
     *
     * @param requestToMongodb
     * @param last
     * @return the final Result
     * @throws InvalidParseOperationException
     * @throws MetaDataExecutionException
     */
    protected Result lastSelectFilterProjection(SelectToMongodb requestToMongodb, Result last)
        throws InvalidParseOperationException, MetaDataExecutionException {
        final Bson roots = QueryToMongodb.getRoots(MetadataDocument.ID, last.getCurrentIds());
        final Bson projection = requestToMongodb.getFinalProjection();
        final Bson orderBy = requestToMongodb.getFinalOrderBy();
        final int offset = requestToMongodb.getFinalOffset();
        final int limit = requestToMongodb.getFinalLimit();
        final FILTERARGS model = requestToMongodb.model();
        LOGGER.debug("To Select: " + MongoDbHelper.bsonToString(roots, false) + " " +
            (projection != null ? MongoDbHelper.bsonToString(projection, false) : "") + " " +
            MongoDbHelper.bsonToString(orderBy, false) + " " + offset + " " + limit);
        if (model == FILTERARGS.UNITS) {
            @SuppressWarnings("unchecked")
            final FindIterable<Unit> iterable =
                (FindIterable<Unit>) MongoDbMetadataHelper.select(MetadataCollections.C_UNIT,
                    roots, projection, orderBy, offset, limit);
            try (final MongoCursor<Unit> cursor = iterable.iterator()) {
                while (cursor.hasNext()) {
                    final Unit unit = cursor.next();
                    last.addId(unit.getId());
                    last.addFinal(unit);
                }
            }
            last.setNbResult(last.getCurrentIds().size());
            return last;
        }
        // OBJECTGROUPS:
        @SuppressWarnings("unchecked")
        final FindIterable<ObjectGroup> iterable =
            (FindIterable<ObjectGroup>) MongoDbMetadataHelper.select(
                MetadataCollections.C_OBJECTGROUP,
                roots, projection, orderBy, offset, limit);
        try (final MongoCursor<ObjectGroup> cursor = iterable.iterator()) {
            while (cursor.hasNext()) {
                final ObjectGroup og = cursor.next();
                last.addId(og.getId());
                last.addFinal(og);
            }
        }
        last.setNbResult(last.getCurrentIds().size());
        return last;
    }

    /**
     * Finalize the queries with last True Update
     *
     * @param requestToMongodb
     * @param last
     * @param requestParser
     * @return the final Result
     * @throws InvalidParseOperationException
     * @throws MetaDataExecutionException
     */
    protected Result lastUpdateFilterProjection(UpdateToMongodb requestToMongodb, Result last,
        RequestParserMultiple requestParser)
        throws InvalidParseOperationException, MetaDataExecutionException {
        Integer tenantId = ParameterHelper.getTenantParameter();
        final Bson roots = QueryToMongodb.getRoots(MetadataDocument.ID, last.getCurrentIds());
        final FILTERARGS model = requestToMongodb.model();

        MongoCollection<MetadataDocument<?>> collection;
        if (model == FILTERARGS.UNITS) {
            collection = MetadataCollections.C_UNIT.getCollection();
        } else {
            collection = MetadataCollections.C_OBJECTGROUP.getCollection();
        }

        List<MetadataDocument<?>> listDocuments = new ArrayList<>();
        FindIterable<MetadataDocument<?>> searchResults = collection.find(roots);
        Iterator<MetadataDocument<?>> it = searchResults.iterator();
        while (it.hasNext()) {
            listDocuments.add(it.next());
        }

        last.setNbResult(0);
        SchemaValidationUtils validator;
        try {
            validator = new SchemaValidationUtils();
        } catch (FileNotFoundException | ProcessingException e) {
            LOGGER.error("Unable to initialize Json Validator");
            throw new MetaDataExecutionException(e);
        }

        for (MetadataDocument<?> document : listDocuments) {
            final String documentId = document.getId();
            final Integer documentVersion = document.getVersion();

            UpdateResult result = null;
            int tries = 0;

            while (result == null && tries < 3) {
                JsonNode jsonDocument = JsonHandler.toJsonNode(document);
                MongoDbInMemory mongoInMemory = new MongoDbInMemory(jsonDocument);
                requestToMongodb.getFinalUpdateActions();
                ObjectNode updatedJsonDocument = (ObjectNode) mongoInMemory.getUpdateJson(requestParser);

                updatedJsonDocument.set(VitamDocument.VERSION, new IntNode(documentVersion + 1));

                if (model == FILTERARGS.UNITS) {
                    SchemaValidationStatus status = validator.validateUpdateUnit(updatedJsonDocument);
                    if (!SchemaValidationStatusEnum.VALID.equals(status.getValidationStatus())) {
                        throw new MetaDataExecutionException("Unable to validate updated Unit");
                    }
                }

                // Make Update
                Bson condition =
                    and(eq(MetadataDocument.ID, documentId), eq(MetadataDocument.VERSION, documentVersion));
                result = collection.replaceOne(condition, document.newInstance(updatedJsonDocument));
                tries++;
            }

            if (result == null) {
                throw new MetaDataExecutionException("Can not modify Document");
            }

            last.setNbResult(last.getNbResult() + result.getModifiedCount());

            try {
                if (model == FILTERARGS.UNITS) {
                    indexFieldsUpdated(last, tenantId);
                } else {
                    indexFieldsOGUpdated(last);
                }
            } catch (Exception e) {
                throw new MetaDataExecutionException("Update concern", e);
            }
        }

        return last;
    }

    /**
     * indexFieldsUpdated : Update index related to Fields updated
     *
     * @param last : contains the Result to be indexed
     *
     * @throws Exception
     */
    private void indexFieldsUpdated(Result last, Integer tenantId) throws Exception {
        final Bson finalQuery;
        if (last.getCurrentIds().isEmpty()) {
            return;
        }
        if (last.getCurrentIds().size() == 1) {
            finalQuery = eq(MetadataDocument.ID, last.getCurrentIds().iterator().next());
        } else {
            finalQuery = in(MetadataDocument.ID, last.getCurrentIds());
        }
        @SuppressWarnings("unchecked")
        final FindIterable<Unit> iterable = (FindIterable<Unit>) MongoDbMetadataHelper
            .select(MetadataCollections.C_UNIT, finalQuery, Unit.UNIT_ES_PROJECTION);
        // TODO maybe retry once if in error ?
        try (final MongoCursor<Unit> cursor = iterable.iterator()) {
            MetadataCollections.C_UNIT.getEsClient().updateBulkUnitsEntriesIndexes(cursor, tenantId);;
        }

    }

    /**
     * indexFieldsOGUpdated : Update index OG related to Fields updated
     *
     * @param last : contains the Result to be indexed
     *
     * @throws Exception
     *
     */
    private void indexFieldsOGUpdated(Result last) throws Exception {
        Integer tenantId = ParameterHelper.getTenantParameter();
        final Bson finalQuery;
        if (last.getCurrentIds().isEmpty()) {
            LOGGER.error("ES update in error since no results to update");
            // no result to update
            return;
        }
        if (last.getCurrentIds().size() == 1) {
            finalQuery = eq(MetadataDocument.ID, last.getCurrentIds().iterator().next());
        } else {
            finalQuery = in(MetadataDocument.ID, last.getCurrentIds());
        }
        @SuppressWarnings("unchecked")
        final FindIterable<ObjectGroup> iterable = (FindIterable<ObjectGroup>) MongoDbMetadataHelper
            .select(MetadataCollections.C_OBJECTGROUP, finalQuery, ObjectGroup.OBJECTGROUP_VITAM_PROJECTION);
        // TODO maybe retry once if in error ?
        try (final MongoCursor<ObjectGroup> cursor = iterable.iterator()) {
            MetadataCollections.C_OBJECTGROUP.getEsClient().updateBulkOGEntriesIndexes(cursor, tenantId);;
        }

    }


    /**
     * removeOGIndexFields : remove index related to Fields deleted
     *
     * @param last : contains the Result to be removed
     *
     * @throws Exception
     *
     */
    private void removeOGIndexFields(Result last) throws Exception {
        Integer tenantId = ParameterHelper.getTenantParameter();
        final Bson finalQuery;
        if (last.getCurrentIds().isEmpty()) {
            LOGGER.error("ES delete in error since no results to delete");
            // no result to delete
            return;
        }
        if (last.getCurrentIds().size() == 1) {
            finalQuery = eq(MetadataDocument.ID, last.getCurrentIds().iterator().next());
        } else {
            finalQuery = in(MetadataDocument.ID, last.getCurrentIds());
        }
        @SuppressWarnings("unchecked")
        final FindIterable<ObjectGroup> iterable = (FindIterable<ObjectGroup>) MongoDbMetadataHelper
            .select(MetadataCollections.C_OBJECTGROUP, finalQuery, ObjectGroup.OBJECTGROUP_VITAM_PROJECTION);
        // TODO maybe retry once if in error ?
        try (final MongoCursor<ObjectGroup> cursor = iterable.iterator()) {
            MetadataCollections.C_OBJECTGROUP.getEsClient().deleteBulkOGEntriesIndexes(cursor, tenantId);
        }

    }

    /**
     * removeUnitIndexFields : remove index related to Fields deleted
     *
     * @param last : contains the Result to be removed
     *
     * @throws Exception
     *
     */
    private void removeUnitIndexFields(Result last) throws Exception {
        Integer tenantId = ParameterHelper.getTenantParameter();
        final Bson finalQuery;
        if (last.getCurrentIds().isEmpty()) {
            LOGGER.error("ES delete in error since no results to delete");
            // no result to delete
            return;
        }
        if (last.getCurrentIds().size() == 1) {
            finalQuery = eq(MetadataDocument.ID, last.getCurrentIds().iterator().next());
        } else {
            finalQuery = in(MetadataDocument.ID, last.getCurrentIds());
        }
        @SuppressWarnings("unchecked")
        final FindIterable<Unit> iterable = (FindIterable<Unit>) MongoDbMetadataHelper
            .select(MetadataCollections.C_UNIT, finalQuery, Unit.UNIT_ES_PROJECTION);
        // TODO maybe retry once if in error ?
        try (final MongoCursor<Unit> cursor = iterable.iterator()) {
            MetadataCollections.C_UNIT.getEsClient().deleteBulkUnitsEntriesIndexes(cursor, tenantId);
        }
    }

    /**
     * Finalize the queries with last True Insert
     *
     * @param requestToMongodb
     * @param last
     * @return the final Result
     * @throws InvalidParseOperationException
     * @throws MetaDataAlreadyExistException
     * @throws MetaDataExecutionException
     * @throws MetaDataNotFoundException
     */
    protected Result lastInsertFilterProjection(InsertToMongodb requestToMongodb, Result last)
        throws InvalidParseOperationException, MetaDataAlreadyExistException, MetaDataExecutionException,
        MetaDataNotFoundException {
        final Document data = requestToMongodb.getFinalData();
        LOGGER.debug("To Insert: ", data);
        final FILTERARGS model = requestToMongodb.model();
        try {
            if (model == FILTERARGS.UNITS) {
                final Unit unit = new Unit(data);
                if (MongoDbMetadataHelper.exists(MetadataCollections.C_UNIT, unit.getId())) {
                    // Should not exist
                    throw new MetaDataAlreadyExistException("Unit already exists: " + unit.getId());
                }
                unit.save();
                @SuppressWarnings("unchecked")
                final FindIterable<Unit> iterable =
                    (FindIterable<Unit>) MongoDbMetadataHelper.select(MetadataCollections.C_UNIT,
                        in(MetadataDocument.ID, last.getCurrentIds()), Unit.UNIT_VITAM_PROJECTION);
                final Set<String> notFound = new HashSet<>(last.getCurrentIds());
                // TODO P2 optimize by trying to update only once the unit
                try (MongoCursor<Unit> cursor = iterable.iterator()) {
                    while (cursor.hasNext()) {
                        final Unit parentUnit = cursor.next();
                        parentUnit.addUnit(unit);
                        notFound.remove(parentUnit.getId());
                    }
                }
                if (!notFound.isEmpty()) {
                    // FIXME P1 some Junit failed on this
                    LOGGER.error("Cannot find parent: " + notFound);
                    throw new MetaDataNotFoundException("Cannot find Parent: " + notFound);
                }
                last.clear();
                last.addId(unit.getId());
                last.setNbResult(1);


                if (unit.getString(MetadataDocument.OG) != null && !unit.getString(MetadataDocument.OG).isEmpty()) {
                    // find the unit that we just save, to take sps field, and save it in the object group
                    MetadataDocument newUnit =
                        MongoDbMetadataHelper.findOne(MetadataCollections.C_UNIT, unit.getString(MetadataDocument.ID));
                    List originatingAgencies = newUnit.get(MetadataDocument.ORIGINATING_AGENCIES, List.class);

                    Bson updateSps = Updates.addEachToSet(MetadataDocument.ORIGINATING_AGENCIES, originatingAgencies);
                    Bson updateUp = Updates.addToSet(MetadataDocument.UP, unit.getString(MetadataDocument.ID));
                    Bson updateOps =
                        Updates.addToSet(MetadataDocument.OPS, VitamThreadUtils.getVitamSession().getRequestId());


                    Bson update = combine(updateSps, updateUp, updateOps);
                    MetadataCollections.C_OBJECTGROUP.getCollection()
                        .updateOne(eq(ID, unit.getString(MetadataDocument.OG)),
                            update,
                            new UpdateOptions().upsert(false));
                }


                insertBulk(requestToMongodb, last);
                // FIXME P1 should handle micro update on parents in ES
                return last;
            }
            // OBJECTGROUPS:
            // TODO P1 add unit tests
            final ObjectGroup og = new ObjectGroup(data);
            if (MongoDbMetadataHelper.exists(MetadataCollections.C_OBJECTGROUP, og.getId())) {
                // Should not exist
                throw new MetaDataAlreadyExistException("ObjectGroup already exists: " + og.getId());
            }
            if (last.getCurrentIds().isEmpty() && og.getFathersUnitIds(false).isEmpty()) {
                // Must not be
                LOGGER.debug("No Unit parent defined");
                throw new MetaDataNotFoundException("No Unit parent defined");
            }
            og.save();
            @SuppressWarnings("unchecked")
            final FindIterable<Unit> iterable =
                (FindIterable<Unit>) MongoDbMetadataHelper.select(MetadataCollections.C_UNIT,
                    in(MetadataDocument.ID, last.getCurrentIds()), Unit.UNIT_OBJECTGROUP_PROJECTION);
            final Set<String> notFound = new HashSet<>(last.getCurrentIds());
            // TODO P2 optimize by trying to update only once the og
            try (MongoCursor<Unit> cursor = iterable.iterator()) {
                while (cursor.hasNext()) {
                    final Unit parentUnit = cursor.next();
                    parentUnit.addObjectGroup(og);
                    notFound.remove(parentUnit.getId());
                }
            }
            if (!notFound.isEmpty()) {
                // FIXME P1 some Junit failed on this
                LOGGER.error("Cannot find parent: " + notFound);
                throw new MetaDataNotFoundException("Cannot find Parent: " + notFound);
            }
            last.clear();
            last.addId(og.getId());
            last.setNbResult(1);
            insertBulk(requestToMongodb, last);
            return last;
        } catch (final MongoWriteException e) {
            throw e;
        } catch (final MongoException e) {
            throw new MetaDataExecutionException("Insert concern", e);
        }
    }

    /**
     * Bulk insert in ES
     *
     * @param requestToMongodb
     * @param result
     * @throws MetaDataExecutionException
     */
    private void insertBulk(InsertToMongodb requestToMongodb, Result result) throws MetaDataExecutionException {
        // index Metadata
        Integer tenantId = ParameterHelper.getTenantParameter();
        final Set<String> ids = result.getCurrentIds();
        final FILTERARGS model = requestToMongodb.model();
        // index Unit
        if (model == FILTERARGS.UNITS) {
            final Bson finalQuery = in(MetadataDocument.ID, ids);
            @SuppressWarnings("unchecked")
            final FindIterable<Unit> iterable = (FindIterable<Unit>) MongoDbMetadataHelper
                .select(MetadataCollections.C_UNIT, finalQuery, Unit.UNIT_ES_PROJECTION);
            // TODO maybe retry once if in error ?
            try (final MongoCursor<Unit> cursor = iterable.iterator()) {
                MetadataCollections.C_UNIT.getEsClient().insertBulkUnitsEntriesIndexes(cursor, tenantId);
            }
        } else if (model == FILTERARGS.OBJECTGROUPS) {
            // index OG
            final Bson finalQuery = in(MetadataDocument.ID, ids);
            @SuppressWarnings("unchecked")
            final FindIterable<ObjectGroup> iterable = (FindIterable<ObjectGroup>) MongoDbMetadataHelper
                .select(MetadataCollections.C_OBJECTGROUP, finalQuery, null);
            // TODO maybe retry once if in error ?
            try (final MongoCursor<ObjectGroup> cursor = iterable.iterator()) {
                MetadataCollections.C_OBJECTGROUP.getEsClient().insertBulkOGEntriesIndexes(cursor, tenantId);
            }
        }
    }

    /**
     * Finalize the queries with last True Delete
     *
     * @param requestToMongodb
     * @param last
     * @return the final Result
     * @throws InvalidParseOperationException
     * @throws MetaDataExecutionException
     */
    protected Result lastDeleteFilterProjection(DeleteToMongodb requestToMongodb, Result last)
        throws InvalidParseOperationException, MetaDataExecutionException {
        final Bson roots = QueryToMongodb.getRoots(MetadataDocument.ID, last.getCurrentIds());
        LOGGER.debug("To Delete: " + MongoDbHelper.bsonToString(roots, false));
        final FILTERARGS model = requestToMongodb.model();
        try {
            if (model == FILTERARGS.UNITS) {
                final DeleteResult result = MongoDbMetadataHelper.delete(MetadataCollections.C_UNIT,
                    roots, last.getCurrentIds().size());
                last.setNbResult(result.getDeletedCount());
                removeUnitIndexFields(last);
                return last;
            }
            // TODO P1 add unit tests
            // OBJECTGROUPS:
            final DeleteResult result =
                MongoDbMetadataHelper.delete(MetadataCollections.C_OBJECTGROUP,
                    roots, last.getCurrentIds().size());
            last.setNbResult(result.getDeletedCount());
            removeOGIndexFields(last);
            return last;
        } catch (final MetaDataExecutionException e) {
            throw e;
        } catch (final Exception e) {
            throw new MetaDataExecutionException("Delete concern", e);
        }
    }

}
