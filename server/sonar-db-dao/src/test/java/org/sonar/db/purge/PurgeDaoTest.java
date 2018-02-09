/*
 * SonarQube
 * Copyright (C) 2009-2018 SonarSource SA
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
import java.io.ByteArrayInputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.Random;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;
import org.apache.commons.lang.math.RandomUtils;
import org.apache.commons.lang.time.DateUtils;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.mockito.ArgumentCaptor;
import org.sonar.api.resources.Scopes;
import org.sonar.api.utils.System2;
import org.sonar.core.util.CloseableIterator;
import org.sonar.core.util.UuidFactoryFast;
import org.sonar.core.util.Uuids;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.db.DbTester;
import org.sonar.db.ce.CeActivityDto;
import org.sonar.db.ce.CeQueueDto;
import org.sonar.db.ce.CeQueueDto.Status;
import org.sonar.db.ce.CeTaskCharacteristicDto;
import org.sonar.db.component.BranchType;
import org.sonar.db.component.ComponentDbTester;
import org.sonar.db.component.ComponentDto;
import org.sonar.db.component.ComponentTesting;
import org.sonar.db.measure.MeasureDto;
import org.sonar.db.measure.custom.CustomMeasureDto;
import org.sonar.db.metric.MetricDto;
import org.sonar.db.property.PropertyDto;
import org.sonar.db.rule.RuleDefinitionDto;

import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static org.apache.commons.lang.RandomStringUtils.randomAlphabetic;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;
import static org.sonar.db.ce.CeTaskTypes.REPORT;
import static org.sonar.db.component.ComponentTesting.newDirectory;
import static org.sonar.db.component.ComponentTesting.newFileDto;
import static org.sonar.db.component.ComponentTesting.newModuleDto;
import static org.sonar.db.component.ComponentTesting.newProjectCopy;
import static org.sonar.db.webhook.WebhookDbTesting.newWebhookDeliveryDto;
import static org.sonar.db.webhook.WebhookDbTesting.selectAllDeliveryUuids;

public class PurgeDaoTest {

  private static final String THE_PROJECT_UUID = "P1";
  private static final long THE_PROJECT_ID = 1L;

  private System2 system2 = mock(System2.class);

  @Rule
  public DbTester dbTester = DbTester.create(system2);
  @Rule
  public ExpectedException expectedException = ExpectedException.none();

  private DbClient dbClient = dbTester.getDbClient();
  private DbSession dbSession = dbTester.getSession();
  private PurgeDao underTest = dbTester.getDbClient().purgeDao();

  @Test
  public void purge_failed_ce_tasks() {
    dbTester.prepareDbUnit(getClass(), "shouldDeleteAbortedBuilds.xml");

    underTest.purge(dbSession, newConfigurationWith30Days(), PurgeListener.EMPTY, new PurgeProfiler());
    dbSession.commit();

    dbTester.assertDbUnit(getClass(), "shouldDeleteAbortedBuilds-result.xml", "snapshots");
  }

  @Test
  public void purge_history_of_project() {
    dbTester.prepareDbUnit(getClass(), "shouldPurgeProject.xml");
    underTest.purge(dbSession, newConfigurationWith30Days(), PurgeListener.EMPTY, new PurgeProfiler());
    dbSession.commit();
    dbTester.assertDbUnit(getClass(), "shouldPurgeProject-result.xml", "projects", "snapshots");
  }

  @Test
  public void purge_inactive_short_living_branches() {
    when(system2.now()).thenReturn(new Date().getTime());
    RuleDefinitionDto rule = dbTester.rules().insert();
    ComponentDto project = dbTester.components().insertMainBranch();
    ComponentDto longBranch = dbTester.components().insertProjectBranch(project);
    ComponentDto recentShortBranch = dbTester.components().insertProjectBranch(project, b -> b.setBranchType(BranchType.SHORT));

    // short branch with other components and issues, updated 31 days ago
    when(system2.now()).thenReturn(DateUtils.addDays(new Date(), -31).getTime());
    ComponentDto shortBranch = dbTester.components().insertProjectBranch(project, b -> b.setBranchType(BranchType.SHORT));
    ComponentDto module = dbTester.components().insertComponent(newModuleDto(shortBranch));
    ComponentDto subModule = dbTester.components().insertComponent(newModuleDto(module));
    ComponentDto file = dbTester.components().insertComponent(newFileDto(subModule));
    dbTester.issues().insert(rule, shortBranch, file);
    dbTester.issues().insert(rule, shortBranch, subModule);
    dbTester.issues().insert(rule, shortBranch, module);

    // back to present
    when(system2.now()).thenReturn(new Date().getTime());
    underTest.purge(dbSession, newConfigurationWith30Days(system2, project.uuid()), PurgeListener.EMPTY, new PurgeProfiler());
    dbSession.commit();

    assertThat(getUuidsInTableProjects()).containsOnly(project.uuid(), longBranch.uuid(), recentShortBranch.uuid());
  }

  @Test
  public void shouldDeleteHistoricalDataOfDirectoriesAndFiles() {
    dbTester.prepareDbUnit(getClass(), "shouldDeleteHistoricalDataOfDirectoriesAndFiles.xml");
    PurgeConfiguration conf = new PurgeConfiguration(new IdUuidPair(THE_PROJECT_ID, "PROJECT_UUID"), asList(Scopes.DIRECTORY, Scopes.FILE),
      30, Optional.of(30), System2.INSTANCE, Collections.emptyList());

    underTest.purge(dbSession, conf, PurgeListener.EMPTY, new PurgeProfiler());
    dbSession.commit();

    dbTester.assertDbUnit(getClass(), "shouldDeleteHistoricalDataOfDirectoriesAndFiles-result.xml", "projects", "snapshots", "project_measures");
  }

  @Test
  public void close_issues_clean_index_and_file_sources_of_disabled_components_specified_by_uuid_in_configuration() {
    dbTester.prepareDbUnit(getClass(), "close_issues_clean_index_and_files_sources_of_specified_components.xml");
    when(system2.now()).thenReturn(1450000000000L);
    underTest.purge(dbSession, newConfigurationWith30Days(system2, THE_PROJECT_UUID, "P1", "EFGH", "GHIJ"), PurgeListener.EMPTY, new PurgeProfiler());
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
  public void selectPurgeableAnalyses() {
    dbTester.prepareDbUnit(getClass(), "shouldSelectPurgeableAnalysis.xml");
    List<PurgeableAnalysisDto> analyses = underTest.selectPurgeableAnalyses(THE_PROJECT_UUID, dbSession);

    assertThat(analyses).hasSize(3);
    assertThat(getById(analyses, "u1").isLast()).isTrue();
    assertThat(getById(analyses, "u1").hasEvents()).isFalse();
    assertThat(getById(analyses, "u1").getVersion()).isNull();
    assertThat(getById(analyses, "u4").isLast()).isFalse();
    assertThat(getById(analyses, "u4").hasEvents()).isFalse();
    assertThat(getById(analyses, "u4").getVersion()).isNull();
    assertThat(getById(analyses, "u5").isLast()).isFalse();
    assertThat(getById(analyses, "u5").hasEvents()).isTrue();
    assertThat(getById(analyses, "u5").getVersion()).isEqualTo("V5");
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
  public void delete_branch_and_associated_data() {
    ComponentDto project = dbTester.components().insertMainBranch();
    ComponentDto branch = dbTester.components().insertProjectBranch(project);

    underTest.deleteBranch(dbSession, project.uuid());
    dbSession.commit();
    assertThat(dbTester.countRowsOfTable("project_branches")).isEqualTo(1);
    assertThat(dbTester.countRowsOfTable("projects")).isEqualTo(1);
  }

  @Test
  public void delete_row_in_ce_activity_when_deleting_project() {
    ComponentDto projectToBeDeleted = ComponentTesting.newPrivateProjectDto(dbTester.getDefaultOrganization());
    ComponentDto anotherLivingProject = ComponentTesting.newPrivateProjectDto(dbTester.getDefaultOrganization());
    dbClient.componentDao().insert(dbSession, projectToBeDeleted, anotherLivingProject);

    // Insert 2 rows in CE_ACTIVITY : one for the project that will be deleted, and one on another project
    insertCeActivity(projectToBeDeleted);
    insertCeActivity(anotherLivingProject);
    dbSession.commit();

    underTest.deleteProject(dbSession, projectToBeDeleted.uuid());
    dbSession.commit();

    assertThat(dbTester.countRowsOfTable("ce_activity")).isEqualTo(1);
  }

  @Test
  public void delete_row_in_ce_task_input_referring_to_a_row_in_ce_activity_when_deleting_project() {
    ComponentDto projectToBeDeleted = ComponentTesting.newPrivateProjectDto(dbTester.getDefaultOrganization());
    ComponentDto anotherLivingProject = ComponentTesting.newPrivateProjectDto(dbTester.getDefaultOrganization());
    dbClient.componentDao().insert(dbSession, projectToBeDeleted, anotherLivingProject);

    // Insert 2 rows in CE_ACTIVITY : one for the project that will be deleted, and one on another project
    CeActivityDto toBeDeleted = insertCeActivity(projectToBeDeleted);
    insertCeTaskInput(toBeDeleted.getUuid());
    CeActivityDto toNotDelete = insertCeActivity(anotherLivingProject);
    insertCeTaskInput(toNotDelete.getUuid());
    insertCeTaskInput("non existing task");
    dbSession.commit();

    underTest.deleteProject(dbSession, projectToBeDeleted.uuid());
    dbSession.commit();

    assertThat(dbTester.select("select uuid as \"UUID\" from ce_activity"))
      .extracting(row -> (String) row.get("UUID"))
      .containsOnly(toNotDelete.getUuid());
    assertThat(dbTester.select("select task_uuid as \"UUID\" from ce_task_input"))
      .extracting(row -> (String) row.get("UUID"))
      .containsOnly(toNotDelete.getUuid(), "non existing task");
  }

  @Test
  public void delete_row_in_ce_scanner_context_referring_to_a_row_in_ce_activity_when_deleting_project() {
    ComponentDto projectToBeDeleted = ComponentTesting.newPrivateProjectDto(dbTester.getDefaultOrganization());
    ComponentDto anotherLivingProject = ComponentTesting.newPrivateProjectDto(dbTester.getDefaultOrganization());
    dbClient.componentDao().insert(dbSession, projectToBeDeleted, anotherLivingProject);

    // Insert 2 rows in CE_ACTIVITY : one for the project that will be deleted, and one on another project
    CeActivityDto toBeDeleted = insertCeActivity(projectToBeDeleted);
    insertCeScannerContext(toBeDeleted.getUuid());
    CeActivityDto toNotDelete = insertCeActivity(anotherLivingProject);
    insertCeScannerContext(toNotDelete.getUuid());
    insertCeScannerContext("non existing task");
    dbSession.commit();

    underTest.deleteProject(dbSession, projectToBeDeleted.uuid());
    dbSession.commit();

    assertThat(dbTester.select("select uuid as \"UUID\" from ce_activity"))
      .extracting(row -> (String) row.get("UUID"))
      .containsOnly(toNotDelete.getUuid());
    assertThat(dbTester.select("select task_uuid as \"UUID\" from ce_scanner_context"))
      .extracting(row -> (String) row.get("UUID"))
      .containsOnly(toNotDelete.getUuid(), "non existing task");
  }

  @Test
  public void delete_row_in_ce_task_characteristics_referring_to_a_row_in_ce_activity_when_deleting_project() {
    ComponentDto projectToBeDeleted = ComponentTesting.newPrivateProjectDto(dbTester.getDefaultOrganization());
    ComponentDto anotherLivingProject = ComponentTesting.newPrivateProjectDto(dbTester.getDefaultOrganization());
    dbClient.componentDao().insert(dbSession, projectToBeDeleted, anotherLivingProject);

    // Insert 2 rows in CE_ACTIVITY : one for the project that will be deleted, and one on another project
    CeActivityDto toBeDeleted = insertCeActivity(projectToBeDeleted);
    insertCeTaskCharacteristics(toBeDeleted.getUuid(), 3);
    CeActivityDto toNotDelete = insertCeActivity(anotherLivingProject);
    insertCeTaskCharacteristics(toNotDelete.getUuid(), 2);
    insertCeTaskCharacteristics("non existing task", 5);
    dbSession.commit();

    underTest.deleteProject(dbSession, projectToBeDeleted.uuid());
    dbSession.commit();

    assertThat(dbTester.select("select uuid as \"UUID\" from ce_activity"))
      .extracting(row -> (String) row.get("UUID"))
      .containsOnly(toNotDelete.getUuid());
    assertThat(dbTester.select("select task_uuid as \"UUID\" from ce_task_characteristics"))
      .extracting(row -> (String) row.get("UUID"))
      .containsOnly(toNotDelete.getUuid(), "non existing task");
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
  public void delete_row_in_ce_task_input_referring_to_a_row_in_ce_queue_when_deleting_project() {
    ComponentDto projectToBeDeleted = ComponentTesting.newPrivateProjectDto(dbTester.getDefaultOrganization());
    ComponentDto anotherLivingProject = ComponentTesting.newPrivateProjectDto(dbTester.getDefaultOrganization());
    dbClient.componentDao().insert(dbSession, projectToBeDeleted, anotherLivingProject);

    // Insert 2 rows in CE_ACTIVITY : one for the project that will be deleted, and one on another project
    CeQueueDto toBeDeleted = insertCeQueue(projectToBeDeleted);
    insertCeTaskInput(toBeDeleted.getUuid());
    CeQueueDto toNotDelete = insertCeQueue(anotherLivingProject);
    insertCeTaskInput(toNotDelete.getUuid());
    insertCeTaskInput("non existing task");
    dbSession.commit();

    underTest.deleteProject(dbSession, projectToBeDeleted.uuid());
    dbSession.commit();

    assertThat(dbTester.select("select uuid as \"UUID\" from ce_queue"))
      .extracting(row -> (String) row.get("UUID"))
      .containsOnly(toNotDelete.getUuid());
    assertThat(dbTester.select("select task_uuid as \"UUID\" from ce_task_input"))
      .extracting(row -> (String) row.get("UUID"))
      .containsOnly(toNotDelete.getUuid(), "non existing task");
  }

  @Test
  public void delete_row_in_ce_scanner_context_referring_to_a_row_in_ce_queue_when_deleting_project() {
    ComponentDto projectToBeDeleted = ComponentTesting.newPrivateProjectDto(dbTester.getDefaultOrganization());
    ComponentDto anotherLivingProject = ComponentTesting.newPrivateProjectDto(dbTester.getDefaultOrganization());
    dbClient.componentDao().insert(dbSession, projectToBeDeleted, anotherLivingProject);

    // Insert 2 rows in CE_ACTIVITY : one for the project that will be deleted, and one on another project
    CeQueueDto toBeDeleted = insertCeQueue(projectToBeDeleted);
    insertCeScannerContext(toBeDeleted.getUuid());
    CeQueueDto toNotDelete = insertCeQueue(anotherLivingProject);
    insertCeScannerContext(toNotDelete.getUuid());
    insertCeScannerContext("non existing task");
    dbSession.commit();

    underTest.deleteProject(dbSession, projectToBeDeleted.uuid());
    dbSession.commit();

    assertThat(dbTester.select("select uuid as \"UUID\" from ce_queue"))
      .extracting(row -> (String) row.get("UUID"))
      .containsOnly(toNotDelete.getUuid());
    assertThat(dbTester.select("select task_uuid as \"UUID\" from ce_scanner_context"))
      .extracting(row -> (String) row.get("UUID"))
      .containsOnly(toNotDelete.getUuid(), "non existing task");
  }

  @Test
  public void delete_row_in_ce_task_characteristics_referring_to_a_row_in_ce_queue_when_deleting_project() {
    ComponentDto projectToBeDeleted = ComponentTesting.newPrivateProjectDto(dbTester.getDefaultOrganization());
    ComponentDto anotherLivingProject = ComponentTesting.newPrivateProjectDto(dbTester.getDefaultOrganization());
    dbClient.componentDao().insert(dbSession, projectToBeDeleted, anotherLivingProject);

    // Insert 2 rows in CE_ACTIVITY : one for the project that will be deleted, and one on another project
    CeQueueDto toBeDeleted = insertCeQueue(projectToBeDeleted);
    insertCeTaskCharacteristics(toBeDeleted.getUuid(), 3);
    CeQueueDto toNotDelete = insertCeQueue(anotherLivingProject);
    insertCeTaskCharacteristics(toNotDelete.getUuid(), 2);
    insertCeTaskCharacteristics("non existing task", 5);
    dbSession.commit();

    underTest.deleteProject(dbSession, projectToBeDeleted.uuid());
    dbSession.commit();

    assertThat(dbTester.select("select uuid as \"UUID\" from ce_queue"))
      .extracting(row -> (String) row.get("UUID"))
      .containsOnly(toNotDelete.getUuid());
    assertThat(dbTester.select("select task_uuid as \"UUID\" from ce_task_characteristics"))
      .extracting(row -> (String) row.get("UUID"))
      .containsOnly(toNotDelete.getUuid(), "non existing task");
  }

  private ComponentDto insertProjectWithBranchAndRelatedData() {
    RuleDefinitionDto rule = dbTester.rules().insert();
    ComponentDto project = dbTester.components().insertMainBranch();
    ComponentDto branch = dbTester.components().insertProjectBranch(project);
    ComponentDto module = dbTester.components().insertComponent(newModuleDto(branch));
    ComponentDto subModule = dbTester.components().insertComponent(newModuleDto(module));
    ComponentDto file = dbTester.components().insertComponent(newFileDto(subModule));
    dbTester.issues().insert(rule, branch, file);
    dbTester.issues().insert(rule, branch, subModule);
    dbTester.issues().insert(rule, branch, module);
    return project;
  }

  @Test
  public void delete_branch_content_when_deleting_project() {
    ComponentDto anotherLivingProject = insertProjectWithBranchAndRelatedData();
    int projectEntryCount = dbTester.countRowsOfTable("projects");
    int issueCount = dbTester.countRowsOfTable("issues");
    int branchCount = dbTester.countRowsOfTable("project_branches");

    ComponentDto projectToDelete = insertProjectWithBranchAndRelatedData();
    assertThat(dbTester.countRowsOfTable("projects")).isGreaterThan(projectEntryCount);
    assertThat(dbTester.countRowsOfTable("issues")).isGreaterThan(issueCount);
    assertThat(dbTester.countRowsOfTable("project_branches")).isGreaterThan(branchCount);

    underTest.deleteProject(dbSession, projectToDelete.uuid());
    dbSession.commit();

    assertThat(dbTester.countRowsOfTable("projects")).isEqualTo(projectEntryCount);
    assertThat(dbTester.countRowsOfTable("issues")).isEqualTo(issueCount);
    assertThat(dbTester.countRowsOfTable("project_branches")).isEqualTo(branchCount);
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
  public void deleteProject_fails_with_IAE_if_specified_component_is_module() {
    ComponentDto privateProject = dbTester.components().insertPrivateProject();
    ComponentDto module = dbTester.components().insertComponent(ComponentTesting.newModuleDto(privateProject));

    expectedException.expect(IllegalArgumentException.class);
    expectedException.expectMessage("Couldn't find root component with uuid " + module.uuid());

    underTest.deleteProject(dbSession, module.uuid());
  }

  @Test
  public void deleteProject_fails_with_IAE_if_specified_component_is_directory() {
    ComponentDto privateProject = dbTester.components().insertPrivateProject();
    ComponentDto directory = dbTester.components().insertComponent(newDirectory(privateProject, "A/B"));

    expectedException.expect(IllegalArgumentException.class);
    expectedException.expectMessage("Couldn't find root component with uuid " + directory.uuid());

    underTest.deleteProject(dbSession, directory.uuid());
  }

  @Test
  public void deleteProject_fails_with_IAE_if_specified_component_is_file() {
    ComponentDto privateProject = dbTester.components().insertPrivateProject();
    ComponentDto file = dbTester.components().insertComponent(newFileDto(privateProject));

    expectedException.expect(IllegalArgumentException.class);
    expectedException.expectMessage("Couldn't find root component with uuid " + file.uuid());

    underTest.deleteProject(dbSession, file.uuid());
  }

  @Test
  public void deleteProject_fails_with_IAE_if_specified_component_is_subview() {
    ComponentDto view = dbTester.components().insertView();
    ComponentDto subview = dbTester.components().insertComponent(ComponentTesting.newSubView(view));

    expectedException.expect(IllegalArgumentException.class);
    expectedException.expectMessage("Couldn't find root component with uuid " + subview.uuid());

    underTest.deleteProject(dbSession, subview.uuid());
  }

  @Test
  public void delete_view_sub_view_and_tech_project() {
    dbTester.prepareDbUnit(getClass(), "view_sub_view_and_tech_project.xml");

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
    PurgeConfiguration conf = new PurgeConfiguration(new IdUuidPair(THE_PROJECT_ID, "1"), emptyList(),
      0, Optional.empty(), System2.INSTANCE, Collections.emptyList());
    underTest.purge(dbSession, conf, PurgeListener.EMPTY, new PurgeProfiler());
    dbSession.commit();
    dbTester.assertDbUnit(getClass(), "should_delete_all_closed_issues-result.xml", "issues", "issue_changes");
  }

  @Test
  public void deleteProject_deletes_webhook_deliveries() {
    ComponentDto project = dbTester.components().insertPublicProject();
    dbClient.webhookDeliveryDao().insert(dbSession, newWebhookDeliveryDto().setComponentUuid(project.uuid()).setUuid("D1"));
    dbClient.webhookDeliveryDao().insert(dbSession, newWebhookDeliveryDto().setComponentUuid("P2").setUuid("D2"));

    underTest.deleteProject(dbSession, project.uuid());

    assertThat(selectAllDeliveryUuids(dbTester, dbSession)).containsOnly("D2");
  }

  @Test
  public void deleteNonRootComponents_has_no_effect_when_parameter_is_empty() {
    DbSession dbSession = mock(DbSession.class);

    underTest.deleteNonRootComponentsInView(dbSession, Collections.emptyList());

    verifyZeroInteractions(dbSession);
  }

  @Test
  public void deleteNonRootComponents_has_no_effect_when_parameter_contains_only_projects_and_or_views() {
    ComponentDbTester componentDbTester = dbTester.components();

    verifyNoEffect(componentDbTester.insertPrivateProject());
    verifyNoEffect(componentDbTester.insertPublicProject());
    verifyNoEffect(componentDbTester.insertView());
    verifyNoEffect(componentDbTester.insertView(), componentDbTester.insertPrivateProject(), componentDbTester.insertPublicProject());
  }

  @Test
  public void delete_live_measures_when_deleting_project() {
    MetricDto metric = dbTester.measures().insertMetric();

    ComponentDto project1 = dbTester.components().insertPublicProject();
    ComponentDto module1 = dbTester.components().insertComponent(ComponentTesting.newModuleDto(project1));
    dbTester.measures().insertLiveMeasure(project1, metric);
    dbTester.measures().insertLiveMeasure(module1, metric);

    ComponentDto project2 = dbTester.components().insertPublicProject();
    ComponentDto module2 = dbTester.components().insertComponent(ComponentTesting.newModuleDto(project2));
    dbTester.measures().insertLiveMeasure(project2, metric);
    dbTester.measures().insertLiveMeasure(module2, metric);

    underTest.deleteProject(dbSession, project1.uuid());

    assertThat(dbClient.liveMeasureDao().selectByComponentUuidsAndMetricIds(dbSession, asList(project1.uuid(), module1.uuid()), asList(metric.getId()))).isEmpty();
    assertThat(dbClient.liveMeasureDao().selectByComponentUuidsAndMetricIds(dbSession, asList(project2.uuid(), module2.uuid()), asList(metric.getId()))).hasSize(2);
  }

  private void verifyNoEffect(ComponentDto firstRoot, ComponentDto... otherRoots) {
    DbSession dbSession = mock(DbSession.class);

    List<ComponentDto> componentDtos = Stream.concat(Stream.of(firstRoot), Arrays.stream(otherRoots)).collect(Collectors.toList());
    Collections.shuffle(componentDtos); // order of collection must not matter
    underTest.deleteNonRootComponentsInView(dbSession, componentDtos);

    verifyZeroInteractions(dbSession);
  }

  @Test
  public void deleteNonRootComponents_deletes_only_non_root_components_of_a_project_from_table_PROJECTS() {
    ComponentDto project = new Random().nextBoolean() ? dbTester.components().insertPublicProject() : dbTester.components().insertPrivateProject();
    ComponentDto module1 = dbTester.components().insertComponent(ComponentTesting.newModuleDto(project));
    ComponentDto module2 = dbTester.components().insertComponent(ComponentTesting.newModuleDto(module1));
    ComponentDto dir1 = dbTester.components().insertComponent(newDirectory(module1, "A/B"));

    List<ComponentDto> components = asList(
      project,
      module1,
      module2,
      dir1,
      dbTester.components().insertComponent(newDirectory(module2, "A/C")),
      dbTester.components().insertComponent(newFileDto(dir1)),
      dbTester.components().insertComponent(newFileDto(module2)),
      dbTester.components().insertComponent(newFileDto(project)));
    Collections.shuffle(components);

    underTest.deleteNonRootComponentsInView(dbSession, components);

    assertThat(getUuidsInTableProjects())
      .containsOnly(project.uuid());
  }

  @Test
  public void deleteNonRootComponents_deletes_only_non_root_components_of_a_view_from_table_PROJECTS() {
    ComponentDto[] projects = {
      dbTester.components().insertPrivateProject(),
      dbTester.components().insertPrivateProject(),
      dbTester.components().insertPrivateProject()
    };

    ComponentDto view = dbTester.components().insertView();
    ComponentDto subview1 = dbTester.components().insertComponent(ComponentTesting.newSubView(view));
    ComponentDto subview2 = dbTester.components().insertComponent(ComponentTesting.newSubView(subview1));
    List<ComponentDto> components = asList(
      view,
      subview1,
      subview2,
      dbTester.components().insertComponent(newProjectCopy("a", projects[0], view)),
      dbTester.components().insertComponent(newProjectCopy("b", projects[1], subview1)),
      dbTester.components().insertComponent(newProjectCopy("c", projects[2], subview2)));
    Collections.shuffle(components);

    underTest.deleteNonRootComponentsInView(dbSession, components);

    assertThat(getUuidsInTableProjects())
      .containsOnly(view.uuid(), projects[0].uuid(), projects[1].uuid(), projects[2].uuid());
  }

  private Stream<String> getUuidsInTableProjects() {
    return dbTester.select("select uuid as \"UUID\" from projects").stream().map(row -> (String) row.get("UUID"));
  }

  @Test
  public void deleteNonRootComponents_deletes_only_specified_non_root_components_of_a_project_from_table_PROJECTS() {
    ComponentDto project = new Random().nextBoolean() ? dbTester.components().insertPublicProject() : dbTester.components().insertPrivateProject();
    ComponentDto module1 = dbTester.components().insertComponent(ComponentTesting.newModuleDto(project));
    ComponentDto module2 = dbTester.components().insertComponent(ComponentTesting.newModuleDto(module1));
    ComponentDto dir1 = dbTester.components().insertComponent(newDirectory(module1, "A/B"));
    ComponentDto dir2 = dbTester.components().insertComponent(newDirectory(module2, "A/C"));
    ComponentDto file1 = dbTester.components().insertComponent(newFileDto(dir1));
    ComponentDto file2 = dbTester.components().insertComponent(newFileDto(module2));
    ComponentDto file3 = dbTester.components().insertComponent(newFileDto(project));

    underTest.deleteNonRootComponentsInView(dbSession, singletonList(file3));
    assertThat(getUuidsInTableProjects())
      .containsOnly(project.uuid(), module1.uuid(), module2.uuid(), dir1.uuid(), dir2.uuid(), file1.uuid(), file2.uuid());

    underTest.deleteNonRootComponentsInView(dbSession, asList(module1, dir2, file1));
    assertThat(getUuidsInTableProjects())
      .containsOnly(project.uuid(), module2.uuid(), dir1.uuid(), file2.uuid());
  }

  @Test
  public void deleteNonRootComponents_deletes_only_specified_non_root_components_of_a_view_from_table_PROJECTS() {
    ComponentDto[] projects = {
      dbTester.components().insertPrivateProject(),
      dbTester.components().insertPrivateProject(),
      dbTester.components().insertPrivateProject()
    };

    ComponentDto view = dbTester.components().insertView();
    ComponentDto subview1 = dbTester.components().insertComponent(ComponentTesting.newSubView(view));
    ComponentDto subview2 = dbTester.components().insertComponent(ComponentTesting.newSubView(subview1));
    ComponentDto pc1 = dbTester.components().insertComponent(newProjectCopy("a", projects[0], view));
    ComponentDto pc2 = dbTester.components().insertComponent(newProjectCopy("b", projects[1], subview1));
    ComponentDto pc3 = dbTester.components().insertComponent(newProjectCopy("c", projects[2], subview2));

    underTest.deleteNonRootComponentsInView(dbSession, singletonList(pc3));
    assertThat(getUuidsInTableProjects())
      .containsOnly(view.uuid(), projects[0].uuid(), projects[1].uuid(), projects[2].uuid(),
        subview1.uuid(), subview2.uuid(), pc1.uuid(), pc2.uuid());

    underTest.deleteNonRootComponentsInView(dbSession, asList(subview1, pc2));
    assertThat(getUuidsInTableProjects())
      .containsOnly(view.uuid(), projects[0].uuid(), projects[1].uuid(), projects[2].uuid(), subview2.uuid(), pc1.uuid());
  }

  @Test
  public void deleteNonRootComponents_deletes_measures_of_any_non_root_component_of_a_view() {
    ComponentDto view = dbTester.components().insertView();
    ComponentDto subview = dbTester.components().insertComponent(ComponentTesting.newSubView(view));
    ComponentDto pc = dbTester.components().insertComponent(newProjectCopy("a", dbTester.components().insertPrivateProject(), view));
    insertMeasureFor(view, subview, pc);
    assertThat(getComponentUuidsOfMeasures()).containsOnly(view.uuid(), subview.uuid(), pc.uuid());

    underTest.deleteNonRootComponentsInView(dbSession, singletonList(pc));
    assertThat(getComponentUuidsOfMeasures())
      .containsOnly(view.uuid(), subview.uuid());

    underTest.deleteNonRootComponentsInView(dbSession, singletonList(subview));
    assertThat(getComponentUuidsOfMeasures())
      .containsOnly(view.uuid());
  }

  @Test
  public void deleteNonRootComponents_deletes_properties_of_subviews_of_a_view() {
    ComponentDto view = dbTester.components().insertView();
    ComponentDto subview1 = dbTester.components().insertComponent(ComponentTesting.newSubView(view));
    ComponentDto subview2 = dbTester.components().insertComponent(ComponentTesting.newSubView(subview1));
    ComponentDto subview3 = dbTester.components().insertComponent(ComponentTesting.newSubView(view));
    ComponentDto pc = dbTester.components().insertComponent(newProjectCopy("a", dbTester.components().insertPrivateProject(), view));
    insertPropertyFor(view, subview1, subview2, subview3, pc);
    assertThat(getResourceIdOfProperties()).containsOnly(view.getId(), subview1.getId(), subview2.getId(), subview3.getId(), pc.getId());

    underTest.deleteNonRootComponentsInView(dbSession, singletonList(subview1));
    assertThat(getResourceIdOfProperties())
      .containsOnly(view.getId(), subview2.getId(), subview3.getId(), pc.getId());

    underTest.deleteNonRootComponentsInView(dbSession, asList(subview2, subview3, pc));
    assertThat(getResourceIdOfProperties())
      .containsOnly(view.getId(), pc.getId());
  }

  @Test
  public void deleteNonRootComponentsInView_deletes_manual_measures_of_subviews_of_a_view() {
    ComponentDto view = dbTester.components().insertView();
    ComponentDto subview1 = dbTester.components().insertComponent(ComponentTesting.newSubView(view));
    ComponentDto subview2 = dbTester.components().insertComponent(ComponentTesting.newSubView(subview1));
    ComponentDto subview3 = dbTester.components().insertComponent(ComponentTesting.newSubView(view));
    ComponentDto pc = dbTester.components().insertComponent(newProjectCopy("a", dbTester.components().insertPrivateProject(), view));
    insertManualMeasureFor(view, subview1, subview2, subview3, pc);
    assertThat(getComponentUuidsOfManualMeasures()).containsOnly(view.uuid(), subview1.uuid(), subview2.uuid(), subview3.uuid(), pc.uuid());

    underTest.deleteNonRootComponentsInView(dbSession, singletonList(subview1));
    assertThat(getComponentUuidsOfManualMeasures())
      .containsOnly(view.uuid(), subview2.uuid(), subview3.uuid(), pc.uuid());

    underTest.deleteNonRootComponentsInView(dbSession, asList(subview2, subview3, pc));
    assertThat(getComponentUuidsOfManualMeasures())
      .containsOnly(view.uuid(), pc.uuid());
  }

  private void insertManualMeasureFor(ComponentDto... componentDtos) {
    Arrays.stream(componentDtos).forEach(componentDto -> dbClient.customMeasureDao().insert(dbSession, new CustomMeasureDto()
      .setComponentUuid(componentDto.uuid())
      .setMetricId(new Random().nextInt())));
    dbSession.commit();
  }

  private Stream<String> getComponentUuidsOfManualMeasures() {
    return dbTester.select("select component_uuid as \"COMPONENT_UUID\" from manual_measures").stream()
      .map(row -> (String) row.get("COMPONENT_UUID"));
  }

  private Stream<Long> getResourceIdOfProperties() {
    return dbTester.select("select resource_id as \"ID\" from properties").stream()
      .map(row -> (Long) row.get("ID"));
  }

  private void insertPropertyFor(ComponentDto... components) {
    Stream.of(components).forEach(componentDto -> dbTester.properties().insertProperty(new PropertyDto()
      .setKey(randomAlphabetic(3))
      .setValue(randomAlphabetic(3))
      .setResourceId(componentDto.getId())));
  }

  private Stream<String> getComponentUuidsOfMeasures() {
    return dbTester.select("select component_uuid as \"COMPONENT_UUID\" from project_measures").stream()
      .map(row -> (String) row.get("COMPONENT_UUID"));
  }

  private void insertMeasureFor(ComponentDto... components) {
    Arrays.stream(components).forEach(componentDto -> dbTester.getDbClient().measureDao().insert(dbSession, new MeasureDto()
      .setMetricId(new Random().nextInt())
      .setComponentUuid(componentDto.uuid())
      .setAnalysisUuid(randomAlphabetic(3))));
    dbSession.commit();
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

  private CeQueueDto insertCeQueue(ComponentDto project) {
    CeQueueDto res = new CeQueueDto()
      .setUuid(UuidFactoryFast.getInstance().create())
      .setTaskType("foo")
      .setComponentUuid(project.uuid())
      .setStatus(Status.PENDING)
      .setExecutionCount(0)
      .setCreatedAt(1_2323_222L)
      .setUpdatedAt(1_2323_222L);
    dbClient.ceQueueDao().insert(dbSession, res);
    dbSession.commit();
    return res;
  }

  private void insertCeScannerContext(String uuid) {
    dbClient.ceScannerContextDao().insert(dbSession, uuid, CloseableIterator.from(Arrays.asList("a", "b", "c").iterator()));
    dbSession.commit();
  }

  private void insertCeTaskCharacteristics(String uuid, int count) {
    List<CeTaskCharacteristicDto> dtos = IntStream.range(0, count)
      .mapToObj(i -> new CeTaskCharacteristicDto()
        .setUuid(UuidFactoryFast.getInstance().create())
        .setTaskUuid(uuid)
        .setKey("key_" + uuid.hashCode() + i)
        .setValue("value_" + uuid.hashCode() + i))
      .collect(Collectors.toList());
    dbClient.ceTaskCharacteristicsDao().insert(dbSession, dtos);
    dbSession.commit();
  }

  private void insertCeTaskInput(String uuid) {
    dbClient.ceTaskInputDao().insert(dbSession, uuid, new ByteArrayInputStream("some content man!".getBytes()));
    dbSession.commit();
  }

  private static PurgeableAnalysisDto getById(List<PurgeableAnalysisDto> snapshots, String uuid) {
    return snapshots.stream()
      .filter(snapshot -> uuid.equals(snapshot.getAnalysisUuid()))
      .findFirst()
      .orElse(null);
  }

  private static PurgeConfiguration newConfigurationWith30Days() {
    return new PurgeConfiguration(new IdUuidPair(THE_PROJECT_ID, THE_PROJECT_UUID), emptyList(), 30, Optional.of(30), System2.INSTANCE, Collections.emptyList());
  }

  private static PurgeConfiguration newConfigurationWith30Days(System2 system2, String rootProjectUuid, String... disabledComponentUuids) {
    return new PurgeConfiguration(new IdUuidPair(THE_PROJECT_ID, rootProjectUuid), emptyList(), 30, Optional.of(30), system2, asList(disabledComponentUuids));
  }

}
