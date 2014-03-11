/*
 * SonarQube, open source software quality management tool.
 * Copyright (C) 2008-2014 SonarSource
 * mailto:contact AT sonarsource DOT com
 *
 * SonarQube is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * SonarQube is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */
package org.sonar.graph;

public final class DsmTopologicalSorter<V> {

  private final Dsm<V> dsm;
  private int leftOrderedIndex;
  private int rightOrderedIndex;

  private DsmTopologicalSorter(Dsm<V> dsm) {
    this.dsm = dsm;
    leftOrderedIndex = 0;
    rightOrderedIndex = dsm.getDimension() - 1;
  }

  public static <V> void sort(Dsm<V> dsm) {
    DsmTopologicalSorter<V> partitionner = new DsmTopologicalSorter<V>(dsm);
    boolean dsmCanBeSorted = true;
    while (dsmCanBeSorted) {
      boolean dsmCanBeSortedOnLeft = partitionner.pushToLeftVerticesWithoutIncomingEdges();
      boolean dsmCanBeSortedOnRight = partitionner.pushToRightVerticesWithoutOutgointEdges();
      dsmCanBeSorted = dsmCanBeSortedOnLeft || dsmCanBeSortedOnRight;
    }
    boolean isCyclicGraph = partitionner.leftOrderedIndex < partitionner.rightOrderedIndex;
    if (isCyclicGraph) {
      throw new IllegalStateException("Can't sort a cyclic graph.");
    }
  }

  private boolean pushToLeftVerticesWithoutIncomingEdges() {
    boolean permutationsDone = false;
    for (int i = leftOrderedIndex; i <= rightOrderedIndex; i++) {
      if (dsm.getNumberOfIncomingEdges(i, leftOrderedIndex, rightOrderedIndex) == 0) {
        dsm.permute(i, leftOrderedIndex);
        leftOrderedIndex++;
        permutationsDone = true;
      }
    }
    return permutationsDone;
  }

  private boolean pushToRightVerticesWithoutOutgointEdges() {
    boolean permutationsDone = false;
    for (int i = leftOrderedIndex; i <= rightOrderedIndex; i++) {
      if (dsm.getNumberOfOutgoingEdges(i, leftOrderedIndex, rightOrderedIndex) == 0) {
        dsm.permute(i, rightOrderedIndex);
        rightOrderedIndex--;
        permutationsDone = true;
      }
    }
    return permutationsDone;
  }
}
