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
package org.sonar.server.issue.index;

import java.util.Date;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.sonar.api.issue.Issue;
import org.sonar.db.component.ComponentDto;

import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static org.apache.commons.lang3.RandomStringUtils.randomAlphanumeric;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;
import static org.sonar.db.component.ComponentTesting.newBranchComponent;
import static org.sonar.db.component.ComponentTesting.newBranchDto;
import static org.sonar.db.component.ComponentTesting.newPrivateProjectDto;
import static org.sonar.server.issue.IssueDocTesting.newDoc;
import static org.sonar.server.issue.IssueDocTesting.newDocForProject;

class IssueIndexProjectStatisticsTest extends IssueIndexTestCommon {

  @Test
  void searchProjectStatistics_returns_empty_list_if_no_input() {
    List<ProjectStatistics> result = underTest.searchProjectStatistics(emptyList(), emptyList(), "unknownUser");
    assertThat(result).isEmpty();
  }

  @Test
  void searchProjectStatistics_returns_empty_list_if_the_input_does_not_match_anything() {
    List<ProjectStatistics> result = underTest.searchProjectStatistics(singletonList("unknownProjectUuid"), singletonList(1_111_234_567_890L), "unknownUser");
    assertThat(result).isEmpty();
  }

  @Test
  void searchProjectStatistics_does_not_return_results_if_assignee_does_not_match() {
    ComponentDto project = newPrivateProjectDto();
    String user1Uuid = randomAlphanumeric(40);
    String user2Uuid = randomAlphanumeric(40);
    long from = 1_111_234_567_890L;
    indexIssues(newDocForProject("issue1", project).setAssigneeUuid(user1Uuid).setFuncCreationDate(new Date(from + 1L)));

    List<ProjectStatistics> result = underTest.searchProjectStatistics(singletonList(project.uuid()), singletonList(from), user2Uuid);

    assertThat(result).isEmpty();
  }

  @Test
  void searchProjectStatistics_returns_results_if_assignee_matches() {
    ComponentDto project = newPrivateProjectDto();
    String user1Uuid = randomAlphanumeric(40);
    long from = 1_111_234_567_890L;
    indexIssues(newDocForProject("issue1", project).setAssigneeUuid(user1Uuid).setFuncCreationDate(new Date(from + 1L)));

    List<ProjectStatistics> result = underTest.searchProjectStatistics(singletonList(project.uuid()), singletonList(from), user1Uuid);

    assertThat(result).extracting(ProjectStatistics::getProjectUuid).containsExactly(project.uuid());
  }

  @Test
  void searchProjectStatistics_returns_results_if_functional_date_is_strictly_after_from_date() {
    ComponentDto project = newPrivateProjectDto();
    String userUuid = randomAlphanumeric(40);
    long from = 1_111_234_567_890L;
    indexIssues(newDocForProject("issue1", project).setAssigneeUuid(userUuid).setFuncCreationDate(new Date(from + 1L)));

    List<ProjectStatistics> result = underTest.searchProjectStatistics(singletonList(project.uuid()), singletonList(from), userUuid);

    assertThat(result).extracting(ProjectStatistics::getProjectUuid).containsExactly(project.uuid());
  }

  @Test
  void searchProjectStatistics_does_not_return_results_if_functional_date_is_same_as_from_date() {
    ComponentDto project = newPrivateProjectDto();
    String userUuid = randomAlphanumeric(40);
    long from = 1_111_234_567_890L;
    indexIssues(newDocForProject("issue1", project).setAssigneeUuid(userUuid).setFuncCreationDate(new Date(from)));

    List<ProjectStatistics> result = underTest.searchProjectStatistics(singletonList(project.uuid()), singletonList(from), userUuid);

    assertThat(result).extracting(ProjectStatistics::getProjectUuid).containsExactly(project.uuid());
  }

  @Test
  void searchProjectStatistics_does_not_return_resolved_issues() {
    ComponentDto project = newPrivateProjectDto();
    String userUuid = randomAlphanumeric(40);
    long from = 1_111_234_567_890L;
    indexIssues(
      newDocForProject("issue1", project).setAssigneeUuid(userUuid).setFuncCreationDate(new Date(from + 1L)).setResolution(Issue.RESOLUTION_FALSE_POSITIVE),
      newDocForProject("issue1", project).setAssigneeUuid(userUuid).setFuncCreationDate(new Date(from + 1L)).setResolution(Issue.RESOLUTION_FIXED),
      newDocForProject("issue1", project).setAssigneeUuid(userUuid).setFuncCreationDate(new Date(from + 1L)).setResolution(Issue.RESOLUTION_REMOVED),
      newDocForProject("issue1", project).setAssigneeUuid(userUuid).setFuncCreationDate(new Date(from + 1L)).setResolution(Issue.RESOLUTION_WONT_FIX));

    List<ProjectStatistics> result = underTest.searchProjectStatistics(singletonList(project.uuid()), singletonList(from), userUuid);

    assertThat(result).isEmpty();
  }

  @Test
  void searchProjectStatistics_does_not_return_results_if_functional_date_is_before_from_date() {
    ComponentDto project = newPrivateProjectDto();
    String userUuid = randomAlphanumeric(40);
    long from = 1_111_234_567_890L;
    indexIssues(newDocForProject("issue1", project).setAssigneeUuid(userUuid).setFuncCreationDate(new Date(from - 1000L)));

    List<ProjectStatistics> result = underTest.searchProjectStatistics(singletonList(project.uuid()), singletonList(from), userUuid);

    assertThat(result).isEmpty();
  }

  @Test
  void searchProjectStatistics_returns_issue_count() {
    ComponentDto project = newPrivateProjectDto();
    String userUuid = randomAlphanumeric(40);
    long from = 1_111_234_567_890L;
    indexIssues(
      newDocForProject("issue1", project).setAssigneeUuid(userUuid).setFuncCreationDate(new Date(from + 1L)),
      newDocForProject("issue2", project).setAssigneeUuid(userUuid).setFuncCreationDate(new Date(from + 1L)),
      newDocForProject("issue3", project).setAssigneeUuid(userUuid).setFuncCreationDate(new Date(from + 1L)));

    List<ProjectStatistics> result = underTest.searchProjectStatistics(singletonList(project.uuid()), singletonList(from), userUuid);

    assertThat(result).extracting(ProjectStatistics::getIssueCount).containsExactly(3L);
  }

  @Test
  void searchProjectStatistics_returns_issue_count_for_multiple_projects() {
    ComponentDto project1 = newPrivateProjectDto();
    ComponentDto project2 = newPrivateProjectDto();
    ComponentDto project3 = newPrivateProjectDto();
    String userUuid = randomAlphanumeric(40);
    long from = 1_111_234_567_890L;
    indexIssues(
      newDocForProject("issue1", project1).setAssigneeUuid(userUuid).setFuncCreationDate(new Date(from + 1L)),
      newDocForProject("issue2", project1).setAssigneeUuid(userUuid).setFuncCreationDate(new Date(from + 1L)),
      newDocForProject("issue3", project1).setAssigneeUuid(userUuid).setFuncCreationDate(new Date(from + 1L)),

      newDocForProject("issue4", project3).setAssigneeUuid(userUuid).setFuncCreationDate(new Date(from + 1L)),
      newDocForProject("issue5", project3).setAssigneeUuid(userUuid).setFuncCreationDate(new Date(from + 1L)));

    List<ProjectStatistics> result = underTest.searchProjectStatistics(
      asList(project1.uuid(), project2.uuid(), project3.uuid()),
      asList(from, from, from),
      userUuid);

    assertThat(result)
      .extracting(ProjectStatistics::getProjectUuid, ProjectStatistics::getIssueCount)
      .containsExactlyInAnyOrder(
        tuple(project1.uuid(), 3L),
        tuple(project3.uuid(), 2L));
  }

  @Test
  void searchProjectStatistics_returns_max_date_for_multiple_projects() {
    ComponentDto project1 = newPrivateProjectDto();
    ComponentDto project2 = newPrivateProjectDto();
    ComponentDto project3 = newPrivateProjectDto();
    String userUuid = randomAlphanumeric(40);
    long from = 1_111_234_567_000L;
    indexIssues(
      newDocForProject("issue1", project1).setAssigneeUuid(userUuid).setFuncCreationDate(new Date(from + 1_000L)),
      newDocForProject("issue2", project1).setAssigneeUuid(userUuid).setFuncCreationDate(new Date(from + 2_000L)),
      newDocForProject("issue3", project1).setAssigneeUuid(userUuid).setFuncCreationDate(new Date(from + 3_000L)),

      newDocForProject("issue4", project3).setAssigneeUuid(userUuid).setFuncCreationDate(new Date(from + 4_000L)),
      newDocForProject("issue5", project3).setAssigneeUuid(userUuid).setFuncCreationDate(new Date(from + 5_000L)));

    List<ProjectStatistics> result = underTest.searchProjectStatistics(
      asList(project1.uuid(), project2.uuid(), project3.uuid()),
      asList(from, from, from),
      userUuid);

    assertThat(result)
      .extracting(ProjectStatistics::getProjectUuid, ProjectStatistics::getLastIssueDate)
      .containsExactlyInAnyOrder(
        tuple(project1.uuid(), from + 3_000L),
        tuple(project3.uuid(), from + 5_000L));
  }

  @Test
  void searchProjectStatistics_return_branch_issues() {
    ComponentDto project = newPrivateProjectDto();
    ComponentDto branch = newBranchComponent(project, newBranchDto(project).setKey("branch"));
    String userUuid = randomAlphanumeric(40);
    long from = 1_111_234_567_890L;
    indexIssues(
      newDoc("issue1", project.uuid(), branch).setAssigneeUuid(userUuid).setFuncCreationDate(new Date(from + 1L)),
      newDoc("issue2", project.uuid(), branch).setAssigneeUuid(userUuid).setFuncCreationDate(new Date(from + 2L)),
      newDocForProject("issue3", project).setAssigneeUuid(userUuid).setFuncCreationDate(new Date(from + 1L)));

    List<ProjectStatistics> result = underTest.searchProjectStatistics(singletonList(project.uuid()), singletonList(from), userUuid);

    assertThat(result)
      .extracting(ProjectStatistics::getIssueCount, ProjectStatistics::getProjectUuid, ProjectStatistics::getLastIssueDate)
      .containsExactly(
        tuple(2L, branch.uuid(), from + 2L),
        tuple(1L, project.uuid(), from + 1L));
  }
}
