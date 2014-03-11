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

import java.util.Arrays;
import java.util.List;


public final class DsmManualSorter<V> {

  private final Dsm<V> dsm;
  private final List<V> verticesInDesiredOrder;

  private DsmManualSorter(Dsm<V> dsm, List<V> verticesInDesiredOrder) {
    this.dsm = dsm;
    this.verticesInDesiredOrder = verticesInDesiredOrder;
  }

  public static <V> void sort(Dsm<V> dsm, List<V> vertices) {
    DsmManualSorter<V> sorter = new DsmManualSorter<V>(dsm, vertices);
    sorter.sort();
  }

  public static <V> void sort(Dsm<V> dsm, V... vertices) {
    sort(dsm, Arrays.asList(vertices));
  }

  private void sort() {
    for (int desiredIndex = 0; desiredIndex < verticesInDesiredOrder.size(); desiredIndex++) {
      int currentIndex = getCurrentIndex(verticesInDesiredOrder.get(desiredIndex));
      dsm.permute(currentIndex, desiredIndex);
    }
  }

  private int getCurrentIndex(V v) {
    for (int currentIndex = 0; currentIndex < dsm.getVertices().length; currentIndex++) {
      if (dsm.getVertices()[currentIndex].equals(v)) {
        return currentIndex;
      }
    }
    throw new IllegalStateException("Vertex " + v + " is not contained in the DSM.");
  }

}
