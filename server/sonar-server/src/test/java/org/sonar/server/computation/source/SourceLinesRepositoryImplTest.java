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

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.sonar.batch.protocol.output.BatchReport;
import org.sonar.server.computation.batch.BatchReportReaderRule;
import org.sonar.server.computation.component.Component;

import static org.assertj.core.api.Assertions.assertThat;
import static org.sonar.server.computation.component.ReportComponent.builder;

public class SourceLinesRepositoryImplTest {

  static final String FILE_UUID = "FILE_UUID";
  static final String FILE_KEY = "FILE_KEY";
  static final int FILE_REF = 2;

  static final Component FILE = builder(Component.Type.FILE, FILE_REF)
    .setKey(FILE_KEY)
    .setUuid(FILE_UUID)
    .build();

  @Rule
  public ExpectedException thrown = ExpectedException.none();

  @Rule
  public BatchReportReaderRule reportReader = new BatchReportReaderRule();

  SourceLinesRepositoryImpl underTest = new SourceLinesRepositoryImpl(reportReader);

  @Test
  public void read_lines_from_report() throws Exception {
    reportReader.putComponent(createFileBatchComponent(2));
    reportReader.putFileSourceLines(FILE_REF, "line1", "line2");

    assertThat(underTest.readLines(FILE)).containsOnly("line1", "line2");
  }

  @Test
  public void read_lines_add_at_most_one_extra_empty_line_when_sourceLine_has_less_elements_then_lineCount() throws Exception {
    reportReader.putComponent(createFileBatchComponent(10));
    reportReader.putFileSourceLines(FILE_REF, "line1", "line2");

    assertThat(underTest.readLines(FILE)).containsOnly("line1", "line2", "");
  }

  @Test
  public void read_lines_reads_all_lines_from_sourceLines_when_it_has_more_elements_then_lineCount() throws Exception {
    reportReader.putComponent(createFileBatchComponent(2));
    reportReader.putFileSourceLines(FILE_REF, "line1", "line2", "line3");

    assertThat(underTest.readLines(FILE)).containsOnly("line1", "line2", "line3");
  }

  @Test
  public void not_fail_to_read_lines_on_empty_file_from_report() throws Exception {
    // File exist but there's no line
    reportReader.putComponent(createFileBatchComponent(0));
    reportReader.putFileSourceLines(FILE_REF);

    // Should not try to read source file from the db
    assertThat(underTest.readLines(FILE)).isEmpty();
  }

  @Test
  public void fail_with_ISE_when_file_has_no_source() throws Exception {
    thrown.expect(IllegalStateException.class);
    thrown.expectMessage("File 'ReportComponent{ref=2, key='FILE_KEY', type=FILE}' has no source code");

    underTest.readLines(FILE);
  }

  @Test
  public void fail_with_NPE_to_read_lines_on_null_component() throws Exception {
    thrown.expect(NullPointerException.class);
    thrown.expectMessage("Component should not be bull");

    underTest.readLines(null);
  }

  @Test
  public void fail_with_IAE_to_read_lines_on_not_file_component() throws Exception {
    thrown.expect(IllegalArgumentException.class);
    thrown.expectMessage("Component 'ReportComponent{ref=123, key='NotFile', type=PROJECT}' is not a file");

    underTest.readLines(builder(Component.Type.PROJECT, 123).setKey("NotFile").build());
  }

  private static BatchReport.Component createFileBatchComponent(int lineCount) {
    return BatchReport.Component.newBuilder().setRef(FILE_REF).setLines(lineCount).build();
  }

}
