/*
 * SonarQube
 * Copyright (C) 2009-2025 SonarSource SA
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
package org.sonar.db.jira.dto;

import java.io.Serializable;

public final class JiraOrganizationBindingDto implements Serializable {

  private String id;
  private long createdAt;
  private long updatedAt;
  private String sonarOrganizationUuid;
  private String jiraInstanceUrl;
  private String jiraCloudId;
  private String jiraAccessToken;
  private long jiraAccessTokenExpiresAt;
  private String jiraRefreshToken;
  private long jiraRefreshTokenCreatedAt;
  private long jiraRefreshTokenUpdatedAt;
  private String updatedBy;
  private boolean isTokenShared;

  public JiraOrganizationBindingDto() {
    // Sonar rule S1258
  }

  public String getId() {
    return id;
  }

  public JiraOrganizationBindingDto setId(String id) {
    this.id = id;
    return this;
  }

  public long getCreatedAt() {
    return createdAt;
  }

  public JiraOrganizationBindingDto setCreatedAt(long createdAt) {
    this.createdAt = createdAt;
    return this;
  }

  public long getUpdatedAt() {
    return updatedAt;
  }

  public JiraOrganizationBindingDto setUpdatedAt(long updatedAt) {
    this.updatedAt = updatedAt;
    return this;
  }

  public String getSonarOrganizationUuid() {
    return sonarOrganizationUuid;
  }

  public JiraOrganizationBindingDto setSonarOrganizationUuid(String sonarOrganizationUuid) {
    this.sonarOrganizationUuid = sonarOrganizationUuid;
    return this;
  }

  public String getJiraInstanceUrl() {
    return jiraInstanceUrl;
  }

  public JiraOrganizationBindingDto setJiraInstanceUrl(String jiraInstanceUrl) {
    this.jiraInstanceUrl = jiraInstanceUrl;
    return this;
  }

  public String getJiraCloudId() {
    return jiraCloudId;
  }

  public JiraOrganizationBindingDto setJiraCloudId(String jiraCloudId) {
    this.jiraCloudId = jiraCloudId;
    return this;
  }

  public String getJiraAccessToken() {
    return jiraAccessToken;
  }

  public JiraOrganizationBindingDto setJiraAccessToken(String jiraAccessToken) {
    this.jiraAccessToken = jiraAccessToken;
    return this;
  }

  public long getJiraAccessTokenExpiresAt() {
    return jiraAccessTokenExpiresAt;
  }

  public JiraOrganizationBindingDto setJiraAccessTokenExpiresAt(long jiraAccessTokenExpiresAt) {
    this.jiraAccessTokenExpiresAt = jiraAccessTokenExpiresAt;
    return this;
  }

  public String getJiraRefreshToken() {
    return jiraRefreshToken;
  }

  public JiraOrganizationBindingDto setJiraRefreshToken(String jiraRefreshToken) {
    this.jiraRefreshToken = jiraRefreshToken;
    return this;
  }

  public long getJiraRefreshTokenCreatedAt() {
    return jiraRefreshTokenCreatedAt;
  }

  public JiraOrganizationBindingDto setJiraRefreshTokenCreatedAt(long jiraRefreshTokenCreatedAt) {
    this.jiraRefreshTokenCreatedAt = jiraRefreshTokenCreatedAt;
    return this;
  }

  public long getJiraRefreshTokenUpdatedAt() {
    return jiraRefreshTokenUpdatedAt;
  }

  public JiraOrganizationBindingDto setJiraRefreshTokenUpdatedAt(long jiraRefreshTokenUpdatedAt) {
    this.jiraRefreshTokenUpdatedAt = jiraRefreshTokenUpdatedAt;
    return this;
  }

  public String getUpdatedBy() {
    return updatedBy;
  }

  public JiraOrganizationBindingDto setUpdatedBy(String updatedBy) {
    this.updatedBy = updatedBy;
    return this;
  }

  public boolean isTokenShared() {
    return isTokenShared;
  }

  public JiraOrganizationBindingDto setIsTokenShared(boolean isTokenShared) {
    this.isTokenShared = isTokenShared;
    return this;
  }
}
