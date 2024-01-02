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
package org.sonar.server.component;

import java.util.List;
import org.junit.Rule;
import org.junit.Test;
import org.sonar.api.resources.ResourceType;
import org.sonar.api.resources.ResourceTypes;
import org.sonar.api.utils.System2;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.db.DbTester;
import org.sonar.db.component.BranchDto;
import org.sonar.db.component.ComponentDto;
import org.sonar.db.component.ComponentTesting;
import org.sonar.db.component.SnapshotDto;
import org.sonar.db.issue.IssueDto;
import org.sonar.db.project.ProjectDto;
import org.sonar.db.rule.RuleDto;
import org.sonar.db.webhook.WebhookDto;
import org.sonar.server.es.TestProjectIndexers;

import static java.util.Arrays.asList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.sonar.db.component.ComponentTesting.newFileDto;
import static org.sonar.server.es.ProjectIndexer.Cause.PROJECT_DELETION;

public class ComponentCleanerServiceTest {

  private final System2 system2 = System2.INSTANCE;

  @Rule
  public final DbTester db = DbTester.create(system2);

  private final DbClient dbClient = db.getDbClient();
  private final DbSession dbSession = db.getSession();
  private final TestProjectIndexers projectIndexers = new TestProjectIndexers();
  private final ResourceTypes mockResourceTypes = mock(ResourceTypes.class);
  private final ComponentCleanerService underTest = new ComponentCleanerService(dbClient, mockResourceTypes, projectIndexers);

  @Test
  public void delete_project_from_db_and_index() {
    DbData data1 = insertProjectData();
    DbData data2 = insertProjectData();

    underTest.delete(dbSession, data1.project);

    assertNotExists(data1);
    assertExists(data2);
  }

  @Test
  public void delete_list_of_projects_from_db_and_index() {
    DbData data1 = insertProjectData();
    DbData data2 = insertProjectData();
    DbData data3 = insertProjectData();

    underTest.delete(dbSession, asList(data1.project, data2.project));
    dbSession.commit();

    assertNotExists(data1);
    assertNotExists(data2);
    assertExists(data3);
  }

  @Test
  public void delete_list_of_components_from_db() {
    ComponentDto componentDto1 = db.components().insertPublicProject();
    ComponentDto componentDto2 = db.components().insertPublicProject();
    ComponentDto componentDto3 = db.components().insertPublicProject();

    mockResourceTypeAsValidProject();

    underTest.deleteComponents(dbSession, asList(componentDto1, componentDto2));
    dbSession.commit();

    assertNotExists(componentDto1);
    assertNotExists(componentDto2);
    assertExists(componentDto3);
  }

  @Test
  public void fail_with_IAE_if_project_non_deletable() {
    ComponentDto componentDto1 = db.components().insertPublicProject();
    ComponentDto componentDto2 = db.components().insertPublicProject();

    mockResourceTypeAsNonDeletable();

    dbSession.commit();

    List<ComponentDto> componentDtos = asList(componentDto1, componentDto2);

    assertThatThrownBy(() -> underTest.deleteComponents(dbSession, componentDtos))
      .withFailMessage("Only projects can be deleted")
      .isInstanceOf(IllegalArgumentException.class);
  }

  @Test
  public void delete_application_from_db_and_index() {
    DbData data1 = insertProjectData();
    DbData data2 = insertProjectData();
    DbData data3 = insertProjectData();
    ProjectDto app1 = insertApplication(data2.project);
    ProjectDto app2 = insertApplication(data3.project);

    underTest.deleteApplication(dbSession, app1);
    dbSession.commit();

    assertProjectOrAppExists(app1, false);
    assertProjectOrAppExists(app2, true);
    assertExists(data1);
    assertExists(data2);
    assertExists(data3);
  }

  private ProjectDto insertApplication(ProjectDto project) {
    ProjectDto app = db.components().insertPublicApplicationDto();
    db.components().addApplicationProject(app, project);
    return app;
  }

  @Test
  public void delete_branch() {
    DbData data1 = insertProjectData();
    DbData data2 = insertProjectData();
    DbData data3 = insertProjectData();

    underTest.deleteBranch(dbSession, data1.branch);
    dbSession.commit();

    assertNotExists(data1);
    assertExists(data2);
    assertExists(data3);
  }

  @Test
  public void delete_webhooks_from_projects() {
    ProjectDto project1 = db.components().insertPrivateProjectDto();
    WebhookDto webhook1 = db.webhooks().insertWebhook(project1);
    db.webhookDelivery().insert(webhook1);
    ProjectDto project2 = db.components().insertPrivateProjectDto();
    WebhookDto webhook2 = db.webhooks().insertWebhook(project2);
    db.webhookDelivery().insert(webhook2);
    ProjectDto projectNotToBeDeleted = db.components().insertPrivateProjectDto();
    WebhookDto webhook3 = db.webhooks().insertWebhook(projectNotToBeDeleted);
    db.webhookDelivery().insert(webhook3);

    ProjectDto projectDto1 = dbClient.projectDao().selectByUuid(dbSession, project1.getUuid()).get();
    ProjectDto projectDto2 = dbClient.projectDao().selectByUuid(dbSession, project2.getUuid()).get();

    mockResourceTypeAsValidProject();

    underTest.delete(dbSession, asList(projectDto1, projectDto2));

    assertThat(db.countRowsOfTable(db.getSession(), "webhooks")).isOne();
    assertThat(db.countRowsOfTable(db.getSession(), "webhook_deliveries")).isOne();
  }

  @Test
  public void fail_with_IAE_if_not_a_project() {
    mockResourceTypeAsValidProject();
    ComponentDto project = ComponentTesting.newPrivateProjectDto();
    db.components().insertComponent(project);
    ComponentDto file = newFileDto(project, null);
    dbClient.componentDao().insert(dbSession, file);
    dbSession.commit();

    assertThatThrownBy(() -> underTest.delete(dbSession, file))
      .isInstanceOf(IllegalArgumentException.class);
  }

  private DbData insertProjectData() {
    ComponentDto componentDto = db.components().insertPublicProject();
    ProjectDto project = dbClient.projectDao().selectByUuid(dbSession, componentDto.uuid()).get();
    BranchDto branch = dbClient.branchDao().selectByUuid(dbSession, project.getUuid()).get();

    RuleDto rule = db.rules().insert();
    IssueDto issue = db.issues().insert(rule, project, componentDto);
    SnapshotDto analysis = db.components().insertSnapshot(componentDto);
    mockResourceTypeAsValidProject();
    return new DbData(project, branch, analysis, issue);
  }

  private void mockResourceTypeAsValidProject() {
    ResourceType resourceType = mock(ResourceType.class);
    when(resourceType.getBooleanProperty(anyString())).thenReturn(true);
    when(mockResourceTypes.get(anyString())).thenReturn(resourceType);
  }

  private void mockResourceTypeAsNonDeletable() {
    ResourceType resourceType = mock(ResourceType.class);
    when(resourceType.getBooleanProperty("deletable")).thenReturn(false);
    when(mockResourceTypes.get(anyString())).thenReturn(resourceType);
  }

  private void assertNotExists(DbData data) {
    assertDataInDb(data, false);
    assertThat(projectIndexers.hasBeenCalled(data.branch.getUuid(), PROJECT_DELETION)).isTrue();
  }

  private void assertExists(DbData data) {
    assertDataInDb(data, true);
    assertThat(projectIndexers.hasBeenCalled(data.branch.getUuid(), PROJECT_DELETION)).isFalse();
  }

  private void assertDataInDb(DbData data, boolean exists) {
    assertProjectOrAppExists(data.project, exists);
    assertThat(dbClient.snapshotDao().selectByUuid(dbSession, data.snapshot.getUuid()).isPresent()).isEqualTo(exists);
    assertThat(dbClient.issueDao().selectByKey(dbSession, data.issue.getKey()).isPresent()).isEqualTo(exists);
  }

  private void assertProjectOrAppExists(ProjectDto appOrProject, boolean exists) {
    assertThat(dbClient.projectDao().selectByUuid(dbSession, appOrProject.getUuid()).isPresent()).isEqualTo(exists);
    assertThat(dbClient.componentDao().selectByUuid(dbSession, appOrProject.getUuid()).isPresent()).isEqualTo(exists);
    assertThat(dbClient.branchDao().selectByUuid(dbSession, appOrProject.getUuid()).isPresent()).isEqualTo(exists);
  }

  private void assertNotExists(ComponentDto componentDto) {
    assertComponentExists(componentDto, false);
  }

  private void assertExists(ComponentDto componentDto) {
    assertComponentExists(componentDto, true);
  }

  private void assertComponentExists(ComponentDto componentDto, boolean exists) {
    assertThat(dbClient.componentDao().selectByUuid(dbSession, componentDto.uuid()).isPresent()).isEqualTo(exists);
  }
  
  private static class DbData {
    final ProjectDto project;
    final BranchDto branch;
    final SnapshotDto snapshot;
    final IssueDto issue;

    DbData(ProjectDto project, BranchDto branch, SnapshotDto snapshot, IssueDto issue) {
      this.project = project;
      this.branch = branch;
      this.snapshot = snapshot;
      this.issue = issue;
    }
  }
}
