/*
 * SonarQube
 * Copyright (C) 2009-2020 SonarSource SA
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

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
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
import org.sonar.db.organization.OrganizationDto;
import org.sonar.db.project.ProjectDto;
import org.sonar.db.rule.RuleDefinitionDto;
import org.sonar.db.webhook.WebhookDto;
import org.sonar.server.es.TestProjectIndexers;

import static java.util.Arrays.asList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.sonar.db.component.ComponentTesting.newFileDto;
import static org.sonar.server.es.ProjectIndexer.Cause.PROJECT_DELETION;

public class ComponentCleanerServiceTest {

  private System2 system2 = System2.INSTANCE;

  @Rule
  public DbTester db = DbTester.create(system2);

  @Rule
  public ExpectedException expectedException = ExpectedException.none();

  private DbClient dbClient = db.getDbClient();
  private DbSession dbSession = db.getSession();
  private TestProjectIndexers projectIndexers = new TestProjectIndexers();
  private ResourceTypes mockResourceTypes = mock(ResourceTypes.class);
  private ComponentCleanerService underTest = new ComponentCleanerService(dbClient, mockResourceTypes, projectIndexers);

  @Test
  public void delete_project_from_db_and_index() {
    DbData data1 = insertData();
    DbData data2 = insertData();

    underTest.delete(dbSession, data1.project);

    assertNotExists(data1);
    assertExists(data2);
  }

  @Test
  public void delete_list_of_projects_from_db_and_index() {
    DbData data1 = insertData();
    DbData data2 = insertData();
    DbData data3 = insertData();

    underTest.delete(dbSession, asList(data1.project, data2.project));
    dbSession.commit();

    assertNotExists(data1);
    assertNotExists(data2);
    assertExists(data3);
  }

  @Test
  public void delete_branch() {
    DbData data1 = insertData();
    DbData data2 = insertData();
    DbData data3 = insertData();

    underTest.deleteBranch(dbSession, data1.branch);
    dbSession.commit();

    assertNotExists(data1);
    assertExists(data2);
    assertExists(data3);
  }

  @Test
  public void delete_webhooks_from_projects() {
    OrganizationDto organization = db.organizations().insert();
    ProjectDto project1 = db.components().insertPrivateProjectDto(organization);
    WebhookDto webhook1 = db.webhooks().insertWebhook(project1);
    db.webhookDelivery().insert(webhook1);
    ProjectDto project2 = db.components().insertPrivateProjectDto(organization);
    WebhookDto webhook2 = db.webhooks().insertWebhook(project2);
    db.webhookDelivery().insert(webhook2);
    ProjectDto projectNotToBeDeleted = db.components().insertPrivateProjectDto(organization);
    WebhookDto webhook3 = db.webhooks().insertWebhook(projectNotToBeDeleted);
    db.webhookDelivery().insert(webhook3);

    ProjectDto projectDto1 = dbClient.projectDao().selectByUuid(dbSession, project1.getUuid()).get();
    ProjectDto projectDto2 = dbClient.projectDao().selectByUuid(dbSession, project2.getUuid()).get();

    mockResourceTypeAsValidProject();

    underTest.delete(dbSession, asList(projectDto1, projectDto2));

    assertThat(db.countRowsOfTable(db.getSession(), "webhooks")).isEqualTo(1);
    assertThat(db.countRowsOfTable(db.getSession(), "webhook_deliveries")).isEqualTo(1);
  }

  @Test
  public void fail_with_IAE_if_not_a_project() {
    mockResourceTypeAsValidProject();
    ComponentDto project = ComponentTesting.newPrivateProjectDto(db.organizations().insert());
    dbClient.componentDao().insert(dbSession, project);
    ComponentDto file = newFileDto(project, null);
    dbClient.componentDao().insert(dbSession, file);
    dbSession.commit();

    expectedException.expect(IllegalArgumentException.class);
    underTest.delete(dbSession, file);
  }

  private DbData insertData() {
    OrganizationDto organization = db.organizations().insert();
    ComponentDto componentDto = db.components().insertPublicProject(organization);
    ProjectDto project = dbClient.projectDao().selectByUuid(dbSession, componentDto.uuid()).get();
    BranchDto branch = dbClient.branchDao().selectByUuid(dbSession, project.getUuid()).get();
    ComponentDto component = dbClient.componentDao().selectByKey(dbSession, project.getKey()).get();
    RuleDefinitionDto rule = db.rules().insert();
    IssueDto issue = db.issues().insert(rule, project, component);
    SnapshotDto analysis = db.components().insertSnapshot(component);
    mockResourceTypeAsValidProject();
    return new DbData(project, branch, analysis, issue);
  }

  private void mockResourceTypeAsValidProject() {
    ResourceType resourceType = mock(ResourceType.class);
    when(resourceType.getBooleanProperty(anyString())).thenReturn(true);
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
    assertThat(dbClient.componentDao().selectByUuid(dbSession, data.project.getUuid()).isPresent()).isEqualTo(exists);
    assertThat(dbClient.branchDao().selectByUuid(dbSession, data.branch.getUuid()).isPresent()).isEqualTo(exists);
    assertThat(dbClient.snapshotDao().selectByUuid(dbSession, data.snapshot.getUuid()).isPresent()).isEqualTo(exists);
    assertThat(dbClient.issueDao().selectByKey(dbSession, data.issue.getKey()).isPresent()).isEqualTo(exists);
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
