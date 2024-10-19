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
package org.sonarqube.ws.client.issues;

public class PullRequest {

  private String projectKey;
  private String branchName;
  private String languages;
  private String ruleRepositories;
  private String resolvedOnly;
  private String changedSince;

  public String getProjectKey() {
    return projectKey;
  }

  public PullRequest setProjectKey(String projectKey) {
    this.projectKey = projectKey;
    return this;
  }

  public String getBranchName() {
    return branchName;
  }

  public PullRequest setBranchName(String branchName) {
    this.branchName = branchName;
    return this;
  }

  public String getLanguages() {
    return languages;
  }

  public PullRequest setLanguages(String languages) {
    this.languages = languages;
    return this;
  }

  public String getRuleRepositories() {
    return ruleRepositories;
  }

  public PullRequest setRuleRepositories(String ruleRepositories) {
    this.ruleRepositories = ruleRepositories;
    return this;
  }

  public String getResolvedOnly() {
    return resolvedOnly;
  }

  public PullRequest setResolvedOnly(String resolvedOnly) {
    this.resolvedOnly = resolvedOnly;
    return this;
  }

  public String getChangedSince() {
    return changedSince;
  }

  public PullRequest setChangedSince(String changedSince) {
    this.changedSince = changedSince;
    return this;
  }
}
