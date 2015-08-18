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

package org.sonar.server.permission;

import com.google.common.collect.Maps;
import java.util.Collection;
import java.util.Map;
import javax.annotation.Nullable;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.search.SearchHit;
import org.junit.After;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.sonar.api.web.UserRole;
import org.sonar.db.DbSession;
import org.sonar.db.component.ComponentDto;
import org.sonar.db.user.GroupDto;
import org.sonar.db.user.RoleDao;
import org.sonar.db.user.UserDto;
import org.sonar.db.component.ComponentTesting;
import org.sonar.server.db.DbClient;
import org.sonar.server.es.EsClient;
import org.sonar.server.issue.index.IssueIndexDefinition;
import org.sonar.server.tester.ServerTester;
import org.sonar.server.tester.UserSessionRule;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * New tests should be added in order to be able to remove PermissionServiceTest
 */
public class PermissionServiceMediumTest {

  @ClassRule
  public static ServerTester tester = new ServerTester().withStartupTasks().withEsIndexes();
  @Rule
  public UserSessionRule userSessionRule = UserSessionRule.forServerTester(tester);

  DbClient db;
  DbSession session;
  PermissionService underTest;

  ComponentDto project;

  @Before
  public void setUp() {
    tester.clearDbAndIndexes();
    db = tester.get(DbClient.class);
    session = db.openSession(false);
    underTest = tester.get(PermissionService.class);

    project = ComponentTesting.newProjectDto();
    db.componentDao().insert(session, project);
    session.commit();
  }

  @After
  public void after() {
    session.close();
  }

  @Test
  public void add_project_permission_to_user() {
    // init
    userSessionRule.login("admin").addProjectPermissions(UserRole.ADMIN, project.key());
    UserDto user = new UserDto().setLogin("john").setName("John");
    db.userDao().insert(session, user);
    session.commit();
    assertThat(tester.get(RoleDao.class).selectUserPermissions(session, user.getLogin(), project.getId())).isEmpty();
    assertThat(countIssueAuthorizationDocs()).isZero();

    // add permission
    underTest.addPermission(params(user.getLogin(), null, project.key(), UserRole.USER));
    session.commit();

    // Check db
    assertThat(tester.get(RoleDao.class).selectUserPermissions(session, user.getLogin(), project.getId())).hasSize(1);

    // Check index of issue authorizations
    assertThat(countIssueAuthorizationDocs()).isEqualTo(1);
  }

  @Test
  public void remove_project_permission_to_user() {
    userSessionRule.login("admin").addProjectPermissions(UserRole.ADMIN, project.key());

    UserDto user1 = new UserDto().setLogin("user1").setName("User1");
    db.userDao().insert(session, user1);

    UserDto user2 = new UserDto().setLogin("user2").setName("User2");
    db.userDao().insert(session, user2);
    session.commit();

    underTest.addPermission(params(user1.getLogin(), null, project.key(), UserRole.USER));
    underTest.addPermission(params(user2.getLogin(), null, project.key(), UserRole.USER));
    underTest.removePermission(params(user1.getLogin(), null, project.key(), UserRole.USER));
    session.commit();

    // Check in db
    assertThat(tester.get(RoleDao.class).selectUserPermissions(session, user1.getLogin(), project.getId())).isEmpty();
    assertThat(tester.get(RoleDao.class).selectUserPermissions(session, user2.getLogin(), project.getId())).hasSize(1);

    // Check index of issue authorizations
    assertThat(countIssueAuthorizationDocs()).isEqualTo(1);
  }

  @Test
  public void remove_all_component_user_permissions() {
    userSessionRule.login("admin").addProjectPermissions(UserRole.ADMIN, project.key());

    UserDto user = new UserDto().setLogin("user1").setName("User1");
    db.userDao().insert(session, user);
    session.commit();

    underTest.addPermission(params(user.getLogin(), null, project.key(), UserRole.USER));
    underTest.removePermission(params(user.getLogin(), null, project.key(), UserRole.USER));
    session.commit();

    // Check in db
    assertThat(tester.get(RoleDao.class).selectUserPermissions(session, user.getLogin(), project.getId())).isEmpty();

    // Check index of issue authorizations
    SearchResponse docs = getAllIndexDocs();
    assertThat(docs.getHits().getTotalHits()).isEqualTo(1L);
    SearchHit doc = docs.getHits().getAt(0);
    assertThat((Collection) doc.sourceAsMap().get(IssueIndexDefinition.FIELD_AUTHORIZATION_USERS)).hasSize(0);
    assertThat((Collection) doc.sourceAsMap().get(IssueIndexDefinition.FIELD_AUTHORIZATION_GROUPS)).hasSize(0);
  }

  private SearchResponse getAllIndexDocs() {
    return tester.get(EsClient.class).prepareSearch(IssueIndexDefinition.INDEX).setTypes(IssueIndexDefinition.TYPE_AUTHORIZATION).get();
  }

  @Test
  public void add_and_remove_permission_to_group() {
    // init
    userSessionRule.login("admin").addProjectPermissions(UserRole.ADMIN, project.key());
    GroupDto group = new GroupDto().setName("group1");
    db.groupDao().insert(session, group);
    session.commit();
    assertThat(tester.get(RoleDao.class).selectGroupPermissions(session, group.getName(), project.getId())).isEmpty();

    // add permission
    PermissionChange change = new PermissionChange().setPermission(UserRole.USER).setGroupName(group.getName()).setComponentKey(project.key());
    underTest.addPermission(change);
    session.commit();

    // Check db
    assertThat(tester.get(RoleDao.class).selectGroupPermissions(session, group.getName(), project.getId())).hasSize(1);

    // Check index of issue authorizations
    assertThat(countIssueAuthorizationDocs()).isEqualTo(1);

    // remove permission
    underTest.removePermission(change);
    session.commit();
    assertThat(tester.get(RoleDao.class).selectGroupPermissions(session, group.getName(), project.getId())).hasSize(0);

    SearchResponse docs = getAllIndexDocs();
    assertThat(docs.getHits().getTotalHits()).isEqualTo(1L);
    SearchHit doc = docs.getHits().getAt(0);
    assertThat((Collection) doc.sourceAsMap().get(IssueIndexDefinition.FIELD_AUTHORIZATION_USERS)).hasSize(0);
    assertThat((Collection) doc.sourceAsMap().get(IssueIndexDefinition.FIELD_AUTHORIZATION_GROUPS)).hasSize(0);
  }

  private Map<String, Object> params(@Nullable String login, @Nullable String group, @Nullable String component, String permission) {
    Map<String, Object> params = Maps.newHashMap();
    params.put("user", login);
    params.put("group", group);
    params.put("component", component);
    params.put("permission", permission);
    return params;
  }

  private long countIssueAuthorizationDocs() {
    return tester.get(EsClient.class).prepareCount(IssueIndexDefinition.INDEX).setTypes(IssueIndexDefinition.TYPE_AUTHORIZATION).get().getCount();
  }
}
