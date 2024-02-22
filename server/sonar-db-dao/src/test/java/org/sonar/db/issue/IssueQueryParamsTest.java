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

import java.util.List;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class IssueQueryParamsTest {
  private final List<String> ruleRepositories = List.of("js-security", "java");

  @Test
  void validate_issue_query_parameters_structure() {
    boolean resolvedOnly = false;
    long changedSince = 1_000_000L;
    String branchUuid = "master-branch-uuid";

    IssueQueryParams queryParameters = new IssueQueryParams(branchUuid, null, ruleRepositories, null, resolvedOnly, changedSince);

    assertThat(queryParameters.getBranchUuid()).isEqualTo(branchUuid);
    assertThat(queryParameters.getLanguages()).isNotNull().isEmpty();
    assertThat(queryParameters.getRuleRepositories()).isEqualTo(ruleRepositories);
    assertThat(queryParameters.getExcludingRuleRepositories()).isNotNull().isEmpty();
    assertThat(queryParameters.isResolvedOnly()).isFalse();
    assertThat(queryParameters.getChangedSince()).isEqualTo(changedSince);

  }
}
