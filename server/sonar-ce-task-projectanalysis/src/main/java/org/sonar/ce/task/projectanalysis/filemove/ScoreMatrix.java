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

import java.util.Arrays;

final class ScoreMatrix {
  private final ScoreFile[] removedFiles;
  private final ScoreFile[] newFiles;
  private final int[][] scores;
  private final int maxScore;

  public ScoreMatrix(ScoreFile[] removedFiles, ScoreFile[] newFiles, int[][] scores, int maxScore) {
    this.removedFiles = removedFiles;
    this.newFiles = newFiles;
    this.scores = scores;
    this.maxScore = maxScore;
  }

  public void accept(ScoreMatrixVisitor visitor) {
    for (int removedFileIndex = 0; removedFileIndex < removedFiles.length; removedFileIndex++) {
      for (int newFileIndex = 0; newFileIndex < newFiles.length; newFileIndex++) {
        int score = scores[removedFileIndex][newFileIndex];
        visitor.visit(removedFiles[removedFileIndex], newFiles[newFileIndex], score);
      }
    }
  }

  public String toCsv(char separator) {
    StringBuilder res = new StringBuilder();
    // first row: empty column, then one column for each report file (its uuid)
    res.append("newFiles=>").append(separator);
    Arrays.stream(newFiles).forEach(f -> res.append(f.getFileUuid()).append('(').append(f.getLineCount()).append(')').append(separator));
    // rows with data: column with db file (its uuid), then one column for each value
    accept(new ScoreMatrixVisitor() {
      private ScoreFile previousRemovedFile = null;

      @Override
      public void visit(ScoreFile removedFile, ScoreFile newFile, int score) {
        if (previousRemovedFile != removedFile) {
          res.append('\n').append(removedFile.getFileUuid()).append('(').append(removedFile.getLineCount()).append(')').append(separator);
          previousRemovedFile = removedFile;
        }
        res.append(score).append(separator);
      }
    });
    return res.toString();
  }

  @FunctionalInterface
  public interface ScoreMatrixVisitor {
    void visit(ScoreFile removedFile, ScoreFile newFile, int score);
  }

  public int getMaxScore() {
    return maxScore;
  }

  static class ScoreFile {
    private final String fileUuid;
    private final int lineCount;

    ScoreFile(String fileUuid, int lineCount) {
      this.fileUuid = fileUuid;
      this.lineCount = lineCount;
    }

    public String getFileUuid() {
      return fileUuid;
    }

    public int getLineCount() {
      return lineCount;
    }

  }
}
