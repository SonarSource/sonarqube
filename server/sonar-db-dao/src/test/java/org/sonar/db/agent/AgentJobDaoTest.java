/*
 * SonarQube
 * Copyright (C) SonarSource Sàrl
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
package org.sonar.db.agent;

import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.sonar.db.DbTester;

import static org.assertj.core.api.Assertions.assertThat;
import static org.sonar.db.Pagination.forPage;

class AgentJobDaoTest {

  @RegisterExtension
  private final DbTester db = DbTester.create();

  private final AgentJobDao underTest = db.getDbClient().agentJobDao();

  @Test
  void selectByQuery_whenNoFilters_shouldReturnAllOrderedByCreatedAtDesc() {
    insertAgentJob("job-1", "HUNTER", "SUCCEEDED", 1_000L);
    insertAgentJob("job-2", "REMEDIATION", "FAILED", 3_000L);
    insertAgentJob("job-3", "HUNTER", "RUNNING", 2_000L);

    List<AgentJobDto> result = underTest.selectByQuery(db.getSession(), new AgentJobQuery(), forPage(1).andSize(10));

    assertThat(result).extracting(AgentJobDto::getId).containsExactly("job-2", "job-3", "job-1");
  }

  @Test
  void selectByQuery_whenFilteringByJobId_shouldReturnMatchingJobOnly() {
    insertAgentJob("job-1", "HUNTER", "SUCCEEDED", 1_000L);
    insertAgentJob("job-2", "REMEDIATION", "SUCCEEDED", 2_000L);

    AgentJobQuery query = new AgentJobQuery().setJobIds(List.of("job-2"));
    List<AgentJobDto> result = underTest.selectByQuery(db.getSession(), query, forPage(1).andSize(10));

    assertThat(result).extracting(AgentJobDto::getId).containsExactly("job-2");
  }

  @Test
  void selectByQuery_whenFilteringByMultipleJobIds_shouldReturnAllMatchingJobs() {
    insertAgentJob("job-1", "HUNTER", "SUCCEEDED", 1_000L);
    insertAgentJob("job-2", "REMEDIATION", "SUCCEEDED", 2_000L);
    insertAgentJob("job-3", "REMEDIATION", "SUCCEEDED", 3_000L);

    AgentJobQuery query = new AgentJobQuery().setJobIds(List.of("job-1", "job-3"));
    List<AgentJobDto> result = underTest.selectByQuery(db.getSession(), query, forPage(1).andSize(10));

    assertThat(result).extracting(AgentJobDto::getId).containsExactlyInAnyOrder("job-1", "job-3");
  }

  @Test
  void selectByQuery_whenJobIdsIsEmpty_shouldShortCircuitToEmptyWithoutMatchingEverything() {
    insertAgentJob("job-1", "HUNTER", "SUCCEEDED", 1_000L);

    AgentJobQuery query = new AgentJobQuery().setJobIds(List.of());
    List<AgentJobDto> result = underTest.selectByQuery(db.getSession(), query, forPage(1).andSize(10));

    assertThat(result).isEmpty();
  }

  @Test
  void selectByQuery_whenJobIdsExceedsOracleBindLimit_shouldShortCircuitToEmpty() {
    insertAgentJob("job-1", "HUNTER", "SUCCEEDED", 1_000L);
    List<String> tooManyIds = new ArrayList<>(List.of("job-1"));
    for (int i = 0; i < AgentJobQuery.MAX_JOB_IDS; i++) {
      tooManyIds.add("job-id-" + i);
    }

    AgentJobQuery query = new AgentJobQuery().setJobIds(tooManyIds);
    List<AgentJobDto> result = underTest.selectByQuery(db.getSession(), query, forPage(1).andSize(10));

    assertThat(result).isEmpty();
  }

  @Test
  void selectByQuery_whenFilteringByStatus_shouldReturnMatchingJobsOnly() {
    insertAgentJob("job-1", "HUNTER", "SUCCEEDED", 1_000L);
    insertAgentJob("job-2", "REMEDIATION", "FAILED", 2_000L);
    insertAgentJob("job-3", "HUNTER", "RUNNING", 3_000L);

    AgentJobQuery query = new AgentJobQuery().setStatuses(List.of("SUCCEEDED", "FAILED"));
    List<AgentJobDto> result = underTest.selectByQuery(db.getSession(), query, forPage(1).andSize(10));

    assertThat(result).extracting(AgentJobDto::getId).containsExactlyInAnyOrder("job-1", "job-2");
  }

  @Test
  void selectByQuery_whenFilteringByAgentType_shouldReturnMatchingJobsOnly() {
    insertAgentJob("job-1", "HUNTER", "SUCCEEDED", 1_000L);
    insertAgentJob("job-2", "REMEDIATION", "SUCCEEDED", 2_000L);

    AgentJobQuery query = new AgentJobQuery().setAgentTypes(List.of("REMEDIATION"));
    List<AgentJobDto> result = underTest.selectByQuery(db.getSession(), query, forPage(1).andSize(10));

    assertThat(result).extracting(AgentJobDto::getId).containsExactly("job-2");
  }

  @Test
  void selectByQuery_whenFilteringByCreatedAtRange_shouldReturnMatchingJobsOnly() {
    insertAgentJob("job-1", "HUNTER", "SUCCEEDED", 1_000L);
    insertAgentJob("job-2", "HUNTER", "SUCCEEDED", 2_000L);
    insertAgentJob("job-3", "HUNTER", "SUCCEEDED", 3_000L);

    AgentJobQuery query = new AgentJobQuery().setMinCreatedAt(1_500L).setMaxCreatedAt(2_500L);
    List<AgentJobDto> result = underTest.selectByQuery(db.getSession(), query, forPage(1).andSize(10));

    assertThat(result).extracting(AgentJobDto::getId).containsExactly("job-2");
  }

  @Test
  void selectByQuery_whenFilteringByMultipleAgentTypes_shouldReturnAllMatchingTypes() {
    insertAgentJob("job-1", "HUNTER", "SUCCEEDED", 1_000L);
    insertAgentJob("job-2", "REMEDIATION", "SUCCEEDED", 2_000L);

    AgentJobQuery query = new AgentJobQuery().setAgentTypes(List.of("HUNTER", "REMEDIATION"));
    List<AgentJobDto> result = underTest.selectByQuery(db.getSession(), query, forPage(1).andSize(10));

    assertThat(result).extracting(AgentJobDto::getId).containsExactlyInAnyOrder("job-1", "job-2");
  }

  @Test
  void selectByQuery_whenFilteringByMinCreatedAtOnly_shouldReturnJobsCreatedOnOrAfter() {
    insertAgentJob("job-1", "HUNTER", "SUCCEEDED", 1_000L);
    insertAgentJob("job-2", "HUNTER", "SUCCEEDED", 2_000L);
    insertAgentJob("job-3", "HUNTER", "SUCCEEDED", 3_000L);

    AgentJobQuery query = new AgentJobQuery().setMinCreatedAt(2_000L);
    List<AgentJobDto> result = underTest.selectByQuery(db.getSession(), query, forPage(1).andSize(10));

    assertThat(result).extracting(AgentJobDto::getId).containsExactlyInAnyOrder("job-2", "job-3");
  }

  @Test
  void selectByQuery_whenFilteringByMaxCreatedAtOnly_shouldReturnJobsCreatedOnOrBefore() {
    insertAgentJob("job-1", "HUNTER", "SUCCEEDED", 1_000L);
    insertAgentJob("job-2", "HUNTER", "SUCCEEDED", 2_000L);
    insertAgentJob("job-3", "HUNTER", "SUCCEEDED", 3_000L);

    AgentJobQuery query = new AgentJobQuery().setMaxCreatedAt(2_000L);
    List<AgentJobDto> result = underTest.selectByQuery(db.getSession(), query, forPage(1).andSize(10));

    assertThat(result).extracting(AgentJobDto::getId).containsExactlyInAnyOrder("job-1", "job-2");
  }

  @Test
  void selectByQuery_whenCombiningAllFilters_shouldReturnOnlyJobsMatchingEveryFilter() {
    insertAgentJob("job-1", "HUNTER", "PENDING", 1_000L); // wrong type and status
    insertAgentJob("job-2", "REMEDIATION", "PENDING", 1_500L); // too old
    insertAgentJob("job-3", "REMEDIATION", "PENDING", 2_000L); // matches everything
    insertAgentJob("job-4", "REMEDIATION", "SUCCEEDED", 2_000L); // wrong status
    insertAgentJob("job-5", "REMEDIATION", "PENDING", 4_000L); // too recent, and excluded by jobIds anyway

    AgentJobQuery query = new AgentJobQuery()
      .setJobIds(List.of("job-3", "job-5"))
      .setStatuses(List.of("PENDING", "RUNNING"))
      .setAgentTypes(List.of("REMEDIATION"))
      .setMinCreatedAt(1_800L)
      .setMaxCreatedAt(3_000L);
    List<AgentJobDto> result = underTest.selectByQuery(db.getSession(), query, forPage(1).andSize(10));

    assertThat(result).extracting(AgentJobDto::getId).containsExactly("job-3");
  }

  @Test
  void selectByQuery_shouldPaginate() {
    insertAgentJob("job-1", "HUNTER", "SUCCEEDED", 1_000L);
    insertAgentJob("job-2", "HUNTER", "SUCCEEDED", 2_000L);
    insertAgentJob("job-3", "HUNTER", "SUCCEEDED", 3_000L);

    List<AgentJobDto> firstPage = underTest.selectByQuery(db.getSession(), new AgentJobQuery(), forPage(1).andSize(2));
    List<AgentJobDto> secondPage = underTest.selectByQuery(db.getSession(), new AgentJobQuery(), forPage(2).andSize(2));

    assertThat(firstPage).extracting(AgentJobDto::getId).containsExactly("job-3", "job-2");
    assertThat(secondPage).extracting(AgentJobDto::getId).containsExactly("job-1");
  }

  @Test
  void selectByQuery_shouldMapAllColumns() {
    db.executeInsert("agent_jobs",
      "id", "job-1",
      "project_id", "project-1",
      "branch", "main",
      "repository_url", "https://example.com/repo.git",
      "revision", "abc123",
      "agent_type", "REMEDIATION",
      "workflow_type", "PULL_REQUEST",
      "analysis_type", "INCREMENTAL",
      "status", "FAILED",
      "substatus", "some-failure-reason",
      "error_key", "some-error-key",
      "findings_count", 5,
      "created_at", 1_000L,
      "updated_at", 2_000L,
      "started_at", 1_100L,
      "finished_at", 1_900L);

    List<AgentJobDto> result = underTest.selectByQuery(db.getSession(), new AgentJobQuery(), forPage(1).andSize(10));

    assertThat(result).hasSize(1);
    AgentJobDto dto = result.get(0);
    assertThat(dto.getId()).isEqualTo("job-1");
    assertThat(dto.getProjectId()).isEqualTo("project-1");
    assertThat(dto.getBranch()).isEqualTo("main");
    assertThat(dto.getRepositoryUrl()).isEqualTo("https://example.com/repo.git");
    assertThat(dto.getRevision()).isEqualTo("abc123");
    assertThat(dto.getAgentType()).isEqualTo("REMEDIATION");
    assertThat(dto.getWorkflowType()).isEqualTo("PULL_REQUEST");
    assertThat(dto.getAnalysisType()).isEqualTo("INCREMENTAL");
    assertThat(dto.getStatus()).isEqualTo("FAILED");
    assertThat(dto.getSubStatus()).isEqualTo("some-failure-reason");
    assertThat(dto.getErrorKey()).isEqualTo("some-error-key");
    assertThat(dto.getFindingsCount()).isEqualTo(5);
    assertThat(dto.getCreatedAt()).isEqualTo(1_000L);
    assertThat(dto.getUpdatedAt()).isEqualTo(2_000L);
    assertThat(dto.getStartedAt()).isEqualTo(1_100L);
    assertThat(dto.getFinishedAt()).isEqualTo(1_900L);
    assertThat(dto.getProjectKey()).isNull();
    assertThat(dto.getProjectName()).isNull();
  }

  @Test
  void selectByQuery_whenProjectExists_shouldReturnProjectKeyAndName() {
    var project = db.components().insertPrivateProject();
    db.executeInsert("agent_jobs",
      "id", "job-1",
      "project_id", project.getProjectDto().getUuid(),
      "repository_url", "https://example.com/job-1.git",
      "agent_type", "HUNTER",
      "analysis_type", "FULL",
      "status", "SUCCEEDED",
      "created_at", 1_000L,
      "updated_at", 1_000L);

    List<AgentJobDto> result = underTest.selectByQuery(db.getSession(), new AgentJobQuery(), forPage(1).andSize(10));

    assertThat(result).hasSize(1);
    AgentJobDto dto = result.get(0);
    assertThat(dto.getProjectKey()).isEqualTo(project.getProjectDto().getKey());
    assertThat(dto.getProjectName()).isEqualTo(project.getProjectDto().getName());
  }

  @Test
  void selectByQuery_whenProjectHasBeenDeleted_shouldReturnNullProjectKeyAndName() {
    insertAgentJob("job-1", "HUNTER", "SUCCEEDED", 1_000L);

    List<AgentJobDto> result = underTest.selectByQuery(db.getSession(), new AgentJobQuery(), forPage(1).andSize(10));

    assertThat(result).hasSize(1);
    AgentJobDto dto = result.get(0);
    assertThat(dto.getProjectKey()).isNull();
    assertThat(dto.getProjectName()).isNull();
  }

  @Test
  void countByQuery_shouldCountMatchingJobsIgnoringPagination() {
    insertAgentJob("job-1", "HUNTER", "SUCCEEDED", 1_000L);
    insertAgentJob("job-2", "HUNTER", "SUCCEEDED", 2_000L);
    insertAgentJob("job-3", "HUNTER", "RUNNING", 3_000L);

    int count = underTest.countByQuery(db.getSession(), new AgentJobQuery().setStatuses(List.of("SUCCEEDED")));

    assertThat(count).isEqualTo(2);
  }

  @Test
  void countByQuery_whenFilteringByAgentTypeAndCreatedAtRange_shouldCountMatchingOnly() {
    insertAgentJob("job-1", "HUNTER", "SUCCEEDED", 1_000L);
    insertAgentJob("job-2", "REMEDIATION", "SUCCEEDED", 2_000L);
    insertAgentJob("job-3", "REMEDIATION", "SUCCEEDED", 3_000L);
    insertAgentJob("job-4", "REMEDIATION", "SUCCEEDED", 5_000L);

    AgentJobQuery query = new AgentJobQuery()
      .setAgentTypes(List.of("REMEDIATION"))
      .setMinCreatedAt(1_500L)
      .setMaxCreatedAt(4_000L);
    int count = underTest.countByQuery(db.getSession(), query);

    assertThat(count).isEqualTo(2);
  }

  @Test
  void countByQuery_whenFilteringByJobIds_shouldCountMatchingOnly() {
    insertAgentJob("job-1", "HUNTER", "SUCCEEDED", 1_000L);
    insertAgentJob("job-2", "HUNTER", "SUCCEEDED", 2_000L);
    insertAgentJob("job-3", "HUNTER", "SUCCEEDED", 3_000L);

    int count = underTest.countByQuery(db.getSession(), new AgentJobQuery().setJobIds(List.of("job-1", "job-3")));

    assertThat(count).isEqualTo(2);
  }

  @Test
  void countByQuery_whenJobIdsExceedsOracleBindLimit_shouldShortCircuitToZero() {
    insertAgentJob("job-1", "HUNTER", "SUCCEEDED", 1_000L);
    List<String> tooManyIds = new ArrayList<>(List.of("job-1"));
    for (int i = 0; i < AgentJobQuery.MAX_JOB_IDS; i++) {
      tooManyIds.add("job-id-" + i);
    }

    int count = underTest.countByQuery(db.getSession(), new AgentJobQuery().setJobIds(tooManyIds));

    assertThat(count).isZero();
  }

  @Test
  void countByQuery_whenNoMatch_shouldReturnZero() {
    int count = underTest.countByQuery(db.getSession(), new AgentJobQuery());

    assertThat(count).isZero();
  }

  private void insertAgentJob(String id, String agentType, String status, long createdAt) {
    db.executeInsert("agent_jobs",
      "id", id,
      "project_id", "project-" + id,
      "repository_url", "https://example.com/" + id + ".git",
      "agent_type", agentType,
      "analysis_type", "FULL",
      "status", status,
      "created_at", createdAt,
      "updated_at", createdAt);
  }
}
