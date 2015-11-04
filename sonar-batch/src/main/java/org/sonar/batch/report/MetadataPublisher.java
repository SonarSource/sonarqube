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
package org.sonar.batch.report;

import org.sonar.api.CoreProperties;
import org.sonar.api.batch.bootstrap.ProjectDefinition;
import org.sonar.api.config.Settings;
import org.sonar.api.resources.Project;
import org.sonar.batch.cpd.index.SonarDuplicationsIndex;
import org.sonar.batch.index.BatchComponent;
import org.sonar.batch.index.BatchComponentCache;
import org.sonar.batch.protocol.output.BatchReport;
import org.sonar.batch.protocol.output.BatchReportWriter;
import org.sonar.batch.scan.ImmutableProjectReactor;

public class MetadataPublisher implements ReportPublisherStep {

  private final BatchComponentCache componentCache;
  private final ImmutableProjectReactor reactor;
  private final Settings settings;

  public MetadataPublisher(BatchComponentCache componentCache, ImmutableProjectReactor reactor, Settings settings) {
    this.componentCache = componentCache;
    this.reactor = reactor;
    this.settings = settings;
  }

  @Override
  public void publish(BatchReportWriter writer) {
    ProjectDefinition root = reactor.getRoot();
    BatchComponent rootProject = componentCache.getRoot();
    BatchReport.Metadata.Builder builder = BatchReport.Metadata.newBuilder()
      .setAnalysisDate(((Project) rootProject.resource()).getAnalysisDate().getTime())
      // Here we want key without branch
      .setProjectKey(root.getKey())
      .setCrossProjectDuplicationActivated(SonarDuplicationsIndex.isCrossProjectDuplicationEnabled(settings))
      .setRootComponentRef(rootProject.batchId());
    String branch = root.properties().get(CoreProperties.PROJECT_BRANCH_PROPERTY);
    if (branch != null) {
      builder.setBranch(branch);
    }
    writer.writeMetadata(builder.build());
  }
}
