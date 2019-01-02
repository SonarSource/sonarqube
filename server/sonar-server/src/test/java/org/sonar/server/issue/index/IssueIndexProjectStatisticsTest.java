/*
 * SonarQube
 * Copyright (C) 2009-2019 SonarSource SA
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
import org.junit.Rule;
import org.junit.Test;
import org.sonar.api.issue.Issue;
import org.sonar.api.utils.System2;
import org.sonar.db.component.ComponentDto;
import org.sonar.db.organization.OrganizationDto;
import org.sonar.server.es.EsTester;
import org.sonar.server.permission.index.IndexPermissions;
import org.sonar.server.permission.index.PermissionIndexerTester;
import org.sonar.server.permission.index.WebAuthorizationTypeSupport;
import org.sonar.server.tester.UserSessionRule;

import static java.util.Arrays.asList;
import static java.util.Arrays.stream;
import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static java.util.stream.Collectors.toList;
import static org.apache.commons.lang.RandomStringUtils.randomAlphanumeric;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;
import static org.mockito.Mockito.mock;
import static org.sonar.api.resources.Qualifiers.PROJECT;
import static org.sonar.db.component.ComponentTesting.newBranchDto;
import static org.sonar.db.component.ComponentTesting.newPrivateProjectDto;
import static org.sonar.db.component.ComponentTesting.newProjectBranch;
import static org.sonar.db.organization.OrganizationTesting.newOrganizationDto;
import static org.sonar.server.issue.IssueDocTesting.newDoc;

public class IssueIndexProjectStatisticsTest {

  private System2 system2 = mock(System2.class);
  @Rule
  public EsTester es = EsTester.create();
  @Rule
  public UserSessionRule userSessionRule = UserSessionRule.standalone();

  private IssueIndexer issueIndexer = new IssueIndexer(es.client(), null, new IssueIteratorFactory(null));
  private PermissionIndexerTester authorizationIndexer = new PermissionIndexerTester(es, issueIndexer);

  private IssueIndex underTest = new IssueIndex(es.client(), system2, userSessionRule, new WebAuthorizationTypeSupport(userSessionRule));

  @Test
  public void searchProjectStatistics_returns_empty_list_if_no_input() {
    List<ProjectStatistics> result = underTest.searchProjectStatistics(emptyList(), emptyList(), "unknownUser");
    assertThat(result).isEmpty();
  }

  @Test
  public void searchProjectStatistics_returns_empty_list_if_the_input_does_not_match_anything() {
    List<ProjectStatistics> result = underTest.searchProjectStatistics(singletonList("unknownProjectUuid"), singletonList(1_111_234_567_890L), "unknownUser");
    assertThat(result).isEmpty();
  }

  @Test
  public void searchProjectStatistics_returns_something() {
    OrganizationDto organization = newOrganizationDto();
    ComponentDto project = newPrivateProjectDto(organization);
    String userUuid = randomAlphanumeric(40);
    long from = 1_111_234_567_890L;
    indexIssues(newDoc("issue1", project).setAssigneeUuid(userUuid).setFuncCreationDate(new Date(from + 1L)));

    List<ProjectStatistics> result = underTest.searchProjectStatistics(singletonList(project.uuid()), singletonList(from), userUuid);

    assertThat(result).extracting(ProjectStatistics::getProjectUuid).containsExactly(project.uuid());
  }

  @Test
  public void searchProjectStatistics_does_not_return_results_if_assignee_does_not_match() {
    OrganizationDto org1 = newOrganizationDto();
    ComponentDto project = newPrivateProjectDto(org1);
    String user1Uuid = randomAlphanumeric(40);
    String user2Uuid = randomAlphanumeric(40);
    long from = 1_111_234_567_890L;
    indexIssues(newDoc("issue1", project).setAssigneeUuid(user1Uuid).setFuncCreationDate(new Date(from + 1L)));

    List<ProjectStatistics> result = underTest.searchProjectStatistics(singletonList(project.uuid()), singletonList(from), user2Uuid);

    assertThat(result).isEmpty();
  }

  @Test
  public void searchProjectStatistics_returns_results_if_assignee_matches() {
    OrganizationDto org1 = newOrganizationDto();
    ComponentDto project = newPrivateProjectDto(org1);
    String user1Uuid = randomAlphanumeric(40);
    long from = 1_111_234_567_890L;
    indexIssues(newDoc("issue1", project).setAssigneeUuid(user1Uuid).setFuncCreationDate(new Date(from + 1L)));

    List<ProjectStatistics> result = underTest.searchProjectStatistics(singletonList(project.uuid()), singletonList(from), user1Uuid);

    assertThat(result).extracting(ProjectStatistics::getProjectUuid).containsExactly(project.uuid());
  }

  @Test
  public void searchProjectStatistics_returns_results_if_functional_date_is_strictly_after_from_date() {
    OrganizationDto org1 = newOrganizationDto();
    ComponentDto project = newPrivateProjectDto(org1);
    String userUuid = randomAlphanumeric(40);
    long from = 1_111_234_567_890L;
    indexIssues(newDoc("issue1", project).setAssigneeUuid(userUuid).setFuncCreationDate(new Date(from + 1L)));

    List<ProjectStatistics> result = underTest.searchProjectStatistics(singletonList(project.uuid()), singletonList(from), userUuid);

    assertThat(result).extracting(ProjectStatistics::getProjectUuid).containsExactly(project.uuid());
  }

  @Test
  public void searchProjectStatistics_does_not_return_results_if_functional_date_is_same_as_from_date() {
    OrganizationDto org1 = newOrganizationDto();
    ComponentDto project = newPrivateProjectDto(org1);
    String userUuid = randomAlphanumeric(40);
    long from = 1_111_234_567_890L;
    indexIssues(newDoc("issue1", project).setAssigneeUuid(userUuid).setFuncCreationDate(new Date(from)));

    List<ProjectStatistics> result = underTest.searchProjectStatistics(singletonList(project.uuid()), singletonList(from), userUuid);

    assertThat(result).extracting(ProjectStatistics::getProjectUuid).containsExactly(project.uuid());
  }

  @Test
  public void searchProjectStatistics_does_not_return_resolved_issues() {
    OrganizationDto org1 = newOrganizationDto();
    ComponentDto project = newPrivateProjectDto(org1);
    String userUuid = randomAlphanumeric(40);
    long from = 1_111_234_567_890L;
    indexIssues(
      newDoc("issue1", project).setAssigneeUuid(userUuid).setFuncCreationDate(new Date(from + 1L)).setResolution(Issue.RESOLUTION_FALSE_POSITIVE),
      newDoc("issue1", project).setAssigneeUuid(userUuid).setFuncCreationDate(new Date(from + 1L)).setResolution(Issue.RESOLUTION_FIXED),
      newDoc("issue1", project).setAssigneeUuid(userUuid).setFuncCreationDate(new Date(from + 1L)).setResolution(Issue.RESOLUTION_REMOVED),
      newDoc("issue1", project).setAssigneeUuid(userUuid).setFuncCreationDate(new Date(from + 1L)).setResolution(Issue.RESOLUTION_WONT_FIX));

    List<ProjectStatistics> result = underTest.searchProjectStatistics(singletonList(project.uuid()), singletonList(from), userUuid);

    assertThat(result).isEmpty();
  }

  @Test
  public void searchProjectStatistics_does_not_return_results_if_functional_date_is_before_from_date() {
    OrganizationDto org1 = newOrganizationDto();
    ComponentDto project = newPrivateProjectDto(org1);
    String userUuid = randomAlphanumeric(40);
    long from = 1_111_234_567_890L;
    indexIssues(newDoc("issue1", project).setAssigneeUuid(userUuid).setFuncCreationDate(new Date(from - 1000L)));

    List<ProjectStatistics> result = underTest.searchProjectStatistics(singletonList(project.uuid()), singletonList(from), userUuid);

    assertThat(result).isEmpty();
  }

  @Test
  public void searchProjectStatistics_returns_issue_count() {
    OrganizationDto org1 = newOrganizationDto();
    ComponentDto project = newPrivateProjectDto(org1);
    String userUuid = randomAlphanumeric(40);
    long from = 1_111_234_567_890L;
    indexIssues(
      newDoc("issue1", project).setAssigneeUuid(userUuid).setFuncCreationDate(new Date(from + 1L)),
      newDoc("issue2", project).setAssigneeUuid(userUuid).setFuncCreationDate(new Date(from + 1L)),
      newDoc("issue3", project).setAssigneeUuid(userUuid).setFuncCreationDate(new Date(from + 1L)));

    List<ProjectStatistics> result = underTest.searchProjectStatistics(singletonList(project.uuid()), singletonList(from), userUuid);

    assertThat(result).extracting(ProjectStatistics::getIssueCount).containsExactly(3L);
  }

  @Test
  public void searchProjectStatistics_returns_issue_count_for_multiple_projects() {
    OrganizationDto org1 = newOrganizationDto();
    ComponentDto project1 = newPrivateProjectDto(org1);
    ComponentDto project2 = newPrivateProjectDto(org1);
    ComponentDto project3 = newPrivateProjectDto(org1);
    String userUuid = randomAlphanumeric(40);
    long from = 1_111_234_567_890L;
    indexIssues(
      newDoc("issue1", project1).setAssigneeUuid(userUuid).setFuncCreationDate(new Date(from + 1L)),
      newDoc("issue2", project1).setAssigneeUuid(userUuid).setFuncCreationDate(new Date(from + 1L)),
      newDoc("issue3", project1).setAssigneeUuid(userUuid).setFuncCreationDate(new Date(from + 1L)),

      newDoc("issue4", project3).setAssigneeUuid(userUuid).setFuncCreationDate(new Date(from + 1L)),
      newDoc("issue5", project3).setAssigneeUuid(userUuid).setFuncCreationDate(new Date(from + 1L)));

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
  public void searchProjectStatistics_returns_max_date_for_multiple_projects() {
    OrganizationDto org1 = newOrganizationDto();
    ComponentDto project1 = newPrivateProjectDto(org1);
    ComponentDto project2 = newPrivateProjectDto(org1);
    ComponentDto project3 = newPrivateProjectDto(org1);
    String userUuid = randomAlphanumeric(40);
    long from = 1_111_234_567_000L;
    indexIssues(
      newDoc("issue1", project1).setAssigneeUuid(userUuid).setFuncCreationDate(new Date(from + 1_000L)),
      newDoc("issue2", project1).setAssigneeUuid(userUuid).setFuncCreationDate(new Date(from + 2_000L)),
      newDoc("issue3", project1).setAssigneeUuid(userUuid).setFuncCreationDate(new Date(from + 3_000L)),

      newDoc("issue4", project3).setAssigneeUuid(userUuid).setFuncCreationDate(new Date(from + 4_000L)),
      newDoc("issue5", project3).setAssigneeUuid(userUuid).setFuncCreationDate(new Date(from + 5_000L)));

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
  public void searchProjectStatistics_return_branch_issues() {
    OrganizationDto organization = newOrganizationDto();
    ComponentDto project = newPrivateProjectDto(organization);
    ComponentDto branch = newProjectBranch(project, newBranchDto(project).setKey("branch"));
    String userUuid = randomAlphanumeric(40);
    long from = 1_111_234_567_890L;
    indexIssues(
      newDoc("issue1", branch).setAssigneeUuid(userUuid).setFuncCreationDate(new Date(from + 1L)),
      newDoc("issue2", branch).setAssigneeUuid(userUuid).setFuncCreationDate(new Date(from + 2L)),
      newDoc("issue3", project).setAssigneeUuid(userUuid).setFuncCreationDate(new Date(from + 1L)));

    List<ProjectStatistics> result = underTest.searchProjectStatistics(singletonList(project.uuid()), singletonList(from), userUuid);

    assertThat(result)
      .extracting(ProjectStatistics::getIssueCount, ProjectStatistics::getProjectUuid, ProjectStatistics::getLastIssueDate)
      .containsExactly(
        tuple(2L, branch.uuid(), from + 2L),
        tuple(1L, project.uuid(), from + 1L));
  }

  private void indexIssues(IssueDoc... issues) {
    issueIndexer.index(asList(issues).iterator());
    authorizationIndexer.allow(stream(issues).map(issue -> new IndexPermissions(issue.projectUuid(), PROJECT).allowAnyone()).collect(toList()));
  }
}
