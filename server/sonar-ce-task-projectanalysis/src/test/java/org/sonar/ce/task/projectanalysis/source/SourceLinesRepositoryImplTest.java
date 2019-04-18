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
package org.sonar.ce.task.projectanalysis.source;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.sonar.ce.task.projectanalysis.batch.BatchReportReaderRule;
import org.sonar.ce.task.projectanalysis.component.Component;
import org.sonar.ce.task.projectanalysis.component.FileAttributes;
import org.sonar.core.util.CloseableIterator;

import static org.assertj.core.api.Assertions.assertThat;
import static org.sonar.ce.task.projectanalysis.component.ReportComponent.builder;

public class SourceLinesRepositoryImplTest {

  private static final String FILE_UUID = "FILE_UUID";
  private static final String FILE_KEY = "FILE_KEY";
  private static final int FILE_REF = 2;

  @Rule
  public ExpectedException thrown = ExpectedException.none();

  @Rule
  public BatchReportReaderRule reportReader = new BatchReportReaderRule();

  private SourceLinesRepositoryImpl underTest = new SourceLinesRepositoryImpl(reportReader);

  @Test
  public void read_lines_from_report() {
    reportReader.putFileSourceLines(FILE_REF, "line1", "line2");

    assertThat(underTest.readLines(createComponent(2))).toIterable().containsOnly("line1", "line2");
  }

  @Test
  public void read_lines_adds_one_extra_empty_line_when_sourceLine_has_elements_count_equals_to_lineCount_minus_1() {
    reportReader.putFileSourceLines(FILE_REF, "line1", "line2");

    assertThat(underTest.readLines(createComponent(3))).toIterable().containsOnly("line1", "line2", "");
  }

  @Test
  public void read_lines_throws_ISE_when_sourceLine_has_less_elements_then_lineCount_minus_1() {
    reportReader.putFileSourceLines(FILE_REF, "line1", "line2");

    thrown.expect(IllegalStateException.class);
    thrown.expectMessage("Source of file 'ReportComponent{ref=2, key='FILE_KEY', type=FILE}' has less lines (2) than the expected number (10)");

    consume(underTest.readLines(createComponent(10)));
  }

  @Test
  public void read_lines_throws_ISE_when_sourceLines_has_more_elements_then_lineCount() {
    reportReader.putFileSourceLines(FILE_REF, "line1", "line2", "line3");

    thrown.expect(IllegalStateException.class);
    thrown.expectMessage("Source of file 'ReportComponent{ref=2, key='FILE_KEY', type=FILE}' has at least one more line than the expected number (2)");

    consume(underTest.readLines(createComponent(2)));
  }

  @Test
  public void fail_with_ISE_when_file_has_no_source() {
    thrown.expect(IllegalStateException.class);
    thrown.expectMessage("File 'ReportComponent{ref=2, key='FILE_KEY', type=FILE}' has no source code");

    underTest.readLines(builder(Component.Type.FILE, FILE_REF)
      .setKey(FILE_KEY)
      .setUuid(FILE_UUID)
      .build());
  }

  @Test
  public void fail_with_NPE_to_read_lines_on_null_component() {
    thrown.expect(NullPointerException.class);
    thrown.expectMessage("Component should not be null");

    underTest.readLines(null);
  }

  @Test
  public void fail_with_IAE_to_read_lines_on_not_file_component() {
    thrown.expect(IllegalArgumentException.class);
    thrown.expectMessage("Component 'ReportComponent{ref=123, key='NotFile', type=PROJECT}' is not a file");

    underTest.readLines(builder(Component.Type.PROJECT, 123).setKey("NotFile").build());
  }

  private static Component createComponent(int lineCount) {
    return builder(Component.Type.FILE, FILE_REF)
      .setKey(FILE_KEY)
      .setUuid(FILE_UUID)
      .setFileAttributes(new FileAttributes(false, null, lineCount))
      .build();
  }

  private static void consume(CloseableIterator<String> stringCloseableIterator) {
    try {
      while (stringCloseableIterator.hasNext()) {
        stringCloseableIterator.next();
      }
    } finally {
      stringCloseableIterator.close();
    }
  }

}
