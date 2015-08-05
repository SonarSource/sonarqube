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
import org.sonar.api.batch.BatchSide;
import org.sonar.api.issue.Issue;
import org.sonar.api.issue.IssueComment;
import org.sonar.api.issue.batch.IssueFilter;
import org.sonar.api.resources.Project;
import org.sonar.api.rule.RuleKey;
import org.sonar.api.utils.Duration;
import org.sonar.api.utils.KeyValueFormat;
import org.sonar.batch.protocol.output.BatchReport;

@BatchSide
public class IssueFilters {

  private static final class IssueAdapterForFilter implements Issue {
    private final Project project;
    private final org.sonar.batch.protocol.output.BatchReport.Issue rawIssue;
    private final String componentKey;

    private IssueAdapterForFilter(Project project, org.sonar.batch.protocol.output.BatchReport.Issue rawIssue, String componentKey) {
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
      return rawIssue.hasAttributes() ? KeyValueFormat.parse(rawIssue.getAttributes()) : Collections.<String, String>emptyMap();
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

  private final org.sonar.api.issue.IssueFilter[] exclusionFilters;
  private final IssueFilter[] filters;
  private final Project project;

  public IssueFilters(Project project, org.sonar.api.issue.IssueFilter[] exclusionFilters, IssueFilter[] filters) {
    this.project = project;
    this.exclusionFilters = exclusionFilters;
    this.filters = filters;
  }

  public IssueFilters(Project project, org.sonar.api.issue.IssueFilter[] exclusionFilters) {
    this(project, exclusionFilters, new IssueFilter[0]);
  }

  public IssueFilters(Project project, IssueFilter[] filters) {
    this(project, new org.sonar.api.issue.IssueFilter[0], filters);
  }

  public IssueFilters(Project project) {
    this(project, new org.sonar.api.issue.IssueFilter[0], new IssueFilter[0]);
  }

  public boolean accept(String componentKey, BatchReport.Issue rawIssue) {
    Issue issue = new IssueAdapterForFilter(project, rawIssue, componentKey);
    if (new DefaultIssueFilterChain(filters).accept(issue)) {
      // Apply deprecated rules only if filter chain accepts the current issue
      for (org.sonar.api.issue.IssueFilter filter : exclusionFilters) {
        if (!filter.accept(issue)) {
          return false;
        }
      }
      return true;
    } else {
      return false;
    }
  }
}
