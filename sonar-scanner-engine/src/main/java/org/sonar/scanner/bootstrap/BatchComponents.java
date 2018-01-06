/*
 * SonarQube
 * Copyright (C) 2009-2018 SonarSource SA
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
package org.sonar.scanner.bootstrap;

import com.google.common.collect.Lists;
import java.util.Collection;
import java.util.List;
import org.sonar.core.component.DefaultResourceTypes;
import org.sonar.core.config.CorePropertyDefinitions;
import org.sonar.core.issue.tracking.Tracker;
import org.sonar.scanner.cpd.CpdComponents;
import org.sonar.scanner.genericcoverage.GenericCoverageSensor;
import org.sonar.scanner.genericcoverage.GenericTestExecutionSensor;
import org.sonar.scanner.issue.tracking.ServerIssueFromWs;
import org.sonar.scanner.issue.tracking.TrackedIssue;
import org.sonar.scanner.scan.report.JSONReport;
import org.sonar.scanner.scm.ScmConfiguration;
import org.sonar.scanner.scm.ScmPublisher;
import org.sonar.scanner.source.ZeroCoverageSensor;
import org.sonar.scanner.task.ListTask;
import org.sonar.scanner.task.ScanTask;
import org.sonar.scanner.task.Tasks;

public class BatchComponents {
  private BatchComponents() {
    // only static stuff
  }

  public static Collection<Object> all(GlobalAnalysisMode analysisMode) {
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
      components.add(ScmPublisher.class);

      components.add(ZeroCoverageSensor.class);

      // CPD
      components.addAll(CpdComponents.all());

      // Generic coverage
      components.add(GenericCoverageSensor.class);
      components.addAll(GenericCoverageSensor.properties());
      components.add(GenericTestExecutionSensor.class);
      components.addAll(GenericTestExecutionSensor.properties());

    } else {
      // Issues tracking
      components.add(new Tracker<TrackedIssue, ServerIssueFromWs>());
      components.add(JSONReport.class);
    }
    return components;
  }
}
