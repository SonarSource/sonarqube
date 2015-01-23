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
package org.sonar.batch.protocol.output.issue;

import javax.annotation.Nullable;

import java.util.Collection;
import java.util.Date;

public class ReportIssue {

  private Long componentBatchId;
  private String ruleKey;
  private String ruleRepo;
  private Integer line;
  private String message;
  private Double effortToFix;
  private String severity;
  private Collection<String> tags;

  // Temporary fields that should be removed when aggregation/issue tracking is done by computation
  private boolean isNew;
  private String key;
  private Long debtInMinutes;
  private String resolution;
  private String status;
  private String checksum;
  private boolean manualSeverity;
  private String reporter;
  private String assignee;
  private String actionPlanKey;
  private String attributes;
  private String authorLogin;
  private Date creationDate;
  private Date closeDate;
  private Date updateDate;
  private Long selectedAt;
  private String diffFields;
  private boolean isChanged;


  public ReportIssue setKey(String key) {
    this.key = key;
    return this;
  }

  public String key() {
    return key;
  }

  public ReportIssue setComponentBatchId(@Nullable Long resourceBatchId) {
    this.componentBatchId = resourceBatchId;
    return this;
  }

  public Long componentBatchId() {
    return componentBatchId;
  }

  public ReportIssue setNew(boolean isNew) {
    this.isNew = isNew;
    return this;
  }

  public boolean isNew() {
    return isNew;
  }

  public ReportIssue setLine(Integer line) {
    this.line = line;
    return this;
  }

  public Integer line() {
    return line;
  }

  public ReportIssue setMessage(String message) {
    this.message = message;
    return this;
  }

  public String message() {
    return message;
  }

  public ReportIssue setEffortToFix(Double effortToFix) {
    this.effortToFix = effortToFix;
    return this;
  }

  public Double effortToFix() {
    return effortToFix;
  }

  public ReportIssue setDebt(Long debtInMinutes) {
    this.debtInMinutes = debtInMinutes;
    return this;
  }

  public Long debt() {
    return debtInMinutes;
  }

  public ReportIssue setResolution(String resolution) {
    this.resolution = resolution;
    return this;
  }

  public String resolution() {
    return resolution;
  }

  public ReportIssue setStatus(String status) {
    this.status = status;
    return this;
  }

  public String status() {
    return status;
  }

  public ReportIssue setSeverity(String severity) {
    this.severity = severity;
    return this;
  }

  public String severity() {
    return severity;
  }

  public ReportIssue setChecksum(String checksum) {
    this.checksum = checksum;
    return this;
  }

  public String checksum() {
    return checksum;
  }

  public ReportIssue setManualSeverity(boolean manualSeverity) {
    this.manualSeverity = manualSeverity;
    return this;
  }

  public boolean isManualSeverity() {
    return manualSeverity;
  }

  public ReportIssue setReporter(String reporter) {
    this.reporter = reporter;
    return this;
  }

  public String reporter() {
    return reporter;
  }

  public ReportIssue setAssignee(String assignee) {
    this.assignee = assignee;
    return this;
  }

  public String assignee() {
    return assignee;
  }

  public Collection<String> tags() {
    return tags;
  }

  public ReportIssue setTags(Collection<String> s) {
    this.tags = s;
    return this;
  }

  public ReportIssue setRuleKey(String ruleRepo, String ruleKey) {
    this.ruleRepo = ruleRepo;
    this.ruleKey = ruleKey;
    return this;
  }

  public String ruleRepo() {
    return ruleRepo;
  }

  public String ruleKey() {
    return ruleKey;
  }

  public ReportIssue setActionPlanKey(String actionPlanKey) {
    this.actionPlanKey = actionPlanKey;
    return this;
  }

  public String actionPlanKey() {
    return actionPlanKey;
  }

  public ReportIssue setAttributes(String attributes) {
    this.attributes = attributes;
    return this;
  }

  public String issueAttributes() {
    return attributes;
  }

  public ReportIssue setAuthorLogin(String authorLogin) {
    this.authorLogin = authorLogin;
    return this;
  }

  public String authorLogin() {
    return authorLogin;
  }

  public ReportIssue setCreationDate(Date creationDate) {
    this.creationDate = creationDate;
    return this;
  }

  public Date creationDate() {
    return creationDate;
  }

  public ReportIssue setCloseDate(Date closeDate) {
    this.closeDate = closeDate;
    return this;
  }

  public Date closeDate() {
    return closeDate;
  }

  public ReportIssue setUpdateDate(Date updateDate) {
    this.updateDate = updateDate;
    return this;
  }

  public Date updateDate() {
    return updateDate;
  }

  public ReportIssue setSelectedAt(Long selectedAt) {
    this.selectedAt = selectedAt;
    return this;
  }

  public Long selectedAt() {
    return selectedAt;
  }

  public ReportIssue setDiffFields(@Nullable String diffFields) {
    this.diffFields = diffFields;
    return this;
  }

  public String diffFields() {
    return diffFields;
  }

  public ReportIssue setChanged(boolean isChanged) {
    this.isChanged = isChanged;
    return this;
  }

  public boolean isChanged() {
    return isChanged;
  }

}
