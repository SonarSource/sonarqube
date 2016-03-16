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
package org.sonar.batch.bootstrap;

import com.google.common.collect.Lists;
import java.util.Collection;
import java.util.List;
import org.sonar.api.batch.AnalysisMode;
import org.sonar.batch.cpd.CpdComponents;
import org.sonar.batch.issue.tracking.ServerIssueFromWs;
import org.sonar.batch.issue.tracking.TrackedIssue;
import org.sonar.batch.scan.report.ConsoleReport;
import org.sonar.batch.scan.report.HtmlReport;
import org.sonar.batch.scan.report.IssuesReportBuilder;
import org.sonar.batch.scan.report.JSONReport;
import org.sonar.batch.scan.report.RuleNameProvider;
import org.sonar.batch.scan.report.SourceProvider;
import org.sonar.batch.scm.ScmConfiguration;
import org.sonar.batch.scm.ScmSensor;
import org.sonar.batch.source.CodeColorizerSensor;
import org.sonar.batch.source.LinesSensor;
import org.sonar.batch.source.ZeroCoverageSensor;
import org.sonar.batch.task.ListTask;
import org.sonar.batch.task.ScanTask;
import org.sonar.batch.task.Tasks;
import org.sonar.core.component.DefaultResourceTypes;
import org.sonar.core.config.CorePropertyDefinitions;
import org.sonar.core.issue.tracking.Tracker;

public class BatchComponents {
  private BatchComponents() {
    // only static stuff
  }

  public static Collection<Object> all(AnalysisMode analysisMode) {
    List<Object> components = Lists.newArrayList(
      DefaultResourceTypes.get(),

      // Tasks
      Tasks.class,
      ListTask.DEFINITION,
      ListTask.class,
      ScanTask.DEFINITION,
      ScanTask.class);
    components.addAll(CorePropertyDefinitions.all());
    if (!analysisMode.isIssues()) {
      // SCM
      components.add(ScmConfiguration.class);
      components.add(ScmSensor.class);

      components.add(LinesSensor.class);
      components.add(ZeroCoverageSensor.class);
      components.add(CodeColorizerSensor.class);

      // CPD
      components.addAll(CpdComponents.all());
    } else {
      // Issues tracking
      components.add(new Tracker<TrackedIssue, ServerIssueFromWs>());

      // Issues report
      components.add(ConsoleReport.class);
      components.add(JSONReport.class);
      components.add(HtmlReport.class);
      components.add(IssuesReportBuilder.class);
      components.add(SourceProvider.class);
      components.add(RuleNameProvider.class);
    }
    return components;
  }
}
