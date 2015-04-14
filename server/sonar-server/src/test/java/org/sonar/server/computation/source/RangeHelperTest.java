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

import org.junit.Test;
import org.sonar.batch.protocol.output.BatchReport;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Fail.failBecauseExceptionWasNotThrown;

public class RangeHelperTest {

  @Test
  public void append_range() throws Exception {
    StringBuilder element = new StringBuilder();
    RangeHelper.appendRange(element, BatchReport.Range.newBuilder()
      .setStartLine(1).setEndLine(1)
      .setStartOffset(2).setEndOffset(3)
      .build(),
      1, 5);
    assertThat(element.toString()).isEqualTo("2,3,");
  }

  @Test
  public void append_range_om_existing_element() throws Exception {
    StringBuilder element = new StringBuilder("1,2,a");
    RangeHelper.appendRange(element, BatchReport.Range.newBuilder()
      .setStartLine(1).setEndLine(1)
      .setStartOffset(3).setEndOffset(4)
      .build(),
      1, 5);
    assertThat(element.toString()).isEqualTo("1,2,a;3,4,");
  }

  @Test
  public void append_range_not_finishing_in_current_line() throws Exception {
    StringBuilder element = new StringBuilder();
    RangeHelper.appendRange(element, BatchReport.Range.newBuilder()
      .setStartLine(1).setEndLine(3)
      .setStartOffset(2).setEndOffset(3)
      .build(),
      1, 5);
    assertThat(element.toString()).isEqualTo("2,4,");
  }

  @Test
  public void append_range_that_began_in_previous_line_and_finish_in_current_line() throws Exception {
    StringBuilder element = new StringBuilder();
    RangeHelper.appendRange(element, BatchReport.Range.newBuilder()
      .setStartLine(1).setEndLine(3)
      .setStartOffset(2).setEndOffset(3)
      .build(),
      3, 5);
    assertThat(element.toString()).isEqualTo("0,3,");
  }

  @Test
  public void append_range_that_began_in_previous_line_and_not_finishing_in_current_line() throws Exception {
    StringBuilder element = new StringBuilder();
    RangeHelper.appendRange(element, BatchReport.Range.newBuilder()
      .setStartLine(1).setEndLine(3)
      .setStartOffset(2).setEndOffset(3)
      .build(),
      2, 5);
    assertThat(element.toString()).isEqualTo("0,4,");
  }

  @Test
  public void fail_when_end_offset_is_before_start_offset() {
    try {
      RangeHelper.appendRange(new StringBuilder(), BatchReport.Range.newBuilder()
        .setStartLine(1).setEndLine(1)
        .setStartOffset(4).setEndOffset(2)
        .build(),
        1, 5);
      failBecauseExceptionWasNotThrown(IllegalArgumentException.class);
    } catch (IllegalArgumentException e) {
      assertThat(e).hasMessage("End offset 2 cannot be defined before start offset 4 on line 1");
    }
  }

  @Test
  public void fail_when_end_offset_is_higher_than_line_length() {
    try {
      RangeHelper.appendRange(new StringBuilder(), BatchReport.Range.newBuilder()
        .setStartLine(1).setEndLine(1)
        .setStartOffset(4).setEndOffset(10)
        .build(),
        1, 5);
      failBecauseExceptionWasNotThrown(IllegalArgumentException.class);
    } catch (IllegalArgumentException e) {
      assertThat(e).hasMessage("End offset 10 is defined outside the length (5) of the line 1");
    }
  }

  @Test
  public void fail_when_start_offset_is_higher_than_line_length() {
    try {
      RangeHelper.appendRange(new StringBuilder(), BatchReport.Range.newBuilder()
        .setStartLine(1).setEndLine(1)
        .setStartOffset(10).setEndOffset(11)
        .build(),
        1, 5);
      failBecauseExceptionWasNotThrown(IllegalArgumentException.class);
    } catch (IllegalArgumentException e) {
      assertThat(e).hasMessage("Start offset 10 is defined outside the length (5) of the line 1");
    }
  }
}
