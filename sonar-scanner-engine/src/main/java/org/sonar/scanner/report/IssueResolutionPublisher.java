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
package org.sonar.scanner.report;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.sonar.api.batch.fs.TextRange;
import org.sonar.api.batch.fs.internal.DefaultInputFile;
import org.sonar.api.batch.sensor.issue.IssueResolution;
import org.sonar.api.rule.RuleKey;
import org.sonar.scanner.issue.IssueResolutionCache;
import org.sonar.scanner.protocol.output.ScannerReport;
import org.sonar.scanner.protocol.output.ScannerReportWriter;

public class IssueResolutionPublisher implements ReportPublisherStep {

  private final IssueResolutionCache cache;

  public IssueResolutionPublisher(IssueResolutionCache cache) {
    this.cache = cache;
  }

  @Override
  public void publish(ScannerReportWriter writer) {
    Map<Integer, List<IssueResolution>> byComponent = new LinkedHashMap<>();
    for (IssueResolution data : cache.getAll()) {
      int componentRef = ((DefaultInputFile) data.inputFile()).scannerId();
      byComponent.computeIfAbsent(componentRef, k -> new ArrayList<>()).add(data);
    }
    byComponent.forEach((componentRef, dataList) -> {
      List<ScannerReport.IssueResolution> protoData = new ArrayList<>();
      for (IssueResolution data : dataList) {
        ScannerReport.IssueResolution.Builder builder = ScannerReport.IssueResolution.newBuilder()
          .addAllRuleKeys(data.ruleKeys().stream().map(RuleKey::toString).toList())
          .setStatus(mapStatus(data.status()))
          .setComment(data.comment())
          .setTextRange(toProtobufTextRange(data.textRange()));
        protoData.add(builder.build());
      }
      writer.writeIssueResolution(componentRef, protoData);
    });
  }

  private static ScannerReport.IssueResolutionStatus mapStatus(IssueResolution.Status status) {
    return switch (status) {
      case DEFAULT -> ScannerReport.IssueResolutionStatus.DEFAULT;
      case FALSE_POSITIVE -> ScannerReport.IssueResolutionStatus.FALSE_POSITIVE;
    };
  }

  private static ScannerReport.TextRange toProtobufTextRange(TextRange textRange) {
    return ScannerReport.TextRange.newBuilder()
      .setStartLine(textRange.start().line())
      .setStartOffset(textRange.start().lineOffset())
      .setEndLine(textRange.end().line())
      .setEndOffset(textRange.end().lineOffset())
      .build();
  }
}
