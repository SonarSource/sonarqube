/*
 * SonarQube
 * Copyright (C) 2009-2019 SonarSource SA
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
package org.sonar.server.issue.index;

import com.google.common.collect.Maps;
import java.util.Collection;
import java.util.Date;
import java.util.Map;
import javax.annotation.CheckForNull;
import javax.annotation.Nullable;
import org.sonar.api.rule.Severity;
import org.sonar.api.rules.RuleType;
import org.sonar.api.utils.Duration;
import org.sonar.server.es.BaseDoc;
import org.sonar.server.permission.index.AuthorizationDoc;

import static org.sonar.server.issue.index.IssueIndexDefinition.TYPE_ISSUE;

public class IssueDoc extends BaseDoc {

  public IssueDoc(Map<String, Object> fields) {
    super(TYPE_ISSUE, fields);
  }

  public IssueDoc() {
    super(TYPE_ISSUE, Maps.newHashMapWithExpectedSize(32));
  }

  @Override
  public String getId() {
    return key();
  }

  public String key() {
    return getField(IssueIndexDefinition.FIELD_ISSUE_KEY);
  }

  public String componentUuid() {
    return getField(IssueIndexDefinition.FIELD_ISSUE_COMPONENT_UUID);
  }

  @CheckForNull
  public String moduleUuid() {
    return getField(IssueIndexDefinition.FIELD_ISSUE_MODULE_UUID);
  }

  public String modulePath() {
    return getField(IssueIndexDefinition.FIELD_ISSUE_MODULE_PATH);
  }

  public String projectUuid() {
    return getField(IssueIndexDefinition.FIELD_ISSUE_PROJECT_UUID);
  }

  public String branchUuid() {
    return getField(IssueIndexDefinition.FIELD_ISSUE_BRANCH_UUID);
  }

  public boolean isMainBranch() {
    return getField(IssueIndexDefinition.FIELD_ISSUE_IS_MAIN_BRANCH);
  }

  public Integer ruleId() {
    return getField(IssueIndexDefinition.FIELD_ISSUE_RULE_ID);
  }

  public String language() {
    return getField(IssueIndexDefinition.FIELD_ISSUE_LANGUAGE);
  }

  public String severity() {
    return getField(IssueIndexDefinition.FIELD_ISSUE_SEVERITY);
  }

  @CheckForNull
  public Integer line() {
    return getNullableField(IssueIndexDefinition.FIELD_ISSUE_LINE);
  }

  public String status() {
    return getField(IssueIndexDefinition.FIELD_ISSUE_STATUS);
  }

  @CheckForNull
  public String resolution() {
    return getNullableField(IssueIndexDefinition.FIELD_ISSUE_RESOLUTION);
  }

  @CheckForNull
  public String assigneeUuid() {
    return getNullableField(IssueIndexDefinition.FIELD_ISSUE_ASSIGNEE_UUID);
  }

  /**
   * Functional date
   */
  public Date creationDate() {
    return getFieldAsDate(IssueIndexDefinition.FIELD_ISSUE_FUNC_CREATED_AT);
  }

  /**
   * Functional date
   */
  public Date updateDate() {
    return getFieldAsDate(IssueIndexDefinition.FIELD_ISSUE_FUNC_UPDATED_AT);
  }

  @CheckForNull
  public Date closeDate() {
    return getNullableFieldAsDate(IssueIndexDefinition.FIELD_ISSUE_FUNC_CLOSED_AT);
  }

  @CheckForNull
  public String authorLogin() {
    return getNullableField(IssueIndexDefinition.FIELD_ISSUE_AUTHOR_LOGIN);
  }

  public RuleType type() {
    return RuleType.valueOf(getField(IssueIndexDefinition.FIELD_ISSUE_TYPE));
  }

  @CheckForNull
  public Duration effort() {
    Number effort = getNullableField(IssueIndexDefinition.FIELD_ISSUE_EFFORT);
    return (effort != null) ? Duration.create(effort.longValue()) : null;
  }

  @CheckForNull
  public String filePath() {
    return getNullableField(IssueIndexDefinition.FIELD_ISSUE_FILE_PATH);
  }

  @CheckForNull
  public String directoryPath() {
    return getNullableField(IssueIndexDefinition.FIELD_ISSUE_DIRECTORY_PATH);
  }

  @CheckForNull
  public String organizationUuid() {
    return getNullableField(IssueIndexDefinition.FIELD_ISSUE_ORGANIZATION_UUID);
  }

  public IssueDoc setKey(@Nullable String s) {
    setField(IssueIndexDefinition.FIELD_ISSUE_KEY, s);
    return this;
  }

  public IssueDoc setComponentUuid(@Nullable String s) {
    setField(IssueIndexDefinition.FIELD_ISSUE_COMPONENT_UUID, s);
    return this;
  }

  public IssueDoc setModuleUuid(@Nullable String s) {
    setField(IssueIndexDefinition.FIELD_ISSUE_MODULE_UUID, s);
    return this;
  }

  public IssueDoc setProjectUuid(String s) {
    setField(IssueIndexDefinition.FIELD_ISSUE_PROJECT_UUID, s);
    setParent(AuthorizationDoc.idOf(s));
    return this;
  }

  public IssueDoc setBranchUuid(String s) {
    setField(IssueIndexDefinition.FIELD_ISSUE_BRANCH_UUID, s);
    return this;
  }

  public IssueDoc setIsMainBranch(boolean b) {
    setField(IssueIndexDefinition.FIELD_ISSUE_IS_MAIN_BRANCH, b);
    return this;
  }

  public IssueDoc setRuleId(Integer s) {
    setField(IssueIndexDefinition.FIELD_ISSUE_RULE_ID, s);
    return this;
  }

  public IssueDoc setLanguage(@Nullable String s) {
    setField(IssueIndexDefinition.FIELD_ISSUE_LANGUAGE, s);
    return this;
  }

  public IssueDoc setSeverity(@Nullable String s) {
    setField(IssueIndexDefinition.FIELD_ISSUE_SEVERITY, s);
    setField(IssueIndexDefinition.FIELD_ISSUE_SEVERITY_VALUE, Severity.ALL.indexOf(s));
    return this;
  }

  public IssueDoc setLine(@Nullable Integer i) {
    setField(IssueIndexDefinition.FIELD_ISSUE_LINE, i);
    return this;
  }

  public IssueDoc setStatus(@Nullable String s) {
    setField(IssueIndexDefinition.FIELD_ISSUE_STATUS, s);
    return this;
  }

  public IssueDoc setResolution(@Nullable String s) {
    setField(IssueIndexDefinition.FIELD_ISSUE_RESOLUTION, s);
    return this;
  }

  public IssueDoc setAssigneeUuid(@Nullable String s) {
    setField(IssueIndexDefinition.FIELD_ISSUE_ASSIGNEE_UUID, s);
    return this;
  }

  public IssueDoc setFuncUpdateDate(@Nullable Date d) {
    setField(IssueIndexDefinition.FIELD_ISSUE_FUNC_UPDATED_AT, d);
    return this;
  }

  public IssueDoc setFuncCreationDate(@Nullable Date d) {
    setField(IssueIndexDefinition.FIELD_ISSUE_FUNC_CREATED_AT, d);
    return this;
  }

  public IssueDoc setFuncCloseDate(@Nullable Date d) {
    setField(IssueIndexDefinition.FIELD_ISSUE_FUNC_CLOSED_AT, d);
    return this;
  }

  public IssueDoc setAuthorLogin(@Nullable String s) {
    setField(IssueIndexDefinition.FIELD_ISSUE_AUTHOR_LOGIN, s);
    return this;
  }

  public IssueDoc setEffort(@Nullable Long l) {
    setField(IssueIndexDefinition.FIELD_ISSUE_EFFORT, l);
    return this;
  }

  public IssueDoc setFilePath(@Nullable String s) {
    setField(IssueIndexDefinition.FIELD_ISSUE_FILE_PATH, s);
    return this;
  }

  public IssueDoc setDirectoryPath(@Nullable String s) {
    setField(IssueIndexDefinition.FIELD_ISSUE_DIRECTORY_PATH, s);
    return this;
  }

  public IssueDoc setModuleUuidPath(@Nullable String s) {
    setField(IssueIndexDefinition.FIELD_ISSUE_MODULE_PATH, s);
    return this;
  }

  @CheckForNull
  public Collection<String> getTags() {
    return getNullableField(IssueIndexDefinition.FIELD_ISSUE_TAGS);
  }

  public IssueDoc setTags(@Nullable Collection<String> tags) {
    setField(IssueIndexDefinition.FIELD_ISSUE_TAGS, tags);
    return this;
  }

  public IssueDoc setType(RuleType type) {
    setField(IssueIndexDefinition.FIELD_ISSUE_TYPE, type.toString());
    return this;
  }

  public IssueDoc setOrganizationUuid(String s) {
    setField(IssueIndexDefinition.FIELD_ISSUE_ORGANIZATION_UUID, s);
    return this;
  }

  @CheckForNull
  public Collection<String> getOwaspTop10() {
    return getNullableField(IssueIndexDefinition.FIELD_ISSUE_OWASP_TOP_10);
  }

  public IssueDoc setOwaspTop10(@Nullable Collection<String> o) {
    setField(IssueIndexDefinition.FIELD_ISSUE_OWASP_TOP_10, o);
    return this;
  }

  @CheckForNull
  public Collection<String> getSansTop25() {
    return getNullableField(IssueIndexDefinition.FIELD_ISSUE_SANS_TOP_25);
  }

  public IssueDoc setSansTop25(@Nullable Collection<String> s) {
    setField(IssueIndexDefinition.FIELD_ISSUE_SANS_TOP_25, s);
    return this;
  }

  @CheckForNull
  public Collection<String> getCwe() {
    return getNullableField(IssueIndexDefinition.FIELD_ISSUE_CWE);
  }

  public IssueDoc setCwe(@Nullable Collection<String> c) {
    setField(IssueIndexDefinition.FIELD_ISSUE_CWE, c);
    return this;
  }

  @CheckForNull
  public Collection<String> getSonarSourceSecurityCategories() {
    return getNullableField(IssueIndexDefinition.FIELD_ISSUE_SONARSOURCE_SECURITY);
  }

  public IssueDoc setSonarSourceSecurityCategories(@Nullable Collection<String> c) {
    setField(IssueIndexDefinition.FIELD_ISSUE_SONARSOURCE_SECURITY, c);
    return this;
  }
}
