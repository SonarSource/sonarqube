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
package org.sonar.server.v2.api.agentic.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Date;
import java.util.List;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.db.Pagination;
import org.sonar.db.agent.AgentJobDao;
import org.sonar.db.agent.AgentJobDto;
import org.sonar.db.agent.AgentJobQuery;
import org.sonar.server.tester.UserSessionRule;
import org.sonar.server.v2.api.ControllerTester;
import org.sonar.server.v2.api.agentic.response.AgenticJobsSearchRestResponse;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.sonar.api.utils.DateUtils.parseEndingDateOrDateTime;
import static org.sonar.api.utils.DateUtils.parseStartingDateOrDateTime;
import static org.sonar.server.v2.WebApiEndpoints.AGENTIC_JOBS_ENDPOINT;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@RunWith(MockitoJUnitRunner.class)
public class DefaultAgenticJobsControllerTest {

  private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

  @Rule
  public UserSessionRule userSession = UserSessionRule.standalone();

  @Mock
  private DbClient dbClient;
  @Mock
  private DbSession dbSession;
  @Mock
  private AgentJobDao agentJobDao;
  private MockMvc mockMvc;

  @Before
  public void setUp() {
    mockMvc = ControllerTester.getMockMvc(new DefaultAgenticJobsController(userSession, dbClient));
    when(dbClient.openSession(false)).thenReturn(dbSession);
    when(dbClient.agentJobDao()).thenReturn(agentJobDao);
  }

  @Test
  public void search_whenCallerIsNotAdmin_shouldReturnForbidden() throws Exception {
    userSession.logIn().setNonSystemAdministrator();
    mockMvc.perform(get(AGENTIC_JOBS_ENDPOINT))
      .andExpectAll(
        status().isForbidden(),
        content().json("{\"message\":\"Insufficient privileges\"}"));
  }

  @Test
  public void search_whenNotLoggedIn_shouldReturnUnauthorized() throws Exception {
    mockMvc.perform(get(AGENTIC_JOBS_ENDPOINT))
      .andExpectAll(
        status().isUnauthorized(),
        content().json("{\"message\":\"Authentication is required\"}"));
  }

  @Test
  public void search_whenNoParameters_shouldUseDefaultPaginationAndNoFilters() throws Exception {
    userSession.logIn().setSystemAdministrator();
    when(agentJobDao.countByQuery(eq(dbSession), any())).thenReturn(0);
    when(agentJobDao.selectByQuery(eq(dbSession), any(), any())).thenReturn(List.of());

    MvcResult mvcResult = mockMvc.perform(get(AGENTIC_JOBS_ENDPOINT))
      .andExpect(status().isOk())
      .andReturn();

    AgenticJobsSearchRestResponse response = OBJECT_MAPPER.readValue(mvcResult.getResponse().getContentAsString(), AgenticJobsSearchRestResponse.class);
    assertThat(response.jobs()).isEmpty();
    assertThat(response.page().pageIndex()).isEqualTo(1);
    assertThat(response.page().pageSize()).isEqualTo(50);
    assertThat(response.page().total()).isZero();

    ArgumentCaptor<AgentJobQuery> queryCaptor = ArgumentCaptor.forClass(AgentJobQuery.class);
    ArgumentCaptor<Pagination> paginationCaptor = ArgumentCaptor.forClass(Pagination.class);
    verify(agentJobDao).selectByQuery(eq(dbSession), queryCaptor.capture(), paginationCaptor.capture());
    assertThat(queryCaptor.getValue().getJobIds()).isNull();
    assertThat(queryCaptor.getValue().getStatuses()).isNull();
    assertThat(queryCaptor.getValue().getAgentTypes()).isNull();
    assertThat(queryCaptor.getValue().getMinCreatedAt()).isNull();
    assertThat(queryCaptor.getValue().getMaxCreatedAt()).isNull();
    assertThat(paginationCaptor.getValue().getPage()).isEqualTo(1);
    assertThat(paginationCaptor.getValue().getPageSize()).isEqualTo(50);
  }

  @Test
  public void search_whenSingleIdProvided_shouldForwardIt() throws Exception {
    userSession.logIn().setSystemAdministrator();
    when(agentJobDao.countByQuery(eq(dbSession), any())).thenReturn(0);
    when(agentJobDao.selectByQuery(eq(dbSession), any(), any())).thenReturn(List.of());

    mockMvc.perform(get(AGENTIC_JOBS_ENDPOINT).param("id", "job-1"))
      .andExpect(status().isOk());

    ArgumentCaptor<AgentJobQuery> queryCaptor = ArgumentCaptor.forClass(AgentJobQuery.class);
    verify(agentJobDao).selectByQuery(eq(dbSession), queryCaptor.capture(), any());
    assertThat(queryCaptor.getValue().getJobIds()).containsExactly("job-1");
  }

  @Test
  public void search_whenMultipleIdsProvided_shouldForwardAllOfThem() throws Exception {
    userSession.logIn().setSystemAdministrator();
    when(agentJobDao.countByQuery(eq(dbSession), any())).thenReturn(0);
    when(agentJobDao.selectByQuery(eq(dbSession), any(), any())).thenReturn(List.of());

    mockMvc.perform(get(AGENTIC_JOBS_ENDPOINT).param("id", "job-1,job-2"))
      .andExpect(status().isOk());

    ArgumentCaptor<AgentJobQuery> queryCaptor = ArgumentCaptor.forClass(AgentJobQuery.class);
    verify(agentJobDao).selectByQuery(eq(dbSession), queryCaptor.capture(), any());
    assertThat(queryCaptor.getValue().getJobIds()).containsExactly("job-1", "job-2");
  }

  @Test
  public void search_whenStatusAndTypeFiltersProvided_shouldMapStatusToDbVocabularyAndForward() throws Exception {
    userSession.logIn().setSystemAdministrator();
    when(agentJobDao.countByQuery(eq(dbSession), any())).thenReturn(0);
    when(agentJobDao.selectByQuery(eq(dbSession), any(), any())).thenReturn(List.of());

    mockMvc.perform(get(AGENTIC_JOBS_ENDPOINT)
      .param("status", "IN_PROGRESS,COMPLETED")
      .param("type", "REMEDIATION")
      .param("pageIndex", "2")
      .param("pageSize", "10"))
      .andExpect(status().isOk());

    ArgumentCaptor<AgentJobQuery> queryCaptor = ArgumentCaptor.forClass(AgentJobQuery.class);
    ArgumentCaptor<Pagination> paginationCaptor = ArgumentCaptor.forClass(Pagination.class);
    verify(agentJobDao).selectByQuery(eq(dbSession), queryCaptor.capture(), paginationCaptor.capture());
    assertThat(queryCaptor.getValue().getStatuses()).containsExactly("RUNNING", "SUCCEEDED");
    assertThat(queryCaptor.getValue().getAgentTypes()).containsExactly("REMEDIATION");
    assertThat(paginationCaptor.getValue().getPage()).isEqualTo(2);
    assertThat(paginationCaptor.getValue().getPageSize()).isEqualTo(10);
  }

  @Test
  public void search_whenAllStatusValuesProvided_shouldMapEachToItsDbVocabularyEquivalent() throws Exception {
    userSession.logIn().setSystemAdministrator();
    when(agentJobDao.countByQuery(eq(dbSession), any())).thenReturn(0);
    when(agentJobDao.selectByQuery(eq(dbSession), any(), any())).thenReturn(List.of());

    mockMvc.perform(get(AGENTIC_JOBS_ENDPOINT).param("status", "PENDING,IN_PROGRESS,COMPLETED,FAILED"))
      .andExpect(status().isOk());

    ArgumentCaptor<AgentJobQuery> queryCaptor = ArgumentCaptor.forClass(AgentJobQuery.class);
    verify(agentJobDao).selectByQuery(eq(dbSession), queryCaptor.capture(), any());
    assertThat(queryCaptor.getValue().getStatuses()).containsExactly("PENDING", "RUNNING", "SUCCEEDED", "FAILED");
  }

  @Test
  public void search_whenSingleStatusProvided_shouldForwardItsDbVocabularyEquivalent() throws Exception {
    userSession.logIn().setSystemAdministrator();
    when(agentJobDao.countByQuery(eq(dbSession), any())).thenReturn(0);
    when(agentJobDao.selectByQuery(eq(dbSession), any(), any())).thenReturn(List.of());

    mockMvc.perform(get(AGENTIC_JOBS_ENDPOINT).param("status", "PENDING"))
      .andExpect(status().isOk());

    ArgumentCaptor<AgentJobQuery> queryCaptor = ArgumentCaptor.forClass(AgentJobQuery.class);
    verify(agentJobDao).selectByQuery(eq(dbSession), queryCaptor.capture(), any());
    assertThat(queryCaptor.getValue().getStatuses()).containsExactly("PENDING");
  }

  @Test
  public void search_whenSingleTypeProvided_shouldForwardIt() throws Exception {
    userSession.logIn().setSystemAdministrator();
    when(agentJobDao.countByQuery(eq(dbSession), any())).thenReturn(0);
    when(agentJobDao.selectByQuery(eq(dbSession), any(), any())).thenReturn(List.of());

    mockMvc.perform(get(AGENTIC_JOBS_ENDPOINT).param("type", "HUNTER"))
      .andExpect(status().isOk());

    ArgumentCaptor<AgentJobQuery> queryCaptor = ArgumentCaptor.forClass(AgentJobQuery.class);
    verify(agentJobDao).selectByQuery(eq(dbSession), queryCaptor.capture(), any());
    assertThat(queryCaptor.getValue().getAgentTypes()).containsExactly("HUNTER");
  }

  @Test
  public void search_whenBothTypesProvided_shouldForwardBoth() throws Exception {
    userSession.logIn().setSystemAdministrator();
    when(agentJobDao.countByQuery(eq(dbSession), any())).thenReturn(0);
    when(agentJobDao.selectByQuery(eq(dbSession), any(), any())).thenReturn(List.of());

    mockMvc.perform(get(AGENTIC_JOBS_ENDPOINT).param("type", "HUNTER,REMEDIATION"))
      .andExpect(status().isOk());

    ArgumentCaptor<AgentJobQuery> queryCaptor = ArgumentCaptor.forClass(AgentJobQuery.class);
    verify(agentJobDao).selectByQuery(eq(dbSession), queryCaptor.capture(), any());
    assertThat(queryCaptor.getValue().getAgentTypes()).containsExactly("HUNTER", "REMEDIATION");
  }

  @Test
  public void search_whenOnlyCreatedAfterProvided_shouldSetOnlyMinCreatedAt() throws Exception {
    userSession.logIn().setSystemAdministrator();
    when(agentJobDao.countByQuery(eq(dbSession), any())).thenReturn(0);
    when(agentJobDao.selectByQuery(eq(dbSession), any(), any())).thenReturn(List.of());

    mockMvc.perform(get(AGENTIC_JOBS_ENDPOINT).param("createdAfter", "2026-01-01"))
      .andExpect(status().isOk());

    ArgumentCaptor<AgentJobQuery> queryCaptor = ArgumentCaptor.forClass(AgentJobQuery.class);
    verify(agentJobDao).selectByQuery(eq(dbSession), queryCaptor.capture(), any());
    Date expected = parseStartingDateOrDateTime("2026-01-01");
    assertThat(queryCaptor.getValue().getMinCreatedAt()).isEqualTo(expected.getTime());
    assertThat(queryCaptor.getValue().getMaxCreatedAt()).isNull();
  }

  @Test
  public void search_whenOnlyCreatedBeforeProvided_shouldSetOnlyMaxCreatedAt() throws Exception {
    userSession.logIn().setSystemAdministrator();
    when(agentJobDao.countByQuery(eq(dbSession), any())).thenReturn(0);
    when(agentJobDao.selectByQuery(eq(dbSession), any(), any())).thenReturn(List.of());

    mockMvc.perform(get(AGENTIC_JOBS_ENDPOINT).param("createdBefore", "2026-01-02"))
      .andExpect(status().isOk());

    ArgumentCaptor<AgentJobQuery> queryCaptor = ArgumentCaptor.forClass(AgentJobQuery.class);
    verify(agentJobDao).selectByQuery(eq(dbSession), queryCaptor.capture(), any());
    Date expected = parseEndingDateOrDateTime("2026-01-02");
    assertThat(queryCaptor.getValue().getMaxCreatedAt()).isEqualTo(expected.getTime());
    assertThat(queryCaptor.getValue().getMinCreatedAt()).isNull();
  }

  @Test
  public void search_whenDateFiltersProvided_shouldParseIntoEpochMillisRange() throws Exception {
    userSession.logIn().setSystemAdministrator();
    when(agentJobDao.countByQuery(eq(dbSession), any())).thenReturn(0);
    when(agentJobDao.selectByQuery(eq(dbSession), any(), any())).thenReturn(List.of());

    mockMvc.perform(get(AGENTIC_JOBS_ENDPOINT)
      .param("createdAfter", "2026-01-01")
      .param("createdBefore", "2026-01-02"))
      .andExpect(status().isOk());

    ArgumentCaptor<AgentJobQuery> queryCaptor = ArgumentCaptor.forClass(AgentJobQuery.class);
    verify(agentJobDao).selectByQuery(eq(dbSession), queryCaptor.capture(), any());
    assertThat(queryCaptor.getValue().getMinCreatedAt()).isNotNull();
    assertThat(queryCaptor.getValue().getMaxCreatedAt()).isNotNull();
    assertThat(queryCaptor.getValue().getMinCreatedAt()).isLessThan(queryCaptor.getValue().getMaxCreatedAt());
  }

  @Test
  public void search_whenAllFiltersAndPaginationProvidedTogether_shouldForwardAllOfThemAtOnce() throws Exception {
    userSession.logIn().setSystemAdministrator();
    when(agentJobDao.countByQuery(eq(dbSession), any())).thenReturn(0);
    when(agentJobDao.selectByQuery(eq(dbSession), any(), any())).thenReturn(List.of());

    mockMvc.perform(get(AGENTIC_JOBS_ENDPOINT)
      .param("id", "job-1,job-2")
      .param("status", "FAILED")
      .param("type", "REMEDIATION")
      .param("createdAfter", "2026-01-01")
      .param("createdBefore", "2026-01-31")
      .param("pageIndex", "3")
      .param("pageSize", "5"))
      .andExpect(status().isOk());

    ArgumentCaptor<AgentJobQuery> queryCaptor = ArgumentCaptor.forClass(AgentJobQuery.class);
    ArgumentCaptor<Pagination> paginationCaptor = ArgumentCaptor.forClass(Pagination.class);
    verify(agentJobDao).selectByQuery(eq(dbSession), queryCaptor.capture(), paginationCaptor.capture());
    assertThat(queryCaptor.getValue().getJobIds()).containsExactly("job-1", "job-2");
    assertThat(queryCaptor.getValue().getStatuses()).containsExactly("FAILED");
    assertThat(queryCaptor.getValue().getAgentTypes()).containsExactly("REMEDIATION");
    assertThat(queryCaptor.getValue().getMinCreatedAt()).isEqualTo(parseStartingDateOrDateTime("2026-01-01").getTime());
    assertThat(queryCaptor.getValue().getMaxCreatedAt()).isEqualTo(parseEndingDateOrDateTime("2026-01-31").getTime());
    assertThat(paginationCaptor.getValue().getPage()).isEqualTo(3);
    assertThat(paginationCaptor.getValue().getPageSize()).isEqualTo(5);
  }

  @Test
  public void search_whenInvalidStatus_shouldReturnBadRequest() throws Exception {
    userSession.logIn().setSystemAdministrator();
    mockMvc.perform(get(AGENTIC_JOBS_ENDPOINT).param("status", "NOT_A_STATUS"))
      .andExpectAll(
        status().isBadRequest(),
        content().json("{\"message\":\"Invalid status value: NOT_A_STATUS\"}"));
  }

  @Test
  public void search_whenInvalidType_shouldReturnBadRequest() throws Exception {
    userSession.logIn().setSystemAdministrator();
    mockMvc.perform(get(AGENTIC_JOBS_ENDPOINT).param("type", "NOT_A_TYPE"))
      .andExpectAll(
        status().isBadRequest(),
        content().json("{\"message\":\"Invalid type value: NOT_A_TYPE\"}"));
  }

  @Test
  public void search_whenOneStatusInListIsInvalid_shouldReturnBadRequest() throws Exception {
    userSession.logIn().setSystemAdministrator();
    mockMvc.perform(get(AGENTIC_JOBS_ENDPOINT).param("status", "PENDING,NOT_A_STATUS"))
      .andExpectAll(
        status().isBadRequest(),
        content().json("{\"message\":\"Invalid status value: NOT_A_STATUS\"}"));
  }

  @Test
  public void search_whenOneTypeInListIsInvalid_shouldReturnBadRequest() throws Exception {
    userSession.logIn().setSystemAdministrator();
    mockMvc.perform(get(AGENTIC_JOBS_ENDPOINT).param("type", "HUNTER,NOT_A_TYPE"))
      .andExpectAll(
        status().isBadRequest(),
        content().json("{\"message\":\"Invalid type value: NOT_A_TYPE\"}"));
  }

  @Test
  public void search_whenJobsReturnedByDao_shouldMapDbStatusToApiStatus() throws Exception {
    userSession.logIn().setSystemAdministrator();
    AgentJobDto runningJob = new AgentJobDto()
      .setId("job-1")
      .setProjectId("project-1")
      .setRepositoryUrl("https://example.com/repo.git")
      .setAgentType("HUNTER")
      .setAnalysisType("FULL")
      .setStatus("RUNNING")
      .setCreatedAt(1_000L)
      .setUpdatedAt(1_000L);
    AgentJobDto succeededJob = new AgentJobDto()
      .setId("job-2")
      .setProjectId("project-2")
      .setProjectKey("project-2-key")
      .setProjectName("Project Two")
      .setRepositoryUrl("https://example.com/repo2.git")
      .setAgentType("HUNTER")
      .setAnalysisType("FULL")
      .setStatus("SUCCEEDED")
      .setFindingsCount(3)
      .setCreatedAt(2_000L)
      .setUpdatedAt(2_000L);
    AgentJobDto failedJob = new AgentJobDto()
      .setId("job-3")
      .setProjectId("project-3")
      .setRepositoryUrl("https://example.com/repo3.git")
      .setAgentType("REMEDIATION")
      .setAnalysisType("INCREMENTAL")
      .setStatus("FAILED")
      .setSubStatus("some-failure-reason")
      .setCreatedAt(3_000L)
      .setUpdatedAt(3_000L);
    when(agentJobDao.countByQuery(eq(dbSession), any())).thenReturn(3);
    when(agentJobDao.selectByQuery(eq(dbSession), any(), any())).thenReturn(List.of(runningJob, succeededJob, failedJob));

    MvcResult mvcResult = mockMvc.perform(get(AGENTIC_JOBS_ENDPOINT))
      .andExpect(status().isOk())
      .andReturn();

    AgenticJobsSearchRestResponse response = OBJECT_MAPPER.readValue(mvcResult.getResponse().getContentAsString(), AgenticJobsSearchRestResponse.class);
    assertThat(response.page().total()).isEqualTo(3);
    assertThat(response.jobs()).hasSize(3);
    assertThat(response.jobs().get(0).id()).isEqualTo("job-1");
    assertThat(response.jobs().get(0).status()).isEqualTo("IN_PROGRESS");
    assertThat(response.jobs().get(0).failureReason()).isNull();
    assertThat(response.jobs().get(0).projectKey()).isNull();
    assertThat(response.jobs().get(0).projectName()).isNull();
    assertThat(response.jobs().get(1).id()).isEqualTo("job-2");
    assertThat(response.jobs().get(1).status()).isEqualTo("COMPLETED");
    assertThat(response.jobs().get(1).failureReason()).isNull();
    assertThat(response.jobs().get(1).projectKey()).isEqualTo("project-2-key");
    assertThat(response.jobs().get(1).projectName()).isEqualTo("Project Two");
    assertThat(response.jobs().get(2).id()).isEqualTo("job-3");
    assertThat(response.jobs().get(2).status()).isEqualTo("FAILED");
    assertThat(response.jobs().get(2).failureReason()).isEqualTo("some-failure-reason");
  }
}
