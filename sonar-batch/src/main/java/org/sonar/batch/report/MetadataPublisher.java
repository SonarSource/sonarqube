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

import com.google.common.base.Function;
import javax.annotation.Nonnull;
import org.sonar.api.CoreProperties;
import org.sonar.api.batch.bootstrap.ProjectDefinition;
import org.sonar.api.batch.rule.ActiveRule;
import org.sonar.api.batch.rule.ActiveRules;
import org.sonar.api.resources.Project;
import org.sonar.batch.index.BatchComponent;
import org.sonar.batch.index.BatchComponentCache;
import org.sonar.batch.protocol.output.BatchReport;
import org.sonar.batch.protocol.output.BatchReportWriter;
import org.sonar.batch.scan.ImmutableProjectReactor;

import static com.google.common.collect.FluentIterable.from;

public class MetadataPublisher implements ReportPublisherStep {

  private final BatchComponentCache componentCache;
  private final ImmutableProjectReactor reactor;
  private final ActiveRules activeRules;

  public MetadataPublisher(BatchComponentCache componentCache, ImmutableProjectReactor reactor, ActiveRules activeRules) {
    this.componentCache = componentCache;
    this.reactor = reactor;
    this.activeRules = activeRules;
  }

  @Override
  public void publish(BatchReportWriter writer) {
    ProjectDefinition root = reactor.getRoot();
    BatchComponent rootProject = componentCache.getRoot();
    BatchReport.Metadata.Builder builder = BatchReport.Metadata.newBuilder()
      .setAnalysisDate(((Project) rootProject.resource()).getAnalysisDate().getTime())
      // Here we want key without branch
      .setProjectKey(root.getKey())
      .setRootComponentRef(rootProject.batchId());
    String branch = root.properties().get(CoreProperties.PROJECT_BRANCH_PROPERTY);
    if (branch != null) {
      builder.setBranch(branch);
    }
    builder.addAllActiveRuleKey(from(activeRules.findAll()).transform(ToRuleKey.INSTANCE));
    writer.writeMetadata(builder.build());
  }

  private enum ToRuleKey implements Function<ActiveRule, String> {
    INSTANCE;
    @Nonnull
    @Override
    public String apply(@Nonnull ActiveRule input) {
      return input.ruleKey().toString();
    }
  }
}
