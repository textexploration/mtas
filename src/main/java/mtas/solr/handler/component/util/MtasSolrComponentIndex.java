package mtas.solr.handler.component.util;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.apache.solr.common.util.NamedList;
import org.apache.solr.common.util.SimpleOrderedMap;
import org.apache.solr.handler.component.ResponseBuilder;
import org.apache.solr.handler.component.SearchComponent;
import org.apache.solr.handler.component.ShardRequest;
import mtas.search.spans.util.MtasSpanQuery;
import mtas.codec.util.CodecComponent.ComponentField;
import mtas.codec.util.CodecComponent.ComponentFields;
import mtas.codec.util.CodecComponent.ComponentIndex;
import mtas.codec.util.CodecComponent.IndexItem;
import mtas.solr.handler.component.MtasSolrSearchComponent;

// basic:
// http://localhost:8080/solr/mnCGR/select?q=*:*&mtas=true&mtas.index=true&mtas.index.0.field=text&mtas.index.0.query.type=cql&mtas.index.0.query.value=%5Bpos%3D%22ADJ%22%5D&mtas.index.0.key=adjective&fl=id&rows=2&wt=json&indent=true
//
// block.number
// http://localhost:8080/solr/mnCGR/select?q=*:*&mtas=true&mtas.index=true&mtas.index.0.field=text&mtas.index.0.query.type=cql&mtas.index.0.query.value=[pos%3D%22ADJ%22]&mtas.index.0.key=adjective&mtas.index.0.block.number=2&fl=id&rows=2&wt=json&indent=true
//
// block.size
// http://localhost:8080/solr/mnCGR/select?q=*:*&mtas=true&mtas.index=true&mtas.index.0.field=text&mtas.index.0.query.type=cql&mtas.index.0.query.value=[pos%3D%22ADJ%22]&mtas.index.0.key=adjective&mtas.index.0.block.size=100&fl=id&rows=2&wt=json&indent=true
//
// block.size and block.number : error
// http://localhost:8080/solr/mnCGR/select?q=*:*&mtas=true&mtas.index=true&mtas.index.0.field=text&mtas.index.0.query.type=cql&mtas.index.0.query.value=[pos%3D%22ADJ%22]&mtas.index.0.key=adjective&mtas.index.0.block.size=100&mtas.index.0.block.number=100&fl=id&rows=2&wt=json&indent=true
//
// block.query
// http://localhost:8080/solr/mnCGR/select?q=*:*&mtas=true&mtas.index=true&mtas.index.0.field=text&mtas.index.0.query.type=cql&mtas.index.0.query.value=%5Bpos%3D%22ADJ%22%5D&mtas.index.0.key=adjective&mtas.index.0.block.query.type=cql&mtas.index.0.block.query.value=%3Cs/%3E&fl=id&rows=2&wt=json&indent=true



/**
 * The Class MtasSolrComponentIndex.
 */
public class MtasSolrComponentIndex implements MtasSolrComponent<ComponentIndex> {

  /** The Constant log. */
  private static final Logger log = LoggerFactory.getLogger(MtasSolrComponentIndex.class);

  /** The Constant NAME. */
  public static final String NAME = "index";

  /** The search component. */
  MtasSolrSearchComponent searchComponent;

  /** The Constant PARAM_MTAS_INDEX. */
  public static final String PARAM_MTAS_INDEX = MtasSolrSearchComponent.PARAM_MTAS
      + "." + NAME;

  /** The Constant NAME_MTAS_INDEX_FIELD. */
  public static final String NAME_MTAS_INDEX_FIELD = "field";

  /** The Constant NAME_MTAS_INDEX_KEY. */
  public static final String NAME_MTAS_INDEX_KEY = "key";

  /** The Constant NAME_MTAS_INDEX_QUERY_TYPE. */
  public static final String NAME_MTAS_INDEX_QUERY_TYPE = "query.type";

  /** The Constant NAME_MTAS_INDEX_QUERY_VALUE. */
  public static final String NAME_MTAS_INDEX_QUERY_VALUE = "query.value";

  /** The Constant NAME_MTAS_INDEX_QUERY_PREFIX. */
  public static final String NAME_MTAS_INDEX_QUERY_PREFIX = "query.prefix";

  /** The Constant NAME_MTAS_INDEX_QUERY_IGNORE. */
  public static final String NAME_MTAS_INDEX_QUERY_IGNORE = "query.ignore";

  /** The Constant NAME_MTAS_INDEX_QUERY_MAXIMUM_IGNORE_LENGTH. */
  public static final String NAME_MTAS_INDEX_QUERY_MAXIMUM_IGNORE_LENGTH = "query.maximumIgnoreLength";

  /** The Constant NAME_MTAS_INDEX_QUERY_VARIABLE. */
  public static final String NAME_MTAS_INDEX_QUERY_VARIABLE = "query.variable";

  /** The Constant SUBNAME_MTAS_INDEX_QUERY_VARIABLE_NAME. */
  public static final String SUBNAME_MTAS_INDEX_QUERY_VARIABLE_NAME = "name";

  /** The Constant SUBNAME_MTAS_INDEX_QUERY_VARIABLE_VALUE. */
  public static final String SUBNAME_MTAS_INDEX_QUERY_VARIABLE_VALUE = "value";

  /** The Constant NAME_MTAS_INDEX_BLOCKSIZE. */
  public static final String NAME_MTAS_INDEX_BLOCK_SIZE = "block.size";

  /** The Constant NAME_MTAS_INDEX_BLOCKNUMBER. */
  public static final String NAME_MTAS_INDEX_BLOCK_NUMBER = "block.number";
  
  /** The Constant NAME_MTAS_INDEX_BLOCK_QUERY_TYPE. */
  public static final String NAME_MTAS_INDEX_BLOCK_QUERY_TYPE = "block.query.type";

  /** The Constant NAME_MTAS_INDEX_BLOCK_QUERY_VALUE. */
  public static final String NAME_MTAS_INDEX_BLOCK_QUERY_VALUE = "block.query.value";

  /** The Constant NAME_MTAS_INDEX_BLOCK_QUERY_PREFIX. */
  public static final String NAME_MTAS_INDEX_BLOCK_QUERY_PREFIX = "block.query.prefix";

  /** The Constant NAME_MTAS_INDEX_BLOCK_QUERY_IGNORE. */
  public static final String NAME_MTAS_INDEX_BLOCK_QUERY_IGNORE = "block.query.ignore";

  /** The Constant NAME_MTAS_INDEX_BLOCK_QUERY_MAXIMUM_IGNORE_LENGTH. */
  public static final String NAME_MTAS_INDEX_BLOCK_QUERY_MAXIMUM_IGNORE_LENGTH = "block.query.maximumIgnoreLength";

  /** The Constant NAME_MTAS_INDEX_BLOCK_QUERY_VARIABLE. */
  public static final String NAME_MTAS_INDEX_BLOCK_QUERY_VARIABLE = "block.query.variable";

  /** The Constant SUBNAME_MTAS_INDEX_BLOCK_QUERY_VARIABLE_NAME. */
  public static final String SUBNAME_MTAS_INDEX_BLOCK_QUERY_VARIABLE_NAME = "name";

  /** The Constant SUBNAME_MTAS_INDEX_BLOCK_QUERY_VARIABLE_VALUE. */
  public static final String SUBNAME_MTAS_INDEX_BLOCK_QUERY_VARIABLE_VALUE = "value";

  /** The Constant NAME_MTAS_INDEX_MATCH. */
  public static final String NAME_MTAS_INDEX_MATCH = "match";

  /** The Constant NAME_MTAS_INDEX_LIST_PREFIX. */
  public static final String NAME_MTAS_INDEX_LIST_PREFIX = "list.prefix";
  
  /** The Constant NAME_MTAS_INDEX_LIST_NUMBER. */
  public static final String NAME_MTAS_INDEX_LIST_NUMBER = "list.number";
  
  /** The Constant NAME_MTAS_INDEX_LIST_SORT. */
  public static final String NAME_MTAS_INDEX_LIST_SORT = "list.sort";

  /**
   * Instantiates a new mtas solr component index.
   *
   * @param searchComponent the search component
   */
  public MtasSolrComponentIndex(MtasSolrSearchComponent searchComponent) {
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
  public void prepare(ResponseBuilder rb, ComponentFields mtasFields)
      throws IOException {
    Set<String> ids = MtasSolrResultUtil
        .getIdsFromParameters(rb.req.getParams(), PARAM_MTAS_INDEX);
    if (!ids.isEmpty()) {
      int tmpCounter = 0;
      String[] fields = new String[ids.size()];
      String[] queryTypes = new String[ids.size()];
      String[] queryValues = new String[ids.size()];
      String[] queryPrefixes = new String[ids.size()];
      HashMap<String, String[]>[] queryVariables = new HashMap[ids.size()];
      String[] queryIgnores = new String[ids.size()];
      String[] queryMaximumIgnoreLengths = new String[ids.size()];
      String[] keys = new String[ids.size()];
      String[] blockSizes = new String[ids.size()];
      String[] blockNumbers = new String[ids.size()];
      String[] blockQueryTypes = new String[ids.size()];
      String[] blockQueryValues = new String[ids.size()];
      String[] blockQueryPrefixes = new String[ids.size()];
      HashMap<String, String[]>[] blockQueryVariables = new HashMap[ids.size()];
      String[] blockQueryIgnores = new String[ids.size()];
      String[] blockQueryMaximumIgnoreLengths = new String[ids.size()];
      String[] matches = new String[ids.size()];
      String[] listPrefixes = new String[ids.size()];
      String[] listNumbers = new String[ids.size()];
      String[] listSorts = new String[ids.size()];
      for (String id : ids) {
        fields[tmpCounter] = rb.req.getParams()
            .get(PARAM_MTAS_INDEX + "." + id + "." + NAME_MTAS_INDEX_FIELD, null);
        keys[tmpCounter] = rb.req.getParams()
            .get(PARAM_MTAS_INDEX + "." + id + "." + NAME_MTAS_INDEX_KEY,
                String.valueOf(tmpCounter))
            .trim();
        queryTypes[tmpCounter] = rb.req.getParams().get(
            PARAM_MTAS_INDEX + "." + id + "." + NAME_MTAS_INDEX_QUERY_TYPE, null);
        queryValues[tmpCounter] = rb.req.getParams().get(
            PARAM_MTAS_INDEX + "." + id + "." + NAME_MTAS_INDEX_QUERY_VALUE,
            null);
        queryPrefixes[tmpCounter] = rb.req.getParams().get(
            PARAM_MTAS_INDEX + "." + id + "." + NAME_MTAS_INDEX_QUERY_PREFIX,
            null);
        queryIgnores[tmpCounter] = rb.req.getParams().get(
            PARAM_MTAS_INDEX + "." + id + "." + NAME_MTAS_INDEX_QUERY_IGNORE,
            null);
        queryMaximumIgnoreLengths[tmpCounter] = rb.req.getParams()
            .get(PARAM_MTAS_INDEX + "." + id + "."
                + NAME_MTAS_INDEX_QUERY_MAXIMUM_IGNORE_LENGTH, null);
        Set<String> vIds = MtasSolrResultUtil.getIdsFromParameters(
            rb.req.getParams(),
            PARAM_MTAS_INDEX + "." + id + "." + NAME_MTAS_INDEX_QUERY_VARIABLE);
        queryVariables[tmpCounter] = new HashMap<>();
        if (!vIds.isEmpty()) {
          HashMap<String, ArrayList<String>> tmpVariables = new HashMap<>();
          for (String vId : vIds) {
            String name = rb.req.getParams().get(
                PARAM_MTAS_INDEX + "." + id + "." + NAME_MTAS_INDEX_QUERY_VARIABLE
                + "." + vId + "." + SUBNAME_MTAS_INDEX_QUERY_VARIABLE_NAME,
                null);
            if (name != null) {
              if (!tmpVariables.containsKey(name)) {
                tmpVariables.put(name, new ArrayList<String>());
              }
              String value = rb.req.getParams()
                  .get(PARAM_MTAS_INDEX + "." + id + "."
                      + NAME_MTAS_INDEX_QUERY_VARIABLE + "." + vId + "."
                      + SUBNAME_MTAS_INDEX_QUERY_VARIABLE_VALUE, null);
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
            queryVariables[tmpCounter].put(entry.getKey(),
                entry.getValue().toArray(new String[entry.getValue().size()]));
          }
        }
        blockSizes[tmpCounter] = rb.req.getParams().get(
            PARAM_MTAS_INDEX + "." + id + "." + NAME_MTAS_INDEX_BLOCK_SIZE, null);
        blockNumbers[tmpCounter] = rb.req.getParams()
            .get(PARAM_MTAS_INDEX + "." + id + "." + NAME_MTAS_INDEX_BLOCK_NUMBER, null);
        blockQueryTypes[tmpCounter] = rb.req.getParams().get(
            PARAM_MTAS_INDEX + "." + id + "." + NAME_MTAS_INDEX_BLOCK_QUERY_TYPE, null);
        blockQueryValues[tmpCounter] = rb.req.getParams().get(
            PARAM_MTAS_INDEX + "." + id + "." + NAME_MTAS_INDEX_BLOCK_QUERY_VALUE,
            null);
        blockQueryPrefixes[tmpCounter] = rb.req.getParams().get(
            PARAM_MTAS_INDEX + "." + id + "." + NAME_MTAS_INDEX_BLOCK_QUERY_PREFIX,
            null);
        blockQueryIgnores[tmpCounter] = rb.req.getParams().get(
            PARAM_MTAS_INDEX + "." + id + "." + NAME_MTAS_INDEX_BLOCK_QUERY_IGNORE,
            null);
        blockQueryMaximumIgnoreLengths[tmpCounter] = rb.req.getParams()
            .get(PARAM_MTAS_INDEX + "." + id + "."
                + NAME_MTAS_INDEX_QUERY_MAXIMUM_IGNORE_LENGTH, null);
        Set<String> bvIds = MtasSolrResultUtil.getIdsFromParameters(
            rb.req.getParams(),
            PARAM_MTAS_INDEX + "." + id + "." + NAME_MTAS_INDEX_BLOCK_QUERY_VARIABLE);
        blockQueryVariables[tmpCounter] = new HashMap<>();
        if (!bvIds.isEmpty()) {
          HashMap<String, ArrayList<String>> tmpVariables = new HashMap<>();
          for (String bvId : bvIds) {
            String name = rb.req.getParams().get(
                PARAM_MTAS_INDEX + "." + id + "." + NAME_MTAS_INDEX_BLOCK_QUERY_VARIABLE
                + "." + bvId + "." + SUBNAME_MTAS_INDEX_BLOCK_QUERY_VARIABLE_NAME,
                null);
            if (name != null) {
              if (!tmpVariables.containsKey(name)) {
                tmpVariables.put(name, new ArrayList<String>());
              }
              String value = rb.req.getParams()
                  .get(PARAM_MTAS_INDEX + "." + id + "."
                      + NAME_MTAS_INDEX_BLOCK_QUERY_VARIABLE + "." + bvId + "."
                      + SUBNAME_MTAS_INDEX_BLOCK_QUERY_VARIABLE_VALUE, null);
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
            blockQueryVariables[tmpCounter].put(entry.getKey(),
                entry.getValue().toArray(new String[entry.getValue().size()]));
          }
        }
        matches[tmpCounter] = rb.req.getParams().get(
            PARAM_MTAS_INDEX + "." + id + "." + NAME_MTAS_INDEX_MATCH, null);
        listPrefixes[tmpCounter] = rb.req.getParams().get(
            PARAM_MTAS_INDEX + "." + id + "." + NAME_MTAS_INDEX_LIST_PREFIX, null);
        listNumbers[tmpCounter] = rb.req.getParams().get(
            PARAM_MTAS_INDEX + "." + id + "." + NAME_MTAS_INDEX_LIST_NUMBER, null);
        listSorts[tmpCounter] = rb.req.getParams().get(
            PARAM_MTAS_INDEX + "." + id + "." + NAME_MTAS_INDEX_LIST_SORT, null);
        tmpCounter++;
      }
      String uniqueKeyField = rb.req.getSchema().getUniqueKeyField().getName();
      mtasFields.doIndex = true;
      rb.setNeedDocSet(true);
      for (String field : fields) {
        if (field == null || field.isEmpty()) {
          throw new IOException("no (valid) field in mtas list");
        } else if (!mtasFields.list.containsKey(field)) {
          mtasFields.list.put(field, new ComponentField(uniqueKeyField));
        }
      }
      MtasSolrResultUtil.compareAndCheck(keys, fields, NAME_MTAS_INDEX_KEY,
          NAME_MTAS_INDEX_FIELD, true);
      MtasSolrResultUtil.compareAndCheck(keys, queryValues,
          NAME_MTAS_INDEX_KEY, NAME_MTAS_INDEX_QUERY_VALUE, false);
      MtasSolrResultUtil.compareAndCheck(keys, queryTypes,
          NAME_MTAS_INDEX_KEY, NAME_MTAS_INDEX_QUERY_TYPE, false);
      MtasSolrResultUtil.compareAndCheck(keys, queryPrefixes,
          NAME_MTAS_INDEX_KEY, NAME_MTAS_INDEX_QUERY_PREFIX, false);
      MtasSolrResultUtil.compareAndCheck(keys, queryIgnores,
          NAME_MTAS_INDEX_KEY, NAME_MTAS_INDEX_QUERY_IGNORE, false);
      MtasSolrResultUtil.compareAndCheck(keys, queryMaximumIgnoreLengths,
          NAME_MTAS_INDEX_KEY, NAME_MTAS_INDEX_QUERY_MAXIMUM_IGNORE_LENGTH, 
          false);
      MtasSolrResultUtil.compareAndCheck(keys, blockSizes, NAME_MTAS_INDEX_KEY, NAME_MTAS_INDEX_BLOCK_SIZE,
          false);
      MtasSolrResultUtil.compareAndCheck(keys, blockNumbers, NAME_MTAS_INDEX_KEY, NAME_MTAS_INDEX_BLOCK_NUMBER,
          false);
      MtasSolrResultUtil.compareAndCheck(keys, blockQueryValues,
          NAME_MTAS_INDEX_KEY, NAME_MTAS_INDEX_BLOCK_QUERY_VALUE, false);
      MtasSolrResultUtil.compareAndCheck(keys, blockQueryTypes,
          NAME_MTAS_INDEX_KEY, NAME_MTAS_INDEX_BLOCK_QUERY_TYPE, false);
      MtasSolrResultUtil.compareAndCheck(keys, blockQueryPrefixes,
          NAME_MTAS_INDEX_KEY, NAME_MTAS_INDEX_BLOCK_QUERY_PREFIX, false);
      MtasSolrResultUtil.compareAndCheck(keys, blockQueryIgnores,
          NAME_MTAS_INDEX_KEY, NAME_MTAS_INDEX_BLOCK_QUERY_IGNORE, false);
      MtasSolrResultUtil.compareAndCheck(keys, blockQueryMaximumIgnoreLengths,
          NAME_MTAS_INDEX_KEY, NAME_MTAS_INDEX_BLOCK_QUERY_MAXIMUM_IGNORE_LENGTH, 
          false);
      MtasSolrResultUtil.compareAndCheck(keys, matches,
          NAME_MTAS_INDEX_KEY, NAME_MTAS_INDEX_MATCH, false);
      MtasSolrResultUtil.compareAndCheck(keys, listPrefixes,
          NAME_MTAS_INDEX_KEY, NAME_MTAS_INDEX_LIST_PREFIX, false);
      MtasSolrResultUtil.compareAndCheck(keys, listNumbers,
          NAME_MTAS_INDEX_KEY, NAME_MTAS_INDEX_LIST_NUMBER, false);
      MtasSolrResultUtil.compareAndCheck(keys, listSorts,
          NAME_MTAS_INDEX_KEY, NAME_MTAS_INDEX_LIST_SORT, false);
      for (int i = 0; i < fields.length; i++) {
        ComponentField cf = mtasFields.list.get(fields[i]);
        Integer maximumIgnoreLength = (queryMaximumIgnoreLengths[i] == null)
            ? null : Integer.parseInt(queryMaximumIgnoreLengths[i]);
        MtasSpanQuery q = MtasSolrResultUtil.constructQuery(queryValues[i],
            queryTypes[i], queryPrefixes[i], queryVariables[i], fields[i],
            queryIgnores[i], maximumIgnoreLength);
        // minimize number of queries
        if (cf.spanQueryList.contains(q)) {
          q = cf.spanQueryList.get(cf.spanQueryList.indexOf(q));
        } else {
          cf.spanQueryList.add(q);
        }
        String key = (keys[i] == null) || (keys[i].isEmpty())
            ? String.valueOf(i) + ":" + fields[i] + ":" + queryValues[i] + ":"
            + queryPrefixes[i]
                : keys[i].trim();
            int blockSize = (blockSizes[i]==null || blockSizes[i].isEmpty()) ? 0 : Integer.parseInt(blockSizes[i]);
            int blockNumber = (blockNumbers[i]==null || blockNumbers[i].isEmpty()) ? 0 : Integer.parseInt(blockNumbers[i]);
            MtasSpanQuery blockQuery;
            if(blockQueryTypes[i]==null || blockQueryTypes[i].isEmpty()) {
              blockQuery = null;
            } else {
              Integer maximumIgnoreLengthBlock = (blockQueryMaximumIgnoreLengths[i] == null)
                  ? null : Integer.parseInt(blockQueryMaximumIgnoreLengths[i]);
              blockQuery= MtasSolrResultUtil.constructQuery(blockQueryValues[i],
                  blockQueryTypes[i], blockQueryPrefixes[i], blockQueryVariables[i], fields[i],
                  blockQueryIgnores[i], maximumIgnoreLengthBlock);
              if (cf.spanQueryList.contains(blockQuery)) {
                blockQuery = cf.spanQueryList.get(cf.spanQueryList.indexOf(blockQuery));
              } else {
                cf.spanQueryList.add(blockQuery);
              }
            }
            Integer listNumber = (listNumbers[i]==null || listNumbers[i].isEmpty()) ? null : Integer.parseInt(listNumbers[i]);
            mtasFields.list.get(fields[i]).indexList.add(new ComponentIndex(q,
                key, blockSize, blockNumber, blockQuery, matches[i], listPrefixes[i], listNumber, listSorts[i]));
      }
    }
  }

  /*
   * (non-Javadoc)
   * 
   * @see
   * mtas.solr.handler.component.util.MtasSolrComponent#modifyRequest(org.apache
   * .solr.handler.component.ResponseBuilder,
   * org.apache.solr.handler.component.SearchComponent,
   * org.apache.solr.handler.component.ShardRequest)
   */
  public void modifyRequest(ResponseBuilder rb, SearchComponent who,
      ShardRequest sreq) {
    if (sreq.params.getBool(MtasSolrSearchComponent.PARAM_MTAS, false)) {
      if (sreq.params.getBool(PARAM_MTAS_INDEX, false)
          && (sreq.purpose & ShardRequest.PURPOSE_GET_FIELDS) != 0) {
        // do nothing
      } else {
        Set<String> keys = MtasSolrResultUtil
            .getIdsFromParameters(rb.req.getParams(), PARAM_MTAS_INDEX);
        sreq.params.remove(PARAM_MTAS_INDEX);
        for (String key : keys) {
          sreq.params
          .remove(PARAM_MTAS_INDEX + "." + key + "." + NAME_MTAS_INDEX_FIELD);
          sreq.params
          .remove(PARAM_MTAS_INDEX + "." + key + "." + NAME_MTAS_INDEX_KEY);
          sreq.params.remove(
              PARAM_MTAS_INDEX + "." + key + "." + NAME_MTAS_INDEX_QUERY_VALUE);
          sreq.params.remove(
              PARAM_MTAS_INDEX + "." + key + "." + NAME_MTAS_INDEX_QUERY_TYPE);
          sreq.params.remove(
              PARAM_MTAS_INDEX + "." + key + "." + NAME_MTAS_INDEX_QUERY_PREFIX);
          sreq.params.remove(
              PARAM_MTAS_INDEX + "." + key + "." + NAME_MTAS_INDEX_QUERY_IGNORE);
          sreq.params.remove(PARAM_MTAS_INDEX + "." + key + "."
              + NAME_MTAS_INDEX_QUERY_MAXIMUM_IGNORE_LENGTH);
          Set<String> subKeys = MtasSolrResultUtil
              .getIdsFromParameters(rb.req.getParams(), PARAM_MTAS_INDEX + "."
                  + key + "." + NAME_MTAS_INDEX_QUERY_VARIABLE);
          for (String subKey : subKeys) {
            sreq.params.remove(PARAM_MTAS_INDEX + "." + key + "."
                + NAME_MTAS_INDEX_QUERY_VARIABLE + "." + subKey + "."
                + SUBNAME_MTAS_INDEX_QUERY_VARIABLE_NAME);
            sreq.params.remove(PARAM_MTAS_INDEX + "." + key + "."
                + NAME_MTAS_INDEX_QUERY_VARIABLE + "." + subKey + "."
                + SUBNAME_MTAS_INDEX_QUERY_VARIABLE_VALUE);
          }
          sreq.params
          .remove(PARAM_MTAS_INDEX + "." + key + "." + NAME_MTAS_INDEX_KEY);
          sreq.params.remove(
              PARAM_MTAS_INDEX + "." + key + "." + NAME_MTAS_INDEX_BLOCK_NUMBER);
          sreq.params
          .remove(PARAM_MTAS_INDEX + "." + key + "." + NAME_MTAS_INDEX_BLOCK_SIZE);
          sreq.params
          .remove(PARAM_MTAS_INDEX + "." + key + "." + NAME_MTAS_INDEX_BLOCK_QUERY_VALUE);
          sreq.params
          .remove(PARAM_MTAS_INDEX + "." + key + "." + NAME_MTAS_INDEX_BLOCK_QUERY_TYPE);
          sreq.params
          .remove(PARAM_MTAS_INDEX + "." + key + "." + NAME_MTAS_INDEX_BLOCK_QUERY_PREFIX);
          sreq.params
          .remove(PARAM_MTAS_INDEX + "." + key + "." + NAME_MTAS_INDEX_BLOCK_QUERY_IGNORE);
          sreq.params.remove(PARAM_MTAS_INDEX + "." + key + "."
              + NAME_MTAS_INDEX_BLOCK_QUERY_MAXIMUM_IGNORE_LENGTH);
          Set<String> subBlockKeys = MtasSolrResultUtil
              .getIdsFromParameters(rb.req.getParams(), PARAM_MTAS_INDEX + "."
                  + key + "." + NAME_MTAS_INDEX_BLOCK_QUERY_VARIABLE);
          for (String subBlockKey : subBlockKeys) {
            sreq.params.remove(PARAM_MTAS_INDEX + "." + key + "."
                + NAME_MTAS_INDEX_BLOCK_QUERY_VARIABLE + "." + subBlockKey + "."
                + SUBNAME_MTAS_INDEX_BLOCK_QUERY_VARIABLE_NAME);
            sreq.params.remove(PARAM_MTAS_INDEX + "." + key + "."
                + NAME_MTAS_INDEX_BLOCK_QUERY_VARIABLE + "." + subBlockKey + "."
                + SUBNAME_MTAS_INDEX_QUERY_VARIABLE_VALUE);
          }
          sreq.params
          .remove(PARAM_MTAS_INDEX + "." + key + "." + NAME_MTAS_INDEX_MATCH);
          sreq.params
          .remove(PARAM_MTAS_INDEX + "." + key + "." + NAME_MTAS_INDEX_LIST_PREFIX);
          sreq.params
          .remove(PARAM_MTAS_INDEX + "." + key + "." + NAME_MTAS_INDEX_LIST_NUMBER);
          sreq.params
          .remove(PARAM_MTAS_INDEX + "." + key + "." + NAME_MTAS_INDEX_LIST_SORT);
        }
      }
    }
  }

  /*
   * (non-Javadoc)
   * 
   * @see
   * mtas.solr.handler.component.util.MtasSolrComponent#distributedProcess(org.
   * apache.solr.handler.component.ResponseBuilder,
   * mtas.codec.util.CodecComponent.ComponentFields)
   */
  @SuppressWarnings("unchecked")
  public void distributedProcess(ResponseBuilder rb,
      ComponentFields mtasFields) {
    //nothing to do
  }

  /*
   * (non-Javadoc)
   * 
   * @see
   * mtas.solr.handler.component.util.MtasSolrComponent#create(mtas.codec.util.
   * CodecComponent.BasicComponent, java.lang.Boolean)
   */
  public SimpleOrderedMap<Object> create(ComponentIndex index, Boolean encode) {
    SimpleOrderedMap<Object> mtasIndexResponse = new SimpleOrderedMap<>();
    mtasIndexResponse.add("key", index.key);  
    ArrayList<NamedList<Object>> mtasIndexItemResponses = new ArrayList<>();
    for (int docId : index.uniqueKey.keySet()) {
      NamedList<Object> mtasIndexItemResponse = new SimpleOrderedMap<>();
      mtasIndexItemResponse.add("documentKey", index.uniqueKey.get(docId));
      mtasIndexItemResponse.add("documentMinPosition",
          index.minPosition.get(docId));
      mtasIndexItemResponse.add("documentMaxPosition",
          index.maxPosition.get(docId));
      List<SimpleOrderedMap> dataList = new ArrayList<SimpleOrderedMap>();
      //loop over hits to get totals
      Map<List<Map<String, Set<String>>>,Integer> documentHitTotal = new HashMap<>();
      Map<List<Map<String, Set<String>>>,Integer> documentHitBlockTotal = new HashMap<>();
      for(IndexItem indexItem : index.indexItems.get(docId)) {
        for(Entry<List<Map<String, Set<String>>>,Integer> entry : indexItem.list.entrySet()) {
          if(documentHitTotal.containsKey(entry.getKey())) {
            documentHitTotal.put(entry.getKey(), documentHitTotal.get(entry.getKey()) + entry.getValue());
            documentHitBlockTotal.put(entry.getKey(), documentHitBlockTotal.get(entry.getKey()) + 1);
          } else {
            documentHitTotal.put(entry.getKey(), entry.getValue());
            documentHitBlockTotal.put(entry.getKey(), 1);
          }
        }
      }  
      for(IndexItem indexItem : index.indexItems.get(docId)) {
        SimpleOrderedMap<Object> dataListItem = new SimpleOrderedMap<>();
        dataListItem.add("positionStart", indexItem.startPosition);
        dataListItem.add("positionEnd", indexItem.endPosition);
        dataListItem.add("number", indexItem.number);
        if(index.listPrefixes.size()>0) {
          List<SimpleOrderedMap> dataListItemList = new ArrayList<SimpleOrderedMap>();
          //loop over hits
          for(Entry<List<Map<String, Set<String>>>,Integer> entry : indexItem.list.entrySet()) {
            SimpleOrderedMap<Object> dataListItemListItem = new SimpleOrderedMap<>();
            List<Map<String, Set<String>>> entryKey = entry.getKey();
            //construct hit, looping over position values
            Map<String,Map> dataListItemListHit = new HashMap<String,Map>();
            for(int i=0;i<entryKey.size();i++) {
              dataListItemListHit.put(String.valueOf(i),entryKey.get(i));
            }
            dataListItemListItem.add("number", entry.getValue());
            dataListItemListItem.add("total", documentHitTotal.get(entry.getKey()));
            dataListItemListItem.add("tfibf", 1.0*entry.getValue()/documentHitBlockTotal.get(entry.getKey()));
            dataListItemListItem.add("hit", dataListItemListHit);
            dataListItemList.add(dataListItemListItem);
          }
          //sort
          if(index.listSort.equals(ComponentIndex.INDEX_LIST_SORT_COUNT)) {
            dataListItemList.sort(new Comparator<SimpleOrderedMap>() {
              @Override
              public int compare(SimpleOrderedMap m1, SimpleOrderedMap m2) {
                Integer n1 =  (Integer) m1.get("number");
                Integer n2 =  (Integer) m2.get("number");
                return n2.compareTo(n1);
              }
            });
          } else if(index.listSort.equals(ComponentIndex.INDEX_LIST_SORT_TFIBF)) {
            dataListItemList.sort(new Comparator<SimpleOrderedMap>() {
              @Override
              public int compare(SimpleOrderedMap m1, SimpleOrderedMap m2) {
                Double d1 =  (Double) m1.get("tfibf");
                Double d2 =  (Double) m2.get("tfibf");
                return d2.compareTo(d1);
              }
            });
          }  
          //limit
          if(index.listNumber!=null && index.listNumber<dataListItemList.size()) {
            dataListItem.add("list", dataListItemList.subList(0, index.listNumber));
          } else {
            dataListItem.add("list", dataListItemList);
          }
        }  
        dataList.add(dataListItem);
      }
      mtasIndexItemResponse.add("data", dataList);
      mtasIndexItemResponses.add(mtasIndexItemResponse);
    }  
    mtasIndexResponse.add("list", mtasIndexItemResponses);
    return mtasIndexResponse;
  }

  /*
   * (non-Javadoc)
   * 
   * @see
   * mtas.solr.handler.component.util.MtasSolrComponent#finishStage(org.apache.
   * solr.handler.component.ResponseBuilder)
   */
  public void finishStage(ResponseBuilder rb) {
    if (rb.req.getParams().getBool(MtasSolrSearchComponent.PARAM_MTAS, false)
        && rb.stage >= ResponseBuilder.STAGE_EXECUTE_QUERY
        && rb.stage < ResponseBuilder.STAGE_GET_FIELDS) {
      for (ShardRequest sreq : rb.finished) {
        if (sreq.params.getBool(MtasSolrSearchComponent.PARAM_MTAS, false)
            && sreq.params.getBool(PARAM_MTAS_INDEX, false)) {
          // nothing to do
        }
      }
    }
  }

}
