package mtas.solr.search;

import java.io.BufferedReader;
import java.io.Reader;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;

import mtas.parser.simple.MtasSimpleParser;
import mtas.search.spans.MtasSpanOrQuery;
import mtas.search.spans.util.MtasSpanQuery;

import org.apache.lucene.search.BooleanClause;
import org.apache.lucene.search.BooleanQuery;
import org.apache.lucene.search.Query;
import org.apache.solr.common.params.SolrParams;
import org.apache.solr.request.SolrQueryRequest;
import org.apache.solr.search.QParser;
import org.apache.solr.search.SyntaxError;

/**
 * The Class MtasSimpleQParser.
 */
public class MtasSimpleQParser extends QParser {

  /** The Constant MTAS_SIMPLE_QPARSER_FIELD. */
  public static final String MTAS_SIMPLE_QPARSER_FIELD = "field";

  /** The Constant MTAS_SIMPLE_QPARSER_QUERY. */
  public static final String MTAS_SIMPLE_QPARSER_QUERY = "query";

  /** The Constant MTAS_SIMPLE_QPARSER_IGNORE. */
  public static final String MTAS_SIMPLE_QPARSER_IGNORE = "ignore";

  /** The Constant MTAS_SIMPLE_QPARSER_MAXIMUM_IGNORE_LENGTH. */
  public static final String MTAS_SIMPLE_QPARSER_MAXIMUM_IGNORE_LENGTH = "maximumIgnoreLength";

  /** The Constant MTAS_SIMPLE_QPARSER_PREFIX. */
  public static final String MTAS_SIMPLE_QPARSER_PREFIX = "prefix";

  /** The field. */
  String field = null;

  /** The query. */
  String simpleQuery = null;

  /** The ignore query. */
  String ignoreQuery = null;

  /** The maximum ignore length. */
  Integer maximumIgnoreLength = null;

  /** The default prefix. */
  String defaultPrefix = null;

  /**
   * Instantiates a new mtas SimpleQ parser.
   *
   * @param qstr the qstr
   * @param localParams the local params
   * @param params the params
   * @param req the req
   */
  public MtasSimpleQParser(String qstr, SolrParams localParams, SolrParams params,
      SolrQueryRequest req) {
    super(qstr, localParams, params, req);
    if ((localParams.getParams(MTAS_SIMPLE_QPARSER_FIELD) != null)
        && (localParams.getParams(MTAS_SIMPLE_QPARSER_FIELD).length == 1)) {
      field = localParams.getParams(MTAS_SIMPLE_QPARSER_FIELD)[0];
    }
    if ((localParams.getParams(MTAS_SIMPLE_QPARSER_QUERY) != null)
        && (localParams.getParams(MTAS_SIMPLE_QPARSER_QUERY).length == 1)) {
      simpleQuery = localParams.getParams(MTAS_SIMPLE_QPARSER_QUERY)[0];
    }
    if ((localParams.getParams(MTAS_SIMPLE_QPARSER_IGNORE) != null)
        && (localParams.getParams(MTAS_SIMPLE_QPARSER_IGNORE).length == 1)) {
      ignoreQuery = localParams.getParams(MTAS_SIMPLE_QPARSER_IGNORE)[0];
    }
    if ((localParams.getParams(MTAS_SIMPLE_QPARSER_MAXIMUM_IGNORE_LENGTH) != null)
        && (localParams
            .getParams(MTAS_SIMPLE_QPARSER_MAXIMUM_IGNORE_LENGTH).length == 1)) {
      try {
        maximumIgnoreLength = Integer.parseInt(
            localParams.getParams(MTAS_SIMPLE_QPARSER_MAXIMUM_IGNORE_LENGTH)[0]);
      } catch (NumberFormatException e) {
        maximumIgnoreLength = null;
      }
    }
    if ((localParams.getParams(MTAS_SIMPLE_QPARSER_PREFIX) != null)
        && (localParams.getParams(MTAS_SIMPLE_QPARSER_PREFIX).length == 1)) {
      defaultPrefix = localParams.getParams(MTAS_SIMPLE_QPARSER_PREFIX)[0];
    } 
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.apache.solr.search.QParser#parse()
   */
  @Override
  public Query parse() throws SyntaxError {
    if (field == null) {
      throw new SyntaxError("no " + MTAS_SIMPLE_QPARSER_FIELD);
    } else if (simpleQuery == null) {
      throw new SyntaxError("no " + MTAS_SIMPLE_QPARSER_QUERY);
    } else if (defaultPrefix == null) {
      throw new SyntaxError("no " + MTAS_SIMPLE_QPARSER_PREFIX);
    } else {
      BooleanQuery q = null;
      MtasSpanQuery iq = null;
      if (ignoreQuery != null) {
        Reader ignoreReader = new BufferedReader(new StringReader(ignoreQuery));
        MtasSimpleParser ignoreParser = new MtasSimpleParser(ignoreReader);
        try {
          List<MtasSpanQuery> iql = ignoreParser.parse(field, null, null, null);
          MtasSpanQuery[] iqs = new MtasSpanQuery[iql.size()];
          iq = new MtasSpanOrQuery(iql.toArray(iqs));
        } catch (mtas.parser.simple.TokenMgrError
            | mtas.parser.simple.ParseException e) {
          throw new SyntaxError(e);
        }
      }
      Reader queryReader = new BufferedReader(new StringReader(simpleQuery));
      MtasSimpleParser queryParser = new MtasSimpleParser(queryReader);
      try {
        List<MtasSpanQuery> ql = queryParser.parse(field, defaultPrefix, iq,
            maximumIgnoreLength);
        BooleanQuery.Builder booleanQuery = new BooleanQuery.Builder();
        for (Query query : ql) {
            booleanQuery.add(query, BooleanClause.Occur.MUST);
        }
        q = booleanQuery.build();
      } catch (mtas.parser.simple.TokenMgrError
          | mtas.parser.simple.ParseException e) {
        throw new SyntaxError(e);
      }
      return q;
    }
  }

}
 