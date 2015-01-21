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
package org.sonar.batch.issue.tracking;

import javax.annotation.Nullable;

public class IssueTrackingBlocksRecognizer {

  private final FileHashes a;
  private final FileHashes b;

  public IssueTrackingBlocksRecognizer(FileHashes a, FileHashes b) {
    this.a = a;
    this.b = b;
  }

  public boolean isValidLineInReference(@Nullable Integer line) {
    return (line != null) && (0 <= line - 1) && (line - 1 < a.length());
  }

  public boolean isValidLineInSource(@Nullable Integer line) {
    return (line != null) && (0 <= line - 1) && (line - 1 < b.length());
  }

  /**
   * @param startA number of line from first version of text (numbering starts from 1)
   * @param startB number of line from second version of text (numbering starts from 1)
   */
  public int computeLengthOfMaximalBlock(int startA, int startB) {
    if (!a.getHash(startA).equals(b.getHash(startB))) {
      return 0;
    }
    int length = 0;
    int ai = startA;
    int bi = startB;
    while (ai <= a.length() && bi <= b.length() && a.getHash(ai).equals(b.getHash(bi))) {
      ai++;
      bi++;
      length++;
    }
    ai = startA;
    bi = startB;
    while (ai > 0 && bi > 0 && a.getHash(ai).equals(b.getHash(bi))) {
      ai--;
      bi--;
      length++;
    }
    // Note that position (startA, startB) was counted twice
    return length - 1;
  }

}
