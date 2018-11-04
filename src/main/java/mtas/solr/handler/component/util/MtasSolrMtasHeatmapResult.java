package mtas.solr.handler.component.util;

import java.io.IOException;
import java.io.Serializable;
import java.util.Map;
import java.util.Map.Entry;

import mtas.codec.util.CodecComponent.ComponentHeatmap;

/**
 * The Class MtasSolrMtasResult.
 */
public class MtasSolrMtasHeatmapResult implements Serializable {

  /** The Constant serialVersionUID. */
  private static final long serialVersionUID = 1L;
  
  public int gridLevel;
  public int columns;
  public int rows;
  public double minX;
  public double maxX;
  public double minY;
  public double maxY;
  public MtasSolrMtasResult result;
  public Map<String, MtasSolrMtasResult> functionResults;
  
  public MtasSolrMtasHeatmapResult(ComponentHeatmap heatmap, MtasSolrMtasResult result) {
    gridLevel = heatmap.hm.gridLevel;
    columns = heatmap.hm.columns;
    rows = heatmap.hm.rows;
    minX = heatmap.hm.minX;
    maxX = heatmap.hm.maxX;
    minY = heatmap.hm.minY;
    maxY = heatmap.hm.maxY;
    this.result = result;
  }

  void merge(MtasSolrMtasHeatmapResult newItem) throws IOException {
    if(gridLevel == newItem.gridLevel && rows==newItem.rows && columns==newItem.columns) {
      result.merge(newItem.result);
    }
  }
  
}
