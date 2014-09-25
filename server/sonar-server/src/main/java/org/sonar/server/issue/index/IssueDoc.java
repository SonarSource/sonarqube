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
package org.sonar.server.issue.index;

import org.sonar.api.issue.Issue;
import org.sonar.api.issue.IssueComment;
import org.sonar.api.rule.RuleKey;
import org.sonar.api.utils.Duration;
import org.sonar.server.search.BaseDoc;
import org.sonar.server.search.IndexUtils;

import javax.annotation.CheckForNull;

import java.util.Date;
import java.util.List;
import java.util.Map;

public class IssueDoc extends BaseDoc implements Issue {

  public IssueDoc(Map<String, Object> fields) {
    super(fields);
  }

  @Override
  public String key() {
    return getField(IssueNormalizer.IssueField.KEY.field());
  }

  @Override
  public String componentKey() {
    return getField(IssueNormalizer.IssueField.COMPONENT.field());
  }

  @Override
  public RuleKey ruleKey() {
    return RuleKey.parse((String) getField(IssueNormalizer.IssueField.RULE_KEY.field()));
  }

  @Override
  public String language() {
    return getField(IssueNormalizer.IssueField.LANGUAGE.field());
  }

  @Override
  public String severity() {
    return getField(IssueNormalizer.IssueField.SEVERITY.field());
  }

  @Override
  @CheckForNull
  public String message() {
    return getNullableField(IssueNormalizer.IssueField.MESSAGE.field());
  }

  @Override
  @CheckForNull
  public Integer line() {
    return getNullableField(IssueNormalizer.IssueField.LINE.field());
  }

  @Override
  @CheckForNull
  public Double effortToFix() {
    return getNullableField(IssueNormalizer.IssueField.EFFORT.field());
  }

  @Override
  public String status() {
    return getField(IssueNormalizer.IssueField.STATUS.field());
  }

  @Override
  @CheckForNull
  public String resolution() {
    return getNullableField(IssueNormalizer.IssueField.RESOLUTION.field());
  }

  @Override
  @CheckForNull
  public String reporter() {
    return getNullableField(IssueNormalizer.IssueField.REPORTER.field());
  }

  @Override
  @CheckForNull
  public String assignee() {
    return getNullableField(IssueNormalizer.IssueField.ASSIGNEE.field());
  }

  @Override
  public Date creationDate() {
    return IndexUtils.parseDateTime((String) getField(IssueNormalizer.IssueField.ISSUE_CREATED_AT.field()));
  }

  @Override
  public Date updateDate() {
    return IndexUtils.parseDateTime((String) getField(IssueNormalizer.IssueField.ISSUE_UPDATED_AT.field()));
  }

  @Override
  @CheckForNull
  public Date closeDate() {
    return getNullableField(IssueNormalizer.IssueField.ISSUE_CLOSE_DATE.field()) != null ?
      IndexUtils.parseDateTime((String) getNullableField(IssueNormalizer.IssueField.ISSUE_CLOSE_DATE.field())) :
      null;
  }

  @Override
  @CheckForNull
  public String attribute(String key) {
    return attributes().get(key);
  }

  @Override
  public Map<String, String> attributes() {
    return getField(IssueNormalizer.IssueField.ATTRIBUTES.field());
  }

  @Override
  @CheckForNull
  public String authorLogin() {
    return getNullableField(IssueNormalizer.IssueField.AUTHOR_LOGIN.field());
  }

  @Override
  @CheckForNull
  public String actionPlanKey() {
    return getNullableField(IssueNormalizer.IssueField.ACTION_PLAN.field());
  }

  @Override
  public List<IssueComment> comments() {
    throw new IllegalStateException("Comments are not availables in index");
  }

  @Override
  public boolean isNew() {
    throw new IllegalStateException("Only available for batch");
  }

  @Override
  @CheckForNull
  public Duration debt() {
    Integer debt = getNullableField(IssueNormalizer.IssueField.DEBT.field());
    return (debt != null) ? Duration.create(Long.valueOf(debt)) : null;
  }

  @Override
  public String projectKey() {
    return getField(IssueNormalizer.IssueField.PROJECT.field());
  }
}
