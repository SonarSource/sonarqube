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
package org.sonar.batch.protocol.input.issues;

import javax.annotation.CheckForNull;
import javax.annotation.Nullable;

public class PreviousIssue {

  private String key;
  private String componentKey;
  private String ruleKey;
  private String ruleRepo;
  private Integer line;
  private String message;
  private String overriddenSeverity;
  private String resolution;
  private String status;
  private String checksum;
  private String assigneeLogin;

  public PreviousIssue setKey(String key) {
    this.key = key;
    return this;
  }

  public String key() {
    return key;
  }

  public PreviousIssue setComponentKey(@Nullable String key) {
    this.componentKey = key;
    return this;
  }

  public String componentKey() {
    return componentKey;
  }

  public PreviousIssue setLine(Integer line) {
    this.line = line;
    return this;
  }

  public Integer line() {
    return line;
  }

  public PreviousIssue setMessage(String message) {
    this.message = message;
    return this;
  }

  public String message() {
    return message;
  }

  public PreviousIssue setResolution(String resolution) {
    this.resolution = resolution;
    return this;
  }

  public String resolution() {
    return resolution;
  }

  public PreviousIssue setStatus(String status) {
    this.status = status;
    return this;
  }

  public String status() {
    return status;
  }

  public PreviousIssue setOverriddenSeverity(@Nullable String overriddenSeverity) {
    this.overriddenSeverity = overriddenSeverity;
    return this;
  }

  @CheckForNull
  public String overriddenSeverity() {
    return overriddenSeverity;
  }

  public PreviousIssue setChecksum(String checksum) {
    this.checksum = checksum;
    return this;
  }

  public String checksum() {
    return checksum;
  }

  public PreviousIssue setAssigneeLogin(String assigneeLogin) {
    this.assigneeLogin = assigneeLogin;
    return this;
  }

  public String assigneeLogin() {
    return assigneeLogin;
  }

  public PreviousIssue setRuleKey(String ruleRepo, String ruleKey) {
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

}
