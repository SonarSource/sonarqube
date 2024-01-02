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
package org.sonar.db.portfolio;

import java.util.Set;

public class PortfolioProjectDto {
  private String uuid;
  private String portfolioUuid;
  private String portfolioKey;
  private String projectUuid;
  private String projectKey;
  private Set<String> branchUuids;
  private String mainBranchUuid;
  private long createdAt;

  public String getUuid() {
    return uuid;
  }

  public PortfolioProjectDto setUuid(String uuid) {
    this.uuid = uuid;
    return this;
  }

  public String getMainBranchUuid() {
    return mainBranchUuid;
  }

  public void setMainBranchUuid(String mainBranchUuid) {
    this.mainBranchUuid = mainBranchUuid;
  }

  public String getPortfolioUuid() {
    return portfolioUuid;
  }

  public PortfolioProjectDto setPortfolioUuid(String portfolioUuid) {
    this.portfolioUuid = portfolioUuid;
    return this;
  }

  public String getProjectUuid() {
    return projectUuid;
  }

  public PortfolioProjectDto setProjectUuid(String projectUuid) {
    this.projectUuid = projectUuid;
    return this;
  }

  public long getCreatedAt() {
    return createdAt;
  }

  public PortfolioProjectDto setCreatedAt(long createdAt) {
    this.createdAt = createdAt;
    return this;
  }

  public String getPortfolioKey() {
    return portfolioKey;
  }

  public PortfolioProjectDto setPortfolioKey(String portfolioKey) {
    this.portfolioKey = portfolioKey;
    return this;
  }

  public String getProjectKey() {
    return projectKey;
  }

  public PortfolioProjectDto setProjectKey(String projectKey) {
    this.projectKey = projectKey;
    return this;
  }

  public Set<String> getBranchUuids() {
    return branchUuids;
  }

  public void setBranchUuids(Set<String> branchUuids) {
    this.branchUuids = branchUuids;
  }
}
