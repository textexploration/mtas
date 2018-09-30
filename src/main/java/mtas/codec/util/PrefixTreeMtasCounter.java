package mtas.codec.util;

import java.io.IOException;
import java.util.Arrays;

import mtas.codec.util.CodecComponent.ComponentHeatmap;

import org.apache.lucene.index.LeafReaderContext;
import org.apache.lucene.index.PostingsEnum;
import org.apache.lucene.search.DocIdSet;
import org.apache.lucene.search.DocIdSetIterator;
import org.apache.lucene.spatial.prefix.AbstractVisitingPrefixTreeQuery;
import org.apache.lucene.spatial.prefix.PrefixTreeStrategy;
import org.apache.lucene.spatial.prefix.tree.Cell;
import org.apache.lucene.spatial.prefix.tree.SpatialPrefixTree;
import org.apache.lucene.util.Bits;

/**
 * Based on PrefixTreeFacetCounter 
 */



public class PrefixTreeMtasCounter {

  /** A callback/visitor of gridlevel counts. */
  public static abstract class GridVisitor {
    /** Called for cells with a leaf, or cells at the target gridlevel.  {@code count} is greater than zero.
     * When an ancestor cell is given with non-zero count, the count can be considered to be added to all cells
     * below. You won't necessarily get a cell at level {@code gridLevel} if the indexed data is courser (bigger).
     */
    public abstract void visit(Cell cell, long[] cellValues);



  }

  private PrefixTreeMtasCounter() {
  }

  /** Lower-level per-leaf segment method. */
  public static void compute(final PrefixTreeStrategy strategy, final LeafReaderContext context, 
                             final int number, final int[] docSet, final long[] values,
                             ComponentHeatmap heatmap, final GridVisitor gridVisitor)
      throws IOException {
    final SpatialPrefixTree tree = strategy.getGrid();
    
    Bits acceptDocs;
    if (number==0) {
      acceptDocs = new Bits.MatchNoBits(context.reader().maxDoc());
    } else if (number == context.reader().maxDoc()) {
      acceptDocs = new Bits.MatchAllBits(context.reader().maxDoc());
    } else {
      acceptDocs = new Bits() {
        int p = docSet[0];
        int i = 0;
      
        @Override
        public boolean get(int index) {
          if(index<p) {
            p=docSet[0];
            i=0;
          }
          while(index>p) {
            i++; 
            if(i<number) {
              p=docSet[i];
            } else {
              return false;
            }
          } 
          return index==p;
        }
  
        @Override
        public int length() {
          return context.reader().maxDoc();
        }
      };
    }  

    //scanLevel is an optimization knob of AbstractVisitingPrefixTreeFilter. It's unlikely
    // another scanLevel would be much faster and it tends to be a risky knob (can help a little, can hurt a ton).
    // TODO use RPT's configured scan level?  Do we know better here?  Hard to say.
    final int scanLevel = tree.getMaxLevels();
    //AbstractVisitingPrefixTreeFilter is a Lucene Filter.  We don't need a filter; we use it for its great prefix-tree
    // traversal code.  TODO consider refactoring if/when it makes sense (more use cases than this)
    new AbstractVisitingPrefixTreeQuery(heatmap.boundsShape, strategy.getFieldName(), tree, heatmap.gridLevel, scanLevel) {

      @Override
      public String toString(String field) {
        return "anonPrefixTreeQuery";//un-used
      }

      @Override
      public DocIdSet getDocIdSet(LeafReaderContext contexts) throws IOException {
        assert heatmap.gridLevel == super.detailLevel;//same thing, FYI. (constant)

        return new VisitorTemplate(context) {

          @Override
          protected void start() throws IOException {
          }

          @Override
          protected DocIdSet finish() throws IOException {
            return null;//unused;
          }

          @Override
          protected boolean visitPrefix(Cell cell) throws IOException {
            // At gridLevel...
            if (cell.getLevel() == heatmap.gridLevel) {
              // Count docs
              visitLeaf(cell);//we're not a leaf but we treat it as such at grid level
              return false;//don't descend further; this is enough detail
            }

            // We optimize for discriminating filters (reflected in acceptDocs) and short-circuit if no
            // matching docs. We could do this at all levels or never but the closer we get to the grid level, the
            // higher the probability this is worthwhile. We do when docFreq == 1 because it's a cheap check, especially
            // due to "pulsing" in the codec.
            //TODO this opt should move to VisitorTemplate (which contains an optimization TODO to this effect)
            if (cell.getLevel() == heatmap.gridLevel - 1 || termsEnum.docFreq() == 1) {
              if (!hasDocsAtThisTerm()) {
                return false;
              }
            }
            return true;
          }

          @Override
          protected void visitLeaf(Cell cell) throws IOException {
            final long[] cellValues = computeValuesAtThisTerm();
            if (cellValues.length > 0) {
              gridVisitor.visit(cell, cellValues);
            }
          }

          private long[] computeValuesAtThisTerm() throws IOException {
            long[] cellValues = new long[number];
            int cellValuesCounter=0;            
            int docSetCounter=0;            
            postingsEnum = termsEnum.postings(postingsEnum, PostingsEnum.NONE);
            while (postingsEnum.nextDoc() != DocIdSetIterator.NO_MORE_DOCS && docSetCounter<number) {
              while(postingsEnum.docID()<docSet[docSetCounter]) {
                postingsEnum.nextDoc();
              }
              while(docSetCounter<number && docSet[docSetCounter]<postingsEnum.docID()) {
                docSetCounter++;
              }
              if(docSetCounter>=number) {
                break;
              } else {
                if(postingsEnum.docID()==docSet[docSetCounter]) {
                  cellValues[cellValuesCounter] = values[docSetCounter];
                  cellValuesCounter++;
                }
              }  
            }
            return Arrays.copyOf(cellValues, cellValuesCounter);
          }

          private boolean hasDocsAtThisTerm() throws IOException {
            postingsEnum = termsEnum.postings(postingsEnum, PostingsEnum.NONE);
            int nextDoc = postingsEnum.nextDoc();
            while (nextDoc != DocIdSetIterator.NO_MORE_DOCS && acceptDocs.get(nextDoc) == false) {
              nextDoc = postingsEnum.nextDoc();
            }
            return nextDoc != DocIdSetIterator.NO_MORE_DOCS;
          }

        }.getDocIdSet();
      }
    }.getDocIdSet(context);
  }
  
//  /** Lower-level per-leaf segment method. */
//  public static void computeOld(final PrefixTreeStrategy strategy, final LeafReaderContext context, final Bits acceptDocs,
//                             ComponentHeatmap heatmap, final GridVisitor gridVisitor)
//      throws IOException {
//    if (acceptDocs != null && acceptDocs.length() != context.reader().maxDoc()) {
//      throw new IllegalArgumentException(
//          "acceptDocs bits length " + acceptDocs.length() +" != leaf maxdoc " + context.reader().maxDoc());
//    }
//    final SpatialPrefixTree tree = strategy.getGrid();
//
//    //scanLevel is an optimization knob of AbstractVisitingPrefixTreeFilter. It's unlikely
//    // another scanLevel would be much faster and it tends to be a risky knob (can help a little, can hurt a ton).
//    // TODO use RPT's configured scan level?  Do we know better here?  Hard to say.
//    final int scanLevel = tree.getMaxLevels();
//    //AbstractVisitingPrefixTreeFilter is a Lucene Filter.  We don't need a filter; we use it for its great prefix-tree
//    // traversal code.  TODO consider refactoring if/when it makes sense (more use cases than this)
//    new AbstractVisitingPrefixTreeQuery(heatmap.boundsShape, strategy.getFieldName(), tree, heatmap.gridLevel, scanLevel) {
//
//      @Override
//      public String toString(String field) {
//        return "anonPrefixTreeQuery";//un-used
//      }
//
//      @Override
//      public DocIdSet getDocIdSet(LeafReaderContext contexts) throws IOException {
//        assert heatmap.gridLevel == super.detailLevel;//same thing, FYI. (constant)
//
//        return new VisitorTemplate(context) {
//
//          @Override
//          protected void start() throws IOException {
//          }
//
//          @Override
//          protected DocIdSet finish() throws IOException {
//            return null;//unused;
//          }
//
//          @Override
//          protected boolean visitPrefix(Cell cell) throws IOException {
//            // At gridLevel...
//            if (cell.getLevel() == heatmap.gridLevel) {
//              // Count docs
//              visitLeaf(cell);//we're not a leaf but we treat it as such at grid level
//              return false;//don't descend further; this is enough detail
//            }
//
//            // We optimize for discriminating filters (reflected in acceptDocs) and short-circuit if no
//            // matching docs. We could do this at all levels or never but the closer we get to the grid level, the
//            // higher the probability this is worthwhile. We do when docFreq == 1 because it's a cheap check, especially
//            // due to "pulsing" in the codec.
//            //TODO this opt should move to VisitorTemplate (which contains an optimization TODO to this effect)
//            if (cell.getLevel() == heatmap.gridLevel - 1 || termsEnum.docFreq() == 1) {
//              if (!hasDocsAtThisTerm()) {
//                return false;
//              }
//            }
//            return true;
//          }
//
//          @Override
//          protected void visitLeaf(Cell cell) throws IOException {
//            final int count = countDocsAtThisTerm();
//            if (count > 0) {
//              gridVisitor.visit(cell, count);
//            }
//          }
//
//          private int countDocsAtThisTerm() throws IOException {
//            if (acceptDocs == null) {
//              return termsEnum.docFreq();
//            }
//            int count = 0;
//            postingsEnum = termsEnum.postings(postingsEnum, PostingsEnum.NONE);
//            while (postingsEnum.nextDoc() != DocIdSetIterator.NO_MORE_DOCS) {
//              if (acceptDocs.get(postingsEnum.docID()) == false) {
//                continue;
//              }
//              count++;
//            }
//            return count;
//          }
//
//          private boolean hasDocsAtThisTerm() throws IOException {
//            if (acceptDocs == null) {
//              return true;
//            }
//            postingsEnum = termsEnum.postings(postingsEnum, PostingsEnum.NONE);
//            int nextDoc = postingsEnum.nextDoc();
//            while (nextDoc != DocIdSetIterator.NO_MORE_DOCS && acceptDocs.get(nextDoc) == false) {
//              nextDoc = postingsEnum.nextDoc();
//            }
//            return nextDoc != DocIdSetIterator.NO_MORE_DOCS;
//          }
//
//        }.getDocIdSet();
//      }
//    }.getDocIdSet(context);
//  }

  
}

