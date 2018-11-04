package mtas.parser;

import static org.junit.Assert.*;

import java.io.BufferedReader;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import mtas.parser.simple.ParseException;
import mtas.parser.simple.util.MtasSimpleParserWordQuery;
import mtas.parser.simple.MtasSimpleParser;
import mtas.search.spans.MtasSpanSequenceItem;
import mtas.search.spans.MtasSpanSequenceQuery;
import mtas.search.spans.util.MtasSpanQuery;
import mtas.search.spans.util.MtasSpanUniquePositionQuery;

/**
 * The Class MtasCQLParserTestWord.
 */
public class MtasSimpleParserTest {

  /** The log. */
  private static Log log = LogFactory.getLog(MtasSimpleParserTest.class);

  
  private void testSimpleParse(String field, String defaultPrefix, String simple,
     MtasSpanQuery q) {
    List<MtasSpanQuery> qList = new ArrayList<>();
    qList.add(q);
    testSimpleParse(field,defaultPrefix,simple,qList);
  }
  
  private void testSimpleParse(String field, String defaultPrefix, String simple,
      List<MtasSpanQuery> qList) {
    MtasSimpleParser p = new MtasSimpleParser(
        new BufferedReader(new StringReader(simple)));
    try {
      assertEquals(p.parse(field, defaultPrefix, null, null), qList);
      // System.out.println("Tested CQL parsing:\t"+cql);
    } catch (ParseException e) {
      // System.out.println("Error CQL parsing:\t"+cql);
      log.error(e);
    }
  }

  /**
   * Test CQL equivalent.
   *
   * @param field the field
   * @param defaultPrefix the default prefix
   * @param cql1 the cql 1
   * @param cql2 the cql 2
   */
  private void testSimpleEquivalent(String field, String defaultPrefix,
      String simple1, String simple2) {
    MtasSimpleParser p1 = new MtasSimpleParser(
        new BufferedReader(new StringReader(simple1)));
    MtasSimpleParser p2 = new MtasSimpleParser(
        new BufferedReader(new StringReader(simple2)));
    try {
      assertEquals(p1.parse(field, defaultPrefix, null, null),
          p2.parse(field, defaultPrefix, null, null));
      // System.out.println("Tested CQL equivalent:\t"+cql1+" and "+cql2);
    } catch (ParseException e) {
      // System.out.println("Error CQL equivalent:\t"+cql1+" and "+cql2);
      log.error(e);
    }
  }


  @org.junit.Test
  public void singleWord() throws ParseException {
    String field = "testveld";
    String prefix = "lemma";
    String simple = "koe";
    MtasSpanQuery q = new MtasSimpleParserWordQuery(field, prefix, simple);
    testSimpleParse(field, prefix, simple, new MtasSpanUniquePositionQuery(q));
  }
  
  @org.junit.Test
  public void singleWordPrefix() throws ParseException {
    String field = "testveld";
    String prefix = "lemma";
    String simple = "pos:N";
    MtasSpanQuery q = new MtasSimpleParserWordQuery(field, "pos", "N");
    testSimpleParse(field, prefix, simple, new MtasSpanUniquePositionQuery(q));
  }
  
  @org.junit.Test
  public void singleWordEscaped() throws ParseException {
    String field = "testveld";
    String prefix = "lemma";
    String simple = "\\\"koe\\\"";
    MtasSpanQuery q = new MtasSimpleParserWordQuery(field, prefix, simple);
    testSimpleParse(field, prefix, simple, new MtasSpanUniquePositionQuery(q));
  }
  
  @org.junit.Test(expected = ParseException.class)
  public void singleWordWronglyEscaped() throws ParseException  {
    String field = "testveld";
    String prefix = "lemma";
    String simple = "\"koe\\\"";
    MtasSimpleParser p = new MtasSimpleParser(
        new BufferedReader(new StringReader(simple)));
    p.parse(field, prefix, null, null);
  }
  
   @org.junit.Test
  public void singleWordSequence() throws ParseException {
    String field = "testveld";
    String prefix = "lemma";
    String simple = "\"een echte test\"";
    List<String> words = new ArrayList<>(Arrays.asList(simple.replace("\"","").split(" ")));
    List<MtasSpanSequenceItem> qList = new ArrayList<>();
    for(String word : words) {
      qList.add(new MtasSpanSequenceItem(new MtasSimpleParserWordQuery(field, prefix, word), false));
    }
    MtasSpanQuery q = new MtasSpanSequenceQuery(qList, null, null);
    testSimpleParse(field, prefix, simple, new MtasSpanUniquePositionQuery(q));
  }
  
  @org.junit.Test
  public void singleWordSequenceEscape() throws ParseException {
    String field = "testveld";
    String prefix = "lemma";
    String simple = "\"een \\\"echte\\\" test\"";
    List<String> words = new ArrayList<>(Arrays.asList(simple.replaceAll("^\"","").replaceAll("\"$","").split(" ")));
    List<MtasSpanSequenceItem> qList = new ArrayList<>();
    for(String word : words) {
      qList.add(new MtasSpanSequenceItem(new MtasSimpleParserWordQuery(field, prefix, word), false));
    }
    MtasSpanQuery q = new MtasSpanSequenceQuery(qList, null, null);
    testSimpleParse(field, prefix, simple, new MtasSpanUniquePositionQuery(q));
  }
  
  @org.junit.Test
  public void multipleWords() throws ParseException {
    String field = "testveld";
    String prefix = "lemma";
    String simple = "koe paard schaap";
    List<String> words = new ArrayList<>(Arrays.asList(simple.split(" ")));
    List<MtasSpanQuery> qList = new ArrayList<>();
    for(String word : words) {
      qList.add(new MtasSpanUniquePositionQuery(new MtasSimpleParserWordQuery(field, prefix, word)));
    }
    testSimpleParse(field, prefix, simple, qList);
  }
  
  @org.junit.Test
  public void multipleWordsAndSequences() throws ParseException {
    String field = "testveld";
    String prefix = "lemma";
    String simple = "koe \"paard schaap\" geit";
    List<MtasSpanQuery> qList = new ArrayList<>();
    List<MtasSpanSequenceItem> sList = new ArrayList<>();
    sList.add(new MtasSpanSequenceItem(new MtasSimpleParserWordQuery(field, prefix, "paard"), false));
    sList.add(new MtasSpanSequenceItem(new MtasSimpleParserWordQuery(field, prefix, "schaap"), false));
    qList.add(new MtasSpanUniquePositionQuery(new MtasSimpleParserWordQuery(field, prefix, "koe")));
    qList.add(new MtasSpanUniquePositionQuery(new MtasSpanSequenceQuery(sList, null, null)));
    qList.add(new MtasSpanUniquePositionQuery(new MtasSimpleParserWordQuery(field, prefix, "geit")));
    testSimpleParse(field, prefix, simple, qList);
  }
  
 

  
}
