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
import org.sonar.api.rule.Severity;
import org.sonar.api.utils.Duration;
import org.sonar.api.utils.KeyValueFormat;
import org.sonar.server.es.IssueIndexDefinition;
import org.sonar.server.search.BaseDoc;

import javax.annotation.CheckForNull;
import javax.annotation.Nullable;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Map;

public class IssueDoc extends BaseDoc implements Issue {

  public IssueDoc(Map<String, Object> fields) {
    super(fields);
  }

  @Override
  public String key() {
    return getField(IssueIndexDefinition.FIELD_ISSUE_KEY);
  }

  @Override
  public String componentKey() {
    throw new IllegalStateException("componentKey is not available on server side");
  }

  @Override
  public String componentUuid() {
    return getField(IssueNormalizer.IssueField.COMPONENT.field());
  }

  public String moduleUuid() {
    return getField(IssueNormalizer.IssueField.MODULE.field());
  }

  @Override
  public String projectKey() {
    throw new IllegalStateException("projectKey is not available on server side");
  }

  @Override
  public String projectUuid() {
    return getField(IssueNormalizer.IssueField.PROJECT.field());
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
    return getFieldAsDate(IssueNormalizer.IssueField.ISSUE_CREATED_AT.field());
  }

  @Override
  public Date updateDate() {
    return getFieldAsDate(IssueNormalizer.IssueField.ISSUE_UPDATED_AT.field());
  }

  @Override
  @CheckForNull
  public Date closeDate() {
    return getNullableFieldAsDate(IssueNormalizer.IssueField.ISSUE_CLOSE_DATE.field());
  }

  @Override
  @CheckForNull
  public String attribute(String key) {
    return attributes().get(key);
  }

  @Override
  public Map<String, String> attributes() {
    String data = getNullableField(IssueNormalizer.IssueField.ATTRIBUTES.field());
    if (data == null) {
      return Collections.emptyMap();
    } else {
      return KeyValueFormat.parse(data);
    }
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
    throw new IllegalStateException("isNew is only available for batch");
  }

  @Override
  @CheckForNull
  public Duration debt() {
    Number debt = getNullableField(IssueNormalizer.IssueField.DEBT.field());
    return (debt != null) ? Duration.create(debt.longValue()) : null;
  }

  @CheckForNull
  public String filePath() {
    return getNullableField(IssueNormalizer.IssueField.FILE_PATH.field());
  }

  public void setKey(@Nullable String s) {
    setField(IssueIndexDefinition.FIELD_ISSUE_KEY, s);
  }

  public void setComponentUuid(@Nullable String s) {
    setField(IssueIndexDefinition.FIELD_ISSUE_COMPONENT_UUID, s);
  }

  public void setModuleUuid(@Nullable String s) {
    setField(IssueIndexDefinition.FIELD_ISSUE_MODULE_UUID, s);
  }

  public void setProjectUuid(@Nullable String s) {
    setField(IssueIndexDefinition.FIELD_ISSUE_PROJECT_UUID, s);
  }

  public void setRuleKey(@Nullable String s) {
    setField(IssueIndexDefinition.FIELD_ISSUE_RULE_KEY, s);
  }

  public void setLanguage(@Nullable String s) {
    setField(IssueIndexDefinition.FIELD_ISSUE_LANGUAGE, s);
  }

  public void setSeverity(@Nullable String s) {
    setField(IssueIndexDefinition.FIELD_ISSUE_SEVERITY, s);
    setField(IssueIndexDefinition.FIELD_ISSUE_SEVERITY_VALUE, Severity.ALL.indexOf(s));
  }

  public void setMessage(@Nullable String s) {
    setField(IssueIndexDefinition.FIELD_ISSUE_MESSAGE, s);
  }

  public void setLine(@Nullable Integer i) {
    setField(IssueIndexDefinition.FIELD_ISSUE_LINE, i);
  }

  public void setEffortToFix(@Nullable Double d) {
    setField(IssueIndexDefinition.FIELD_ISSUE_EFFORT, d);
  }

  public void setStatus(@Nullable String s) {
    setField(IssueIndexDefinition.FIELD_ISSUE_STATUS, s);
  }

  public void setResolution(@Nullable String s) {
    setField(IssueIndexDefinition.FIELD_ISSUE_RESOLUTION, s);
  }

  public void setReporter(@Nullable String s) {
    setField(IssueIndexDefinition.FIELD_ISSUE_REPORTER, s);
  }

  public void setAssignee(@Nullable String s) {
    setField(IssueIndexDefinition.FIELD_ISSUE_ASSIGNEE, s);
  }

  public void setCreationDate(@Nullable Date d) {
    setField(IssueIndexDefinition.FIELD_ISSUE_CREATED_AT, d);
  }

  public void setUpdateDate(@Nullable Date d) {
    setField(IssueIndexDefinition.FIELD_ISSUE_UPDATED_AT, d);
  }

  public void setFuncCreationDate(@Nullable Date d) {
    setField(IssueIndexDefinition.FIELD_ISSUE_FUNC_CREATED_AT, d);
  }

  public void setFuncUpdateDate(@Nullable Date d) {
    setField(IssueIndexDefinition.FIELD_ISSUE_FUNC_UPDATED_AT, d);
  }

  public void setFuncCloseDate(@Nullable Date d) {
    setField(IssueIndexDefinition.FIELD_ISSUE_FUNC_CLOSED_AT, d);
  }

  public void setAttributes(@Nullable String s) {
    setField(IssueIndexDefinition.FIELD_ISSUE_ATTRIBUTES, s);
  }

  public void setAuthorLogin(@Nullable String s) {
    setField(IssueIndexDefinition.FIELD_ISSUE_AUTHOR_LOGIN, s);
  }

  public void setActionPlanKey(@Nullable String s) {
    setField(IssueIndexDefinition.FIELD_ISSUE_ACTION_PLAN, s);
  }

  public void setDebt(@Nullable Long l) {
    setField(IssueIndexDefinition.FIELD_ISSUE_DEBT, l);
  }

  public void setFilePath(@Nullable String s) {
    setField(IssueIndexDefinition.FIELD_ISSUE_FILE_PATH, s);
  }

  public void setModuleUuidPath(@Nullable String s) {
    setField(IssueIndexDefinition.FIELD_ISSUE_MODULE_PATH, s);
  }
}
