/*
 * SonarQube
 * Copyright (C) 2009-2016 SonarSource SA
 * mailto:contact AT sonarsource DOT com
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

import java.util.Date;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.sonar.api.config.MapSettings;
import org.sonar.api.resources.ResourceType;
import org.sonar.api.resources.ResourceTypes;
import org.sonar.api.rule.RuleKey;
import org.sonar.api.utils.System2;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.db.DbTester;
import org.sonar.db.component.ComponentDto;
import org.sonar.db.component.SnapshotDto;
import org.sonar.db.component.SnapshotTesting;
import org.sonar.db.issue.IssueDto;
import org.sonar.db.rule.RuleDto;
import org.sonar.db.rule.RuleTesting;
import org.sonar.server.component.es.ProjectMeasuresIndexDefinition;
import org.sonar.server.component.es.ProjectMeasuresIndexer;
import org.sonar.server.es.EsTester;
import org.sonar.server.exceptions.NotFoundException;
import org.sonar.server.issue.IssueTesting;
import org.sonar.server.issue.index.IssueIndexDefinition;
import org.sonar.server.issue.index.IssueIndexer;
import org.sonar.server.permission.index.AuthorizationIndexer;
import org.sonar.server.test.index.TestDoc;
import org.sonar.server.test.index.TestIndexDefinition;
import org.sonar.server.test.index.TestIndexer;

import static java.util.Arrays.asList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.sonar.db.component.ComponentTesting.newFileDto;
import static org.sonar.db.component.ComponentTesting.newProjectDto;
import static org.sonar.server.issue.index.IssueIndexDefinition.TYPE_ISSUE;

public class ComponentCleanerServiceTest {

  @Rule
  public DbTester db = DbTester.create(System2.INSTANCE);

  @Rule
  public EsTester es = new EsTester(
    new IssueIndexDefinition(new MapSettings()),
    new TestIndexDefinition(new MapSettings()),
    new ProjectMeasuresIndexDefinition(new MapSettings()));

  @Rule
  public ExpectedException expectedException = ExpectedException.none();

  DbClient dbClient = db.getDbClient();
  DbSession dbSession = db.getSession();

  AuthorizationIndexer authorizationIndexer = new AuthorizationIndexer(dbClient, es.client());
  IssueIndexer issueIndexer = new IssueIndexer(dbClient, es.client());
  TestIndexer testIndexer = new TestIndexer(dbClient, es.client());
  ProjectMeasuresIndexer projectMeasuresIndexer = new ProjectMeasuresIndexer(dbClient, es.client());

  ResourceTypes mockResourceTypes = mock(ResourceTypes.class);

  ComponentCleanerService underTest = new ComponentCleanerService(dbClient,
    authorizationIndexer, issueIndexer, testIndexer, projectMeasuresIndexer,
    mockResourceTypes,
    new ComponentFinder(dbClient));

  @Test
  public void delete_project_by_key_in_db() {
    DbData data1 = insertDataInDb(1);
    DbData data2 = insertDataInDb(2);

    underTest.delete(data1.project.key());

    assertDataDoesNotExistInDB(data1);
    assertDataStillExistsInDb(data2);
  }

  @Test
  public void delete_project_by_key_in_index() throws Exception {
    IndexData data1 = insertDataInEs(1);
    IndexData data2 = insertDataInEs(2);

    underTest.delete(data1.project.key());

    assertDataDoesNotExistInIndex(data1);
    assertDataStillExistsInIndex(data2);
  }

  @Test
  public void delete_projects_in_db() {
    DbData data1 = insertDataInDb(1);
    DbData data2 = insertDataInDb(2);
    DbData data3 = insertDataInDb(3);

    underTest.delete(dbSession, asList(data1.project, data2.project));
    dbSession.commit();

    assertDataDoesNotExistInDB(data1);
    assertDataDoesNotExistInDB(data2);
    assertDataStillExistsInDb(data3);
  }

  @Test
  public void delete_projects_in_index() throws Exception {
    IndexData data1 = insertDataInEs(1);
    IndexData data2 = insertDataInEs(2);
    IndexData data3 = insertDataInEs(3);

    underTest.delete(dbSession, asList(data1.project, data2.project));
    dbSession.commit();

    assertDataDoesNotExistInIndex(data1);
    assertDataDoesNotExistInIndex(data2);
    assertDataStillExistsInIndex(data3);
  }

  @Test
  public void fail_to_delete_unknown_project() throws Exception {
    expectedException.expect(NotFoundException.class);
    underTest.delete("unknown");
  }

  @Test
  public void fail_to_delete_not_project_scope() throws Exception {
    mockResourceTypeAsValidProject();
    ComponentDto project = newProjectDto();
    dbClient.componentDao().insert(dbSession, project);
    ComponentDto file = newFileDto(project, null);
    dbClient.componentDao().insert(dbSession, file);
    dbSession.commit();

    expectedException.expect(IllegalArgumentException.class);
    underTest.delete(file.key());
  }

  @Test
  public void fail_to_delete_not_deletable_resource_type() throws Exception {
    ResourceType resourceType = mock(ResourceType.class);
    when(resourceType.getBooleanProperty("deletable")).thenReturn(false);
    when(mockResourceTypes.get(anyString())).thenReturn(resourceType);
    ComponentDto project = newProjectDto();
    dbClient.componentDao().insert(dbSession, project);
    dbSession.commit();

    expectedException.expect(IllegalArgumentException.class);
    underTest.delete(project.key());
  }

  @Test
  public void fail_to_delete_null_resource_type() throws Exception {
    when(mockResourceTypes.get(anyString())).thenReturn(null);
    ComponentDto project = newProjectDto();
    dbClient.componentDao().insert(dbSession, project);
    dbSession.commit();

    expectedException.expect(IllegalArgumentException.class);
    underTest.delete(project.key());
  }

  private DbData insertDataInDb(int id) {
    String suffix = String.valueOf(id);
    ComponentDto project = newProjectDto("project-uuid-" + suffix)
      .setKey("project-key-" + suffix);
    RuleDto rule = RuleTesting.newDto(RuleKey.of("sonarqube", "rule-" + suffix));
    dbClient.ruleDao().insert(dbSession, rule);
    IssueDto issue = IssueTesting.newDto(rule, project, project).setKee("issue-key-" + suffix).setUpdatedAt(new Date().getTime());
    dbClient.componentDao().insert(dbSession, project);
    SnapshotDto snapshot = dbClient.snapshotDao().insert(dbSession, SnapshotTesting.newAnalysis(project));
    dbClient.issueDao().insert(dbSession, issue);
    dbSession.commit();
    mockResourceTypeAsValidProject();
    return new DbData(project, snapshot, issue);
  }

  private void mockResourceTypeAsValidProject() {
    ResourceType resourceType = mock(ResourceType.class);
    when(resourceType.getBooleanProperty(anyString())).thenReturn(true);
    when(mockResourceTypes.get(anyString())).thenReturn(resourceType);
  }

  private void assertDataStillExistsInDb(DbData data) {
    assertDataInDb(data, true);
  }

  private void assertDataDoesNotExistInDB(DbData data) {
    assertDataInDb(data, false);
  }

  private void assertDataInDb(DbData data, boolean exists) {
    assertThat(dbClient.componentDao().selectByUuid(dbSession, data.project.uuid()).isPresent()).isEqualTo(exists);
    assertThat(dbClient.snapshotDao().selectByUuid(dbSession, data.snapshot.getUuid()).isPresent()).isEqualTo(exists);
    assertThat(dbClient.issueDao().selectByKey(dbSession, data.issue.getKey()).isPresent()).isEqualTo(exists);
  }

  private IndexData insertDataInEs(int id) throws Exception {
    mockResourceTypeAsValidProject();

    String suffix = String.valueOf(id);
    ComponentDto project = newProjectDto("project-uuid-" + suffix)
      .setKey("project-key-" + suffix);
    dbClient.componentDao().insert(dbSession, project);
    dbSession.commit();
    projectMeasuresIndexer.index();
    authorizationIndexer.index(project.uuid());

    String issueKey = "issue-key-" + suffix;
    es.putDocuments(IssueIndexDefinition.INDEX, TYPE_ISSUE, IssueTesting.newDoc(issueKey, project));

    TestDoc testDoc = new TestDoc().setUuid("test-uuid-" + suffix).setProjectUuid(project.uuid()).setFileUuid(project.uuid());
    es.putDocuments(TestIndexDefinition.INDEX, TestIndexDefinition.TYPE, testDoc);

    return new IndexData(project, issueKey, testDoc.getId());
  }

  private void assertDataStillExistsInIndex(IndexData data) {
    assertDataInIndex(data, true);
  }

  private void assertDataDoesNotExistInIndex(IndexData data) {
    assertDataInIndex(data, false);
  }

  private void assertDataInIndex(IndexData data, boolean exists) {
    if (exists) {
      assertThat(es.getIds(IssueIndexDefinition.INDEX, IssueIndexDefinition.TYPE_ISSUE)).contains(data.issueKey);
      assertThat(es.getIds(IssueIndexDefinition.INDEX, IssueIndexDefinition.TYPE_AUTHORIZATION)).contains(data.project.uuid());
      assertThat(es.getIds(TestIndexDefinition.INDEX, TestIndexDefinition.TYPE)).contains(data.testId);
      assertThat(es.getIds(ProjectMeasuresIndexDefinition.INDEX_PROJECT_MEASURES, ProjectMeasuresIndexDefinition.TYPE_PROJECT_MEASURES)).contains(data.project.uuid());
    } else {
      assertThat(es.getIds(IssueIndexDefinition.INDEX, IssueIndexDefinition.TYPE_ISSUE)).doesNotContain(data.issueKey);
      assertThat(es.getIds(IssueIndexDefinition.INDEX, IssueIndexDefinition.TYPE_AUTHORIZATION)).doesNotContain(data.project.uuid());
      assertThat(es.getIds(TestIndexDefinition.INDEX, TestIndexDefinition.TYPE)).doesNotContain(data.testId);
      assertThat(es.getIds(ProjectMeasuresIndexDefinition.INDEX_PROJECT_MEASURES, ProjectMeasuresIndexDefinition.TYPE_PROJECT_MEASURES)).doesNotContain(data.project.uuid());
    }
  }

  private static class DbData {
    final ComponentDto project;
    final SnapshotDto snapshot;
    final IssueDto issue;

    public DbData(ComponentDto project, SnapshotDto snapshot, IssueDto issue) {
      this.project = project;
      this.snapshot = snapshot;
      this.issue = issue;
    }
  }

  private static class IndexData {
    final ComponentDto project;
    final String issueKey;
    final String testId;

    public IndexData(ComponentDto project, String issueKey, String testId) {
      this.project = project;
      this.issueKey = issueKey;
      this.testId = testId;
    }
  }
}
