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
package org.sonar.duplications.detector.original;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.sonar.duplications.block.Block;
import org.sonar.duplications.block.ByteArray;
import org.sonar.duplications.index.CloneGroup;
import org.sonar.duplications.index.CloneIndex;
import org.sonar.duplications.index.ClonePart;

/**
 * Implementation of algorithm described in paper
 * <a href="http://www4.in.tum.de/~juergens/publications/icsm2010_crc.pdf">Index-Based Code Clone Detection: Incremental, Distributed, Scalable</a>
 * by Benjamin Hummel, Elmar Juergens, Michael Conradt and Lars Heinemann.
 */
public final class OriginalCloneDetectionAlgorithm {

  private final CloneIndex cloneIndex;
  private final Filter filter = new Filter();
  private String originResourceId;

  private OriginalCloneDetectionAlgorithm(CloneIndex cloneIndex) {
    this.cloneIndex = cloneIndex;
  }

  private BlocksGroup[] createGroups(Collection<Block> fileBlocks) {
    // 2: let f be the list of tuples corresponding to filename sorted by statement index
    // either read from the index or calculated on the fly
    int size = fileBlocks.size();

    // Godin: create one group per unique hash
    // TODO Godin: can we create map with expected size?
    Map<ByteArray, BlocksGroup> groupsByHash = new HashMap<>();
    for (Block fileBlock : fileBlocks) {
      ByteArray hash = fileBlock.getBlockHash();
      BlocksGroup sameHash = groupsByHash.get(hash);
      if (sameHash == null) {
        sameHash = BlocksGroup.empty();
        groupsByHash.put(hash, sameHash);
      }
      sameHash.blocks.add(fileBlock);
    }

    // Godin: retrieve blocks from index
    for (Map.Entry<ByteArray, BlocksGroup> entry : groupsByHash.entrySet()) {
      ByteArray hash = entry.getKey();
      BlocksGroup group = entry.getValue();
      for (Block blockFromIndex : cloneIndex.getBySequenceHash(hash)) {
        // Godin: skip blocks for this file if they come from index
        if (!originResourceId.equals(blockFromIndex.getResourceId())) {
          group.blocks.add(blockFromIndex);
        }
      }
      Collections.sort(group.blocks, BlocksGroup.BlockComparator.INSTANCE);
    }

    // 3: let c be a list with c(0) = empty
    BlocksGroup[] sameHashBlocksGroups = new BlocksGroup[size + 2];
    sameHashBlocksGroups[0] = BlocksGroup.empty();
    // 4: for i := 1 to length(f) do
    for (Block fileBlock : fileBlocks) {
      ByteArray hash = fileBlock.getBlockHash();
      int i = fileBlock.getIndexInFile() + 1;
      // 5: retrieve tuples with same sequence hash as f(i)
      // 6: store this set as c(i)
      sameHashBlocksGroups[i] = groupsByHash.get(hash);
    }

    // Godin: allows to report clones at the end of file, because condition at line 13 would be evaluated as true
    sameHashBlocksGroups[size + 1] = BlocksGroup.empty();

    return sameHashBlocksGroups;
  }

  private void findClones(Collection<Block> fileBlocks) {
    originResourceId = fileBlocks.iterator().next().getResourceId();

    BlocksGroup[] sameHashBlocksGroups = createGroups(fileBlocks);

    // 7: for i := 1 to length(c) do
    for (int i = 1; i < sameHashBlocksGroups.length; i++) {
      // In the main loop (starting from Line 7), we first check
      // whether any new clones might start at this position. If there
      // is only a single tuple with this hash (which has to belong
      // to the inspected file at the current location) we skip this loop
      // iteration. The same holds if all tuples at position i have already
      // been present at position i − 1, as in this case any clone group
      // found at position i would be included in a clone group starting
      // at position i − 1.

      // Although we use the subset operator in the
      // algorithm description, this is not really a subset operation,
      // as of course the statement index of the tuples in c(i) will be
      // increased by 1 compared to the corresponding ones in c(i − 1)
      // and the hash and info fields will differ.

      // 8: if |c(i)| < 2 or c(i) subsumed by c(i - 1) then
      if (sameHashBlocksGroups[i].size() < 2 || sameHashBlocksGroups[i].subsumedBy(sameHashBlocksGroups[i - 1], 1)) {
        // 9: continue with next loop iteration
        continue;
      }

      // The set a introduced in Line 10 is called the active set and
      // contains all tuples corresponding to clones which have not yet
      // been reported. At each iteration of the inner loop the set a
      // is reduced to tuples which are also present in c(j); again the
      // intersection operator has to account for the increased statement
      // index and different hash and info fields. The new value is
      // stored in a0. Clones are only reported, if tuples are lost in
      // Line 12, as otherwise all current clones could be prolonged
      // by one statement. Clone reporting matches tuples that, after
      // correction of the statement index, appear in both c(i) and a,
      // each matched pair corresponds to a single clone. Its location
      // can be extracted from the filename and info fields.

      // 10: let a := c(i)
      BlocksGroup currentBlocksGroup = sameHashBlocksGroups[i];
      // 11: for j := i + 1 to length(c) do
      for (int j = i + 1; j < sameHashBlocksGroups.length; j++) {
        // 12: let a0 := a intersect c(j)
        BlocksGroup intersectedBlocksGroup = currentBlocksGroup.intersect(sameHashBlocksGroups[j]);

        // 13: if |a0| < |a| then
        if (intersectedBlocksGroup.size() < currentBlocksGroup.size()) {
          // 14: report clones from c(i) to a (see text)

          // One problem of this algorithm is that clone classes with
          // multiple instances in the same file are encountered and
          // reported multiple times. Furthermore, when calculating the clone
          // groups for all files in a system, clone groups will be reported
          // more than once as well. Both cases can be avoided, by
          // checking whether the first element of a0 (with respect to a
          // fixed order) is equal to f(j) and only report in this case.

          Block first = currentBlocksGroup.first(originResourceId);
          if (first != null && first.getIndexInFile() == j - 2) {
            // Godin: We report clones, which start in i-1 and end in j-2, so length is j-2-(i-1)+1=j-i
            reportClones(sameHashBlocksGroups[i], currentBlocksGroup, j - i);
          }
        }
        // 15: a := a0
        currentBlocksGroup = intersectedBlocksGroup;

        // Line 16 early exits the inner loop if either no more clones are starting
        // from position i (i.e., a is too small), or if all tuples from a
        // have already been in c(i − 1), corrected for statement index.
        // In this case they have already been reported in the previous
        // iteration of the outer loop.

        // IMPORTANT Godin: note that difference in indexes between "a" and "c(i-1)" greater than one,
        // so method subsumedBy should take this into account

        // 16: if |a| < 2 or a subsumed by c(i-1) then
        if (currentBlocksGroup.size() < 2 || currentBlocksGroup.subsumedBy(sameHashBlocksGroups[i - 1], j - i + 1)) {
          // 17: break inner loop
          break;
        }
      }
    }
  }

  private void reportClones(BlocksGroup beginGroup, BlocksGroup endGroup, int cloneLength) {
    List<Block[]> pairs = beginGroup.pairs(endGroup, cloneLength);

    ClonePart origin = null;
    List<ClonePart> parts = new ArrayList<>();

    for (int i = 0; i < pairs.size(); i++) {
      Block[] pair = pairs.get(i);
      Block firstBlock = pair[0];
      Block lastBlock = pair[1];
      ClonePart part = new ClonePart(firstBlock.getResourceId(),
        firstBlock.getIndexInFile(),
        firstBlock.getStartLine(),
        lastBlock.getEndLine());

      if (originResourceId.equals(part.getResourceId())) {
        if (origin == null || part.getUnitStart() < origin.getUnitStart()) {
          origin = part;
        }
      }

      parts.add(part);
    }

    filter.add(CloneGroup.builder().setLength(cloneLength).setOrigin(origin).setParts(parts).build());
  }

  /**
   * Performs detection and returns list of clone groups between file (which represented as a collection of blocks) and index.
   * Note that this method ignores blocks for this file, that will be retrieved from index.
   */
  public static List<CloneGroup> detect(CloneIndex cloneIndex, Collection<Block> fileBlocks) {
    if (fileBlocks.isEmpty()) {
      return Collections.emptyList();
    }
    OriginalCloneDetectionAlgorithm reporter = new OriginalCloneDetectionAlgorithm(cloneIndex);
    reporter.findClones(fileBlocks);
    return reporter.filter.getResult();
  }
}
