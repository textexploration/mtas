package mtas.solr.handler.component.util;

import java.io.IOException;
import java.lang.invoke.MethodHandles;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.apache.lucene.spatial.prefix.PrefixTreeStrategy;
import org.apache.lucene.spatial.query.SpatialArgs;
import org.apache.lucene.spatial.query.SpatialOperation;
import org.apache.solr.common.SolrException;
import org.apache.solr.common.util.NamedList;
import org.apache.solr.common.util.SimpleOrderedMap;
import org.apache.solr.handler.component.ResponseBuilder;
import org.apache.solr.handler.component.SearchComponent;
import org.apache.solr.handler.component.ShardRequest;
import org.apache.solr.handler.component.ShardResponse;
import org.apache.solr.schema.AbstractSpatialPrefixTreeFieldType;
import org.apache.solr.schema.FieldType;
import org.apache.solr.schema.RptWithGeometrySpatialField;
import org.apache.solr.schema.SchemaField;
import org.apache.solr.schema.SpatialRecursivePrefixTreeFieldType;
import org.apache.solr.util.DistanceUnits;
import org.apache.solr.util.SpatialUtils;
import org.locationtech.spatial4j.context.SpatialContext;
import org.locationtech.spatial4j.shape.Shape;

import mtas.codec.util.CodecComponent.ComponentField;
import mtas.codec.util.CodecComponent.ComponentFields;
import mtas.codec.util.CodecComponent.ComponentHeatmap;
import mtas.codec.util.CodecComponent.SubComponentFunction;
import mtas.codec.util.collector.MtasDataCollector;
import mtas.codec.util.heatmap.HeatmapMtasCounter.Heatmap;
import mtas.parser.function.ParseException;
import mtas.search.spans.util.MtasSpanQuery;
import mtas.solr.handler.component.MtasSolrSearchComponent;

/**
 * The Class MtasSolrComponentGroup.
 */
public class MtasSolrComponentHeatmap implements MtasSolrComponent<ComponentHeatmap> {

  
  /** The Constant log. */
    private static final Logger log = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

  /** The search component. */
  MtasSolrSearchComponent searchComponent;

  /** The Constant NAME. */
  public static final String NAME = "heatmap";

  /** The Constant PARAM_MTAS_GROUP. */
  public static final String PARAM_MTAS_HEATMAP = MtasSolrSearchComponent.PARAM_MTAS + "." + NAME;

  /** The Constant NAME_MTAS_GROUP_FIELD. */
  public static final String NAME_MTAS_HEATMAP_HEATMAP_FIELD = "heatmapField";
  
  /** The Constant NAME_MTAS_STATS_TOKENS_TYPE. */
  public static final String NAME_MTAS_HEATMAP_TYPE = "type";
  
  /** The Constant NAME_MTAS_STATS_SPANS_MINIMUM. */
  public static final String NAME_MTAS_HEATMAP_MINIMUM = "minimum";

  /** The Constant NAME_MTAS_STATS_SPANS_MAXIMUM. */
  public static final String NAME_MTAS_HEATMAP_MAXIMUM = "maximum";


  /** The Constant NAME_MTAS_HEATMAP_QUERY_FIELD. */
  public static final String NAME_MTAS_HEATMAP_QUERY_FIELD = "queryField";
  
  /** The Constant NAME_MTAS_STATS_SPANS_FUNCTION. */
  public static final String NAME_MTAS_HEATMAP_FUNCTION = "function";

  /** The Constant SUBNAME_MTAS_STATS_SPANS_FUNCTION_EXPRESSION. */
  public static final String SUBNAME_MTAS_HEATMAP_FUNCTION_EXPRESSION = "expression";

  /** The Constant SUBNAME_MTAS_STATS_SPANS_FUNCTION_KEY. */
  public static final String SUBNAME_MTAS_HEATMAP_FUNCTION_KEY = "key";

  /** The Constant SUBNAME_MTAS_STATS_SPANS_FUNCTION_TYPE. */
  public static final String SUBNAME_MTAS_HEATMAP_FUNCTION_TYPE = "type";
  
  
  /** The Constant NAME_MTAS_HEATMAP_GEOM. */
  public static final String NAME_MTAS_HEATMAP_GEOM = "geom";
  
  /** The Constant NAME_MTAS_HEATMAP_GRID_LEVEL. */
  public static final String NAME_MTAS_HEATMAP_GRID_LEVEL = "gridLevel";
  
  /** The Constant NAME_MTAS_HEATMAP_DIST_ERR_PCT. */
  public static final String NAME_MTAS_HEATMAP_DIST_ERR_PCT = "distErrPct";
  
  /** The Constant NAME_MTAS_HEATMAP_DIST_ERR. */
  public static final String NAME_MTAS_HEATMAP_DIST_ERR = "distErr";

  /** The Constant NAME_MTAS_HEATMAP_MAX_CELLS. */
  public static final String NAME_MTAS_HEATMAP_MAX_CELLS = "maxCells";
  
  /** The Constant NAME_MTAS_GROUP_KEY. */
  public static final String NAME_MTAS_HEATMAP_KEY = "key";
  
  
  
  
  /** The Constant NAME_MTAS_HEATMAP_QUERY. */
  private static final String NAME_MTAS_HEATMAP_QUERY = "query";

  /** The Constant SUBNAME_MTAS_HEATMAP_QUERY_TYPE. */
  public static final String SUBNAME_MTAS_HEATMAP_QUERY_TYPE = "type";

  /** The Constant SUBNAME_MTAS_HEATMAP_QUERY_VALUE. */
  public static final String SUBNAME_MTAS_HEATMAP_QUERY_VALUE = "value";

  /** The Constant SUBNAME_MTAS_HEATMAP_QUERY_PREFIX. */
  public static final String SUBNAME_MTAS_HEATMAP_QUERY_PREFIX = "prefix";

  /** The Constant SUBNAME_MTAS_HEATMAP_QUERY_IGNORE. */
  public static final String SUBNAME_MTAS_HEATMAP_QUERY_IGNORE = "ignore";

  /** The Constant SUBNAME_MTAS_HEATMAP_QUERY_MAXIMUM_IGNORE_LENGTH. */
  public static final String SUBNAME_MTAS_HEATMAP_QUERY_MAXIMUM_IGNORE_LENGTH = "maximumIgnoreLength";

  /** The Constant SUBNAME_MTAS_HEATMAP_QUERY_VARIABLE. */
  public static final String SUBNAME_MTAS_HEATMAP_QUERY_VARIABLE = "variable";

  /** The Constant SUBNAME_MTAS_HEATMAP_QUERY_VARIABLE_NAME. */
  public static final String SUBNAME_MTAS_HEATMAP_QUERY_VARIABLE_NAME = "name";

  /** The Constant SUBNAME_MTAS_HEATMAP_QUERY_VARIABLE_VALUE. */
  public static final String SUBNAME_MTAS_HEATMAP_QUERY_VARIABLE_VALUE = "value";

  
  
  

  /** The Constant DEFAULT_DIST_ERR_PCT. */
  public static final double DEFAULT_DIST_ERR_PCT = 0.15;

 
  /**
   * Instantiates a new mtas solr component group.
   *
   * @param searchComponent
   *          the search component
   */
  public MtasSolrComponentHeatmap(MtasSolrSearchComponent searchComponent) {
    this.searchComponent = searchComponent;
  }

  /*
   * (non-Javadoc)
   * 
   * @see
   * mtas.solr.handler.component.util.MtasSolrComponent#prepare(org.apache.solr.
   * handler.component.ResponseBuilder,
   * mtas.codec.util.CodecComponent.ComponentFields)
   */
  @Override
public void prepare(ResponseBuilder rb, ComponentFields mtasFields) throws IOException {
    Set<String> ids = MtasSolrResultUtil.getIdsFromParameters(rb.req.getParams(), PARAM_MTAS_HEATMAP);
    if (!ids.isEmpty()) {
      int tmpCounter = 0;
      String[] heatmapFields = new String[ids.size()];
      String[] queryFields = new String[ids.size()];
      String[] minima = new String[ids.size()];
      String[] maxima = new String[ids.size()];
      String[] types = new String[ids.size()];
      String[][] functionExpressions = new String[ids.size()][];
      String[][] functionKeys = new String[ids.size()][];
      String[][] functionTypes = new String[ids.size()][];
      String[][] queryTypes = new String[ids.size()][];
      String[][] queryValues = new String[ids.size()][];
      String[][] queryPrefixes = new String[ids.size()][];
      String[][] queryIgnores = new String[ids.size()][];
      String[][] queryMaximumIgnoreLengths = new String[ids.size()][];
      HashMap<String, String[]>[][] queryVariables = new HashMap[ids.size()][];
      String[] keys = new String[ids.size()];
      String[] geoms = new String[ids.size()];
      String[] gridLevels = new String[ids.size()];
      String[] distErrPcts = new String[ids.size()];
      String[] distErrs = new String[ids.size()];
      String[] maxCells = new String[ids.size()];
      for (String id : ids) {
        keys[tmpCounter] = rb.req.getParams()
            .get(PARAM_MTAS_HEATMAP + "." + id + "." + NAME_MTAS_HEATMAP_KEY, String.valueOf(tmpCounter)).trim();
        heatmapFields[tmpCounter] = rb.req.getParams()
            .get(PARAM_MTAS_HEATMAP + "." + id + "." + NAME_MTAS_HEATMAP_HEATMAP_FIELD, null);
        queryFields[tmpCounter] = rb.req.getParams()
            .get(PARAM_MTAS_HEATMAP + "." + id + "." + NAME_MTAS_HEATMAP_QUERY_FIELD, null);
        
        minima[tmpCounter] = rb.req.getParams().get(PARAM_MTAS_HEATMAP + "."
            + id + "." + NAME_MTAS_HEATMAP_MINIMUM, null);
        maxima[tmpCounter] = rb.req.getParams().get(PARAM_MTAS_HEATMAP + "."
            + id + "." + NAME_MTAS_HEATMAP_MAXIMUM, null);
        types[tmpCounter] = rb.req.getParams().get(PARAM_MTAS_HEATMAP + "."
            + id + "." + NAME_MTAS_HEATMAP_TYPE, null);
        Set<String> functionIds = MtasSolrResultUtil
            .getIdsFromParameters(rb.req.getParams(), PARAM_MTAS_HEATMAP
                + "." + id + "." + NAME_MTAS_HEATMAP_FUNCTION);
        functionExpressions[tmpCounter] = new String[functionIds.size()];
        functionKeys[tmpCounter] = new String[functionIds.size()];
        functionTypes[tmpCounter] = new String[functionIds.size()];
        int tmpSubCounter = 0;
        for (String functionId : functionIds) {
          functionKeys[tmpCounter][tmpSubCounter] = rb.req.getParams()
              .get(
                  PARAM_MTAS_HEATMAP + "." + id + "."
                      + NAME_MTAS_HEATMAP_FUNCTION + "." + functionId + "."
                      + SUBNAME_MTAS_HEATMAP_FUNCTION_KEY,
                  String.valueOf(tmpSubCounter))
              .trim();
          functionExpressions[tmpCounter][tmpSubCounter] = rb.req.getParams()
              .get(PARAM_MTAS_HEATMAP + "." + id + "."
                  + NAME_MTAS_HEATMAP_FUNCTION + "." + functionId + "."
                  + SUBNAME_MTAS_HEATMAP_FUNCTION_EXPRESSION, null);
          functionTypes[tmpCounter][tmpSubCounter] = rb.req.getParams()
              .get(PARAM_MTAS_HEATMAP + "." + id + "."
                  + NAME_MTAS_HEATMAP_FUNCTION + "." + functionId + "."
                  + SUBNAME_MTAS_HEATMAP_FUNCTION_TYPE, null);
          tmpSubCounter++;
        }
        
        Set<String> qIds = MtasSolrResultUtil.getIdsFromParameters(
            rb.req.getParams(),
            PARAM_MTAS_HEATMAP + "." + id + "." + NAME_MTAS_HEATMAP_QUERY);
        if (!qIds.isEmpty()) {
          int tmpQCounter = 0;
          queryTypes[tmpCounter] = new String[qIds.size()];
          queryValues[tmpCounter] = new String[qIds.size()];
          queryPrefixes[tmpCounter] = new String[qIds.size()];
          queryIgnores[tmpCounter] = new String[qIds.size()];
          queryMaximumIgnoreLengths[tmpCounter] = new String[qIds.size()];
          queryVariables[tmpCounter] = new HashMap[qIds.size()];
          for (String qId : qIds) {
            queryTypes[tmpCounter][tmpQCounter] = rb.req.getParams()
                .get(
                    PARAM_MTAS_HEATMAP + "." + id + "." + NAME_MTAS_HEATMAP_QUERY
                        + "." + qId + "." + SUBNAME_MTAS_HEATMAP_QUERY_TYPE,
                    null);
            queryValues[tmpCounter][tmpQCounter] = rb.req.getParams()
                .get(
                    PARAM_MTAS_HEATMAP + "." + id + "." + NAME_MTAS_HEATMAP_QUERY
                        + "." + qId + "." + SUBNAME_MTAS_HEATMAP_QUERY_VALUE,
                    null);
            queryPrefixes[tmpCounter][tmpQCounter] = rb.req.getParams()
                .get(
                    PARAM_MTAS_HEATMAP + "." + id + "." + NAME_MTAS_HEATMAP_QUERY
                        + "." + qId + "." + SUBNAME_MTAS_HEATMAP_QUERY_PREFIX,
                    null);
            queryIgnores[tmpCounter][tmpQCounter] = rb.req.getParams()
                .get(
                    PARAM_MTAS_HEATMAP + "." + id + "." + NAME_MTAS_HEATMAP_QUERY
                        + "." + qId + "." + SUBNAME_MTAS_HEATMAP_QUERY_IGNORE,
                    null);
            queryMaximumIgnoreLengths[tmpCounter][tmpQCounter] = rb.req
                .getParams()
                .get(PARAM_MTAS_HEATMAP + "." + id + "." + NAME_MTAS_HEATMAP_QUERY
                    + "." + qId + "."
                    + SUBNAME_MTAS_HEATMAP_QUERY_MAXIMUM_IGNORE_LENGTH, null);
            Set<String> vIds = MtasSolrResultUtil.getIdsFromParameters(
                rb.req.getParams(),
                PARAM_MTAS_HEATMAP + "." + id + "." + NAME_MTAS_HEATMAP_QUERY + "."
                    + qId + "." + SUBNAME_MTAS_HEATMAP_QUERY_VARIABLE);
            queryVariables[tmpCounter][tmpQCounter] = new HashMap<>();
            if (!vIds.isEmpty()) {
              HashMap<String, ArrayList<String>> tmpVariables = new HashMap<>();
              for (String vId : vIds) {
                String name = rb.req.getParams().get(PARAM_MTAS_HEATMAP + "." + id
                    + "." + NAME_MTAS_HEATMAP_QUERY + "." + qId + "."
                    + SUBNAME_MTAS_HEATMAP_QUERY_VARIABLE + "." + vId + "."
                    + SUBNAME_MTAS_HEATMAP_QUERY_VARIABLE_NAME, null);
                if (name != null) {
                  if (!tmpVariables.containsKey(name)) {
                    tmpVariables.put(name, new ArrayList<String>());
                  }
                  String value = rb.req.getParams().get(PARAM_MTAS_HEATMAP + "."
                      + id + "." + NAME_MTAS_HEATMAP_QUERY + "." + qId + "."
                      + SUBNAME_MTAS_HEATMAP_QUERY_VARIABLE + "." + vId + "."
                      + SUBNAME_MTAS_HEATMAP_QUERY_VARIABLE_VALUE, null);
                  if (value != null) {
                    ArrayList<String> list = new ArrayList<>();
                    String[] subList = value.split("(?<!\\\\),");
                    for (int i = 0; i < subList.length; i++) {
                      list.add(
                          subList[i].replace("\\,", ",").replace("\\\\", "\\"));
                    }
                    tmpVariables.get(name).addAll(list);
                  }
                }
              }
              for (Entry<String, ArrayList<String>> entry : tmpVariables
                  .entrySet()) {
                queryVariables[tmpCounter][tmpQCounter].put(entry.getKey(),
                    entry.getValue()
                        .toArray(new String[entry.getValue().size()]));
              }
            }
            tmpQCounter++;
          }
        } else {
          throw new IOException(
              "no " + NAME_MTAS_HEATMAP_QUERY + " for mtas heatmap " + id);
        }
        geoms[tmpCounter] = rb.req.getParams().get(PARAM_MTAS_HEATMAP + "." + id + "." + NAME_MTAS_HEATMAP_GEOM, null);
        gridLevels[tmpCounter] = rb.req.getParams()
            .get(PARAM_MTAS_HEATMAP + "." + id + "." + NAME_MTAS_HEATMAP_GRID_LEVEL, null);
        distErrPcts[tmpCounter] = rb.req.getParams()
            .get(PARAM_MTAS_HEATMAP + "." + id + "." + NAME_MTAS_HEATMAP_DIST_ERR_PCT, null);
        distErrs[tmpCounter] = rb.req.getParams().get(PARAM_MTAS_HEATMAP + "." + id + "." + NAME_MTAS_HEATMAP_DIST_ERR,
            null);
        maxCells[tmpCounter] = rb.req.getParams().get(PARAM_MTAS_HEATMAP + "." + id + "." + NAME_MTAS_HEATMAP_MAX_CELLS,
            null);
      }
      String uniqueKeyField = rb.req.getSchema().getUniqueKeyField().getName();
      mtasFields.doHeatmap = true;
      rb.setNeedDocSet(true);
      for (String field : queryFields) {
        if (field == null || field.isEmpty()) {
          throw new IOException("no (valid) field in mtas heatmap");
        } else if (!mtasFields.list.containsKey(field)) {
          mtasFields.list.put(field, new ComponentField(uniqueKeyField));
        }
      }
      MtasSolrResultUtil.compareAndCheck(keys, heatmapFields, NAME_MTAS_HEATMAP_KEY, NAME_MTAS_HEATMAP_HEATMAP_FIELD,
          true);
      MtasSolrResultUtil.compareAndCheck(keys, queryFields, NAME_MTAS_HEATMAP_KEY, NAME_MTAS_HEATMAP_QUERY_FIELD, true);
      MtasSolrResultUtil.compareAndCheck(keys, geoms, NAME_MTAS_HEATMAP_KEY, NAME_MTAS_HEATMAP_GEOM, true);
      MtasSolrResultUtil.compareAndCheck(keys, gridLevels, NAME_MTAS_HEATMAP_KEY, NAME_MTAS_HEATMAP_GRID_LEVEL, true);
      MtasSolrResultUtil.compareAndCheck(keys, distErrPcts, NAME_MTAS_HEATMAP_KEY, NAME_MTAS_HEATMAP_DIST_ERR_PCT,
          true);
      MtasSolrResultUtil.compareAndCheck(keys, distErrs, NAME_MTAS_HEATMAP_KEY, NAME_MTAS_HEATMAP_DIST_ERR, true);
      MtasSolrResultUtil.compareAndCheck(keys, maxCells, NAME_MTAS_HEATMAP_KEY, NAME_MTAS_HEATMAP_MAX_CELLS, true);
      for (int i = 0; i < heatmapFields.length; i++) {
        ComponentField cf = mtasFields.list.get(queryFields[i]);
        String key = (keys[i] == null) || (keys[i].isEmpty()) ? String.valueOf(i) + ":" + heatmapFields[i]
            : keys[i].trim();
        String heatmapField = heatmapFields[i];
        String queryField = queryFields[i];
        
        int queryNumber = queryValues[i].length;
        MtasSpanQuery[] ql = new MtasSpanQuery[queryNumber];
        for (int j = 0; j < queryNumber; j++) {
          Integer maximumIgnoreLength = (queryMaximumIgnoreLengths[i][j] == null)
              ? null : Integer.parseInt(queryMaximumIgnoreLengths[i][j]);
          MtasSpanQuery q = MtasSolrResultUtil.constructQuery(queryValues[i][j],
              queryTypes[i][j], queryPrefixes[i][j], queryVariables[i][j],
              queryFields[i], queryIgnores[i][j], maximumIgnoreLength);
          // minimize number of queries
          if (cf.spanQueryList.contains(q)) {
            q = cf.spanQueryList.get(cf.spanQueryList.indexOf(q));
          } else {
            cf.spanQueryList.add(q);
          }
          ql[j] = q;
        }
        Double minimum = (minima[i] == null) || (minima[i].isEmpty()) ? null
            : Double.parseDouble(minima[i]);
        Double maximum = (maxima[i] == null) || (maxima[i].isEmpty()) ? null
            : Double.parseDouble(maxima[i]);
        String type = (types[i] == null) || (types[i].isEmpty()) ? null
            : types[i].trim();
        String[] functionKey = functionKeys[i];
        String[] functionExpression = functionExpressions[i];
        String[] functionType = functionTypes[i];
        
        
        String geom = geoms[i];
        Integer gridLevel = gridLevels[i] != null ? Integer.parseInt(gridLevels[i]) : null;
        Double distErrPct = distErrPcts[i] != null ? Double.parseDouble(distErrPcts[i]) : null;
        Double distErr = distErrs[i] != null ? Double.parseDouble(distErrs[i]) : null;
        Integer maxCellsValue = maxCells[i] != null ? Integer.parseInt(maxCells[i]) : null;
        
        final SchemaField schemaField = rb.req.getSchema().getField(heatmapField);
        final FieldType fieldType = schemaField.getType();
        final PrefixTreeStrategy strategy;
        final DistanceUnits distanceUnits;

        // get strategy and distanceUnits
        if ((fieldType instanceof AbstractSpatialPrefixTreeFieldType)) {
          AbstractSpatialPrefixTreeFieldType<?> rptType = (AbstractSpatialPrefixTreeFieldType<?>) fieldType;
          strategy = (PrefixTreeStrategy) rptType.getStrategy(heatmapField);
          distanceUnits = rptType.getDistanceUnits();
        } else if (fieldType instanceof RptWithGeometrySpatialField) {
          RptWithGeometrySpatialField rptSdvType = (RptWithGeometrySpatialField) fieldType;
          strategy = rptSdvType.getStrategy(heatmapField).getIndexStrategy();
          distanceUnits = rptSdvType.getDistanceUnits();
        } else {
          // FYI we support the term query one too but few people use that one
          throw new SolrException(SolrException.ErrorCode.BAD_REQUEST, "heatmap field needs to be of type "
              + SpatialRecursivePrefixTreeFieldType.class + " or " + RptWithGeometrySpatialField.class);
        }
        // get context
        final SpatialContext ctx = strategy.getSpatialContext();
        // get bounds
        final Shape boundsShape = geom == null ? ctx.getWorldBounds() : SpatialUtils.parseGeomSolrException(geom, ctx);
        // get gridLevel
        final int maxGridLevel = strategy.getGrid().getMaxLevels();
        if (gridLevel != null) {
          if (gridLevel <= 0 || gridLevel > maxGridLevel) {
            throw new SolrException(SolrException.ErrorCode.BAD_REQUEST,
                NAME_MTAS_HEATMAP_GRID_LEVEL + " should be > 0 and <= " + maxGridLevel);
          }
        } else {
          // SpatialArgs has utility methods to resolve a 'distErr' from optionally set
          // distErr & distErrPct. Arguably that
          // should be refactored to feel less weird than using it like this.
          SpatialArgs spatialArgs = new SpatialArgs(SpatialOperation.Intersects/* ignored */,
              boundsShape == null ? ctx.getWorldBounds() : boundsShape);
          if (distErr != null) {
            // convert distErr units based on configured units
            spatialArgs.setDistErr(distErr * distanceUnits.multiplierFromThisUnitToDegrees());
          }
          spatialArgs.setDistErrPct(distErrPct);
          distErr = spatialArgs.resolveDistErr(ctx, DEFAULT_DIST_ERR_PCT);
          if (distErr <= 0) {
            throw new SolrException(SolrException.ErrorCode.BAD_REQUEST,
                NAME_MTAS_HEATMAP_DIST_ERR_PCT + " or " + NAME_MTAS_HEATMAP_DIST_ERR
                    + " should be > 0 or instead provide " + NAME_MTAS_HEATMAP_GRID_LEVEL + "=" + maxGridLevel
                    + " if you insist on maximum detail");
          }
          // The SPT (grid) can lookup a grid level satisfying an error distance
          // constraint
          gridLevel = strategy.getGrid().getLevelForDistance(distErr);
        }
        // add component
        try {
          mtasFields.list.get(queryFields[i]).heatmapList.add(new ComponentHeatmap(key, ql, minimum, maximum, type,
            functionKey, functionExpression, functionType,
            strategy, boundsShape, gridLevel, maxCellsValue));
        } catch (ParseException e) {
          throw new IOException(e.getMessage());
        }  
      }
    }
  }

  /* (non-Javadoc)
   * @see mtas.solr.handler.component.util.MtasSolrComponent#create(mtas.codec.util.CodecComponent.BasicComponent, java.lang.Boolean)
   */
  @Override
  public SimpleOrderedMap<Object> create(ComponentHeatmap heatmap, Boolean encode) throws IOException {
    SimpleOrderedMap<Object> mtasHeatmapResponse = new SimpleOrderedMap<>();
    mtasHeatmapResponse.add("key", heatmap.key);
    heatmap.hm.dataCollector.close();
    Map<MtasDataCollector<?, ?>, HashMap<String, MtasSolrMtasResult>> functionData = new HashMap<>();
    HashMap<String, MtasSolrMtasResult> functionResults = new HashMap<>();    
    for (SubComponentFunction function : heatmap.hm.functions) {
      function.dataCollector.close();
      functionResults.put(function.key, new MtasSolrMtasResult(function.dataCollector,
                      function.dataType,
                      function.statsType,
                      function.statsItems, null, null));
    } 
    functionData.put(heatmap.hm.dataCollector, functionResults);
    MtasSolrMtasHeatmapResult data = new MtasSolrMtasHeatmapResult(heatmap, new MtasSolrMtasResult(
        heatmap.hm.dataCollector,
        heatmap.dataType,
        heatmap.statsType,
        heatmap.statsItems,
        null, functionData));
    if (encode) {
      mtasHeatmapResponse.add("_encoded_", MtasSolrResultUtil.encode(data));
      } else {
      mtasHeatmapResponse.add("heatmap", data);
      MtasSolrResultUtil.rewrite(mtasHeatmapResponse, searchComponent);
    }
    return mtasHeatmapResponse;
  }

  @Override
  public void modifyRequest(ResponseBuilder rb, SearchComponent who, ShardRequest sreq) {
    if (sreq.params.getBool(MtasSolrSearchComponent.PARAM_MTAS, false)
        && sreq.params.getBool(PARAM_MTAS_HEATMAP, false)) {
      if ((sreq.purpose & ShardRequest.PURPOSE_GET_TOP_IDS) != 0) {
        // do nothing
      } else {
        // remove prefix for other requests
        Set<String> keys = MtasSolrResultUtil
            .getIdsFromParameters(rb.req.getParams(), PARAM_MTAS_HEATMAP);
        sreq.params.remove(PARAM_MTAS_HEATMAP);
        for (String key : keys) {
          sreq.params.remove(
              PARAM_MTAS_HEATMAP + "." + key + "." + NAME_MTAS_HEATMAP_QUERY_FIELD);
          sreq.params.remove(
              PARAM_MTAS_HEATMAP + "." + key + "." + NAME_MTAS_HEATMAP_HEATMAP_FIELD);
          sreq.params.remove(
              PARAM_MTAS_HEATMAP + "." + key + "." + NAME_MTAS_HEATMAP_GRID_LEVEL);
          sreq.params.remove(
              PARAM_MTAS_HEATMAP + "." + key + "." + NAME_MTAS_HEATMAP_GEOM);
          sreq.params.remove(
              PARAM_MTAS_HEATMAP + "." + key + "." + NAME_MTAS_HEATMAP_KEY); 
          sreq.params.remove(
              PARAM_MTAS_HEATMAP + "." + key + "." + NAME_MTAS_HEATMAP_MAX_CELLS);          
          sreq.params.remove(
              PARAM_MTAS_HEATMAP + "." + key + "." + NAME_MTAS_HEATMAP_DIST_ERR);
          sreq.params.remove(
              PARAM_MTAS_HEATMAP + "." + key + "." + NAME_MTAS_HEATMAP_DIST_ERR_PCT);
        }
      }
    }
  }

  @Override
  public void finishStage(ResponseBuilder rb) {
    if (rb.req.getParams().getBool(MtasSolrSearchComponent.PARAM_MTAS, false)
        && rb.stage >= ResponseBuilder.STAGE_EXECUTE_QUERY
        && rb.stage < ResponseBuilder.STAGE_GET_FIELDS) {
      for (ShardRequest sreq : rb.finished) {
        if (sreq.params.getBool(MtasSolrSearchComponent.PARAM_MTAS, false)
            && sreq.params.getBool(PARAM_MTAS_HEATMAP, false)) {
          for (ShardResponse shardResponse : sreq.responses) {
            NamedList<Object> response = shardResponse.getSolrResponse()
                .getResponse();
            try {
              ArrayList<NamedList<Object>> data = (ArrayList<NamedList<Object>>) response
                  .findRecursive("mtas", NAME);
              if (data != null) {
                MtasSolrResultUtil.decode(data);
              }
            } catch (ClassCastException e) {
              log.debug("Error", e);
              // shouldn't happen
            }
          }
        }
      }
    }
  }

  @Override
  public void distributedProcess(ResponseBuilder rb, ComponentFields mtasFields) throws IOException {
 // rewrite
    NamedList<Object> mtasResponse = null;
    try {
      mtasResponse = (NamedList<Object>) rb.rsp.getValues().get("mtas");
    } catch (ClassCastException e) {
      log.debug("Error", e);
      mtasResponse = null;
    }
    if (mtasResponse != null) {
      ArrayList<Object> mtasResponseHeatmap;
      try {
        mtasResponseHeatmap = (ArrayList<Object>) mtasResponse.get(NAME);
        if (mtasResponseHeatmap != null) {
          MtasSolrResultUtil.rewrite(mtasResponseHeatmap, searchComponent);
        }
      } catch (ClassCastException e) {
        log.debug("Error", e);
        mtasResponse.remove(NAME);
      }
    }
  }

  /**
   * Format counts val.
   *
   * @param format the format
   * @param columns the columns
   * @param rows the rows
   * @param counts the counts
   * @return the object
   */
  public static Object formatCountsVal(Heatmap hm) {
    return null;
  }
  

  /**
   * As ints 2 D.
   *
   * @param columns the columns
   * @param rows the rows
   * @param counts the counts
   * @return the list
   */
  static List<List<MtasDataCollector<?,?>>> as2D(final int columns, final int rows, final MtasDataCollector<?,?>[] counts) {
    return null;
  }
  
  static String asCompressed(final int columns, final int rows, final MtasDataCollector<?,?>[] counts) {
    return null;
  }
  
 

}
