/*
 * SonarQube, open source software quality management tool.
 * Copyright (C) 2008-2014 SonarSource
 * mailto:contact AT sonarsource DOT com
 *
 * SonarQube is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * SonarQube is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */

package org.sonar.server.component;

import org.junit.After;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Test;
import org.sonar.api.security.DefaultGroups;
import org.sonar.api.web.UserRole;
import org.sonar.core.component.ComponentDto;
import org.sonar.core.issue.db.IssueDto;
import org.sonar.core.permission.PermissionFacade;
import org.sonar.core.persistence.DbSession;
import org.sonar.core.rule.RuleDto;
import org.sonar.server.component.db.ComponentDao;
import org.sonar.server.component.db.SnapshotDao;
import org.sonar.server.db.DbClient;
import org.sonar.server.exceptions.ForbiddenException;
import org.sonar.server.issue.IssueTesting;
import org.sonar.server.issue.index.IssueAuthorizationIndex;
import org.sonar.server.issue.index.IssueIndex;
import org.sonar.server.rule.RuleTesting;
import org.sonar.server.rule.db.RuleDao;
import org.sonar.server.search.IndexDefinition;
import org.sonar.server.search.SearchClient;
import org.sonar.server.tester.ServerTester;
import org.sonar.server.user.MockUserSession;

import java.util.Date;
import java.util.Map;

import static org.fest.assertions.Assertions.assertThat;

public class ComponentServiceMediumTest {

  @ClassRule
  public static ServerTester tester = new ServerTester();

  DbClient db;
  DbSession session;
  ComponentService service;
  ComponentDto project;
  RuleDto rule;

  @Before
  public void setUp() throws Exception {
    tester.clearDbAndIndexes();
    db = tester.get(DbClient.class);
    session = db.openSession(false);
    service = tester.get(ComponentService.class);

    project = ComponentTesting.newProjectDto().setKey("sample:root");
    tester.get(ComponentDao.class).insert(session, project);
    tester.get(SnapshotDao.class).insert(session, SnapshotTesting.createForProject(project));

    // project can be seen by anyone
    tester.get(PermissionFacade.class).insertGroupPermission(project.getId(), DefaultGroups.ANYONE, UserRole.USER, session);
    db.issueAuthorizationDao().synchronizeAfter(session, new Date(0));

    rule = RuleTesting.newXooX1();
    tester.get(RuleDao.class).insert(session, rule);

    session.commit();
  }

  @After
  public void after() {
    session.close();
  }

  @Test
  public void get_by_key() throws Exception {
    assertThat(service.getByKey(project.getKey())).isNotNull();
  }

  @Test
  public void get_nullable_by_key() throws Exception {
    assertThat(service.getNullableByKey(project.getKey())).isNotNull();
    assertThat(service.getNullableByKey("unknown")).isNull();
  }

  @Test
  public void update_project_key() throws Exception {
    ComponentDto file = ComponentTesting.newFileDto(project).setKey("sample:root:src/File.xoo");
    tester.get(ComponentDao.class).insert(session, file);
    tester.get(SnapshotDao.class).insert(session, SnapshotTesting.createForComponent(file, project));

    IssueDto issue = IssueTesting.newDto(rule, file, project);
    db.issueDao().insert(session, issue);

    session.commit();

    MockUserSession.set().setLogin("john").addComponentPermission(UserRole.ADMIN, project.key(), project.key());
    service.updateKey(project.key(), "sample2:root");
    session.commit();

    // Check project key has been updated
    assertThat(service.getNullableByKey(project.key())).isNull();
    assertThat(service.getNullableByKey("sample2:root")).isNotNull();

    // Check file key has been updated
    assertThat(service.getNullableByKey(file.key())).isNull();
    assertThat(service.getNullableByKey("sample2:root:src/File.xoo")).isNotNull();

    // Check issue have been updated
    assertThat(tester.get(IssueIndex.class).getNullableByKey(issue.getKey()).componentKey()).isEqualTo("sample2:root:src/File.xoo");
    assertThat(tester.get(IssueIndex.class).getNullableByKey(issue.getKey()).projectKey()).isEqualTo("sample2:root");

    // Check that no new issue has been added
    assertThat(tester.get(SearchClient.class).prepareCount(IndexDefinition.ISSUES.getIndexName()).setTypes(IndexDefinition.ISSUES.getIndexType()).get().getCount()).isEqualTo(1);

    // Check Issue Authorization index
    assertThat(tester.get(IssueAuthorizationIndex.class).getNullableByKey(project.getKey())).isNull();
    assertThat(tester.get(IssueAuthorizationIndex.class).getNullableByKey("sample2:root")).isNotNull();
    assertThat(
      tester.get(SearchClient.class).prepareCount(IndexDefinition.ISSUES_AUTHORIZATION.getIndexName()).setTypes(IndexDefinition.ISSUES_AUTHORIZATION.getIndexType()).get()
        .getCount()).isEqualTo(1);

    // Check dry run cache have been updated
    assertThat(db.propertiesDao().selectProjectProperties("sample2:root", session)).hasSize(1);
  }

  @Test
  public void update_module_key() throws Exception {
    ComponentDto module = ComponentTesting.newModuleDto(project).setKey("sample:root:module");
    tester.get(ComponentDao.class).insert(session, module);
    tester.get(SnapshotDao.class).insert(session, SnapshotTesting.createForComponent(module, project));

    ComponentDto file = ComponentTesting.newFileDto(module).setKey("sample:root:module:src/File.xoo");
    tester.get(ComponentDao.class).insert(session, file);
    tester.get(SnapshotDao.class).insert(session, SnapshotTesting.createForComponent(file, project));

    IssueDto issue = IssueTesting.newDto(rule, file, project);
    db.issueDao().insert(session, issue);

    session.commit();

    MockUserSession.set().setLogin("john").addComponentPermission(UserRole.ADMIN, project.key(), module.key());
    service.updateKey(module.key(), "sample:root2:module");
    session.commit();

    // Project key has not changed
    assertThat(service.getNullableByKey(project.key())).isNotNull();

    // Check module key has been updated
    assertThat(service.getNullableByKey(module.key())).isNull();
    assertThat(service.getNullableByKey("sample:root2:module")).isNotNull();

    // Check file key has been updated
    assertThat(service.getNullableByKey(file.key())).isNull();
    assertThat(service.getNullableByKey("sample:root2:module:src/File.xoo")).isNotNull();

    // Check issue have been updated
    assertThat(tester.get(IssueIndex.class).getNullableByKey(issue.getKey()).componentKey()).isEqualTo("sample:root2:module:src/File.xoo");
    assertThat(tester.get(IssueIndex.class).getNullableByKey(issue.getKey()).projectKey()).isEqualTo(project.key());

    // Check Issue Authorization index
    assertThat(tester.get(IssueAuthorizationIndex.class).getNullableByKey(project.getKey())).isNotNull();

    // Check dry run cache have been updated -> on a module it's the project cache that is updated
    assertThat(db.propertiesDao().selectProjectProperties(project.key(), session)).hasSize(1);
  }

  @Test
  public void update_provisioned_project_key() throws Exception {
    ComponentDto provisionedProject = ComponentTesting.newProjectDto().setKey("provisionedProject");
    tester.get(ComponentDao.class).insert(session, provisionedProject);

    session.commit();

    MockUserSession.set().setLogin("john").addComponentPermission(UserRole.ADMIN, provisionedProject.key(), provisionedProject.key());
    service.updateKey(provisionedProject.key(), "provisionedProject2");
    session.commit();

    // Check project key has been updated
    assertThat(service.getNullableByKey(provisionedProject.key())).isNull();
    assertThat(service.getNullableByKey("provisionedProject2")).isNotNull();

    // Check dry run cache have been updated
    assertThat(db.propertiesDao().selectProjectProperties("provisionedProject2", session)).hasSize(1);
  }

  @Test(expected = ForbiddenException.class)
  public void fail_to_update_project_key_without_admin_permission() throws Exception {
    MockUserSession.set().setLogin("john").addComponentPermission(UserRole.USER, project.key(), project.key());
    service.updateKey(project.key(), "sample2:root");
  }

  @Test
  public void check_module_keys_before_renaming() throws Exception {
    ComponentDto module = ComponentTesting.newModuleDto(project).setKey("sample:root:module");
    tester.get(ComponentDao.class).insert(session, module);
    tester.get(SnapshotDao.class).insert(session, SnapshotTesting.createForComponent(module, project));

    ComponentDto file = ComponentTesting.newFileDto(module).setKey("sample:root:module:src/File.xoo");
    tester.get(ComponentDao.class).insert(session, file);
    tester.get(SnapshotDao.class).insert(session, SnapshotTesting.createForComponent(file, project));

    session.commit();

    MockUserSession.set().setLogin("john").addProjectPermissions(UserRole.ADMIN, project.key());
    Map<String, String> result = service.checkModuleKeysBeforeRenaming(project.key(), "sample", "sample2");

    assertThat(result).hasSize(2);
    assertThat(result.get("sample:root")).isEqualTo("sample2:root");
    assertThat(result.get("sample:root:module")).isEqualTo("sample2:root:module");
  }

  @Test
  public void check_module_keys_before_renaming_return_duplicate_key() throws Exception {
    ComponentDto module = ComponentTesting.newModuleDto(project).setKey("sample:root:module");
    tester.get(ComponentDao.class).insert(session, module);
    tester.get(SnapshotDao.class).insert(session, SnapshotTesting.createForComponent(module, project));

    ComponentDto module2 = ComponentTesting.newModuleDto(project).setKey("foo:module");
    tester.get(ComponentDao.class).insert(session, module2);
    tester.get(SnapshotDao.class).insert(session, SnapshotTesting.createForComponent(module2, project));

    session.commit();

    MockUserSession.set().setLogin("john").addProjectPermissions(UserRole.ADMIN, project.key());
    Map<String, String> result = service.checkModuleKeysBeforeRenaming(project.key(), "sample:root", "foo");

    assertThat(result).hasSize(2);
    assertThat(result.get("sample:root")).isEqualTo("foo");
    assertThat(result.get("sample:root:module")).isEqualTo("#duplicate_key#");
  }

  @Test(expected = ForbiddenException.class)
  public void fail_to_check_module_keys_before_renaming_without_admin_permission() throws Exception {
    MockUserSession.set().setLogin("john").addComponentPermission(UserRole.USER, project.key(), project.key());
    service.checkModuleKeysBeforeRenaming(project.key(), "sample", "sample2");
  }

  @Test
  public void bulk_update_project_key() throws Exception {
    ComponentDto module = ComponentTesting.newModuleDto(project).setKey("sample:root:module");
    tester.get(ComponentDao.class).insert(session, module);
    tester.get(SnapshotDao.class).insert(session, SnapshotTesting.createForComponent(module, project));

    ComponentDto file = ComponentTesting.newFileDto(module).setKey("sample:root:module:src/File.xoo");
    tester.get(ComponentDao.class).insert(session, file);
    tester.get(SnapshotDao.class).insert(session, SnapshotTesting.createForComponent(file, project));

    IssueDto issue = IssueTesting.newDto(rule, file, project);
    db.issueDao().insert(session, issue);

    session.commit();

    MockUserSession.set().setLogin("john").addProjectPermissions(UserRole.ADMIN, project.key());
    service.bulkUpdateKey(project.key(), "sample", "sample2");
    session.commit();

    // Check project key has been updated
    assertThat(service.getNullableByKey(project.key())).isNull();
    assertThat(service.getNullableByKey("sample2:root")).isNotNull();

    // Check module key has been updated
    assertThat(service.getNullableByKey(module.key())).isNull();
    assertThat(service.getNullableByKey("sample2:root:module")).isNotNull();

    // Check file key has been updated
    assertThat(service.getNullableByKey(file.key())).isNull();
    assertThat(service.getNullableByKey("sample2:root:module:src/File.xoo")).isNotNull();

    // Check issue have been updated
    assertThat(tester.get(IssueIndex.class).getNullableByKey(issue.getKey()).componentKey()).isEqualTo("sample2:root:module:src/File.xoo");
    assertThat(tester.get(IssueIndex.class).getNullableByKey(issue.getKey()).projectKey()).isEqualTo("sample2:root");

    // Check that no new issue has been added
    assertThat(tester.get(SearchClient.class).prepareCount(IndexDefinition.ISSUES.getIndexName()).setTypes(IndexDefinition.ISSUES.getIndexType()).get().getCount()).isEqualTo(1);

    // Check Issue Authorization index
    assertThat(tester.get(IssueAuthorizationIndex.class).getNullableByKey(project.getKey())).isNull();
    assertThat(tester.get(IssueAuthorizationIndex.class).getNullableByKey("sample2:root")).isNotNull();
    assertThat(
      tester.get(SearchClient.class).prepareCount(IndexDefinition.ISSUES_AUTHORIZATION.getIndexName()).setTypes(IndexDefinition.ISSUES_AUTHORIZATION.getIndexType()).get()
        .getCount()).isEqualTo(1);

    // Check dry run cache have been updated
    assertThat(db.propertiesDao().selectProjectProperties("sample2:root", session)).hasSize(1);
  }

  @Test
  public void bulk_update_provisioned_project_key() throws Exception {
    ComponentDto provisionedProject = ComponentTesting.newProjectDto().setKey("provisionedProject");
    tester.get(ComponentDao.class).insert(session, provisionedProject);

    session.commit();

    MockUserSession.set().setLogin("john").addComponentPermission(UserRole.ADMIN, provisionedProject.key(), provisionedProject.key());
    service.bulkUpdateKey(provisionedProject.key(), "provisionedProject", "provisionedProject2");
    session.commit();

    // Check project key has been updated
    assertThat(service.getNullableByKey(provisionedProject.key())).isNull();
    assertThat(service.getNullableByKey("provisionedProject2")).isNotNull();

    // Check dry run cache have been updated
    assertThat(db.propertiesDao().selectProjectProperties("provisionedProject2", session)).hasSize(1);
  }

  @Test(expected = ForbiddenException.class)
  public void fail_to_bulk_update_project_key_without_admin_permission() throws Exception {
    MockUserSession.set().setLogin("john").addProjectPermissions(UserRole.USER, project.key());
    service.bulkUpdateKey("sample:root", "sample", "sample2");
  }

}
