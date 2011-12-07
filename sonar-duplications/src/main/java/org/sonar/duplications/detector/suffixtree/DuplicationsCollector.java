/*
 * Sonar, open source software quality management tool.
 * Copyright (C) 2008-2011 SonarSource
 * mailto:contact AT sonarsource DOT com
 *
 * Sonar is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * Sonar is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with Sonar; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02
 */
package org.sonar.duplications.detector.suffixtree;

import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;

import org.sonar.duplications.block.Block;
import org.sonar.duplications.index.CloneGroup;
import org.sonar.duplications.index.ClonePart;
import org.sonar.duplications.utils.FastStringComparator;
import org.sonar.duplications.utils.SortedListsUtils;

import com.google.common.collect.Lists;

/**
 * Implementation of {@link Search.Collector}, which constructs {@link CloneGroup}s.
 */
public class DuplicationsCollector implements Search.Collector {

  private final TextSet text;
  private final String originResourceId;

  /**
   * Note that LinkedList should provide better performance here, because of use of operation remove.
   * 
   * @see #filter(CloneGroup)
   */
  private final List<CloneGroup> filtered = Lists.newLinkedList();

  private int length;
  private ClonePart origin;
  private final List<ClonePart> parts = Lists.newArrayList();

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

  public void part(int start, int end, int len) {
    length = len;
    Block firstBlock = text.getBlock(start);
    Block lastBlock = text.getBlock(end - 1);

    ClonePart part = new ClonePart(
        firstBlock.getResourceId(),
        firstBlock.getIndexInFile(),
        firstBlock.getFirstLineNumber(),
        lastBlock.getLastLineNumber());

    // TODO Godin: maybe use FastStringComparator here ?
    if (originResourceId.equals(part.getResourceId())) { // part from origin
      if (origin == null) {
        origin = part;
      } else if (part.getUnitStart() < origin.getUnitStart()) {
        origin = part;
      }
    }

    parts.add(part);
  }

  public void endOfGroup() {
    Collections.sort(parts, CLONEPART_COMPARATOR);
    CloneGroup group = new CloneGroup(length, origin, parts);
    filter(group);

    parts.clear();
    origin = null;
  }

  private void filter(CloneGroup current) {
    Iterator<CloneGroup> i = filtered.iterator();
    while (i.hasNext()) {
      CloneGroup earlier = i.next();
      // Note that following two conditions cannot be true together - proof by contradiction:
      // let C be the current clone and A and B were found earlier
      // then since relation is transitive - (A in C) and (C in B) => (A in B)
      // so A should be filtered earlier
      if (containsIn(current, earlier)) {
        // current clone fully covered by clone, which was found earlier
        return;
      }
      // TODO Godin: must prove that this is unused
      // if (containsIn(earlier, current)) {
      // // current clone fully covers clone, which was found earlier
      // i.remove();
      // }
    }
    filtered.add(current);
  }

  private static final Comparator<ClonePart> CLONEPART_COMPARATOR = new Comparator<ClonePart>() {
    public int compare(ClonePart o1, ClonePart o2) {
      int c = RESOURCE_ID_COMPARATOR.compare(o1, o2);
      if (c == 0) {
        return o1.getUnitStart() - o2.getUnitStart();
      }
      return c;
    }
  };

  private static final Comparator<ClonePart> RESOURCE_ID_COMPARATOR = new Comparator<ClonePart>() {
    public int compare(ClonePart o1, ClonePart o2) {
      return FastStringComparator.INSTANCE.compare(o1.getResourceId(), o2.getResourceId());
    }
  };

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
   * This method uses the fact that all parts already sorted by resourceId and unitStart (see {@link #CLONEPART_COMPARATOR}),
   * so running time - O(|A|+|B|).
   * </p>
   */
  private static boolean containsIn(CloneGroup first, CloneGroup second) {
    // TODO Godin: must prove that this is unused
    // if (!first.getOriginPart().getResourceId().equals(second.getOriginPart().getResourceId())) {
    // return false;
    // }
    // if (first.getCloneUnitLength() > second.getCloneUnitLength()) {
    // return false;
    // }
    List<ClonePart> firstParts = first.getCloneParts();
    List<ClonePart> secondParts = second.getCloneParts();
    return SortedListsUtils.contains(secondParts, firstParts, new ContainsInComparator(first.getCloneUnitLength(), second.getCloneUnitLength()))
        && SortedListsUtils.contains(firstParts, secondParts, RESOURCE_ID_COMPARATOR);
  }

  private static class ContainsInComparator implements Comparator<ClonePart> {
    private final int l1, l2;

    public ContainsInComparator(int l1, int l2) {
      this.l1 = l1;
      this.l2 = l2;
    }

    public int compare(ClonePart o1, ClonePart o2) {
      int c = RESOURCE_ID_COMPARATOR.compare(o1, o2);
      if (c == 0) {
        if (o2.getUnitStart() <= o1.getUnitStart()) {
          if (o1.getUnitStart() + l1 <= o2.getUnitStart() + l2) {
            return 0; // match found - stop search
          } else {
            return 1; // continue search
          }
        } else {
          return -1; // o1 < o2 by unitStart - stop search
        }
      } else {
        return c;
      }
    }
  }

}
