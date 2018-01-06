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
package org.sonar.duplications.detector.original;

import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import org.sonar.duplications.detector.ContainsInComparator;
import org.sonar.duplications.index.CloneGroup;
import org.sonar.duplications.index.ClonePart;
import org.sonar.duplications.utils.SortedListsUtils;

/**
 * Performs incremental and brute force algorithm in order to filter clones, which are fully covered by other clones.
 * All clones for filtering must be of the same origin - there is no sanity check on this.
 * In a worst case it performs O(N*N) comparisons.
 * <p>
 * Godin: This implementation was chosen because it simple.
 * And I wasn't able to find big difference in performance with an interval tree, which we had for the moment of testing.
 * Whereas in fact I expected that interval tree would be better for this task.
 * Moreover with interval tree we also can use incremental approach,
 * but we did not had an implementation with remove operation for the moment of testing.
 * </p>
 */
final class Filter {

  /**
   * Note that LinkedList should provide better performance here, because of use of operation remove.
   * 
   * @see #add(CloneGroup)
   */
  private final List<CloneGroup> filtered = new LinkedList<>();

  /**
   * @return current results of filtering
   */
  public List<CloneGroup> getResult() {
    return filtered;
  }

  /**
   * Running time - O(N*2*C), where N - number of clones, which was found earlier and C - time of {@link #containsIn(CloneGroup, CloneGroup)}.
   */
  public void add(CloneGroup current) {
    Iterator<CloneGroup> i = filtered.iterator();
    while (i.hasNext()) {
      CloneGroup earlier = i.next();
      // Note that following two conditions cannot be true together - proof by contradiction:
      // let C be the current clone and A and B were found earlier
      // then since relation is transitive - (A in C) and (C in B) => (A in B)
      // so A should be filtered earlier
      if (Filter.containsIn(current, earlier)) {
        // current clone fully covered by clone, which was found earlier
        return;
      }
      if (Filter.containsIn(earlier, current)) {
        // current clone fully covers clone, which was found earlier
        i.remove();
      }
    }
    filtered.add(current);
  }

  /**
   * Checks that second clone contains first one.
   * <p>
   * Clone A is contained in another clone B, if every part pA from A has part pB in B,
   * which satisfy the conditions:
   * <pre>
   * (pA.resourceId == pB.resourceId) and (pB.unitStart <= pA.unitStart) and (pA.unitEnd <= pB.unitEnd)
   * </pre>
   * And all resourcesId from B exactly the same as all resourceId from A, which means that also every part pB from B has part pA in A,
   * which satisfy the condition:
   * <pre>
   * pB.resourceId == pA.resourceId
   * </pre>
   * So this relation is:
   * <ul>
   * <li>reflexive - A in A</li>
   * <li>transitive - (A in B) and (B in C) => (A in C)</li>
   * <li>antisymmetric - (A in B) and (B in A) <=> (A = B)</li>
   * </ul>
   * </p>
   * <p>
   * <strong>Important: this method relies on fact that all parts were already sorted by resourceId and unitStart by using
   * {@link BlocksGroup.BlockComparator}, which uses {@link org.sonar.duplications.utils.FastStringComparator} for comparison by resourceId.</strong>
   * </p>
   * <p>
   * Running time - O(|A|+|B|).
   * </p>
   */
  static boolean containsIn(CloneGroup first, CloneGroup second) {
    if (first.getCloneUnitLength() > second.getCloneUnitLength()) {
      return false;
    }
    List<ClonePart> firstParts = first.getCloneParts();
    List<ClonePart> secondParts = second.getCloneParts();
    return SortedListsUtils.contains(secondParts, firstParts, new ContainsInComparator(second.getCloneUnitLength(), first.getCloneUnitLength()))
      && SortedListsUtils.contains(firstParts, secondParts, ContainsInComparator.RESOURCE_ID_COMPARATOR);
  }

}
