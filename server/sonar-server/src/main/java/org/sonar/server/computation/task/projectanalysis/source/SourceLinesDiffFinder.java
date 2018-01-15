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

import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class SourceLinesDiffFinder {

  private final List<String> database;
  private final List<String> report;

  public SourceLinesDiffFinder(List<String> database, List<String> report) {
    this.database = database;
    this.report = report;
  }

  public Set<Integer> findNewOrChangedLines() {
    return walk(0, 0, new HashSet<>());
  }

  private Set<Integer> walk(int r, int db, HashSet<Integer> acc) {

    if (r >= report.size()) {
      return acc;
    }

    if (db < database.size()) {

      if (report.get(r).equals(database.get(db))) {
        walk(stepIndex(r), stepIndex(db), acc);
        return acc;
      }

      List<String> remainingDatabase = database.subList(db, database.size());
      if (remainingDatabase.contains(report.get(r))) {
        walk(r, db + remainingDatabase.indexOf(report.get(r)), acc);
        return acc;
      }

    }

    acc.add(r);
    walk(stepIndex(r), db, acc);
    return acc;
  }

  private static int stepIndex(int r) {
    return ++r;
  }

}
