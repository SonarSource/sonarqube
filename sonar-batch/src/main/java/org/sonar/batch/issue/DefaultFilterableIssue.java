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

import org.apache.commons.lang.builder.ToStringBuilder;
import org.apache.commons.lang.builder.ToStringStyle;

import org.sonar.api.resources.Project;
import org.sonar.api.rule.RuleKey;

import java.util.Date;

import org.sonar.batch.protocol.output.BatchReport.Issue;
import org.sonar.api.scan.issue.filter.FilterableIssue;

public class DefaultFilterableIssue implements FilterableIssue {
  private final Issue rawIssue;
  private final Project project;
  private final String componentKey;

  public DefaultFilterableIssue(Project project, Issue rawIssue, String componentKey) {
    this.project = project;
    this.rawIssue = rawIssue;
    this.componentKey = componentKey;

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
  public Date creationDate() {
    return project.getAnalysisDate();
  }

  @Override
  public String projectKey() {
    return project.getEffectiveKey();
  }

  @Override
  public String toString() {
    return ToStringBuilder.reflectionToString(this, ToStringStyle.SHORT_PREFIX_STYLE);
  }

}
