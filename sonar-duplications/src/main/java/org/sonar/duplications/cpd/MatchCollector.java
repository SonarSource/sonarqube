/**
 * BSD-style license; for more info see http://pmd.sourceforge.net/license.html
 */
package org.sonar.duplications.cpd;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import net.sourceforge.pmd.cpd.TokenEntry;

public class MatchCollector {

  private MatchAlgorithm ma;
  private Map<Match.MatchCode, Match> startMap = new HashMap<Match.MatchCode, Match>();
  private Map<String, List<Match>> fileMap = new HashMap<String, List<Match>>();

  public MatchCollector(MatchAlgorithm ma) {
    this.ma = ma;
  }

  public void collect(List<TokenEntry> marks) {
    // first get a pairwise collection of all maximal matches
    for (int i = 0; i < marks.size() - 1; i++) {
      TokenEntry mark1 = marks.get(i);
      for (int j = i + 1; j < marks.size(); j++) {
        TokenEntry mark2 = marks.get(j);
        int diff = mark1.getIndex() - mark2.getIndex();
        if ( -diff < ma.getMinimumTileSize()) {
          continue;
        }
        if (hasPreviousDupe(mark1, mark2)) {
          continue;
        }

        // "match too small" check
        int dupes = countDuplicateTokens(mark1, mark2);
        if (dupes < ma.getMinimumTileSize()) {
          continue;
        }
        // is it still too close together
        if (diff + dupes >= 1) {
          continue;
        }
        determineMatch(mark1, mark2, dupes);
      }
    }
  }

  @SuppressWarnings("PMD.CompareObjectsWithEquals")
  public List<Match> getMatches() {
    List<Match> matchList = new ArrayList<Match>(startMap.values());
    Collections.sort(matchList);
    Set<Match.MatchCode> matchSet = new HashSet<Match.MatchCode>();
    Match.MatchCode matchCode = new Match.MatchCode();
    for (int i = matchList.size(); i > 1; i--) {
      Match match1 = matchList.get(i - 1);
      TokenEntry mark1 = match1.getMarkSet().iterator().next();
      matchSet.clear();
      matchSet.add(match1.getMatchCode());
      for (int j = i - 1; j > 0; j--) {
        Match match2 = matchList.get(j - 1);
        if (match1.getTokenCount() != match2.getTokenCount()) {
          break;
        }
        TokenEntry mark2 = null;
        for (Iterator<TokenEntry> iter = match2.getMarkSet().iterator(); iter.hasNext();) {
          mark2 = iter.next();
          if (mark2 != mark1) {
            break;
          }
        }
        int dupes = countDuplicateTokens(mark1, mark2);
        if (dupes < match1.getTokenCount()) {
          break;
        }
        matchSet.add(match2.getMatchCode());
        match1.getMarkSet().addAll(match2.getMarkSet());
        matchList.remove(i - 2);
        i--;
      }
      if (matchSet.size() == 1) {
        continue;
      }
      // prune the mark set
      Set<TokenEntry> pruned = match1.getMarkSet();
      boolean done = false;
      ArrayList<TokenEntry> a1 = new ArrayList<TokenEntry>(match1.getMarkSet());
      Collections.sort(a1);
      for (int outer = 0; outer < a1.size() - 1 && !done; outer++) {
        TokenEntry cmark1 = a1.get(outer);
        for (int inner = outer + 1; inner < a1.size() && !done; inner++) {
          TokenEntry cmark2 = a1.get(inner);
          matchCode.setFirst(cmark1.getIndex());
          matchCode.setSecond(cmark2.getIndex());
          if ( !matchSet.contains(matchCode)) {
            if (pruned.size() > 2) {
              pruned.remove(cmark2);
            }
            if (pruned.size() == 2) {
              done = true;
            }
          }
        }
      }
    }
    return matchList;
  }

  /**
   * A greedy algorithm for determining non-overlapping matches
   */
  private void determineMatch(TokenEntry mark1, TokenEntry mark2, int dupes) {
    Match match = new Match(dupes, mark1, mark2);
    String fileKey = mark1.getTokenSrcID() + mark2.getTokenSrcID();
    List<Match> pairMatches = fileMap.get(fileKey);
    if (pairMatches == null) {
      pairMatches = new ArrayList<Match>();
      fileMap.put(fileKey, pairMatches);
    }
    boolean add = true;
    for (int i = 0; i < pairMatches.size(); i++) {
      Match other = pairMatches.get(i);
      if (other.getFirstMark().getIndex() + other.getTokenCount() - mark1.getIndex() > 0) {
        boolean ordered = other.getSecondMark().getIndex() - mark2.getIndex() < 0;
        if ((ordered && (other.getEndIndex() - mark2.getIndex() > 0))
            || ( !ordered && (match.getEndIndex() - other.getSecondMark().getIndex()) > 0)) {
          if (other.getTokenCount() >= match.getTokenCount()) {
            add = false;
            break;
          } else {
            pairMatches.remove(i);
            startMap.remove(other.getMatchCode());
          }
        }
      }
    }
    if (add) {
      pairMatches.add(match);
      startMap.put(match.getMatchCode(), match);
    }
  }

  private boolean hasPreviousDupe(TokenEntry mark1, TokenEntry mark2) {
    if (mark1.getIndex() == 0) {
      return false;
    }
    return !matchEnded(ma.tokenAt( -1, mark1), ma.tokenAt( -1, mark2));
  }

  private int countDuplicateTokens(TokenEntry mark1, TokenEntry mark2) {
    int index = 0;
    while ( !matchEnded(ma.tokenAt(index, mark1), ma.tokenAt(index, mark2))) {
      index++;
    }
    return index;
  }

  private boolean matchEnded(TokenEntry token1, TokenEntry token2) {
    return token1.getIdentifier() != token2.getIdentifier() || token1 == TokenEntry.EOF || token2 == TokenEntry.EOF;
  }
}