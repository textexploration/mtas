package mtas.solr.handler.component.util;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.Map.Entry;

import org.apache.solr.common.util.NamedList;
import org.apache.solr.common.util.SimpleOrderedMap;
import org.apache.solr.handler.component.ResponseBuilder;
import org.apache.solr.handler.component.SearchComponent;
import org.apache.solr.handler.component.ShardRequest;

import mtas.codec.util.CodecComponent.ComponentField;
import mtas.codec.util.CodecComponent.ComponentFields;
import mtas.codec.util.CodecComponent.ComponentPage;
import mtas.codec.util.CodecComponent.PageRange;
import mtas.codec.util.CodecComponent.PageRangeData;
import mtas.codec.util.CodecComponent.PageSet;
import mtas.codec.util.CodecComponent.PageSetData;
import mtas.codec.util.CodecComponent.PageWord;
import mtas.codec.util.CodecComponent.PageWordData;
import mtas.solr.handler.component.MtasSolrSearchComponent;

/**
 * The Class MtasSolrComponentPage.
 */
public class MtasSolrComponentPage implements MtasSolrComponent<ComponentPage> {
  
  /** The Constant NAME. */
  public static final String NAME = "page";

  /** The Constant PARAM_MTAS_LIST. */
  public static final String PARAM_MTAS_PAGE = MtasSolrSearchComponent.PARAM_MTAS
      + "." + NAME;
  
  /** The Constant NAME_MTAS_LIST_FIELD. */
  public static final String NAME_MTAS_PAGE_FIELD = "field";

  /** The Constant NAME_MTAS_LIST_KEY. */
  public static final String NAME_MTAS_PAGE_KEY = "key";

  /** The Constant NAME_MTAS_LIST_PREFIX. */
  public static final String NAME_MTAS_PAGE_PREFIX = "prefix";

  /** The Constant NAME_MTAS_LIST_START. */
  public static final String NAME_MTAS_PAGE_START = "start";

  /** The Constant NAME_MTAS_LIST_NUMBER. */
  public static final String NAME_MTAS_PAGE_END = "end";

  /**
   * Instantiates a new mtas solr component page.
   *
   * @param searchComponent the search component
   */
  public MtasSolrComponentPage(MtasSolrSearchComponent searchComponent) {
  }
  
  /* (non-Javadoc)
   * @see mtas.solr.handler.component.util.MtasSolrComponent#prepare(org.apache.solr.handler.component.ResponseBuilder, mtas.codec.util.CodecComponent.ComponentFields)
   */
  @Override
  public void prepare(ResponseBuilder rb, ComponentFields mtasFields)
      throws IOException {
    Set<String> ids = MtasSolrResultUtil
        .getIdsFromParameters(rb.req.getParams(), PARAM_MTAS_PAGE);
    if (!ids.isEmpty()) {
      int tmpCounter = 0;
      String[] fields = new String[ids.size()];
      String[] keys = new String[ids.size()];
      String[] prefixes = new String[ids.size()];
      String[] starts = new String[ids.size()];
      String[] ends = new String[ids.size()];
      for (String id : ids) {
        fields[tmpCounter] = rb.req.getParams()
            .get(PARAM_MTAS_PAGE + "." + id + "." + NAME_MTAS_PAGE_FIELD, null);
        keys[tmpCounter] = rb.req.getParams()
            .get(PARAM_MTAS_PAGE + "." + id + "." + NAME_MTAS_PAGE_KEY,
                String.valueOf(tmpCounter))
            .trim();
        prefixes[tmpCounter] = rb.req.getParams().get(
            PARAM_MTAS_PAGE + "." + id + "." + NAME_MTAS_PAGE_PREFIX, null);
        starts[tmpCounter] = rb.req.getParams()
            .get(PARAM_MTAS_PAGE + "." + id + "." + NAME_MTAS_PAGE_START, null);
        ends[tmpCounter] = rb.req.getParams()
            .get(PARAM_MTAS_PAGE + "." + id + "." + NAME_MTAS_PAGE_END, null);
        tmpCounter++;
      }
      String uniqueKeyField = rb.req.getSchema().getUniqueKeyField().getName();
      mtasFields.doPage = true;
      rb.setNeedDocSet(true);
      for (String field : fields) {
        if (field == null || field.isEmpty()) {
          throw new IOException("no (valid) field in mtas page");
        } else if (!mtasFields.list.containsKey(field)) {
          mtasFields.list.put(field, new ComponentField(uniqueKeyField));
        }
      }
      MtasSolrResultUtil.compareAndCheck(keys, fields, NAME_MTAS_PAGE_KEY,
          NAME_MTAS_PAGE_FIELD, true);
      MtasSolrResultUtil.compareAndCheck(prefixes, fields,
          NAME_MTAS_PAGE_PREFIX, NAME_MTAS_PAGE_FIELD, false);
      MtasSolrResultUtil.compareAndCheck(starts, fields, NAME_MTAS_PAGE_START,
          NAME_MTAS_PAGE_FIELD, false);
      MtasSolrResultUtil.compareAndCheck(ends, fields, NAME_MTAS_PAGE_END,
          NAME_MTAS_PAGE_FIELD, false);
      for (int i = 0; i < fields.length; i++) {
        String key = (keys[i] == null) || (keys[i].isEmpty())
            ? String.valueOf(i) + ":" + fields[i] 
            : keys[i].trim();
        String prefix = prefixes[i];
        int start = (starts[i] == null) || (starts[i].isEmpty()) ? 0
            : Integer.parseInt(starts[i]);
        int end = (starts[i] == null) || (ends[i].isEmpty()) ? 0
            : Integer.parseInt(ends[i]);
        mtasFields.list.get(fields[i]).pageList.add(new ComponentPage(
            fields[i], key, prefix, start, end));
      }
    }
  }

  /* (non-Javadoc)
   * @see mtas.solr.handler.component.util.MtasSolrComponent#create(mtas.codec.util.CodecComponent.BasicComponent, java.lang.Boolean)
   */
  @Override
  public SimpleOrderedMap<Object> create(ComponentPage page, Boolean encode) throws IOException {
    SimpleOrderedMap<Object> mtasPageResponse = new SimpleOrderedMap<>();
    mtasPageResponse.add("key", page.key);
    ArrayList<NamedList<Object>> mtasPageItemResponses = new ArrayList<>();
    for (int docId : page.uniqueKey.keySet()) {
      NamedList<Object> mtasPageItemResponse = new SimpleOrderedMap<>();
      mtasPageItemResponse.add("documentKey", page.uniqueKey.get(docId));
      mtasPageItemResponse.add("documentMinPosition",
          page.minPosition.get(docId));
      mtasPageItemResponse.add("documentMaxPosition",
          page.maxPosition.get(docId));
      NamedList<Object> mtasPageItemData = new SimpleOrderedMap<>();
      //words
      if(page.wordList.containsKey(docId)) {
        Map<Integer, PageWordData> wordList = page.wordList.get(docId);
        SimpleOrderedMap<Object> wordListResult = new SimpleOrderedMap<>();
        for(Entry<Integer, PageWordData> entry : wordList.entrySet()) {
          List<List<Object>> wordPositionResult = new ArrayList<>();
          for(PageWord item: entry.getValue().words) {
            List<Object> itemResult = new ArrayList<>();
            itemResult.add(item.id);
            itemResult.add(item.prefix);
            if((item.postfix!=null && item.postfix.length()>0) || item.parentId!=null) {
              itemResult.add(item.postfix);
              if(item.parentId!=null) {
                itemResult.add(item.parentId);
              }
            }  
            wordPositionResult.add(itemResult);
          }
          wordListResult.add(Integer.toString(entry.getKey()), wordPositionResult);
        }
        mtasPageItemData.add("word", wordListResult);
      }  
      //ranges
      if(page.rangeList.containsKey(docId)) {
        Map<Integer, PageRangeData> rangeList = page.rangeList.get(docId);
        SimpleOrderedMap<Object> rangeListResult = new SimpleOrderedMap<>();
        for(Entry<Integer, PageRangeData> entry : rangeList.entrySet()) {
          List<List<Object>> rangePositionResult = new ArrayList<>();
          for(PageRange item: entry.getValue().ranges) {
            List<Object> itemResult = new ArrayList<>();
            itemResult.add(item.id);
            itemResult.add(Arrays.asList(item.start, item.end));
            itemResult.add(item.prefix);
            if((item.postfix!=null && item.postfix.length()>0) || item.parentId!=null) {
              itemResult.add(item.postfix);
              if(item.parentId!=null) {
                itemResult.add(item.parentId);
              }
            }  
            rangePositionResult.add(itemResult);
          }
          rangeListResult.add(Integer.toString(entry.getKey()), rangePositionResult);
        }
        mtasPageItemData.add("range", rangeListResult);
      }
      //sets 
      if(page.setList.containsKey(docId)) {
        Map<Integer, PageSetData> setList = page.setList.get(docId);
        SimpleOrderedMap<Object> setListResult = new SimpleOrderedMap<>();
        for(Entry<Integer, PageSetData> entry : setList.entrySet()) {
          List<List<Object>> setPositionResult = new ArrayList<>();
          for(PageSet item: entry.getValue().sets) {
            List<Object> itemResult = new ArrayList<>();
            itemResult.add(item.id);
            if(item.positions!=null) {
              itemResult.add(Arrays.stream(item.positions)
                  .boxed()
                  .collect(Collectors.toList()));
            } else {
              itemResult.add(null);
            }
            itemResult.add(item.prefix);
            if((item.postfix!=null && item.postfix.length()>0) || item.parentId!=null) {
              itemResult.add(item.postfix);
              if(item.parentId!=null) {
                itemResult.add(item.parentId);
              }
            }  
            setPositionResult.add(itemResult);
          }
          setListResult.add(Integer.toString(entry.getKey()), setPositionResult);
        }
        mtasPageItemData.add("set", setListResult);
      }
      mtasPageItemResponse.add("data",
          mtasPageItemData);
      mtasPageItemResponses.add(mtasPageItemResponse);
    }
    mtasPageResponse.add("list", mtasPageItemResponses);
    return mtasPageResponse;
  }

  /* (non-Javadoc)
   * @see mtas.solr.handler.component.util.MtasSolrComponent#modifyRequest(org.apache.solr.handler.component.ResponseBuilder, org.apache.solr.handler.component.SearchComponent, org.apache.solr.handler.component.ShardRequest)
   */
  @Override
  public void modifyRequest(ResponseBuilder rb, SearchComponent who, ShardRequest sreq) {
    if (sreq.params.getBool(MtasSolrSearchComponent.PARAM_MTAS, false)) {
      if (sreq.params.getBool(PARAM_MTAS_PAGE, false)
          && (sreq.purpose & ShardRequest.PURPOSE_GET_FIELDS) != 0) {
        // do nothing
      } else {
        Set<String> keys = MtasSolrResultUtil
            .getIdsFromParameters(rb.req.getParams(), PARAM_MTAS_PAGE);
        sreq.params.remove(PARAM_MTAS_PAGE);
        for (String key : keys) {
          sreq.params
              .remove(PARAM_MTAS_PAGE + "." + key + "." + NAME_MTAS_PAGE_FIELD);
          sreq.params
              .remove(PARAM_MTAS_PAGE + "." + key + "." + NAME_MTAS_PAGE_KEY);
          sreq.params.remove(
              PARAM_MTAS_PAGE + "." + key + "." + NAME_MTAS_PAGE_PREFIX);
          sreq.params.remove(
              PARAM_MTAS_PAGE + "." + key + "." + NAME_MTAS_PAGE_START);
          sreq.params.remove(
              PARAM_MTAS_PAGE + "." + key + "." + NAME_MTAS_PAGE_END);
        }
      }
    }
  }

  /* (non-Javadoc)
   * @see mtas.solr.handler.component.util.MtasSolrComponent#finishStage(org.apache.solr.handler.component.ResponseBuilder)
   */
  @Override
  public void finishStage(ResponseBuilder rb) {
    if (rb.req.getParams().getBool(MtasSolrSearchComponent.PARAM_MTAS, false)
        && rb.stage >= ResponseBuilder.STAGE_EXECUTE_QUERY
        && rb.stage < ResponseBuilder.STAGE_GET_FIELDS) {
      for (ShardRequest sreq : rb.finished) {
        if (sreq.params.getBool(MtasSolrSearchComponent.PARAM_MTAS, false)
            && sreq.params.getBool(PARAM_MTAS_PAGE, false)) {
          // nothing to do
        }
      }
    }
  }

  /* (non-Javadoc)
   * @see mtas.solr.handler.component.util.MtasSolrComponent#distributedProcess(org.apache.solr.handler.component.ResponseBuilder, mtas.codec.util.CodecComponent.ComponentFields)
   */
  @Override
  public void distributedProcess(ResponseBuilder rb, ComponentFields mtasFields) throws IOException {
    // nothing to do
  }

}
