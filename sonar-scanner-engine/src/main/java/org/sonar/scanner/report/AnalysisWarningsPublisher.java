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

import java.util.List;
import java.util.stream.Collectors;
import org.sonar.scanner.notifications.DefaultAnalysisWarnings;
import org.sonar.scanner.protocol.output.ScannerReport;
import org.sonar.scanner.protocol.output.ScannerReportWriter;

public class AnalysisWarningsPublisher implements ReportPublisherStep {

  private final DefaultAnalysisWarnings defaultAnalysisWarnings;

  public AnalysisWarningsPublisher(DefaultAnalysisWarnings defaultAnalysisWarnings) {
    this.defaultAnalysisWarnings = defaultAnalysisWarnings;
  }

  @Override
  public void publish(ScannerReportWriter writer) {
    List<DefaultAnalysisWarnings.Message> warnings = defaultAnalysisWarnings.warnings();
    if (warnings.isEmpty()) {
      return;
    }
    writer.writeAnalysisWarnings(warnings.stream()
      .map(AnalysisWarningsPublisher::toProtobufAnalysisWarning)
      .collect(Collectors.toList()));
  }

  private static ScannerReport.AnalysisWarning toProtobufAnalysisWarning(DefaultAnalysisWarnings.Message message) {
    return ScannerReport.AnalysisWarning.newBuilder()
      .setText(message.getText())
      .setTimestamp(message.getTimestamp())
      .build();
  }
}
