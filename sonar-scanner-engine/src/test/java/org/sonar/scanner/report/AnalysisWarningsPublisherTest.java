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
package org.sonar.scanner.report;

import com.google.common.collect.Lists;
import java.io.File;
import java.io.IOException;
import java.util.List;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.sonar.api.notifications.AnalysisWarnings;
import org.sonar.api.utils.System2;
import org.sonar.scanner.notifications.DefaultAnalysisWarnings;
import org.sonar.scanner.protocol.output.ScannerReport;
import org.sonar.scanner.protocol.output.ScannerReportReader;
import org.sonar.scanner.protocol.output.ScannerReportWriter;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

public class AnalysisWarningsPublisherTest {

  @Rule
  public TemporaryFolder temp = new TemporaryFolder();

  private final AnalysisWarnings analysisWarnings;
  private final AnalysisWarningsPublisher underTest;

  public AnalysisWarningsPublisherTest() {
    DefaultAnalysisWarnings defaultAnalysisWarnings = new DefaultAnalysisWarnings(mock(System2.class));
    this.analysisWarnings = defaultAnalysisWarnings;
    this.underTest = new AnalysisWarningsPublisher(defaultAnalysisWarnings);
  }

  @Test
  public void publish_warnings() throws IOException {
    File outputDir = temp.newFolder();
    ScannerReportWriter writer = new ScannerReportWriter(outputDir);

    String warning1 = "warning 1";
    String warning2 = "warning 2";
    analysisWarnings.addUnique(warning1);
    analysisWarnings.addUnique(warning1);
    analysisWarnings.addUnique(warning2);

    underTest.publish(writer);

    ScannerReportReader reader = new ScannerReportReader(outputDir);
    List<ScannerReport.AnalysisWarning> warnings = Lists.newArrayList(reader.readAnalysisWarnings());

    assertThat(warnings)
      .extracting(ScannerReport.AnalysisWarning::getText)
      .containsExactly(warning1, warning2);
  }

  @Test
  public void do_not_write_warnings_report_when_empty() throws IOException {
    File outputDir = temp.newFolder();
    ScannerReportWriter writer = new ScannerReportWriter(outputDir);

    underTest.publish(writer);

    assertThat(writer.getFileStructure().analysisWarnings()).doesNotExist();

    ScannerReportReader reader = new ScannerReportReader(outputDir);
    List<ScannerReport.AnalysisWarning> warnings = Lists.newArrayList(reader.readAnalysisWarnings());

    assertThat(warnings).isEmpty();
  }
}
