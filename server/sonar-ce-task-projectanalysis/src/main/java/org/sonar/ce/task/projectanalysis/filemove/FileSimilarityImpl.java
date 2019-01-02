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

public class FileSimilarityImpl implements FileSimilarity {

  private final SourceSimilarity sourceSimilarity;

  public FileSimilarityImpl(SourceSimilarity sourceSimilarity) {
    this.sourceSimilarity = sourceSimilarity;
  }

  @Override
  public int score(File file1, File file2) {
    // Algorithm could be improved by increasing score
    // depending on filename similarity
    // Current implementation relies on file content only.

    return sourceSimilarity.score(file1.getLineHashes(), file2.getLineHashes());
  }
}
