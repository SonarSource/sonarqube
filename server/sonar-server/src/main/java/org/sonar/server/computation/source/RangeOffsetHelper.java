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

package org.sonar.server.computation.source;

import org.sonar.batch.protocol.output.BatchReport;

public class RangeOffsetHelper {

  static final String OFFSET_SEPARATOR = ",";
  static final String SYMBOLS_SEPARATOR = ";";

  private RangeOffsetHelper() {
    // Only static methods
  }

  public static String offsetToString(BatchReport.Range range, int lineIndex, int lineLength) {
    StringBuilder element = new StringBuilder();

    validateOffsetOrder(range, lineIndex);
    validateStartOffsetNotGreaterThanLineLength(range, lineLength, lineIndex);
    validateEndOffsetNotGreaterThanLineLength(range, lineLength, lineIndex);

    int startOffset = range.getStartLine() == lineIndex ? range.getStartOffset() : 0;
    int endOffset = range.getEndLine() == lineIndex ? range.getEndOffset() : lineLength;

    if (startOffset < endOffset) {
      element.append(startOffset).append(OFFSET_SEPARATOR);
      element.append(endOffset);
    }

    return element.toString();
  }

  private static void validateOffsetOrder(BatchReport.Range range, int line) {
    if (range.getStartLine() == range.getEndLine() && range.getStartOffset() > range.getEndOffset()) {
      throw new IllegalArgumentException(String.format("End offset %s cannot be defined before start offset %s on line %s", range.getEndOffset(), range.getStartOffset(), line));
    }
  }

  private static void validateStartOffsetNotGreaterThanLineLength(BatchReport.Range range, int lineLength, int line) {
    if (range.getStartLine() == line && range.getStartOffset() > lineLength) {
      throw new IllegalArgumentException(String.format("Start offset %s is defined outside the length (%s) of the line %s", range.getStartOffset(), lineLength, line));
    }
  }

  private static void validateEndOffsetNotGreaterThanLineLength(BatchReport.Range range, int lineLength, int line) {
    if (range.getEndLine() == line && range.getEndOffset() > lineLength) {
      throw new IllegalArgumentException(String.format("End offset %s is defined outside the length (%s) of the line %s", range.getEndOffset(), lineLength, line));
    }
  }

}
