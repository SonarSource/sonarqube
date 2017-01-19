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
package org.sonar.server.permission.index;

import com.google.common.collect.ImmutableMap;
import java.util.Collections;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.sonar.api.config.MapSettings;
import org.sonar.api.resources.Qualifiers;
import org.sonar.api.utils.System2;
import org.sonar.db.DbTester;
import org.sonar.db.component.ComponentDbTester;
import org.sonar.db.component.ComponentDto;
import org.sonar.db.component.ComponentTesting;
import org.sonar.db.permission.GroupPermissionDto;
import org.sonar.db.user.GroupDto;
import org.sonar.db.user.UserDbTester;
import org.sonar.db.user.UserDto;
import org.sonar.server.component.index.ComponentIndexDefinition;
import org.sonar.server.es.EsTester;
import org.sonar.server.issue.index.IssueIndexDefinition;
import org.sonar.server.measure.index.ProjectMeasuresIndexDefinition;

import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.sonar.api.security.DefaultGroups.ANYONE;
import static org.sonar.api.web.UserRole.ADMIN;
import static org.sonar.api.web.UserRole.USER;

public class PermissionIndexerTest {

  @Rule
  public ExpectedException expectedException = ExpectedException.none();

  @Rule
  public DbTester dbTester = DbTester.create(System2.INSTANCE);

  @Rule
  public EsTester esTester = new EsTester(
    new IssueIndexDefinition(new MapSettings()),
    new ProjectMeasuresIndexDefinition(new MapSettings()),
    new ComponentIndexDefinition(new MapSettings()));

  private ComponentDbTester componentDbTester = new ComponentDbTester(dbTester);
  private UserDbTester userDbTester = new UserDbTester(dbTester);

  private PermissionIndexerTester authorizationIndexerTester = new PermissionIndexerTester(esTester);

  private PermissionIndexer underTest = new PermissionIndexer(dbTester.getDbClient(), esTester.client());

  @Test
  public void index_all_does_nothing_when_no_data() {
    underTest.indexAllIfEmpty();

    assertThat(esTester.countDocuments("issues", "authorization")).isZero();
  }

  @Test
  public void index_all() {
    ComponentDto project = componentDbTester.insertProject();
    ComponentDto view = componentDbTester.insertView();
    UserDto user = userDbTester.insertUser();
    userDbTester.insertProjectPermissionOnUser(user, USER, project);
    userDbTester.insertProjectPermissionOnUser(user, ADMIN, project);
    userDbTester.insertProjectPermissionOnUser(user, USER, view);
    userDbTester.insertProjectPermissionOnUser(user, ADMIN, view);
    GroupDto group = userDbTester.insertGroup();
    userDbTester.insertProjectPermissionOnGroup(group, USER, project);
    userDbTester.insertProjectPermissionOnAnyone(dbTester.getDefaultOrganization(), USER, project);
    userDbTester.insertProjectPermissionOnGroup(group, USER, view);
    userDbTester.insertProjectPermissionOnAnyone(dbTester.getDefaultOrganization(), USER, view);

    underTest.indexAllIfEmpty();

    authorizationIndexerTester.verifyProjectExistsWithPermission(project.uuid(), asList(group.getName(), ANYONE), singletonList(user.getId()));
    authorizationIndexerTester.verifyViewExistsWithPermissionInRightIndexes(view.uuid(), asList(group.getName(), ANYONE), singletonList(user.getId()));
  }

  @Test
  public void index_all_with_huge_number_of_projects() throws Exception {
    GroupDto group = userDbTester.insertGroup();
    for (int i = 0; i < 1100; i++) {
      ComponentDto project = ComponentTesting.newProjectDto(dbTester.getDefaultOrganization());
      dbTester.getDbClient().componentDao().insert(dbTester.getSession(), project);
      GroupPermissionDto dto = new GroupPermissionDto()
        .setOrganizationUuid(group.getOrganizationUuid())
        .setGroupId(group.getId())
        .setRole(USER)
        .setResourceId(project.getId());
      dbTester.getDbClient().groupPermissionDao().insert(dbTester.getSession(), dto);
    }
    dbTester.getSession().commit();

    underTest.indexAllIfEmpty();

    assertThat(esTester.countDocuments(IssueIndexDefinition.INDEX, IssueIndexDefinition.TYPE_AUTHORIZATION)).isEqualTo(1100);
    assertThat(esTester.countDocuments(ProjectMeasuresIndexDefinition.INDEX_PROJECT_MEASURES, ProjectMeasuresIndexDefinition.TYPE_AUTHORIZATION)).isEqualTo(1100);
    assertThat(esTester.countDocuments(ComponentIndexDefinition.INDEX_COMPONENTS, ComponentIndexDefinition.TYPE_AUTHORIZATION)).isEqualTo(1100);
  }

  @Test
  public void index_all_is_first_truncating_indexes() throws Exception {
    // Insert only one document in issues authorization => only one index is empty
    esTester.client().prepareIndex(IssueIndexDefinition.INDEX, IssueIndexDefinition.TYPE_AUTHORIZATION)
      .setId("ABC")
      .setRouting("ABC")
      .setSource(ImmutableMap.of(IssueIndexDefinition.FIELD_AUTHORIZATION_PROJECT_UUID, "ABC"))
      .setRefresh(true)
      .get();
    GroupDto group = userDbTester.insertGroup();
    ComponentDto project = componentDbTester.insertProject();
    userDbTester.insertProjectPermissionOnGroup(group, USER, project);

    underTest.indexAllIfEmpty();

    authorizationIndexerTester.verifyProjectExistsWithPermission(project.uuid(), asList(group.getName(), ANYONE), emptyList());
    authorizationIndexerTester.verifyProjectDoesNotExist("ABC");
  }

  @Test
  public void index_one_project() throws Exception {
    GroupDto group = userDbTester.insertGroup();
    ComponentDto project1 = componentDbTester.insertProject();
    userDbTester.insertProjectPermissionOnGroup(group, USER, project1);
    ComponentDto project2 = componentDbTester.insertProject();
    userDbTester.insertProjectPermissionOnGroup(group, USER, project2);

    underTest.index(dbTester.getSession(), project1.uuid());

    authorizationIndexerTester.verifyProjectExistsWithPermission(project1.uuid(), asList(group.getName(), ANYONE), emptyList());
    authorizationIndexerTester.verifyProjectDoesNotExist(project2.uuid());
  }

  @Test
  public void index_projects() throws Exception {
    GroupDto group = userDbTester.insertGroup();
    ComponentDto project1 = componentDbTester.insertProject();
    userDbTester.insertProjectPermissionOnGroup(group, USER, project1);
    ComponentDto project2 = componentDbTester.insertProject();
    userDbTester.insertProjectPermissionOnGroup(group, USER, project2);
    ComponentDto project3 = componentDbTester.insertProject();
    userDbTester.insertProjectPermissionOnGroup(group, USER, project3);

    // Only index projects 1 and 2
    underTest.index(dbTester.getSession(), asList(project1.uuid(), project2.uuid()));

    authorizationIndexerTester.verifyProjectExistsWithPermission(project1.uuid(), asList(group.getName(), ANYONE), emptyList());
    authorizationIndexerTester.verifyProjectExistsWithPermission(project2.uuid(), asList(group.getName(), ANYONE), emptyList());
    authorizationIndexerTester.verifyProjectDoesNotExist(project3.uuid());
  }

  @Test
  public void update_existing_permissions() {
    authorizationIndexerTester.indexProjectPermission("ABC", singletonList("dev"), singletonList(10L));

    // remove permissions -> dto has no users nor groups
    underTest.index(new PermissionIndexerDao.Dto("ABC", System.currentTimeMillis(), Qualifiers.PROJECT));

    authorizationIndexerTester.verifyProjectExistsWithoutPermission("ABC");
  }

  @Test
  public void fail_when_trying_to_index_empty_project_uuids() throws Exception {
    expectedException.expect(IllegalArgumentException.class);
    underTest.index(dbTester.getSession(), Collections.emptyList());
  }
}
