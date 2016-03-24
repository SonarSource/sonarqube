/*
 * SonarQube
 * Copyright (C) 2009-2016 SonarSource SA
 * mailto:contact AT sonarsource DOT com
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
package org.sonar.batch.report;

import org.sonar.api.batch.bootstrap.ProjectDefinition;
import org.sonar.api.config.Settings;
import org.sonar.api.resources.Project;
import org.sonar.batch.cpd.index.SonarCpdBlockIndex;
import org.sonar.batch.index.BatchComponent;
import org.sonar.batch.index.BatchComponentCache;
import org.sonar.batch.rule.ModuleQProfiles;
import org.sonar.batch.rule.QProfile;
import org.sonar.batch.scan.ImmutableProjectReactor;
import org.sonar.scanner.protocol.output.ScannerReport;
import org.sonar.scanner.protocol.output.ScannerReportWriter;

public class MetadataPublisher implements ReportPublisherStep {

  private final BatchComponentCache componentCache;
  private final ImmutableProjectReactor reactor;
  private final Settings settings;
  private final ModuleQProfiles qProfiles;

  public MetadataPublisher(BatchComponentCache componentCache, ImmutableProjectReactor reactor, Settings settings, ModuleQProfiles qProfiles) {
    this.componentCache = componentCache;
    this.reactor = reactor;
    this.settings = settings;
    this.qProfiles = qProfiles;
  }

  @Override
  public void publish(ScannerReportWriter writer) {
    ProjectDefinition root = reactor.getRoot();
    BatchComponent rootProject = componentCache.getRoot();
    ScannerReport.Metadata.Builder builder = ScannerReport.Metadata.newBuilder()
      .setAnalysisDate(((Project) rootProject.resource()).getAnalysisDate().getTime())
      // Here we want key without branch
      .setProjectKey(root.getKey())
      .setCrossProjectDuplicationActivated(SonarCpdBlockIndex.isCrossProjectDuplicationEnabled(settings))
      .setRootComponentRef(rootProject.batchId());
    String branch = root.getBranch();
    if (branch != null) {
      builder.setBranch(branch);
    }
    for (QProfile qp : qProfiles.findAll()) {
      builder.getMutableQprofilesPerLanguage().put(qp.getLanguage(), org.sonar.scanner.protocol.output.ScannerReport.Metadata.QProfile.newBuilder()
        .setKey(qp.getKey())
        .setLanguage(qp.getLanguage())
        .setName(qp.getName())
        .setRulesUpdatedAt(qp.getRulesUpdatedAt().getTime()).build());
    }
    writer.writeMetadata(builder.build());
  }
}
