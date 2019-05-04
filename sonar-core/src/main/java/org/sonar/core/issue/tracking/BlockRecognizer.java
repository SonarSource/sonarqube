/*
 * SonarQube
 * Copyright (C) 2009-2019 SonarSource SA
 * mailto:info AT sonarsource DOT com
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */
package org.sonar.core.issue.tracking;

import com.google.common.collect.LinkedHashMultimap;
import com.google.common.collect.Lists;
import com.google.common.collect.Multimap;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

class BlockRecognizer<RAW extends Trackable, BASE extends Trackable> {

  /**
   * If base source code is available, then detect code moves through block hashes.
   * Only the issues associated to a line can be matched here.
   */
  void match(Input<RAW> rawInput, Input<BASE> baseInput, Tracking<RAW, BASE> tracking) {
    BlockHashSequence rawHashSequence = rawInput.getBlockHashSequence();
    BlockHashSequence baseHashSequence = baseInput.getBlockHashSequence();

    Multimap<Integer, RAW> rawsByLine = groupByLine(tracking.getUnmatchedRaws(), rawHashSequence);
    Multimap<Integer, BASE> basesByLine = groupByLine(tracking.getUnmatchedBases(), baseHashSequence);
    Map<Integer, HashOccurrence> occurrencesByHash = new HashMap<>();

    for (Integer line : basesByLine.keySet()) {
      int hash = baseHashSequence.getBlockHashForLine(line);
      HashOccurrence hashOccurrence = occurrencesByHash.get(hash);
      if (hashOccurrence == null) {
        // first occurrence in base
        hashOccurrence = new HashOccurrence();
        hashOccurrence.baseLine = line;
        hashOccurrence.baseCount = 1;
        occurrencesByHash.put(hash, hashOccurrence);
      } else {
        hashOccurrence.baseCount++;
      }
    }

    for (Integer line : rawsByLine.keySet()) {
      int hash = rawHashSequence.getBlockHashForLine(line);
      HashOccurrence hashOccurrence = occurrencesByHash.get(hash);
      if (hashOccurrence != null) {
        hashOccurrence.rawLine = line;
        hashOccurrence.rawCount++;
      }
    }

    for (HashOccurrence hashOccurrence : occurrencesByHash.values()) {
      if (hashOccurrence.baseCount == 1 && hashOccurrence.rawCount == 1) {
        // Guaranteed that baseLine has been moved to rawLine, so we can map all issues on baseLine to all issues on rawLine
        map(rawsByLine.get(hashOccurrence.rawLine), basesByLine.get(hashOccurrence.baseLine), tracking);
        basesByLine.removeAll(hashOccurrence.baseLine);
        rawsByLine.removeAll(hashOccurrence.rawLine);
      }
    }

    // Check if remaining number of lines exceeds threshold. It avoids processing too many combinations.
    if (isOverLimit(basesByLine.keySet().size(), rawsByLine.keySet().size())) {
      return;
    }

    List<LinePair> possibleLinePairs = Lists.newArrayList();
    for (Integer baseLine : basesByLine.keySet()) {
      for (Integer rawLine : rawsByLine.keySet()) {
        int weight = lengthOfMaximalBlock(baseInput.getLineHashSequence(), baseLine, rawInput.getLineHashSequence(), rawLine);
        if (weight > 0) {
          possibleLinePairs.add(new LinePair(baseLine, rawLine, weight));
        }
      }
    }
    Collections.sort(possibleLinePairs, LinePairComparator.INSTANCE);
    for (LinePair linePair : possibleLinePairs) {
      // High probability that baseLine has been moved to rawLine, so we can map all issues on baseLine to all issues on rawLine
      map(rawsByLine.get(linePair.rawLine), basesByLine.get(linePair.baseLine), tracking);
    }
  }

  static boolean isOverLimit(long a, long b) {
    return Math.multiplyExact(a, b) >= 250_000;
  }

  /**
   * @param startLineA number of line from first version of text (numbering starts from 1)
   * @param startLineB number of line from second version of text (numbering starts from 1)
   */
  static int lengthOfMaximalBlock(LineHashSequence hashesA, int startLineA, LineHashSequence hashesB, int startLineB) {
    if (!hashesA.getHashForLine(startLineA).equals(hashesB.getHashForLine(startLineB))) {
      return 0;
    }
    int length = 0;
    int ai = startLineA;
    int bi = startLineB;
    while (ai <= hashesA.length() && bi <= hashesB.length() && hashesA.getHashForLine(ai).equals(hashesB.getHashForLine(bi))) {
      ai++;
      bi++;
      length++;
    }
    ai = startLineA;
    bi = startLineB;
    while (ai > 0 && bi > 0 && hashesA.getHashForLine(ai).equals(hashesB.getHashForLine(bi))) {
      ai--;
      bi--;
      length++;
    }
    // Note that position (startA, startB) was counted twice
    return length - 1;
  }

  private void map(Collection<RAW> raws, Collection<BASE> bases, Tracking<RAW, BASE> result) {
    for (RAW raw : raws) {
      for (BASE base : bases) {
        if (result.containsUnmatchedBase(base) && base.getRuleKey().equals(raw.getRuleKey())) {
          result.match(raw, base);
          break;
        }
      }
    }
  }

  private static <T extends Trackable> Multimap<Integer, T> groupByLine(Stream<T> trackables, BlockHashSequence hashSequence) {
    Multimap<Integer, T> result = LinkedHashMultimap.create();
    trackables.forEach(trackable -> {
      Integer line = trackable.getLine();
      if (hashSequence.hasLine(line)) {
        result.put(line, trackable);
      }
    });
    return result;
  }

  private static class LinePair {
    int baseLine;
    int rawLine;
    int weight;

    public LinePair(int baseLine, int rawLine, int weight) {
      this.baseLine = baseLine;
      this.rawLine = rawLine;
      this.weight = weight;
    }
  }

  private static class HashOccurrence {
    int baseLine;
    int rawLine;
    int baseCount;
    int rawCount;
  }

  private enum LinePairComparator implements Comparator<LinePair> {
    INSTANCE;

    @Override
    public int compare(LinePair o1, LinePair o2) {
      int weightDiff = o2.weight - o1.weight;
      if (weightDiff != 0) {
        return weightDiff;
      } else {
        return Math.abs(o1.baseLine - o1.rawLine) - Math.abs(o2.baseLine - o2.rawLine);
      }
    }
  }
}
