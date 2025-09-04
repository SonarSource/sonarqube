/*
 * SonarQube
 * Copyright (C) 2009-2024 SonarSource SA
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
import java.util.List;
import java.util.Map;
import javax.annotation.CheckForNull;
import javax.annotation.Nullable;
import org.sonar.api.issue.impact.SoftwareQuality;
import org.sonar.api.rule.Severity;
import org.sonar.api.rules.RuleType;
import org.sonar.api.utils.Duration;
import org.sonar.server.es.BaseDoc;
import org.sonar.server.permission.index.AuthorizationDoc;
import org.sonar.server.security.SecurityStandards;
import org.sonar.server.security.SecurityStandards.VulnerabilityProbability;

import static org.sonar.server.issue.index.IssueIndexDefinition.SUB_FIELD_SEVERITY;
import static org.sonar.server.issue.index.IssueIndexDefinition.SUB_FIELD_SOFTWARE_QUALITY;
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

  public String projectUuid() {
    return getField(IssueIndexDefinition.FIELD_ISSUE_PROJECT_UUID);
  }

  public String branchUuid() {
    return getField(IssueIndexDefinition.FIELD_ISSUE_BRANCH_UUID);
  }

  public boolean isMainBranch() {
    return getField(IssueIndexDefinition.FIELD_ISSUE_IS_MAIN_BRANCH);
  }

  public String ruleUuid() {
    return getField(IssueIndexDefinition.FIELD_ISSUE_RULE_UUID);
  }

  public IssueScope scope() {
    return IssueScope.valueOf(getField(IssueIndexDefinition.FIELD_ISSUE_SCOPE));
  }

  public String language() {
    return getField(IssueIndexDefinition.FIELD_ISSUE_LANGUAGE);
  }

  public String severity() {
    return getField(IssueIndexDefinition.FIELD_ISSUE_SEVERITY);
  }

  public String cleanCodeAttributeCategory() {
    return getField(IssueIndexDefinition.FIELD_ISSUE_CLEAN_CODE_ATTRIBUTE_CATEGORY);
  }

  public Collection<Map<String, String>> impacts() {
    return getField(IssueIndexDefinition.FIELD_ISSUE_IMPACTS);
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
  public String issueStatus() {
    return getNullableField(IssueIndexDefinition.FIELD_ISSUE_NEW_STATUS);
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

  public IssueDoc setRuleUuid(String s) {
    setField(IssueIndexDefinition.FIELD_ISSUE_RULE_UUID, s);
    return this;
  }

  public IssueDoc setScope(IssueScope s) {
    setField(IssueIndexDefinition.FIELD_ISSUE_SCOPE, s.toString());
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

  public IssueDoc setCleanCodeAttributeCategory(@Nullable String s) {
    setField(IssueIndexDefinition.FIELD_ISSUE_CLEAN_CODE_ATTRIBUTE_CATEGORY, s);
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

  public IssueDoc setIssueStatus(String s) {
    setField(IssueIndexDefinition.FIELD_ISSUE_NEW_STATUS, s);
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

  public IssueDoc setImpacts(Map<SoftwareQuality, org.sonar.api.issue.impact.Severity> impacts) {
    List<Map<String, String>> convertedMap = impacts
      .entrySet()
      .stream()
      .map(entry -> Map.of(
        SUB_FIELD_SOFTWARE_QUALITY, entry.getKey().name(),
        SUB_FIELD_SEVERITY, entry.getValue().name()))
      .toList();
    setField(IssueIndexDefinition.FIELD_ISSUE_IMPACTS, convertedMap);
    return this;
  }

  @CheckForNull
  public Collection<String> getPciDss32() {
    return getNullableField(IssueIndexDefinition.FIELD_ISSUE_PCI_DSS_32);
  }

  public IssueDoc setPciDss32(@Nullable Collection<String> o) {
    setField(IssueIndexDefinition.FIELD_ISSUE_PCI_DSS_32, o);
    return this;
  }

  public IssueDoc setPciDss40(@Nullable Collection<String> o) {
    setField(IssueIndexDefinition.FIELD_ISSUE_PCI_DSS_40, o);
    return this;
  }

  @CheckForNull
  public Collection<String> getPciDss40() {
    return getNullableField(IssueIndexDefinition.FIELD_ISSUE_PCI_DSS_40);
  }

  public IssueDoc setOwaspAsvs40(@Nullable Collection<String> o) {
    setField(IssueIndexDefinition.FIELD_ISSUE_OWASP_ASVS_40, o);
    return this;
  }

  @CheckForNull
  public Collection<String> getOwaspAsvs40() {
    return getNullableField(IssueIndexDefinition.FIELD_ISSUE_OWASP_ASVS_40);
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
  public Collection<String> getOwaspTop10For2021() {
    return getNullableField(IssueIndexDefinition.FIELD_ISSUE_OWASP_TOP_10_2021);
  }

  public IssueDoc setOwaspTop10For2021(@Nullable Collection<String> o) {
    setField(IssueIndexDefinition.FIELD_ISSUE_OWASP_TOP_10_2021, o);
    return this;
  }

  @CheckForNull
  public Collection<String> getStigAsdV5R3() {
    return getNullableField(IssueIndexDefinition.FIELD_ISSUE_STIG_ASD_V5R3);
  }

  public IssueDoc setStigAsdV5R3(@Nullable Collection<String> o) {
    setField(IssueIndexDefinition.FIELD_ISSUE_STIG_ASD_V5R3, o);
    return this;
  }

  @CheckForNull
  public Collection<String> getCasa() {
    return getNullableField(IssueIndexDefinition.FIELD_ISSUE_CASA);
  }

  public IssueDoc setCasa(@Nullable Collection<String> o) {
    setField(IssueIndexDefinition.FIELD_ISSUE_CASA, o);
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
  public Collection<Double> getCvss() {
    return getNullableField(IssueIndexDefinition.FIELD_ISSUE_CVSS);
  }

  public IssueDoc setCvss(@Nullable Collection<Double> c) {
    setField(IssueIndexDefinition.FIELD_ISSUE_CVSS, c);
    return this;
  }

  @CheckForNull
  public SecurityStandards.SQCategory getSonarSourceSecurityCategory() {
    String key = getNullableField(IssueIndexDefinition.FIELD_ISSUE_SQ_SECURITY_CATEGORY);
    return SecurityStandards.SQCategory.fromKey(key).orElse(null);
  }

  public IssueDoc setSonarSourceSecurityCategory(@Nullable SecurityStandards.SQCategory c) {
    setField(IssueIndexDefinition.FIELD_ISSUE_SQ_SECURITY_CATEGORY, c == null ? null : c.getKey());
    return this;
  }

  @CheckForNull
  public VulnerabilityProbability getVulnerabilityProbability() {
    Integer score = getNullableField(IssueIndexDefinition.FIELD_ISSUE_VULNERABILITY_PROBABILITY);
    return VulnerabilityProbability.byScore(score).orElse(null);
  }

  public IssueDoc setVulnerabilityProbability(@Nullable VulnerabilityProbability v) {
    setField(IssueIndexDefinition.FIELD_ISSUE_VULNERABILITY_PROBABILITY, v == null ? null : v.getScore());
    return this;
  }

  public boolean isNewCodeReference() {
    return getField(IssueIndexDefinition.FIELD_ISSUE_NEW_CODE_REFERENCE);
  }

  public IssueDoc setIsNewCodeReference(boolean b) {
    setField(IssueIndexDefinition.FIELD_ISSUE_NEW_CODE_REFERENCE, b);
    return this;
  }

  @CheckForNull
  public Collection<String> getCodeVariants() {
    return getNullableField(IssueIndexDefinition.FIELD_ISSUE_CODE_VARIANTS);
  }

  public IssueDoc setCodeVariants(@Nullable Collection<String> codeVariants) {
    setField(IssueIndexDefinition.FIELD_ISSUE_CODE_VARIANTS, codeVariants);
    return this;
  }

  public boolean isPrioritizedRule() {
    return getField(IssueIndexDefinition.FIELD_PRIORITIZED_RULE);
  }

  public IssueDoc setPrioritizedRule(boolean prioritizedRule) {
    setField(IssueIndexDefinition.FIELD_PRIORITIZED_RULE, prioritizedRule);
    return this;
  }
}
