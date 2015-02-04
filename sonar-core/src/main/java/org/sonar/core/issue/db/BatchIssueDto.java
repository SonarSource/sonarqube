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

package org.sonar.core.issue.db;

import javax.annotation.CheckForNull;
import javax.annotation.Nullable;

import java.util.Date;

import static org.sonar.api.utils.DateUtils.dateToLong;
import static org.sonar.api.utils.DateUtils.longToDate;

public class BatchIssueDto {

  private String kee;
  private String message;
  private Integer line;
  private String status;
  private String severity;
  private boolean manualSeverity;
  private String resolution;
  private String checksum;
  private String assigneeLogin;
  private String componentKey;
  private String ruleKey;
  private String ruleRepo;
  private Long creationDate;

  public String getAssigneeLogin() {
    return assigneeLogin;
  }

  public BatchIssueDto setAssigneeLogin(String assigneeLogin) {
    this.assigneeLogin = assigneeLogin;
    return this;
  }

  public String getChecksum() {
    return checksum;
  }

  public BatchIssueDto setChecksum(String checksum) {
    this.checksum = checksum;
    return this;
  }

  public String getComponentKey() {
    return componentKey;
  }

  public BatchIssueDto setComponentKey(String componentKey) {
    this.componentKey = componentKey;
    return this;
  }

  public String getKey() {
    return kee;
  }

  public BatchIssueDto setKey(String key) {
    this.kee = key;
    return this;
  }

  public Integer getLine() {
    return line;
  }

  public BatchIssueDto setLine(Integer line) {
    this.line = line;
    return this;
  }

  public String getMessage() {
    return message;
  }

  public BatchIssueDto setMessage(String message) {
    this.message = message;
    return this;
  }

  public String getResolution() {
    return resolution;
  }

  public BatchIssueDto setResolution(String resolution) {
    this.resolution = resolution;
    return this;
  }

  public String getRuleKey() {
    return ruleKey;
  }

  public BatchIssueDto setRuleKey(String ruleKey) {
    this.ruleKey = ruleKey;
    return this;
  }

  public String getRuleRepo() {
    return ruleRepo;
  }

  public BatchIssueDto setRuleRepo(String ruleRepo) {
    this.ruleRepo = ruleRepo;
    return this;
  }

  public String getStatus() {
    return status;
  }

  public BatchIssueDto setStatus(String status) {
    this.status = status;
    return this;
  }

  public boolean isManualSeverity() {
    return manualSeverity;
  }

  public BatchIssueDto setManualSeverity(boolean manualSeverity) {
    this.manualSeverity = manualSeverity;
    return this;
  }

  public String getSeverity() {
    return severity;
  }

  public BatchIssueDto setSeverity(String severity) {
    this.severity = severity;
    return this;
  }

  @CheckForNull
  public Date getCreationDate() {
    return longToDate(creationDate);
  }

  public BatchIssueDto setCreationDate(@Nullable Date creationDate) {
    this.creationDate = dateToLong(creationDate);
    return this;
  }

  public Long getCreationTime() {
    return creationDate;
  }

  public BatchIssueDto setCreationTime(Long time) {
    this.creationDate = time;
    return this;
  }
}
