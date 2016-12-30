package mtas.search.spans.util;

import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;

import org.apache.lucene.search.spans.Spans;

/**
 * The Class MtasIgnoreItem.
 */
public class MtasIgnoreItem {
  
  /** The Constant DEFAULT_MAXIMUM_IGNORE_LENGTH. */
  static final int DEFAULT_MAXIMUM_IGNORE_LENGTH = 10;

  /** The ignore spans. */
  Spans ignoreSpans;
  
  /** The current doc id. */
  int currentDocId;
  
  /** The current position. */
  int currentPosition;
  
  /** The minimum position. */
  int minimumPosition;
  
  /** The maximum ignore length. */
  int maximumIgnoreLength;
  
  /** The base list. */
  HashMap<Integer, HashSet<Integer>> baseStartPositionList;
  HashMap<Integer, HashSet<Integer>> baseEndPositionList;
  
  /** The full list. */
  HashMap<Integer, HashSet<Integer>> fullEndPositionList;
  
  /** The max base end position. */
  HashMap<Integer, Integer> minBaseStartPosition;
  HashMap<Integer, Integer> maxBaseEndPosition;
  
  /** The max full end position. */
  HashMap<Integer, Integer> minFullStartPosition;
HashMap<Integer, Integer> maxFullEndPosition;
  
  /**
   * Instantiates a new mtas ignore item.
   *
   * @param ignoreSpans the ignore spans
   * @param maximumIgnoreLength the maximum ignore length
   */
  public MtasIgnoreItem(Spans ignoreSpans, Integer maximumIgnoreLength) {
    this.ignoreSpans = ignoreSpans;
    currentDocId = -1;
    currentPosition = -1;
    minimumPosition = -1;
    baseStartPositionList = new HashMap<Integer, HashSet<Integer>>();
    baseEndPositionList = new HashMap<Integer, HashSet<Integer>>();
    fullEndPositionList = new HashMap<Integer, HashSet<Integer>>();
    minBaseStartPosition = new HashMap<Integer, Integer>();
    maxBaseEndPosition = new HashMap<Integer, Integer>();
    minFullStartPosition = new HashMap<Integer, Integer>();
    maxFullEndPosition = new HashMap<Integer, Integer>();
    if (maximumIgnoreLength == null) {
      this.maximumIgnoreLength = DEFAULT_MAXIMUM_IGNORE_LENGTH;
    } else {
      this.maximumIgnoreLength = maximumIgnoreLength;
    }
  }

  /**
   * Advance to doc.
   *
   * @param docId the doc id
   * @return true, if successful
   * @throws IOException Signals that an I/O exception has occurred.
   */
  public boolean advanceToDoc(int docId) throws IOException {
    if (ignoreSpans == null || currentDocId == Spans.NO_MORE_DOCS) {
      return false;
    } else if (currentDocId == docId) {
      return true;
    } else {
      baseEndPositionList.clear();
      fullEndPositionList.clear();
      maxBaseEndPosition.clear();
      minFullStartPosition.clear();
      if (currentDocId < docId) {
        currentDocId = ignoreSpans.advance(docId);
        currentPosition = -1;
        minimumPosition = -1;
      }
      return currentDocId == docId;
    }
  }

  /**
   * Gets the max size.
   *
   * @param docId the doc id
   * @param position the position
   * @return the max size
   * @throws IOException Signals that an I/O exception has occurred.
   */
  public int getMinStartPosition(int docId, int position) throws IOException {
    if (ignoreSpans != null && docId == currentDocId) {
      if (position < minimumPosition) {
        throw new IOException(
            "Unexpected position, should be >= " + minimumPosition + "!");
      } else {
        computeFullStartPositionMinimum(position);
        if(minFullStartPosition.containsKey(position)) {
          return minFullStartPosition.get(position);
        } else {
          return 0;
        }
      }      
    } else {
      return 0;
    }
  }
  
  public int getMaxEndPosition(int docId, int position) throws IOException {
    if (ignoreSpans != null && docId == currentDocId) {
      if (position < minimumPosition) {
        throw new IOException(
            "Unexpected position, should be >= " + minimumPosition + "!");
      }
      computeFullEndPositionList(position);
      if(maxFullEndPosition.containsKey(position)) {
        return maxFullEndPosition.get(position);
      } else {
        return 0;
      }
    } else {
      return 0;
    }
  }

  /**
   * Gets the full list.
   *
   * @param docId the doc id
   * @param position the position
   * @return the full list
   * @throws IOException Signals that an I/O exception has occurred.
   */
  public HashSet<Integer> getFullEndPositionList(int docId, int position)
      throws IOException {
    if (ignoreSpans != null && docId == currentDocId) {
      if (position < minimumPosition) {
        throw new IOException(
            "Unexpected startPosition, should be >= " + minimumPosition + "!");
      } else {
        computeFullEndPositionList(position);
        return fullEndPositionList.get(position);
      }
    } else {
      return null;
    }
  }
  
  private void computeFullStartPositionMinimum(int position) throws IOException {
    if (ignoreSpans != null && !minFullStartPosition.containsKey(position)) {
      HashSet<Integer> list = baseStartPositionList.get(position);
      HashSet<Integer> newList = new HashSet<Integer>();
      int minimumStartPosition = position;
      while(list!=null && !list.isEmpty()) {
        newList.clear();
        for(int startPosition : list) {
          if(minFullStartPosition.containsKey(startPosition)) {
            minimumStartPosition = Math.min(minimumStartPosition, minFullStartPosition.get(startPosition));
          } else if(baseStartPositionList.containsKey(startPosition)) {
            newList.addAll(baseStartPositionList.get(startPosition));            
          } else {
            if(startPosition<minimumStartPosition) {
              minimumStartPosition = startPosition;
            }
          }
        }
        list.clear();
        list.addAll(newList);
      }
      minFullStartPosition.put(position, minimumStartPosition);
    }
  }  

  /**
   * Compute full list.
   *
   * @param position the position
   * @throws IOException Signals that an I/O exception has occurred.
   */
  private void computeFullEndPositionList(int position) throws IOException {
    if (ignoreSpans != null && !fullEndPositionList.containsKey(position)) {
      // initial fill
      moveTo(position);
      HashSet<Integer> list = baseEndPositionList.get(position);
      if (list != null && !list.isEmpty()) {
        int maxEndPosition = maxBaseEndPosition.get(position);
        HashSet<Integer> checkList = new HashSet<Integer>();
        HashSet<Integer> subCheckList = new HashSet<Integer>();
        checkList.addAll(list);
        int depth = 1;
        while (!checkList.isEmpty()) {
          if (depth > maximumIgnoreLength) {
            checkList.clear();
            subCheckList.clear();
            throw new IOException("too many successive ignores, maximum is "
                + maximumIgnoreLength);
          } else {
            for (Integer checkItem : checkList) {
              if (fullEndPositionList.get(checkItem) != null) {
                list.addAll(fullEndPositionList.get(checkItem));
                maxEndPosition = Math.max(maxEndPosition,
                    maxFullEndPosition.get(checkItem));
              } else {
                moveTo(checkItem);
                if (baseEndPositionList.containsKey(checkItem)) {
                  list.addAll(baseEndPositionList.get(checkItem));
                  maxEndPosition = Math.max(maxEndPosition,
                      maxBaseEndPosition.get(checkItem));
                  subCheckList.addAll(baseEndPositionList.get(checkItem));
                } else {
                  // ready for checkItem
                }
              }
            }
            checkList.clear();
            checkList.addAll(subCheckList);
            subCheckList.clear();
            depth++;
          }
        }
        fullEndPositionList.put(position, list);
        maxFullEndPosition.put(position, (maxEndPosition - position));
      } else {
        fullEndPositionList.put(position, null);
        maxFullEndPosition.put(position, 0);
      }
    }
  }

  /**
   * Move to.
   *
   * @param position the position
   */
  private void moveTo(int position) {
    while (position >= currentPosition) {
      try {
        currentPosition = ignoreSpans.nextStartPosition();
        if (currentPosition != Spans.NO_MORE_POSITIONS
            && currentPosition >= minimumPosition) {
          if (!baseEndPositionList.containsKey(currentPosition)) {
            baseEndPositionList.put(currentPosition, new HashSet<Integer>());
            maxBaseEndPosition.put(currentPosition, currentPosition);
          } else {
            maxBaseEndPosition.put(currentPosition,
                Math.max(maxBaseEndPosition.get(currentPosition),
                    ignoreSpans.endPosition()));  
          }
          if (!baseStartPositionList.containsKey(ignoreSpans.endPosition())) {
            baseStartPositionList.put(ignoreSpans.endPosition(), new HashSet<Integer>());
            minBaseStartPosition.put(ignoreSpans.endPosition(), ignoreSpans.endPosition());
          } else {
            minBaseStartPosition.put(ignoreSpans.endPosition(),
                Math.min(minBaseStartPosition.get(ignoreSpans.endPosition()),
                    currentPosition)); 
          }
          baseStartPositionList.get(ignoreSpans.endPosition()).add(currentPosition); 
          baseEndPositionList.get(currentPosition).add(ignoreSpans.endPosition());                  
        }
      } catch (IOException e) {
        currentPosition = Spans.NO_MORE_POSITIONS;
        break;
      }
    }
  }

  /**
   * Removes the before.
   *
   * @param docId the doc id
   * @param position the position
   */
  public void removeBefore(int docId, int position) {
    if (ignoreSpans != null && docId == currentDocId) {
      baseStartPositionList.entrySet().removeIf(entry -> entry.getKey() < position);
      baseEndPositionList.entrySet().removeIf(entry -> entry.getKey() < position);
      fullEndPositionList.entrySet().removeIf(entry -> entry.getKey() < position);
      minBaseStartPosition.entrySet()
          .removeIf(entry -> entry.getKey() < position);
      maxBaseEndPosition.entrySet()
      .removeIf(entry -> entry.getKey() < position);
      minFullStartPosition.entrySet()
      .removeIf(entry -> entry.getKey() < position);
      maxFullEndPosition.entrySet()
      .removeIf(entry -> entry.getKey() < position);
      if (minimumPosition < position) {
        minimumPosition = position;
      }
      if (currentPosition < position) {
        currentPosition = position;
      }
    }
  }
}
