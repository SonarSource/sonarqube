/*
 * SonarQube, open source software quality management tool.
 * Copyright (C) 2008-2013 SonarSource
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
package org.sonar.core.issue.db;

import com.google.common.base.Objects;
import com.google.common.base.Preconditions;
import org.apache.commons.lang.builder.ToStringBuilder;
import org.apache.commons.lang.builder.ToStringStyle;
import org.sonar.api.rule.RuleKey;
import org.sonar.api.utils.KeyValueFormat;
import org.sonar.core.issue.DefaultIssue;

import javax.annotation.Nullable;
import java.util.Date;

/**
 * @since 3.6
 */
public final class IssueDto {

  private Long id;
  private String kee;
  private Integer resourceId;
  private Integer ruleId;
  private String severity;
  private boolean manualSeverity;
  private boolean manualIssue;
  private String description;
  private Integer line;
  private Double cost;
  private String status;
  private String resolution;
  private String checksum;
  private String userLogin;
  private String assignee;
  private String authorLogin;
  private String attributes;

  // functional dates
  private Date issueCreationDate;
  private Date issueUpdateDate;
  private Date issueCloseDate;

  // technical dates
  private Date createdAt;
  private Date updatedAt;

  // joins
  private transient String ruleKey;
  private transient String ruleRepo;
  private transient String componentKey;

  public Long getId() {
    return id;
  }

  public IssueDto setId(@Nullable Long id) {
    this.id = id;
    return this;
  }

  public String getKey() {
    return kee;
  }

  public IssueDto setKey(String key) {
    this.kee = key;
    return this;
  }

  public Integer getResourceId() {
    return resourceId;
  }

  public IssueDto setResourceId(Integer resourceId) {
    this.resourceId = resourceId;
    return this;
  }

  public Integer getRuleId() {
    return ruleId;
  }

  public IssueDto setRuleId(Integer ruleId) {
    this.ruleId = ruleId;
    return this;
  }

  public String getSeverity() {
    return severity;
  }

  public IssueDto setSeverity(@Nullable String severity) {
    this.severity = severity;
    return this;
  }

  public boolean isManualSeverity() {
    return manualSeverity;
  }

  public IssueDto setManualSeverity(boolean manualSeverity) {
    this.manualSeverity = manualSeverity;
    return this;
  }

  public boolean isManualIssue() {
    return manualIssue;
  }

  public IssueDto setManualIssue(boolean manualIssue) {
    this.manualIssue = manualIssue;
    return this;
  }

  public String getDescription() {
    return description;
  }

  public IssueDto setDescription(@Nullable String s) {
    this.description = s;
    return this;
  }

  public Integer getLine() {
    return line;
  }

  public IssueDto setLine(@Nullable Integer line) {
    this.line = line;
    return this;
  }

  public Double getCost() {
    return cost;
  }

  public IssueDto setCost(@Nullable Double cost) {
    this.cost = cost;
    return this;
  }

  public String getStatus() {
    return status;
  }

  public IssueDto setStatus(@Nullable String status) {
    this.status = status;
    return this;
  }

  public String getResolution() {
    return resolution;
  }

  public IssueDto setResolution(@Nullable String resolution) {
    this.resolution = resolution;
    return this;
  }

  public String getChecksum() {
    return checksum;
  }

  public IssueDto setChecksum(String checksum) {
    this.checksum = checksum;
    return this;
  }

  public String getUserLogin() {
    return userLogin;
  }

  public IssueDto setUserLogin(@Nullable String userLogin) {
    this.userLogin = userLogin;
    return this;
  }

  public String getAssignee() {
    return assignee;
  }

  public IssueDto setAssignee(@Nullable String s) {
    this.assignee = s;
    return this;
  }

  public String getAuthorLogin() {
    return authorLogin;
  }

  public IssueDto setAuthorLogin(@Nullable String authorLogin) {
    this.authorLogin = authorLogin;
    return this;
  }

  public String getAttributes() {
    return attributes;
  }

  public IssueDto setAttributes(@Nullable String s) {
    Preconditions.checkArgument(s == null || s.length() <= 4000,
      "Issue attributes must not exceed 4000 characters: " + s);
    this.attributes = s;
    return this;
  }

  public Date getCreatedAt() {
    return createdAt;
  }

  public IssueDto setCreatedAt(Date createdAt) {
    this.createdAt = createdAt;
    return this;
  }

  public Date getUpdatedAt() {
    return updatedAt;
  }

  public IssueDto setUpdatedAt(@Nullable Date updatedAt) {
    this.updatedAt = updatedAt;
    return this;
  }

  public Date getIssueCreationDate() {
    return issueCreationDate;
  }

  public IssueDto setIssueCreationDate(@Nullable Date d) {
    this.issueCreationDate = d;
    return this;
  }

  public Date getIssueUpdateDate() {
    return issueUpdateDate;
  }

  public IssueDto setIssueUpdateDate(@Nullable Date d) {
    this.issueUpdateDate = d;
    return this;
  }

  public Date getIssueCloseDate() {
    return issueCloseDate;
  }

  public IssueDto setIssueCloseDate(@Nullable Date d) {
    this.issueCloseDate = d;
    return this;
  }

  public String getRule() {
    return ruleKey;
  }

  public String getRuleRepo() {
    return ruleRepo;
  }

  public String getComponentKey() {
    return componentKey;
  }

  /**
   * Only for unit tests
   */
  public IssueDto setRuleKey_unit_test_only(String repo, String rule) {
    this.ruleRepo = repo;
    this.ruleKey = rule;
    return this;
  }

  /**
   * Only for unit tests
   */
  public IssueDto setComponentKey_unit_test_only(String componentKey) {
    this.componentKey = componentKey;
    return this;
  }

  @Override
  public String toString() {
    return ToStringBuilder.reflectionToString(this, ToStringStyle.SHORT_PREFIX_STYLE);
  }

  public static IssueDto toDto(DefaultIssue issue, Integer componentId, Integer ruleId) {
    return new IssueDto()
      .setKey(issue.key())
      .setLine(issue.line())
      .setDescription(issue.description())
      .setCost(issue.cost())
      .setResolution(issue.resolution())
      .setStatus(issue.status())
      .setSeverity(issue.severity())
      .setChecksum(issue.getChecksum())
      .setManualIssue(issue.manual())
      .setManualSeverity(issue.manualSeverity())
      .setUserLogin(issue.userLogin())
      .setAssignee(issue.assignee())
      .setRuleId(ruleId)
      .setResourceId(componentId)
      .setAttributes(issue.attributes() != null ? KeyValueFormat.format(issue.attributes()) : "")
      .setAuthorLogin(issue.authorLogin())
      .setCreatedAt(issue.technicalCreationDate())
      .setUpdatedAt(issue.technicalUpdateDate())
      .setIssueCreationDate(issue.creationDate())
      .setIssueCloseDate(issue.closeDate())
      .setIssueUpdateDate(issue.updateDate());
  }

  public DefaultIssue toDefaultIssue() {
    DefaultIssue issue = new DefaultIssue();
    issue.setKey(kee);
    issue.setStatus(status);
    issue.setResolution(resolution);
    issue.setDescription(description);
    issue.setCost(cost);
    issue.setLine(line);
    issue.setSeverity(severity);
    issue.setUserLogin(userLogin);
    issue.setAssignee(assignee);
    issue.setAttributes(KeyValueFormat.parse(Objects.firstNonNull(attributes, "")));
    issue.setComponentKey(componentKey);
    issue.setManual(manualIssue);
    issue.setManualSeverity(manualSeverity);
    issue.setRuleKey(RuleKey.of(ruleRepo, ruleKey));
    issue.setAuthorLogin(authorLogin);
    issue.setNew(false);
    issue.setTechnicalCreationDate(createdAt);
    issue.setTechnicalUpdateDate(updatedAt);
    issue.setCreationDate(issueCreationDate);
    issue.setCloseDate(issueCloseDate);
    issue.setUpdateDate(issueUpdateDate);
    return issue;
  }
}
