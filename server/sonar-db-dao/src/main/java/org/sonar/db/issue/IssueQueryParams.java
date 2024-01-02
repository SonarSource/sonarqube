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
package org.sonar.db.issue;

import java.util.ArrayList;
import java.util.List;
import javax.annotation.CheckForNull;
import javax.annotation.Nullable;

import static java.util.Objects.requireNonNullElse;

public class IssueQueryParams {

  private final String branchUuid;
  private final List<String> languages;
  private final boolean resolvedOnly;
  private final Long changedSince;
  private final List<String> ruleRepositories;
  private final List<String> excludingRuleRepositories;

  public IssueQueryParams(String branchUuid, @Nullable List<String> languages, @Nullable List<String> ruleRepositories,
    @Nullable List<String> excludingRuleRepositories, boolean resolvedOnly, @Nullable Long changedSince) {
    this.branchUuid = branchUuid;
    this.languages = requireNonNullElse(languages, new ArrayList<>());
    this.ruleRepositories = requireNonNullElse(ruleRepositories, new ArrayList<>());
    this.excludingRuleRepositories = requireNonNullElse(excludingRuleRepositories, new ArrayList<>());
    this.resolvedOnly = resolvedOnly;
    this.changedSince = changedSince;
  }

  public String getBranchUuid() {
    return branchUuid;
  }

  public List<String> getLanguages() {
    return languages;
  }

  public List<String> getRuleRepositories() {
    return ruleRepositories;
  }

  public List<String> getExcludingRuleRepositories() {
    return excludingRuleRepositories;
  }

  public boolean isResolvedOnly() {
    return resolvedOnly;
  }

  @CheckForNull
  public Long getChangedSince() {
    return changedSince;
  }
}
