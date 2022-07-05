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
package org.sonar.db.issue;

import java.util.List;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class IssueQueryParamsTest {
  private final List<String> languages = List.of("java");
  private final List<String> ruleRepositories = List.of("js-security", "java");

  @Test
  public void validate_issue_query_parameters_structure() {
    boolean resolvedOnly = false;
    long changedSince = 1_000_000L;
    String branchUuid = "master-branch-uuid";

    IssueQueryParams queryParameters = new IssueQueryParams(branchUuid, languages, ruleRepositories, resolvedOnly, changedSince);

    assertThat(queryParameters.getBranchUuid()).isEqualTo(branchUuid);
    assertThat(queryParameters.getLanguages()).isEqualTo(languages);
    assertThat(queryParameters.getRuleRepositories()).isEqualTo(ruleRepositories);
    assertThat(queryParameters.isResolvedOnly()).isFalse();
    assertThat(queryParameters.getChangedSince()).isEqualTo(changedSince);

  }
}
