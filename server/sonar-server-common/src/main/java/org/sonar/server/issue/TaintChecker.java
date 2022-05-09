/*
 * SonarQube
 * Copyright (C) 2009-2022 SonarSource SA
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

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import org.jetbrains.annotations.NotNull;
import org.sonar.db.issue.IssueDto;

public class TaintChecker {

  private static final Set<String> TAINT_REPOSITORIES = Set.of("roslyn.sonaranalyzer.security.cs", "javasecurity", "jssecurity", "tssecurity", "phpsecurity", "pythonsecurity");

  private TaintChecker() {
    throw new IllegalStateException("Utility class, cannot be instantiated.");
  }

  public static List<IssueDto> getTaintIssuesOnly(List<IssueDto> issues) {
    return filterTaintIssues(issues, true);
  }

  public static List<IssueDto> getStandardIssuesOnly(List<IssueDto> issues) {
    return filterTaintIssues(issues, false);
  }

  public static Map<Boolean, List<IssueDto>> mapIssuesByTaintStatus(List<IssueDto> issues) {
    Map<Boolean, List<IssueDto>> issuesMap = new HashMap<>();
    issuesMap.put(true, getTaintIssuesOnly(issues));
    issuesMap.put(false, getStandardIssuesOnly(issues));
    return issuesMap;
  }

  private static List<IssueDto> filterTaintIssues(List<IssueDto> issues, boolean returnTaint) {
    return issues.stream()
      .filter(getTaintIssueFilter(returnTaint))
      .collect(Collectors.toList());
  }

  @NotNull
  private static Predicate<IssueDto> getTaintIssueFilter(boolean returnTaint) {
    if (returnTaint) {
      return issueDto -> TAINT_REPOSITORIES.contains(issueDto.getRuleRepo());
    }
    return issueDto -> !TAINT_REPOSITORIES.contains(issueDto.getRuleRepo());
  }

}
