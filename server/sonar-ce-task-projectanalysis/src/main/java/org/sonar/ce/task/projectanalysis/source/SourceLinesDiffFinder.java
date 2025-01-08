/*
 * SonarQube
 * Copyright (C) 2009-2025 SonarSource SA
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
package org.sonar.ce.task.projectanalysis.source;

import difflib.myers.DifferentiationFailedException;
import difflib.myers.MyersDiff;
import difflib.myers.PathNode;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SourceLinesDiffFinder {
  private static final Logger LOG = LoggerFactory.getLogger(SourceLinesDiffFinder.class);

  public int[] findMatchingLines(List<String> left, List<String> right) {
    int[] index = new int[right.size()];

    int dbLine = left.size();
    int reportLine = right.size();
    try {
      PathNode node = new MyersDiff<String>().buildPath(left, right);

      while (node.prev != null) {
        PathNode prevNode = node.prev;

        if (!node.isSnake()) {
          // additions
          reportLine -= (node.j - prevNode.j);
          // removals
          dbLine -= (node.i - prevNode.i);
        } else {
          // matches
          for (int i = node.i; i > prevNode.i; i--) {
            index[reportLine - 1] = dbLine;
            reportLine--;
            dbLine--;
          }
        }
        node = prevNode;
      }
    } catch (DifferentiationFailedException e) {
      LOG.error("Error finding matching lines", e);
      return index;
    }
    return index;
  }

}
