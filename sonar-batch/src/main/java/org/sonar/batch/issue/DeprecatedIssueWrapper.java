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

import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Map;
import org.sonar.api.batch.fs.TextRange;
import org.sonar.api.batch.rule.Severity;
import org.sonar.api.batch.sensor.issue.internal.DefaultIssue;
import org.sonar.api.issue.Issue;
import org.sonar.api.issue.IssueComment;
import org.sonar.api.rule.RuleKey;
import org.sonar.api.utils.Duration;

public class DeprecatedIssueWrapper implements Issue {

  private final DefaultIssue newIssue;

  public DeprecatedIssueWrapper(DefaultIssue newIssue) {
    this.newIssue = newIssue;
  }

  public DefaultIssue wrapped() {
    return newIssue;
  }

  @Override
  public String key() {
    return null;
  }

  @Override
  public String componentKey() {
    return null;
  }

  @Override
  public RuleKey ruleKey() {
    return newIssue.ruleKey();
  }

  @Override
  public String language() {
    return null;
  }

  @Override
  public String severity() {
    Severity overridenSeverity = newIssue.overriddenSeverity();
    return overridenSeverity != null ? overridenSeverity.name() : null;
  }

  @Override
  public String message() {
    return newIssue.locations().get(0).message();
  }

  @Override
  public Integer line() {
    TextRange textRange = newIssue.locations().get(0).textRange();
    return textRange != null ? textRange.start().line() : null;
  }

  @Override
  public Double effortToFix() {
    return newIssue.effortToFix();
  }

  @Override
  public String status() {
    return null;
  }

  @Override
  public String resolution() {
    return null;
  }

  @Override
  public String reporter() {
    return null;
  }

  @Override
  public String assignee() {
    return null;
  }

  @Override
  public Date creationDate() {
    return null;
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
    return null;
  }

  @Override
  public Map<String, String> attributes() {
    return null;
  }

  @Override
  public String authorLogin() {
    return null;
  }

  @Override
  public String actionPlanKey() {
    return null;
  }

  @Override
  public List<IssueComment> comments() {
    return Collections.emptyList();
  }

  @Override
  public boolean isNew() {
    return false;
  }

  @Override
  public Duration debt() {
    return null;
  }

  @Override
  public String projectKey() {
    return null;
  }

  @Override
  public String projectUuid() {
    return null;
  }

  @Override
  public String componentUuid() {
    return null;
  }

  @Override
  public Collection<String> tags() {
    return Collections.emptyList();
  }

}
