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
package org.sonar.ce.task.projectanalysis.filemove;

import java.util.List;

import static java.lang.Math.max;
import static java.lang.Math.min;

public class SourceSimilarityImpl implements SourceSimilarity {

  @Override
  public <T> int score(List<T> left, List<T> right) {
    if (left.isEmpty() && right.isEmpty()) {
      return 0;
    }
    int distance = levenshteinDistance(left, right);
    return (int) (100 * (1.0 - ((double) distance) / (max(left.size(), right.size()))));
  }

  private static <T> int levenshteinDistance(List<T> left, List<T> right) {
    int len0 = left.size() + 1;
    int len1 = right.size() + 1;

    // the array of distances
    int[] cost = new int[len0];
    int[] newcost = new int[len0];

    // initial cost of skipping prefix in String s0
    for (int i = 0; i < len0; i++) {
      cost[i] = i;
    }

    // dynamically computing the array of distances

    // transformation cost for each letter in s1
    for (int j = 1; j < len1; j++) {
      // initial cost of skipping prefix in String s1
      newcost[0] = j;

      // transformation cost for each letter in s0
      for (int i = 1; i < len0; i++) {
        // matching current letters in both strings
        int match = left.get(i - 1).equals(right.get(j - 1)) ? 0 : 1;

        // computing cost for each transformation
        int costReplace = cost[i - 1] + match;
        int costInsert = cost[i] + 1;
        int costDelete = newcost[i - 1] + 1;

        // keep minimum cost
        newcost[i] = min(min(costInsert, costDelete), costReplace);
      }

      // swap cost/newcost arrays
      int[] swap = cost;
      cost = newcost;
      newcost = swap;
    }

    // the distance is the cost for transforming all letters in both strings
    return cost[len0 - 1];
  }
}
