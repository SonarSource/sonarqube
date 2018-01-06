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
package org.sonar.server.computation.task.projectanalysis.filemove;

import java.util.Map;
import java.util.Objects;
import java.util.Set;

final class ScoreMatrix {
  private final Set<String> dbFileKeys;
  private final Map<String, FileSimilarity.File> reportFileSourcesByKey;
  private final int[][] scores;
  private final int maxScore;

  public ScoreMatrix(Set<String> dbFileKeys, Map<String, FileSimilarity.File> reportFileSourcesByKey, int[][] scores, int maxScore) {
    this.dbFileKeys = dbFileKeys;
    this.reportFileSourcesByKey = reportFileSourcesByKey;
    this.scores = scores;
    this.maxScore = maxScore;
  }

  public void accept(ScoreMatrixVisitor visitor) {
    int dbFileIndex = 0;
    for (String dbFileKey : dbFileKeys) {
      int reportFileIndex = 0;
      for (Map.Entry<String, FileSimilarity.File> reportFileSourceAndKey : reportFileSourcesByKey.entrySet()) {
        int score = scores[dbFileIndex][reportFileIndex];
        visitor.visit(dbFileKey, reportFileSourceAndKey.getKey(), score);
        reportFileIndex++;
      }
      dbFileIndex++;
    }
  }

  public String toCsv(char separator) {
    StringBuilder res = new StringBuilder();
    // first row: empty column, then one column for each report file (its key)
    res.append(separator);
    for (Map.Entry<String, FileSimilarity.File> reportEntry : reportFileSourcesByKey.entrySet()) {
      res.append(reportEntry.getKey()).append(separator);
    }
    // rows with data: column with db file (its key), then one column for each value
    accept(new ScoreMatrixVisitor() {
      private String previousDbFileKey = null;

      @Override
      public void visit(String dbFileKey, String reportFileKey, int score) {
        if (!Objects.equals(previousDbFileKey, dbFileKey)) {
          res.append('\n').append(dbFileKey).append(separator);
          previousDbFileKey = dbFileKey;
        }
        res.append(score).append(separator);
      }
    });
    return res.toString();
  }

  @FunctionalInterface
  public interface ScoreMatrixVisitor {
    void visit(String dbFileKey, String reportFileKey, int score);
  }

  public int getMaxScore() {
    return maxScore;
  }
}
