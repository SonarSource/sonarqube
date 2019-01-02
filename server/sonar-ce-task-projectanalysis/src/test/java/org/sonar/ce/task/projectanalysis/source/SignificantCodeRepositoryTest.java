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

import java.util.ArrayList;
import java.util.List;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.sonar.ce.task.projectanalysis.batch.BatchReportReaderRule;
import org.sonar.ce.task.projectanalysis.component.Component;
import org.sonar.ce.task.projectanalysis.component.FileAttributes;
import org.sonar.core.hash.LineRange;
import org.sonar.scanner.protocol.output.ScannerReport;
import org.sonar.scanner.protocol.output.ScannerReport.LineSgnificantCode;

import static org.assertj.core.api.Assertions.assertThat;
import static org.sonar.ce.task.projectanalysis.component.ReportComponent.builder;

public class SignificantCodeRepositoryTest {
  private static final String FILE_UUID = "FILE_UUID";
  private static final String FILE_KEY = "FILE_KEY";
  private static final int FILE_REF = 2;

  @Rule
  public ExpectedException thrown = ExpectedException.none();

  @Rule
  public BatchReportReaderRule reportReader = new BatchReportReaderRule();

  private SignificantCodeRepository underTest = new SignificantCodeRepository(reportReader);

  @Test
  public void return_empty_if_information_not_available() {
    assertThat(underTest.getRangesPerLine(createComponent(3))).isEmpty();
  }

  @Test
  public void return_null_for_lines_without_information() {
    Component component = createComponent(5);
    List<ScannerReport.LineSgnificantCode> significantCode = new ArrayList<>();

    // line 3 and 5 missing
    significantCode.add(createLineSignificantCode(1, 1, 2));
    significantCode.add(createLineSignificantCode(2, 1, 2));
    significantCode.add(createLineSignificantCode(4, 1, 2));

    reportReader.putSignificantCode(component.getReportAttributes().getRef(), significantCode);
    assertThat(underTest.getRangesPerLine(component)).isNotEmpty();
    LineRange[] lines = underTest.getRangesPerLine(component).get();
    assertThat(lines).hasSize(5);
    assertThat(lines[0]).isNotNull();
    assertThat(lines[1]).isNotNull();
    assertThat(lines[2]).isNull();
    assertThat(lines[3]).isNotNull();
    assertThat(lines[4]).isNull();
  }

  @Test
  public void translate_offset_for_each_line() {
    Component component = createComponent(1);
    List<ScannerReport.LineSgnificantCode> significantCode = new ArrayList<>();

    significantCode.add(createLineSignificantCode(1, 1, 2));

    reportReader.putSignificantCode(component.getReportAttributes().getRef(), significantCode);
    assertThat(underTest.getRangesPerLine(component)).isNotEmpty();
    LineRange[] lines = underTest.getRangesPerLine(component).get();
    assertThat(lines).hasSize(1);
    assertThat(lines[0].startOffset()).isEqualTo(1);
    assertThat(lines[0].endOffset()).isEqualTo(2);
  }

  private static LineSgnificantCode createLineSignificantCode(int line, int start, int end) {
    return LineSgnificantCode.newBuilder()
      .setLine(line)
      .setStartOffset(start)
      .setEndOffset(end)
      .build();
  }

  private static Component createComponent(int lineCount) {
    return builder(Component.Type.FILE, FILE_REF)
      .setKey(FILE_KEY)
      .setUuid(FILE_UUID)
      .setFileAttributes(new FileAttributes(false, null, lineCount))
      .build();
  }
}
