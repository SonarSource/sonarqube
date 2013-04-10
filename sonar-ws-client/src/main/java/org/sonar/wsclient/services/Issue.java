/*
 * Sonar, open source software quality management tool.
 * Copyright (C) 2008-2012 SonarSource
 * mailto:contact AT sonarsource DOT com
 *
 * Sonar is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * Sonar is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with Sonar; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02
 */
package org.sonar.wsclient.services;

import java.util.Date;

/**
 * @since 3.6
 */
public class Issue extends Model {

  private String key;
  private String componentKey;
  private String ruleKey;
  private String ruleRepositoryKey;
  private String severity;
  private String title;
  private String message;
  private Integer line;
  private Double cost;
  private String status;
  private String resolution;
  private String userLogin;
  private String assigneeLogin;
  private Date createdAt;
  private Date updatedAt;
  private Date closedAt;

  public String getKey() {
    return key;
  }

  public Issue setKey(String key) {
    this.key = key;
    return this;
  }

  public String getComponentKey() {
    return componentKey;
  }

  public Issue setComponentKey(String componentKey) {
    this.componentKey = componentKey;
    return this;
  }

  public String getRuleKey() {
    return ruleKey;
  }

  public Issue setRuleKey(String ruleKey) {
    this.ruleKey = ruleKey;
    return this;
  }

  public String getRuleRepositoryKey() {
    return ruleRepositoryKey;
  }

  public Issue setRuleRepositoryKey(String ruleRepositoryKey) {
    this.ruleRepositoryKey = ruleRepositoryKey;
    return this;
  }

  public String getSeverity() {
    return severity;
  }

  public Issue setSeverity(String severity) {
    this.severity = severity;
    return this;
  }

  public String getTitle() {
    return title;
  }

  public Issue setTitle(String title) {
    this.title = title;
    return this;
  }

  public String getMessage() {
    return message;
  }

  public Issue setMessage(String message) {
    this.message = message;
    return this;
  }

  public Integer getLine() {
    return line;
  }

  public Issue setLine(Integer line) {
    this.line = line;
    return this;
  }

  public Double getCost() {
    return cost;
  }

  public Issue setCost(Double cost) {
    this.cost = cost;
    return this;
  }

  public String getStatus() {
    return status;
  }

  public Issue setStatus(String status) {
    this.status = status;
    return this;
  }

  public String getResolution() {
    return resolution;
  }

  public Issue setResolution(String resolution) {
    this.resolution = resolution;
    return this;
  }

  public String getUserLogin() {
    return userLogin;
  }

  public Issue setUserLogin(String userLogin) {
    this.userLogin = userLogin;
    return this;
  }

  public String getAssigneeLogin() {
    return assigneeLogin;
  }

  public Issue setAssigneeLogin(String assigneeLogin) {
    this.assigneeLogin = assigneeLogin;
    return this;
  }

  public Date getCreatedAt() {
    return createdAt;
  }

  public Issue setCreatedAt(Date createdAt) {
    this.createdAt = createdAt;
    return this;
  }

  public Date getUpdatedAt() {
    return updatedAt;
  }

  public Issue setUpdatedAt(Date updatedAt) {
    this.updatedAt = updatedAt;
    return this;
  }

  public Date getClosedAt() {
    return closedAt;
  }

  public Issue setClosedAt(Date closedAt) {
    this.closedAt = closedAt;
    return this;
  }

  @Override
  public String toString() {
    return new StringBuilder()
        .append(key)
        .toString();
  }
}
