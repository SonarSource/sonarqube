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
package org.sonar.batch.issue;

import org.sonar.api.issue.Issue;
import org.sonar.api.issue.IssueComment;
import org.sonar.api.resources.Project;
import org.sonar.api.rule.RuleKey;
import org.sonar.api.utils.Duration;

import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Map;

/**
 * @deprecated since 5.3
 */
@Deprecated
class DeprecatedIssueAdapterForFilter implements Issue {
  private final Project project;
  private final org.sonar.batch.protocol.output.BatchReport.Issue rawIssue;
  private final String componentKey;

  DeprecatedIssueAdapterForFilter(Project project, org.sonar.batch.protocol.output.BatchReport.Issue rawIssue, String componentKey) {
    this.project = project;
    this.rawIssue = rawIssue;
    this.componentKey = componentKey;
  }

  @Override
  public String key() {
    throw unsupported();
  }

  @Override
  public String componentKey() {
    return componentKey;
  }

  @Override
  public RuleKey ruleKey() {
    return RuleKey.of(rawIssue.getRuleRepository(), rawIssue.getRuleKey());
  }

  @Override
  public String language() {
    throw unsupported();
  }

  @Override
  public String severity() {
    return rawIssue.getSeverity().name();
  }

  @Override
  public String message() {
    return rawIssue.getMsg();
  }

  @Override
  public Integer line() {
    return rawIssue.hasLine() ? rawIssue.getLine() : null;
  }

  @Override
  public Double effortToFix() {
    return rawIssue.hasEffortToFix() ? rawIssue.getEffortToFix() : null;
  }

  @Override
  public String status() {
    return Issue.STATUS_OPEN;
  }

  @Override
  public String resolution() {
    return null;
  }

  @Override
  public String reporter() {
    throw unsupported();
  }

  @Override
  public String assignee() {
    return null;
  }

  @Override
  public Date creationDate() {
    return project.getAnalysisDate();
  }

  @Override
  public Date updateDate() {
    return null;
  }

  @Override
  public Date closeDate() {
    return null;
  }

  @Override
  public String attribute(String key) {
    return attributes().get(key);
  }

  @Override
  public Map<String, String> attributes() {
    return Collections.emptyMap();
  }

  @Override
  public String authorLogin() {
    throw unsupported();
  }

  @Override
  public String actionPlanKey() {
    throw unsupported();
  }

  @Override
  public List<IssueComment> comments() {
    throw unsupported();
  }

  @Override
  public boolean isNew() {
    throw unsupported();
  }

  @Override
  public Duration debt() {
    throw unsupported();
  }

  @Override
  public String projectKey() {
    return project.getEffectiveKey();
  }

  @Override
  public String projectUuid() {
    throw unsupported();
  }

  @Override
  public String componentUuid() {
    throw unsupported();
  }

  @Override
  public Collection<String> tags() {
    throw unsupported();
  }

  private static UnsupportedOperationException unsupported() {
    return new UnsupportedOperationException("Not available for issues filters");
  }
}
