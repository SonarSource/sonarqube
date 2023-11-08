/*
 * SonarQube
 * Copyright (C) 2009-2023 SonarSource SA
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
package org.sonar.db.issue;

import java.util.Collections;
import java.util.EnumMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import javax.annotation.CheckForNull;
import org.sonar.api.issue.impact.Severity;
import org.sonar.api.issue.impact.SoftwareQuality;
import org.sonar.core.issue.status.IssueStatus;

public final class IndexedIssueDto {
  private String issueKey = null;
  private String assignee = null;
  private Integer line = null;
  private String resolution = null;
  private String cleanCodeAttribute = null;
  private String ruleCleanCodeAttribute = null;
  private String severity = null;
  private String status = null;
  private Long effort = null;
  private String authorLogin = null;
  private Long issueCloseDate = null;
  private Long issueCreationDate = null;
  private Long issueUpdateDate = null;
  private String ruleUuid = null;
  private String language = null;
  private String componentUuid = null;
  private String path = null;
  private String scope = null;
  private String branchUuid = null;
  private boolean isMain = false;
  private String projectUuid = null;
  private String tags = null;
  private Integer issueType = null;
  private String securityStandards = null;
  private String qualifier = null;
  private boolean isNewCodeReferenceIssue = false;
  private String codeVariants = null;

  private Set<ImpactDto> impacts = new HashSet<>();
  private Set<ImpactDto> ruleDefaultImpacts = new HashSet<>();

  public IndexedIssueDto() {
    // empty constructor
  }

  public String getIssueKey() {
    return issueKey;
  }

  public IndexedIssueDto setIssueKey(String issueKey) {
    this.issueKey = issueKey;
    return this;
  }

  public String getAssignee() {
    return assignee;
  }

  public IndexedIssueDto setAssignee(String assignee) {
    this.assignee = assignee;
    return this;
  }

  public Integer getLine() {
    return line;
  }

  public IndexedIssueDto setLine(Integer line) {
    this.line = line;
    return this;
  }

  public String getResolution() {
    return resolution;
  }

  public IndexedIssueDto setResolution(String resolution) {
    this.resolution = resolution;
    return this;
  }

  public String getSeverity() {
    return severity;
  }

  public IndexedIssueDto setSeverity(String severity) {
    this.severity = severity;
    return this;
  }

  public String getStatus() {
    return status;
  }

  public IndexedIssueDto setStatus(String status) {
    this.status = status;
    return this;
  }

  @CheckForNull
  public String getIssueStatus() {
    return Optional.ofNullable(IssueStatus.of(status, resolution)).map(IssueStatus::name).orElse(null);
  }

  public Long getEffort() {
    return effort;
  }

  public IndexedIssueDto setEffort(Long effort) {
    this.effort = effort;
    return this;
  }

  public String getAuthorLogin() {
    return authorLogin;
  }

  public IndexedIssueDto setAuthorLogin(String authorLogin) {
    this.authorLogin = authorLogin;
    return this;
  }

  public Long getIssueCloseDate() {
    return issueCloseDate;
  }

  public IndexedIssueDto setIssueCloseDate(Long issueCloseDate) {
    this.issueCloseDate = issueCloseDate;
    return this;
  }

  public Long getIssueCreationDate() {
    return issueCreationDate;
  }

  public IndexedIssueDto setIssueCreationDate(Long issueCreationDate) {
    this.issueCreationDate = issueCreationDate;
    return this;
  }

  public Long getIssueUpdateDate() {
    return issueUpdateDate;
  }

  public IndexedIssueDto setIssueUpdateDate(Long issueUpdateDate) {
    this.issueUpdateDate = issueUpdateDate;
    return this;
  }

  public String getRuleUuid() {
    return ruleUuid;
  }

  public IndexedIssueDto setRuleUuid(String ruleUuid) {
    this.ruleUuid = ruleUuid;
    return this;
  }

  public String getLanguage() {
    return language;
  }

  public IndexedIssueDto setLanguage(String language) {
    this.language = language;
    return this;
  }

  public String getComponentUuid() {
    return componentUuid;
  }

  public IndexedIssueDto setComponentUuid(String componentUuid) {
    this.componentUuid = componentUuid;
    return this;
  }

  public String getPath() {
    return path;
  }

  public IndexedIssueDto setPath(String path) {
    this.path = path;
    return this;
  }

  public String getScope() {
    return scope;
  }

  public IndexedIssueDto setScope(String scope) {
    this.scope = scope;
    return this;
  }

  public String getBranchUuid() {
    return branchUuid;
  }

  public IndexedIssueDto setBranchUuid(String branchUuid) {
    this.branchUuid = branchUuid;
    return this;
  }

  public boolean isMain() {
    return isMain;
  }

  public IndexedIssueDto setIsMain(boolean isMain) {
    this.isMain = isMain;
    return this;
  }

  public String getProjectUuid() {
    return projectUuid;
  }

  public IndexedIssueDto setProjectUuid(String projectUuid) {
    this.projectUuid = projectUuid;
    return this;
  }

  public String getTags() {
    return tags;
  }

  public IndexedIssueDto setTags(String tags) {
    this.tags = tags;
    return this;
  }

  /**
   * @deprecated since 10.2
   */
  @Deprecated(since = "10.2")
  public Integer getIssueType() {
    return issueType;
  }

  /**
   * @deprecated since 10.2
   */
  @Deprecated(since = "10.2")
  public IndexedIssueDto setIssueType(Integer issueType) {
    this.issueType = issueType;
    return this;
  }

  public String getSecurityStandards() {
    return securityStandards;
  }

  public IndexedIssueDto setSecurityStandards(String securityStandards) {
    this.securityStandards = securityStandards;
    return this;
  }

  public String getQualifier() {
    return qualifier;
  }

  public IndexedIssueDto setQualifier(String qualifier) {
    this.qualifier = qualifier;
    return this;
  }

  public boolean isNewCodeReferenceIssue() {
    return isNewCodeReferenceIssue;
  }

  public IndexedIssueDto setNewCodeReferenceIssue(boolean newCodeReferenceIssue) {
    isNewCodeReferenceIssue = newCodeReferenceIssue;
    return this;
  }

  public String getCodeVariants() {
    return codeVariants;
  }

  public IndexedIssueDto setCodeVariants(String codeVariants) {
    this.codeVariants = codeVariants;
    return this;
  }

  public Set<ImpactDto> getImpacts() {
    return impacts;
  }

  public Set<ImpactDto> getRuleDefaultImpacts() {
    return ruleDefaultImpacts;
  }

  public Map<SoftwareQuality, Severity> getEffectiveImpacts() {
    EnumMap<SoftwareQuality, Severity> effectiveImpacts = new EnumMap<>(SoftwareQuality.class);
    ruleDefaultImpacts.forEach(impact -> effectiveImpacts.put(impact.getSoftwareQuality(), impact.getSeverity()));
    impacts.forEach(impact -> effectiveImpacts.put(impact.getSoftwareQuality(), impact.getSeverity()));
    return Collections.unmodifiableMap(effectiveImpacts);
  }

  public String getCleanCodeAttribute() {
    if (cleanCodeAttribute != null) {
      return cleanCodeAttribute;
    }
    return ruleCleanCodeAttribute;
  }

  public IndexedIssueDto setCleanCodeAttribute(String cleanCodeAttribute) {
    this.cleanCodeAttribute = cleanCodeAttribute;
    return this;
  }

  public String getRuleCleanCodeAttribute() {
    return ruleCleanCodeAttribute;
  }

  public IndexedIssueDto setRuleCleanCodeAttribute(String ruleCleanCodeAttribute) {
    this.ruleCleanCodeAttribute = ruleCleanCodeAttribute;
    return this;
  }
}
