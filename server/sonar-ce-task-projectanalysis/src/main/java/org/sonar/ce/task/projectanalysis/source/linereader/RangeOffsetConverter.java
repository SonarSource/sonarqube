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
package org.sonar.ce.task.projectanalysis.source.linereader;

import org.sonar.scanner.protocol.output.ScannerReport;

import static java.lang.String.format;

public class RangeOffsetConverter {

  static final String OFFSET_SEPARATOR = ",";
  static final String SYMBOLS_SEPARATOR = ";";

  public String offsetToString(ScannerReport.TextRange range, int lineIndex, int lineLength) {
    validateOffsetOrder(range, lineIndex);
    validateStartOffsetNotGreaterThanLineLength(range, lineLength, lineIndex);
    validateEndOffsetNotGreaterThanLineLength(range, lineLength, lineIndex);

    int startOffset = range.getStartLine() == lineIndex ? range.getStartOffset() : 0;
    int endOffset = range.getEndLine() == lineIndex ? range.getEndOffset() : lineLength;

    StringBuilder element = new StringBuilder();
    if (startOffset < endOffset) {
      element.append(startOffset).append(OFFSET_SEPARATOR);
      element.append(endOffset);
    }

    return element.toString();
  }

  private static void validateOffsetOrder(ScannerReport.TextRange range, int line) {
    checkExpression(range.getStartLine() != range.getEndLine() || range.getStartOffset() <= range.getEndOffset(),
      "End offset %s cannot be defined before start offset %s on line %s", range.getEndOffset(), range.getStartOffset(), line);
  }

  private static void validateStartOffsetNotGreaterThanLineLength(ScannerReport.TextRange range, int lineLength, int line) {
    checkExpression(range.getStartLine() != line || range.getStartOffset() <= lineLength,
      "Start offset %s is defined outside the length (%s) of the line %s", range.getStartOffset(), lineLength, line);
  }

  private static void validateEndOffsetNotGreaterThanLineLength(ScannerReport.TextRange range, int lineLength, int line) {
    checkExpression(range.getEndLine() != line || range.getEndOffset() <= lineLength,
      "End offset %s is defined outside the length (%s) of the line %s", range.getEndOffset(), lineLength, line);
  }

  private static void checkExpression(boolean expression, String errorMessageTemplate, Object... errorMessageArgs) {
    if (!expression) {
      throw new RangeOffsetConverterException(format(errorMessageTemplate, errorMessageArgs));
    }
  }

  public static class RangeOffsetConverterException extends RuntimeException {
    public RangeOffsetConverterException(String message) {
      super(message);
    }
  }

}
