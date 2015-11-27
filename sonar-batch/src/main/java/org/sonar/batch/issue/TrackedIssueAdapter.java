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

import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.sonar.api.issue.Issue;
import org.sonar.api.issue.IssueComment;
import org.sonar.api.rule.RuleKey;
import org.sonar.api.utils.Duration;
import org.sonar.batch.issue.tracking.TrackedIssue;

public class TrackedIssueAdapter implements Issue {
  private TrackedIssue issue;

  public TrackedIssueAdapter(TrackedIssue issue) {
    this.issue = issue;
  }

  @Override
  public String key() {
    return issue.key();
  }

  @Override
  public String componentKey() {
    return issue.componentKey();
  }

  @Override
  public RuleKey ruleKey() {
    return issue.getRuleKey();
  }

  @Override
  public String severity() {
    return issue.severity();
  }

  @Override
  public String message() {
    return issue.getMessage();
  }

  @Override
  public Integer line() {
    return issue.startLine();
  }

  @Override
  public Double effortToFix() {
    return issue.effortToFix();
  }

  @Override
  public String status() {
    return issue.status();
  }

  @Override
  public String resolution() {
    return issue.resolution();
  }

  @Override
  public String reporter() {
    return issue.reporter();
  }

  @Override
  public String assignee() {
    return issue.assignee();
  }

  @Override
  public boolean isNew() {
    return issue.isNew();
  }

  @Override
  public Map<String, String> attributes() {
    return new HashMap<>();
  }

  @Override
  public Date creationDate() {
    return issue.getCreationDate();
  }

  @Override
  public String language() {
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
    return attributes().get(key);
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
    return new ArrayList<>();
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
    return new ArrayList<>();
  }

  @Override
  public boolean equals(Object o) {
    if (o == null || !(o instanceof Issue)) {
      return false;
    }
    Issue that = (Issue) o;
    return !(issue.key() != null ? !issue.key().equals(that.key()) : (that.key() != null));
  }

  @Override
  public int hashCode() {
    return issue.key() != null ? issue.key().hashCode() : 0;
  }
}
