/*
 * SonarQube
 * Copyright (C) 2009-2017 SonarSource SA
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
package org.sonar.db.purge;

import com.google.common.collect.ImmutableList;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import org.apache.commons.lang.math.RandomUtils;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.sonar.api.resources.Scopes;
import org.sonar.api.utils.System2;
import org.sonar.core.util.Uuids;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.db.DbTester;
import org.sonar.db.ce.CeActivityDto;
import org.sonar.db.ce.CeQueueDto;
import org.sonar.db.ce.CeQueueDto.Status;
import org.sonar.db.component.ComponentDto;
import org.sonar.db.component.ComponentTesting;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.sonar.db.ce.CeTaskTypes.REPORT;
import static org.sonar.db.webhook.WebhookDbTesting.newWebhookDeliveryDto;
import static org.sonar.db.webhook.WebhookDbTesting.selectAllDeliveryUuids;

public class PurgeDaoTest {

  private static final String THE_PROJECT_UUID = "P1";
  private static final long THE_PROJECT_ID = 1L;

  private System2 system2 = mock(System2.class);

  @Rule
  public DbTester dbTester = DbTester.create(system2);

  private DbClient dbClient = dbTester.getDbClient();
  private DbSession dbSession = dbTester.getSession();
  private PurgeDao underTest = dbTester.getDbClient().purgeDao();

  @Test
  public void shouldDeleteAbortedBuilds() {
    dbTester.prepareDbUnit(getClass(), "shouldDeleteAbortedBuilds.xml");

    underTest.purge(dbSession, newConfigurationWith30Days(), PurgeListener.EMPTY, new PurgeProfiler());
    dbSession.commit();

    dbTester.assertDbUnit(getClass(), "shouldDeleteAbortedBuilds-result.xml", "snapshots");
  }

  @Test
  public void should_purge_project() {
    dbTester.prepareDbUnit(getClass(), "shouldPurgeProject.xml");
    underTest.purge(dbSession, newConfigurationWith30Days(), PurgeListener.EMPTY, new PurgeProfiler());
    dbSession.commit();
    dbTester.assertDbUnit(getClass(), "shouldPurgeProject-result.xml", "projects", "snapshots");
  }

  @Test
  public void shouldDeleteHistoricalDataOfDirectoriesAndFiles() {
    dbTester.prepareDbUnit(getClass(), "shouldDeleteHistoricalDataOfDirectoriesAndFiles.xml");
    PurgeConfiguration conf = new PurgeConfiguration(
      new IdUuidPair(THE_PROJECT_ID, "ABCD"), new String[] {Scopes.DIRECTORY, Scopes.FILE}, 30, System2.INSTANCE, Collections.emptyList());

    underTest.purge(dbSession, conf, PurgeListener.EMPTY, new PurgeProfiler());
    dbSession.commit();

    dbTester.assertDbUnit(getClass(), "shouldDeleteHistoricalDataOfDirectoriesAndFiles-result.xml", "projects", "snapshots");
  }

  @Test
  public void close_issues_clean_index_and_file_sources_of_disabled_components_specified_by_uuid_in_configuration() {
    dbTester.prepareDbUnit(getClass(), "close_issues_clean_index_and_files_sources_of_specified_components.xml");
    when(system2.now()).thenReturn(1450000000000L);
    underTest.purge(dbSession, newConfigurationWith30Days(system2, "P1", "EFGH", "GHIJ"), PurgeListener.EMPTY, new PurgeProfiler());
    dbSession.commit();
    dbTester.assertDbUnit(getClass(), "close_issues_clean_index_and_files_sources_of_specified_components-result.xml",
      new String[] {"issue_close_date", "issue_update_date"},
      "projects", "snapshots", "issues");
  }

  @Test
  public void shouldDeleteAnalyses() {
    dbTester.prepareDbUnit(getClass(), "shouldDeleteAnalyses.xml");

    underTest.deleteAnalyses(dbSession, new PurgeProfiler(), ImmutableList.of(new IdUuidPair(3, "u3")));
    dbTester.assertDbUnit(getClass(), "shouldDeleteAnalyses-result.xml", "snapshots");
  }

  @Test
  public void shouldSelectPurgeableAnalysis() {
    dbTester.prepareDbUnit(getClass(), "shouldSelectPurgeableAnalysis.xml");
    List<PurgeableAnalysisDto> analyses = underTest.selectPurgeableAnalyses(THE_PROJECT_UUID, dbSession);

    assertThat(analyses).hasSize(3);
    assertThat(getById(analyses, "u1").isLast()).isTrue();
    assertThat(getById(analyses, "u1").hasEvents()).isFalse();
    assertThat(getById(analyses, "u4").isLast()).isFalse();
    assertThat(getById(analyses, "u4").hasEvents()).isFalse();
    assertThat(getById(analyses, "u5").isLast()).isFalse();
    assertThat(getById(analyses, "u5").hasEvents()).isTrue();
  }

  @Test
  public void delete_project_and_associated_data() {
    dbTester.prepareDbUnit(getClass(), "shouldDeleteProject.xml");

    underTest.deleteProject(dbSession, "A");
    dbSession.commit();

    assertThat(dbTester.countRowsOfTable("projects")).isZero();
    assertThat(dbTester.countRowsOfTable("snapshots")).isZero();
    assertThat(dbTester.countRowsOfTable("issues")).isZero();
    assertThat(dbTester.countRowsOfTable("issue_changes")).isZero();
    assertThat(dbTester.countRowsOfTable("file_sources")).isZero();
  }

  @Test
  public void delete_project_in_ce_activity_when_deleting_project() {
    ComponentDto projectToBeDeleted = ComponentTesting.newPrivateProjectDto(dbTester.getDefaultOrganization());
    ComponentDto anotherLivingProject = ComponentTesting.newPrivateProjectDto(dbTester.getDefaultOrganization());
    dbClient.componentDao().insert(dbSession, projectToBeDeleted, anotherLivingProject);

    // Insert 2 rows in CE_ACTIVITY : one for the project that will be deleted, and on on another project
    insertCeActivity(projectToBeDeleted);
    insertCeActivity(anotherLivingProject);
    dbSession.commit();

    underTest.deleteProject(dbSession, projectToBeDeleted.uuid());
    dbSession.commit();

    assertThat(dbTester.countRowsOfTable("ce_activity")).isEqualTo(1);
  }

  @Test
  public void delete_tasks_in_ce_queue_when_deleting_project() {
    ComponentDto projectToBeDeleted = dbTester.components().insertPrivateProject();
    ComponentDto anotherLivingProject = dbTester.components().insertPrivateProject();

    // Insert 3 rows in CE_QUEUE: two for the project that will be deleted (in order to check that status
    // is not involved in deletion), and one on another project
    dbClient.ceQueueDao().insert(dbSession, createCeQueue(projectToBeDeleted, Status.PENDING));
    dbClient.ceQueueDao().insert(dbSession, createCeQueue(projectToBeDeleted, Status.IN_PROGRESS));
    dbClient.ceQueueDao().insert(dbSession, createCeQueue(anotherLivingProject, Status.PENDING));
    dbSession.commit();

    underTest.deleteProject(dbSession, projectToBeDeleted.uuid());
    dbSession.commit();

    assertThat(dbTester.countRowsOfTable("ce_queue")).isEqualTo(1);
    assertThat(dbTester.countSql("select count(*) from ce_queue where component_uuid='" + projectToBeDeleted.uuid() + "'")).isEqualTo(0);
  }

  @Test
  public void delete_view_and_child() {
    dbTester.prepareDbUnit(getClass(), "view_sub_view_and_tech_project.xml");

    underTest.deleteProject(dbSession, "A");
    dbSession.commit();
    assertThat(dbTester.countSql("select count(1) from projects where uuid='A'")).isZero();
    assertThat(dbTester.countRowsOfTable("projects")).isZero();
  }

  @Test
  public void delete_view_sub_view_and_tech_project() {
    dbTester.prepareDbUnit(getClass(), "view_sub_view_and_tech_project.xml");

    // technical project
    underTest.deleteProject(dbSession, "D");
    dbSession.commit();
    assertThat(dbTester.countSql("select count(1) from projects where uuid='D'")).isZero();

    // sub view
    underTest.deleteProject(dbSession, "B");
    dbSession.commit();
    assertThat(dbTester.countSql("select count(1) from projects where uuid='B'")).isZero();

    // view
    underTest.deleteProject(dbSession, "A");
    dbSession.commit();
    assertThat(dbTester.countSql("select count(1) from projects where uuid='A'")).isZero();
  }

  @Test
  public void should_delete_old_closed_issues() {
    PurgeListener purgeListener = mock(PurgeListener.class);
    dbTester.prepareDbUnit(getClass(), "should_delete_old_closed_issues.xml");

    underTest.purge(dbSession, newConfigurationWith30Days(), purgeListener, new PurgeProfiler());
    dbSession.commit();

    dbTester.assertDbUnit(getClass(), "should_delete_old_closed_issues-result.xml", "issues", "issue_changes");

    Class<ArrayList<String>> listClass = (Class<ArrayList<String>>) (Class) ArrayList.class;
    ArgumentCaptor<ArrayList<String>> issueKeys = ArgumentCaptor.forClass(listClass);
    ArgumentCaptor<String> projectUuid = ArgumentCaptor.forClass(String.class);

    verify(purgeListener).onIssuesRemoval(projectUuid.capture(), issueKeys.capture());
    assertThat(projectUuid.getValue()).isEqualTo(THE_PROJECT_UUID);
    assertThat(issueKeys.getValue()).containsOnly("ISSUE-1", "ISSUE-2");
  }

  @Test
  public void should_delete_all_closed_issues() {
    dbTester.prepareDbUnit(getClass(), "should_delete_all_closed_issues.xml");
    PurgeConfiguration conf = new PurgeConfiguration(new IdUuidPair(THE_PROJECT_ID, "1"), new String[0], 0, System2.INSTANCE, Collections.emptyList());
    underTest.purge(dbSession, conf, PurgeListener.EMPTY, new PurgeProfiler());
    dbSession.commit();
    dbTester.assertDbUnit(getClass(), "should_delete_all_closed_issues-result.xml", "issues", "issue_changes");
  }

  @Test
  public void deleteProject_deletes_webhook_deliveries() {
    dbClient.webhookDeliveryDao().insert(dbSession, newWebhookDeliveryDto().setComponentUuid("P1").setUuid("D1"));
    dbClient.webhookDeliveryDao().insert(dbSession, newWebhookDeliveryDto().setComponentUuid("P2").setUuid("D2"));

    underTest.deleteProject(dbSession, "P1");

    assertThat(selectAllDeliveryUuids(dbTester, dbSession)).containsOnly("D2");
  }

  private CeQueueDto createCeQueue(ComponentDto component, Status status) {
    CeQueueDto queueDto = new CeQueueDto();
    queueDto.setUuid(Uuids.create());
    queueDto.setTaskType(REPORT);
    queueDto.setComponentUuid(component.uuid());
    queueDto.setSubmitterLogin("henri");
    queueDto.setCreatedAt(1_300_000_000_000L);
    queueDto.setStatus(status);
    return queueDto;
  }

  private CeActivityDto insertCeActivity(ComponentDto component) {
    Status unusedStatus = Status.values()[RandomUtils.nextInt(Status.values().length)];
    CeQueueDto queueDto = createCeQueue(component, unusedStatus);

    CeActivityDto dto = new CeActivityDto(queueDto);
    dto.setStatus(CeActivityDto.Status.SUCCESS);
    dto.setStartedAt(1_500_000_000_000L);
    dto.setExecutedAt(1_500_000_000_500L);
    dto.setExecutionTimeMs(500L);
    dbClient.ceActivityDao().insert(dbSession, dto);
    return dto;
  }

  private static PurgeableAnalysisDto getById(List<PurgeableAnalysisDto> snapshots, String uuid) {
    return snapshots.stream()
      .filter(snapshot -> uuid.equals(snapshot.getAnalysisUuid()))
      .findFirst()
      .orElse(null);
  }

  private static PurgeConfiguration newConfigurationWith30Days() {
    return new PurgeConfiguration(new IdUuidPair(THE_PROJECT_ID, THE_PROJECT_UUID), new String[0], 30, System2.INSTANCE, Collections.emptyList());
  }

  private static PurgeConfiguration newConfigurationWith30Days(System2 system2, String... disabledComponentUuids) {
    return new PurgeConfiguration(new IdUuidPair(THE_PROJECT_ID, THE_PROJECT_UUID), new String[0], 30, system2, Arrays.asList(disabledComponentUuids));
  }

}
