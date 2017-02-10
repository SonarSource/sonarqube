/*
 * SonarQube
 * Copyright (C) 2009-2017 SonarSource SA
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
package org.sonar.scanner.scan.report;

import javax.annotation.CheckForNull;
import javax.annotation.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sonar.api.batch.ScannerSide;
import org.sonar.api.batch.fs.InputComponent;
import org.sonar.api.batch.fs.internal.DefaultInputModule;
import org.sonar.api.batch.fs.internal.InputModuleHierarchy;
import org.sonar.api.batch.rule.Rule;
import org.sonar.api.batch.rule.Rules;
import org.sonar.api.rules.RulePriority;
import org.sonar.scanner.ProjectAnalysisInfo;
import org.sonar.scanner.issue.IssueCache;
import org.sonar.scanner.issue.tracking.TrackedIssue;
import org.sonar.scanner.scan.filesystem.InputComponentStore;

@ScannerSide
public class IssuesReportBuilder {

  private static final Logger LOG = LoggerFactory.getLogger(IssuesReportBuilder.class);

  private final IssueCache issueCache;
  private final Rules rules;
  private final InputComponentStore inputComponentStore;
  private final InputModuleHierarchy moduleHierarchy;
  private final ProjectAnalysisInfo projectAnalysisInfo;

  public IssuesReportBuilder(IssueCache issueCache, Rules rules, ProjectAnalysisInfo projectAnalysisInfo, InputModuleHierarchy moduleHierarchy,
    InputComponentStore inputComponentStore) {
    this.issueCache = issueCache;
    this.rules = rules;
    this.projectAnalysisInfo = projectAnalysisInfo;
    this.moduleHierarchy = moduleHierarchy;
    this.inputComponentStore = inputComponentStore;
  }

  public IssuesReport buildReport() {
    DefaultInputModule project = moduleHierarchy.root();
    IssuesReport issuesReport = new IssuesReport();
    issuesReport.setNoFile(!inputComponentStore.allFilesToPublish().iterator().hasNext());
    issuesReport.setTitle(project.definition().getName());
    issuesReport.setDate(projectAnalysisInfo.analysisDate());

    processIssues(issuesReport, issueCache.all());

    return issuesReport;
  }

  private void processIssues(IssuesReport issuesReport, Iterable<TrackedIssue> issues) {
    for (TrackedIssue issue : issues) {
      Rule rule = findRule(issue);
      RulePriority severity = RulePriority.valueOf(issue.severity());
      InputComponent resource = inputComponentStore.getByKey(issue.componentKey());
      if (!validate(issue, rule, resource)) {
        continue;
      }
      if (issue.resolution() != null) {
        issuesReport.addResolvedIssueOnResource(resource, rule, severity);
      } else {
        issuesReport.addIssueOnResource(resource, issue, rule, severity);
      }
    }
  }

  private static boolean validate(TrackedIssue issue, @Nullable Rule rule, @Nullable InputComponent resource) {
    if (rule == null) {
      LOG.warn("Unknow rule for issue {}", issue);
      return false;
    }
    if (resource == null) {
      LOG.debug("Unknow resource with key {}", issue.componentKey());
      return false;
    }
    return true;
  }

  @CheckForNull
  private Rule findRule(TrackedIssue issue) {
    return rules.find(issue.getRuleKey());
  }

}
