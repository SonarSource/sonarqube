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
package org.sonar.server.computation.task.projectanalysis.source;

import difflib.myers.MyersDiff;
import difflib.myers.PathNode;
import java.util.List;

public class SourceLinesDiffFinder {

  private final List<String> database;
  private final List<String> report;

  public SourceLinesDiffFinder(List<String> database, List<String> report) {
    this.database = database;
    this.report = report;
  }

  /**
   * Creates a diff between the file in the database and the file in the report using Myers' algorithm, and links matching lines between
   * both files.
   * @return an array with one entry for each line in the report. Those entries point either to a line in the database, or to 0, 
   * in which case it means the line was added.
   */
  public int[] findMatchingLines() {
    int[] index = new int[report.size()];

    int dbLine = database.size();
    int reportLine = report.size();
    try {
      PathNode node = MyersDiff.buildPath(database.toArray(), report.toArray());

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
    } catch (Exception e) {
      return index;
    }
    return index;
  }

}
