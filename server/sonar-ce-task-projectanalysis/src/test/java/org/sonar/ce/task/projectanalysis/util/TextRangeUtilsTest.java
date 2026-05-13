/*
 * SonarQube
 * Copyright (C) SonarSource Sàrl
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
package org.sonar.ce.task.projectanalysis.util;

import org.junit.jupiter.api.Test;
import org.sonar.scanner.protocol.output.ScannerReport;

import static org.assertj.core.api.Assertions.assertThat;

class TextRangeUtilsTest {

  private static ScannerReport.TextRange range(int startLine, int endLine) {
    return ScannerReport.TextRange.newBuilder()
      .setStartLine(startLine)
      .setEndLine(endLine)
      .build();
  }

  @Test
  void containsLine_whenLineIsBeforeRange_returnsFalse() {
    assertThat(TextRangeUtils.containsLine(range(5, 10), 4)).isFalse();
  }

  @Test
  void containsLine_whenLineIsAfterRange_returnsFalse() {
    assertThat(TextRangeUtils.containsLine(range(5, 10), 11)).isFalse();
  }

  @Test
  void containsLine_whenLineIsAtStartOfRange_returnsTrue() {
    assertThat(TextRangeUtils.containsLine(range(5, 10), 5)).isTrue();
  }

  @Test
  void containsLine_whenLineIsAtEndOfRange_returnsTrue() {
    assertThat(TextRangeUtils.containsLine(range(5, 10), 10)).isTrue();
  }

  @Test
  void containsLine_whenLineIsInsideRange_returnsTrue() {
    assertThat(TextRangeUtils.containsLine(range(5, 10), 7)).isTrue();
  }

  @Test
  void containsLine_whenRangeIsSingleLine_returnsTrueOnlyForThatLine() {
    assertThat(TextRangeUtils.containsLine(range(5, 5), 5)).isTrue();
    assertThat(TextRangeUtils.containsLine(range(5, 5), 4)).isFalse();
    assertThat(TextRangeUtils.containsLine(range(5, 5), 6)).isFalse();
  }
}
