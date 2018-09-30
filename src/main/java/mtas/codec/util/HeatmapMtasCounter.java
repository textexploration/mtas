package mtas.codec.util;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import org.apache.lucene.index.LeafReaderContext;
import org.apache.lucene.spatial.prefix.PrefixTreeStrategy;
import org.apache.lucene.spatial.prefix.tree.Cell;
import org.apache.lucene.spatial.prefix.tree.CellIterator;
import org.apache.lucene.spatial.prefix.tree.SpatialPrefixTree;
import org.apache.lucene.util.ArrayUtil;
import org.locationtech.spatial4j.context.SpatialContext;
import org.locationtech.spatial4j.shape.Point;
import org.locationtech.spatial4j.shape.Rectangle;
import org.locationtech.spatial4j.shape.SpatialRelation;

import mtas.codec.util.CodecComponent.ComponentHeatmap;
import mtas.codec.util.collector.MtasDataCollector;

/**
 * Based on org.apache.lucene.spatial.prefix.HeatmapFacetCounter
 */
public class HeatmapMtasCounter {

  /** Maximum number of supported rows (or columns). */
  public static final int MAX_ROWS_OR_COLUMNS = (int) Math.sqrt(ArrayUtil.MAX_ARRAY_LENGTH);
  static {
    Math.multiplyExact(MAX_ROWS_OR_COLUMNS, MAX_ROWS_OR_COLUMNS);// will throw if doesn't stay within integer
  }

  /** Response structure */
  public static class Heatmap  {
    public MtasDataCollector<?, ?> dataCollector;
    public final int columns;
    public final int rows;
    public final Rectangle region;
    public final double minX;
    public final double maxX;
    public final double minY;
    public final double maxY;
    public final double cellWidth;
    public final double cellHeight;
    public final int gridLevel;

    public Heatmap(int columns, int rows, Rectangle region, double cellWidth, double cellHeight,
        int gridLevel) throws IOException {
      this.columns = columns;
      this.rows = rows;
      this.region = region;
      this.minX = region.getMinX();
      this.maxX = region.getMaxX();
      this.minY = region.getMinY();
      this.maxY = region.getMaxY();
      this.cellWidth = cellWidth;
      this.cellHeight = cellHeight;
      this.dataCollector = null;
      this.gridLevel = gridLevel;
    }

    @Override
    public String toString() {
      return "Heatmap{" + columns + "x" + rows + " " + region + '}';
    }
  }
  
  public static void calcValues(PrefixTreeStrategy strategy, LeafReaderContext context, 
      ComponentHeatmap heatmap, int number, int[] docSet, long[] values
      ) throws IOException {
    if (heatmap.maxCells > (MAX_ROWS_OR_COLUMNS * MAX_ROWS_OR_COLUMNS)) {
      throw new IllegalArgumentException("maxCells (" + heatmap.maxCells + ") should be <= " + MAX_ROWS_OR_COLUMNS);
    }
    if (heatmap.boundsShape == null) {
      heatmap.boundsShape = strategy.getSpatialContext().getWorldBounds();
    }
    
    final int  rows = heatmap.hm.rows;
    final int  columns = heatmap.hm.columns;
    double heatMinX = heatmap.hm.minX;
    double heatMaxX = heatmap.hm.maxX;
    double heatMinY = heatmap.hm.minY;
    double heatMaxY = heatmap.hm.maxY;
    final double cellWidth = heatmap.hm.cellWidth;
    final double cellHeight = heatmap.hm.cellHeight;
    
    if (docSet.length==0) {
      return; // short-circuit
    }

    // All ancestor cell counts (of gridLevel) will be captured during grid
    // visiting and applied later. If the data is
    // just points then there won't be any ancestors.
    // grid count of ancestors covering all of the heatmap:
    List<Long> allCellsAncestorsValues = new ArrayList<>(); 
    
    
    // All other ancestors:
    Map<Rectangle, long[]> ancestorsValues = new HashMap<>();

    // Now lets count!
    PrefixTreeMtasCounter.compute(strategy, context, number, docSet, values, heatmap,
        new PrefixTreeMtasCounter.GridVisitor() {
      
          @Override
          public void visit(Cell cell, long[] cellValues) {
            final double heatMinX = heatmap.hm.region.getMinX();
            final Rectangle rect = (Rectangle) cell.getShape();
            if (cell.getLevel() == heatmap.gridLevel) {// heatmap level; count it directly
              // convert to col & row
              int column;
              if (rect.getMinX() >= heatMinX) {
                column = (int) Math.round((rect.getMinX() - heatMinX) / cellWidth);
              } else { // due to dateline wrap
                column = (int) Math.round((rect.getMinX() + 360 - heatMinX) / cellWidth);
              }
              int row = (int) Math.round((rect.getMinY() - heatMinY) / cellHeight);
              // note: unfortunately, it's possible for us to visit adjacent cells to the
              // heatmap (if the SpatialPrefixTree
              // allows adjacent cells to overlap on the seam), so we need to skip them
              if (column < 0 || column >= heatmap.hm.columns || row < 0 || row >= heatmap.hm.rows) {
                return;
              }
              // increment
              String key = String.valueOf(column * heatmap.hm.rows + row);
              try {
                heatmap.hm.dataCollector.add(key, cellValues, cellValues.length);
              } catch (IOException e) {
                e.printStackTrace();
              }
            } else if (rect.relate(heatmap.hm.region) == SpatialRelation.CONTAINS) {// containing ancestor
              for(long cellValue : cellValues) {
                allCellsAncestorsValues.add(cellValue);
              }  
            } else { // ancestor
              // note: not particularly efficient (possible put twice, and Integer wrapper);
              // oh well
              long[] existingValues = (long[]) ancestorsValues.put(rect, cellValues);
              if (existingValues != null) {
                long[] newValues = new long[existingValues.length + cellValues.length];
                System.arraycopy(existingValues, 0, newValues, 0, existingValues.length);
                System.arraycopy(cellValues, 0, newValues, existingValues.length, cellValues.length);
                ancestorsValues.put(rect, newValues);
              }
            }
          }
      
        });

    // Update the heatmap counts with ancestor counts

    // Apply allCellsAncestorCount
    if (!allCellsAncestorsValues.isEmpty()) {
      int n = heatmap.hm.columns*heatmap.hm.rows;   
      for (int i = 0; i < n; i++) {
        long[] allCellsAncestorsValuesArray = allCellsAncestorsValues.stream().mapToLong(l -> l).toArray();
        heatmap.hm.dataCollector.add(String.valueOf(i), allCellsAncestorsValuesArray, allCellsAncestorsValuesArray.length);
      }
    }
    int[] pair = new int[2];// output of intersectInterval
    for (Map.Entry<Rectangle, long[]> entry : ancestorsValues.entrySet()) {
      Rectangle rect = entry.getKey(); // from a cell (thus doesn't cross DL)
      final long[] ancestorValuesEntry = entry.getValue();

      // note: we approach this in a way that eliminates int overflow/underflow (think
      // huge cell, tiny heatmap)
      intersectInterval(heatMinY, heatMaxY, cellHeight, rows, rect.getMinY(), rect.getMaxY(), pair);
      final int startRow = pair[0];
      final int endRow = pair[1];

      if (!heatmap.hm.region.getCrossesDateLine()) {
        intersectInterval(heatMinX, heatMaxX, cellWidth, columns, rect.getMinX(), rect.getMaxX(), pair);
        final int startCol = pair[0];
        final int endCol = pair[1];
        incrementRange(heatmap.hm, startCol, endCol, startRow, endRow, ancestorValuesEntry);

      } else {
        // note: the cell rect might intersect 2 disjoint parts of the heatmap, so we do
        // the left & right separately
        final int leftColumns = (int) Math.round((180 - heatMinX) / cellWidth);
        final int rightColumns = heatmap.hm.columns - leftColumns;
        // left half of dateline:
        if (rect.getMaxX() > heatMinX) {
          intersectInterval(heatMinX, 180, cellWidth, leftColumns, rect.getMinX(), rect.getMaxX(), pair);
          final int startCol = pair[0];
          final int endCol = pair[1];
          incrementRange(heatmap.hm, startCol, endCol, startRow, endRow, ancestorValuesEntry);
        }
        // right half of dateline
        if (rect.getMinX() < heatMaxX) {
          intersectInterval(-180, heatMaxX, cellWidth, rightColumns, rect.getMinX(), rect.getMaxX(), pair);
          final int startCol = pair[0] + leftColumns;
          final int endCol = pair[1] + leftColumns;
          incrementRange(heatmap.hm, startCol, endCol, startRow, endRow, ancestorValuesEntry);
        }
      }
    }
  }
  
  public static void createHeatmap(ComponentHeatmap heatmap) throws IOException {
    final Rectangle inputRect = heatmap.boundsShape.getBoundingBox();
    // First get the rect of the cell at the bottom-left at depth gridLevel
    final SpatialPrefixTree grid = heatmap.strategy.getGrid();
    final SpatialContext ctx = grid.getSpatialContext();
    final Point cornerPt = ctx.makePoint(inputRect.getMinX(), inputRect.getMinY());
    final CellIterator cellIterator = grid.getTreeCellIterator(cornerPt, heatmap.gridLevel);
    Cell cornerCell = null;
    while (cellIterator.hasNext()) {
      cornerCell = cellIterator.next();
    }
    assert cornerCell != null && cornerCell.getLevel() == heatmap.gridLevel : "Cell not at target level: "
        + cornerCell;
    final Rectangle cornerRect = (Rectangle) cornerCell.getShape();
    assert cornerRect.hasArea();
    // Now calculate the number of columns and rows necessary to cover the inputRect
    double heatMinX = cornerRect.getMinX();// note: we might change this below...
    final double cellWidth = cornerRect.getWidth();
    final Rectangle worldRect = ctx.getWorldBounds();
    final int columns = calcRowsOrCols(cellWidth, heatMinX, inputRect.getWidth(), inputRect.getMinX(), worldRect.getWidth());
    double heatMinY = cornerRect.getMinY();
    final double cellHeight = cornerRect.getHeight();
    final int rows = calcRowsOrCols(cellHeight, heatMinY, inputRect.getHeight(), inputRect.getMinY(), worldRect.getHeight());
    assert rows > 0 && columns > 0;
    if (columns > MAX_ROWS_OR_COLUMNS || rows > MAX_ROWS_OR_COLUMNS || columns * rows > heatmap.maxCells) {
      throw new IllegalArgumentException(
          "Too many cells (" + columns + " x " + rows + ") for level " + heatmap.gridLevel + " shape " + inputRect);
    }

    // Create resulting heatmap bounding rectangle & Heatmap object.
    final double halfCellWidth = cellWidth / 2.0;
    // if X world-wraps, use world bounds' range
    if (columns * cellWidth + halfCellWidth > worldRect.getWidth()) {
      heatMinX = worldRect.getMinX();
    }
    double heatMaxX = heatMinX + columns * cellWidth;
    if (Math.abs(heatMaxX - worldRect.getMaxX()) < halfCellWidth) {// numeric conditioning issue
      heatMaxX = worldRect.getMaxX();
    } else if (heatMaxX > worldRect.getMaxX()) {// wraps dateline (won't happen if !geo)
      heatMaxX = heatMaxX - worldRect.getMaxX() + worldRect.getMinX();
    }
    final double halfCellHeight = cellHeight / 2.0;
    double heatMaxY = heatMinY + rows * cellHeight;
    if (Math.abs(heatMaxY - worldRect.getMaxY()) < halfCellHeight) {// numeric conditioning issue
      heatMaxY = worldRect.getMaxY();
    }
    heatmap.hm = new Heatmap(columns, rows, ctx.makeRectangle(heatMinX, heatMaxX, heatMinY, heatMaxY), cellWidth,
        cellHeight, heatmap.gridLevel);
    heatmap.hm.dataCollector = DataCollector.getCollector(DataCollector.COLLECTOR_TYPE_LIST, 
        heatmap.dataType, heatmap.statsType, heatmap.statsItems, null,null,null, null,null,null);
    heatmap.hm.dataCollector.initNewList(columns*rows);
  }

  private static void intersectInterval(double heatMin, double heatMax, double heatCellLen, int numCells,
      double cellMin, double cellMax, int[] out) {
    assert heatMin < heatMax && cellMin < cellMax;
    // precondition: we know there's an intersection
    if (heatMin >= cellMin) {
      out[0] = 0;
    } else {
      out[0] = (int) Math.round((cellMin - heatMin) / heatCellLen);
    }
    if (heatMax <= cellMax) {
      out[1] = numCells - 1;
    } else {
      out[1] = (int) Math.round((cellMax - heatMin) / heatCellLen) - 1;
    }
  }
  
  private static void incrementRange(Heatmap heatmap, int startColumn, int endColumn, int startRow, int endRow,
      long[] cellValues) {
    // startColumn & startRow are not necessarily within the heatmap range; likewise
    // numRows/columns may overlap.
    if (startColumn < 0) {
      endColumn += startColumn;
      startColumn = 0;
    }
    endColumn = Math.min(heatmap.columns - 1, endColumn);

    if (startRow < 0) {
      endRow += startRow;
      startRow = 0;
    }
    endRow = Math.min(heatmap.rows - 1, endRow);

    if (startRow > endRow) {
      return;// short-circuit
    }
    for (int c = startColumn; c <= endColumn; c++) {
      int cBase = c * heatmap.rows;
      for (int r = startRow; r <= endRow; r++) {
        try {
          heatmap.dataCollector.add(String.valueOf(cBase + r), cellValues, cellValues.length);
        } catch (IOException e) {
          e.printStackTrace();
        }
      }
    }
  }

  /**
   * Computes the number of intervals (rows or columns) to cover a range given the
   * sizes.
   */
  private static int calcRowsOrCols(double cellRange, double cellMin, double requestRange, double requestMin,
      double worldRange) {
    assert requestMin >= cellMin;
    // Idealistically this wouldn't be so complicated but we concern ourselves with
    // overflow and edge cases
    double range = (requestRange + (requestMin - cellMin));
    if (range == 0) {
      return 1;
    }
    final double intervals = Math.ceil(range / cellRange);
    if (intervals > Integer.MAX_VALUE) {
      return Integer.MAX_VALUE;// should result in an error soon (exceed thresholds)
    }
    // ensures we don't have more intervals than world bounds (possibly due to
    // rounding/edge issue)
    final long intervalsMax = Math.round(worldRange / cellRange);
    if (intervalsMax > Integer.MAX_VALUE) {
      // just return intervals
      return (int) intervals;
    }
    return Math.min((int) intervalsMax, (int) intervals);
  }

  private HeatmapMtasCounter() {
  }
}
