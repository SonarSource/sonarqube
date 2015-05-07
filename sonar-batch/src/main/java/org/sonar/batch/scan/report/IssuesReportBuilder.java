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
package org.sonar.batch.scan.report;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sonar.api.BatchSide;
import org.sonar.api.issue.Issue;
import org.sonar.api.issue.internal.DefaultIssue;
import org.sonar.api.resources.Project;
import org.sonar.api.rule.RuleKey;
import org.sonar.api.rules.Rule;
import org.sonar.api.rules.RuleFinder;
import org.sonar.api.rules.RulePriority;
import org.sonar.batch.ProjectTree;
import org.sonar.batch.index.BatchResource;
import org.sonar.batch.index.ResourceCache;
import org.sonar.batch.issue.IssueCache;
import org.sonar.batch.scan.filesystem.InputPathCache;

import javax.annotation.CheckForNull;

@BatchSide
public class IssuesReportBuilder {

  private static final Logger LOG = LoggerFactory.getLogger(IssuesReportBuilder.class);

  private final IssueCache issueCache;
  private final RuleFinder ruleFinder;
  private final ResourceCache resourceCache;
  private final ProjectTree projectTree;
  private final InputPathCache inputPathCache;

  public IssuesReportBuilder(IssueCache issueCache, RuleFinder ruleFinder, ResourceCache resourceCache, ProjectTree projectTree, InputPathCache inputPathCache) {
    this.issueCache = issueCache;
    this.ruleFinder = ruleFinder;
    this.resourceCache = resourceCache;
    this.projectTree = projectTree;
    this.inputPathCache = inputPathCache;
  }

  public IssuesReport buildReport() {
    Project project = projectTree.getRootProject();
    IssuesReport issuesReport = new IssuesReport();
    issuesReport.setNoFile(!inputPathCache.allFiles().iterator().hasNext());
    issuesReport.setTitle(project.getName());
    issuesReport.setDate(project.getAnalysisDate());

    processIssues(issuesReport, issueCache.all());

    return issuesReport;
  }

  private void processIssues(IssuesReport issuesReport, Iterable<DefaultIssue> issues) {
    for (Issue issue : issues) {
      Rule rule = findRule(issue);
      RulePriority severity = RulePriority.valueOf(issue.severity());
      BatchResource resource = resourceCache.get(issue.componentKey());
      if (!validate(issue, rule, resource)) {
        continue;
      }
      if (issue.resolution() != null) {
        issuesReport.addResolvedIssueOnResource(resource, issue, rule, severity);
      } else {
        issuesReport.addIssueOnResource(resource, issue, rule, severity);
      }
    }
  }

  private boolean validate(Issue issue, Rule rule, BatchResource resource) {
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
  private Rule findRule(Issue issue) {
    RuleKey ruleKey = issue.ruleKey();
    return ruleFinder.findByKey(ruleKey);
  }

}
