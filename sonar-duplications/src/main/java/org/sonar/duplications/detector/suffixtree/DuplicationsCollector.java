/*
 * SonarQube
 * Copyright (C) 2009-2018 SonarSource SA
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
package org.sonar.duplications.detector.suffixtree;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.sonar.duplications.block.Block;
import org.sonar.duplications.detector.ContainsInComparator;
import org.sonar.duplications.index.CloneGroup;
import org.sonar.duplications.index.ClonePart;
import org.sonar.duplications.utils.SortedListsUtils;

/**
 * Implementation of {@link Search.Collector}, which constructs {@link CloneGroup}s.
 */
public class DuplicationsCollector extends Search.Collector {

  private final TextSet text;
  private final String originResourceId;

  private final List<CloneGroup> filtered = new ArrayList<>();

  private int length;
  private int count;
  private int[][] blockNumbers;

  public DuplicationsCollector(TextSet text) {
    this.text = text;
    this.originResourceId = text.getBlock(0).getResourceId();
  }

  /**
   * @return current result
   */
  public List<CloneGroup> getResult() {
    return filtered;
  }

  @Override
  public void startOfGroup(int size, int length) {
    this.blockNumbers = new int[size][2];
    this.length = length;
  }

  /**
   * Constructs ClonePart and saves it for future processing in {@link #endOfGroup()}.
   *
   * @param start number of first block from text for this part
   * @param end number of last block from text for this part
   */
  @Override
  public void part(int start, int end) {
    blockNumbers[count][0] = start;
    blockNumbers[count][1] = end - 1;
    count++;
  }

  /**
   * Constructs CloneGroup and saves it.
   */
  @Override
  public void endOfGroup() {
    ClonePart origin = null;

    CloneGroup.Builder builder = CloneGroup.builder().setLength(length);

    List<ClonePart> parts = new ArrayList<>(count);
    for (int[] b : blockNumbers) {
      Block firstBlock = text.getBlock(b[0]);
      Block lastBlock = text.getBlock(b[1]);
      ClonePart part = new ClonePart(
        firstBlock.getResourceId(),
        firstBlock.getIndexInFile(),
        firstBlock.getStartLine(),
        lastBlock.getEndLine());

      // TODO Godin: maybe use FastStringComparator here ?
      if (originResourceId.equals(part.getResourceId())) {
        // part from origin
        if (origin == null) {
          origin = part;
          // To calculate length important to use the origin, because otherwise block may come from DB without required data
          builder.setLengthInUnits(lastBlock.getEndUnit() - firstBlock.getStartUnit() + 1);
        } else if (part.getUnitStart() < origin.getUnitStart()) {
          origin = part;
        }
      }

      parts.add(part);
    }

    Collections.sort(parts, ContainsInComparator.CLONEPART_COMPARATOR);
    builder.setOrigin(origin).setParts(parts);

    filter(builder.build());

    reset();
  }

  /**
   * Prepare for processing of next duplication.
   */
  private void reset() {
    blockNumbers = null;
    count = 0;
  }

  /**
   * Saves CloneGroup, if it is not included into previously saved.
   * <p>
   * Current CloneGroup can not include none of CloneGroup, which were constructed before.
   * Proof:
   * According to an order of visiting nodes in suffix tree - length of earlier >= length of current.
   * If length of earlier > length of current, then earlier not contained in current.
   * If length of earlier = length of current, then earlier can be contained in current only
   * when current has exactly the same and maybe some additional CloneParts as earlier,
   * what in his turn will mean that two inner-nodes on same depth will satisfy condition
   * current.startSize <= earlier.startSize <= earlier.endSize <= current.endSize , which is not possible for different inner-nodes on same depth.
   * </p>
   * Thus this method checks only that none of CloneGroup, which was constructed before, does not include current CloneGroup.
   */
  private void filter(CloneGroup current) {
    for (CloneGroup earlier : filtered) {
      if (containsIn(current, earlier)) {
        return;
      }
    }
    filtered.add(current);
  }

  /**
   * Checks that second CloneGroup includes first one.
   * <p>
   * CloneGroup A is included in another CloneGroup B, if every part pA from A has part pB in B,
   * which satisfy the conditions:
   * <pre>
   * (pA.resourceId == pB.resourceId) and (pB.unitStart <= pA.unitStart) and (pA.unitEnd <= pB.unitEnd)
   * </pre>
   * And all resourcesId from B exactly the same as all resourceId from A, which means that also every part pB from B has part pA in A,
   * which satisfy the condition:
   * <pre>
   * pB.resourceId == pA.resourceId
   * </pre>
   * Inclusion is the partial order, thus this relation is:
   * <ul>
   * <li>reflexive - A in A</li>
   * <li>transitive - (A in B) and (B in C) => (A in C)</li>
   * <li>antisymmetric - (A in B) and (B in A) <=> (A = B)</li>
   * </ul>
   * </p>
   * <p>
   * This method uses the fact that all parts already sorted by resourceId and unitStart (see {@link ContainsInComparator#CLONEPART_COMPARATOR}),
   * so running time - O(|A|+|B|).
   * </p>
   */
  private static boolean containsIn(CloneGroup first, CloneGroup second) {
    List<ClonePart> firstParts = first.getCloneParts();
    List<ClonePart> secondParts = second.getCloneParts();
    // TODO Godin: according to tests seems that if first part of condition is true, then second part can not be false
    // if this can be proved, then second part can be removed
    return SortedListsUtils.contains(secondParts, firstParts, new ContainsInComparator(second.getCloneUnitLength(), first.getCloneUnitLength()))
      && SortedListsUtils.contains(firstParts, secondParts, ContainsInComparator.RESOURCE_ID_COMPARATOR);
  }

}
