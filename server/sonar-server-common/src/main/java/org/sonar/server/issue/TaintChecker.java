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
package org.sonar.server.issue;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;
import org.jetbrains.annotations.NotNull;
import org.sonar.api.config.Configuration;
import org.sonar.api.rules.RuleType;
import org.sonar.core.issue.DefaultIssue;
import org.sonar.db.issue.IssueDto;

public class TaintChecker {
  protected static final String EXTRA_TAINT_REPOSITORIES = "sonar.issues.taint.extra.repositories";

  private final Configuration config;

  private final List<String> taintRepositories;

  public TaintChecker(Configuration config) {
    this.config = config;
    this.taintRepositories = initializeRepositories();
  }

  public List<IssueDto> getTaintIssuesOnly(List<IssueDto> issues) {
    return filterTaintIssues(issues, true);
  }

  public List<IssueDto> getStandardIssuesOnly(List<IssueDto> issues) {
    return filterTaintIssues(issues, false);
  }

  public Map<Boolean, List<IssueDto>> mapIssuesByTaintStatus(List<IssueDto> issues) {
    Map<Boolean, List<IssueDto>> issuesMap = new HashMap<>();
    issuesMap.put(true, getTaintIssuesOnly(issues));
    issuesMap.put(false, getStandardIssuesOnly(issues));
    return issuesMap;
  }

  private List<IssueDto> filterTaintIssues(List<IssueDto> issues, boolean returnTaint) {
    return issues.stream()
      .filter(getTaintIssueFilter(returnTaint))
      .toList();
  }

  @NotNull
  private Predicate<IssueDto> getTaintIssueFilter(boolean returnTaint) {
    if (returnTaint) {
      return issueDto -> taintRepositories.contains(issueDto.getRuleRepo());
    }
    return issueDto -> !taintRepositories.contains(issueDto.getRuleRepo());
  }

  public List<String> getTaintRepositories() {
    return taintRepositories;
  }

  private List<String> initializeRepositories() {
    List<String> repositories = new ArrayList<>(List.of("roslyn.sonaranalyzer.security.cs",
      "javasecurity", "jssecurity", "tssecurity", "phpsecurity", "pythonsecurity"));

    if (!config.hasKey(EXTRA_TAINT_REPOSITORIES)) {
      return repositories;
    }

    repositories.addAll(Arrays.stream(config.getStringArray(EXTRA_TAINT_REPOSITORIES)).toList());

    return repositories;
  }


  public boolean isTaintVulnerability(DefaultIssue issue) {
    return taintRepositories.contains(issue.getRuleKey().repository())
      && issue.getLocations() != null
      && !RuleType.SECURITY_HOTSPOT.equals(issue.type());
  }

}
