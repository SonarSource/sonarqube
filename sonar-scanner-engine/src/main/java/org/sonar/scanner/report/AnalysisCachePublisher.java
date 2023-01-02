/*
 * SonarQube
 * Copyright (C) 2009-2023 SonarSource SA
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

import com.google.protobuf.ByteString;
import java.util.Map;
import org.sonar.scanner.cache.AnalysisCacheEnabled;
import org.sonar.scanner.cache.ScannerWriteCache;
import org.sonar.scanner.protocol.internal.ScannerInternal;
import org.sonar.scanner.protocol.internal.ScannerInternal.AnalysisCacheMsg;
import org.sonar.scanner.protocol.output.ScannerReportWriter;
import org.sonar.scanner.scan.branch.BranchConfiguration;

public class AnalysisCachePublisher implements ReportPublisherStep {
  private final AnalysisCacheEnabled analysisCacheEnabled;
  private final BranchConfiguration branchConfiguration;
  private final ScannerWriteCache cache;

  public AnalysisCachePublisher(AnalysisCacheEnabled analysisCacheEnabled, BranchConfiguration branchConfiguration, ScannerWriteCache cache) {
    this.analysisCacheEnabled = analysisCacheEnabled;
    this.branchConfiguration = branchConfiguration;
    this.cache = cache;
  }

  @Override
  public void publish(ScannerReportWriter writer) {
    if (!analysisCacheEnabled.isEnabled() || branchConfiguration.isPullRequest() || cache.getCache().isEmpty()) {
      return;
    }
    AnalysisCacheMsg.Builder analysisCacheMsg = ScannerInternal.AnalysisCacheMsg.newBuilder();

    for (Map.Entry<String, byte[]> entry : cache.getCache().entrySet()) {
      analysisCacheMsg.putMap(entry.getKey(), ByteString.copyFrom(entry.getValue()));
    }

    writer.writeAnalysisCache(analysisCacheMsg.build());
  }
}
