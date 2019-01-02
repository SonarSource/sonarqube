/*
 * SonarQube
 * Copyright (C) 2009-2019 SonarSource SA
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
package org.sonar.server.permission.index;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import org.assertj.core.api.Assertions;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.sonar.api.utils.System2;
import org.sonar.core.util.stream.MoreCollectors;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.db.DbTester;
import org.sonar.db.component.ComponentDbTester;
import org.sonar.db.component.ComponentDto;
import org.sonar.db.component.ComponentTesting;
import org.sonar.db.organization.OrganizationDto;
import org.sonar.db.permission.GroupPermissionDto;
import org.sonar.db.user.GroupDto;
import org.sonar.db.user.UserDbTester;
import org.sonar.db.user.UserDto;

import static java.util.Arrays.asList;
import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.sonar.api.resources.Qualifiers.APP;
import static org.sonar.api.resources.Qualifiers.PROJECT;
import static org.sonar.api.resources.Qualifiers.VIEW;
import static org.sonar.api.web.UserRole.ADMIN;
import static org.sonar.api.web.UserRole.USER;

public class PermissionIndexerDaoTest {

  @Rule
  public DbTester dbTester = DbTester.create(System2.INSTANCE);

  private DbClient dbClient = dbTester.getDbClient();
  private DbSession dbSession = dbTester.getSession();

  private ComponentDbTester componentDbTester = new ComponentDbTester(dbTester);
  private UserDbTester userDbTester = new UserDbTester(dbTester);

  private OrganizationDto organization;
  private ComponentDto publicProject;
  private ComponentDto privateProject1;
  private ComponentDto privateProject2;
  private ComponentDto view1;
  private ComponentDto view2;
  private ComponentDto application;
  private UserDto user1;
  private UserDto user2;
  private GroupDto group;

  private PermissionIndexerDao underTest = new PermissionIndexerDao();

  @Before
  public void setUp() {
    organization = dbTester.organizations().insert();
    publicProject = componentDbTester.insertPublicProject(organization);
    privateProject1 = componentDbTester.insertPrivateProject(organization);
    privateProject2 = componentDbTester.insertPrivateProject(organization);
    view1 = componentDbTester.insertView(organization);
    view2 = componentDbTester.insertView(organization);
    application = componentDbTester.insertApplication(organization);
    user1 = userDbTester.insertUser();
    user2 = userDbTester.insertUser();
    group = userDbTester.insertGroup(organization);
  }

  @Test
  public void select_all() {
    insertTestDataForProjectsAndViews();

    Collection<IndexPermissions> dtos = underTest.selectAll(dbClient, dbSession);
    Assertions.assertThat(dtos).hasSize(6);

    IndexPermissions publicProjectAuthorization = getByProjectUuid(publicProject.uuid(), dtos);
    isPublic(publicProjectAuthorization, PROJECT);

    IndexPermissions view1Authorization = getByProjectUuid(view1.uuid(), dtos);
    isPublic(view1Authorization, VIEW);

    IndexPermissions applicationAuthorization = getByProjectUuid(application.uuid(), dtos);
    isPublic(applicationAuthorization, APP);

    IndexPermissions privateProject1Authorization = getByProjectUuid(privateProject1.uuid(), dtos);
    assertThat(privateProject1Authorization.getGroupIds()).containsOnly(group.getId());
    assertThat(privateProject1Authorization.isAllowAnyone()).isFalse();
    assertThat(privateProject1Authorization.getUserIds()).containsOnly(user1.getId(), user2.getId());
    assertThat(privateProject1Authorization.getQualifier()).isEqualTo(PROJECT);

    IndexPermissions privateProject2Authorization = getByProjectUuid(privateProject2.uuid(), dtos);
    assertThat(privateProject2Authorization.getGroupIds()).isEmpty();
    assertThat(privateProject2Authorization.isAllowAnyone()).isFalse();
    assertThat(privateProject2Authorization.getUserIds()).containsOnly(user1.getId());
    assertThat(privateProject2Authorization.getQualifier()).isEqualTo(PROJECT);

    IndexPermissions view2Authorization = getByProjectUuid(view2.uuid(), dtos);
    isPublic(view2Authorization, VIEW);
  }

  @Test
  public void selectByUuids() {
    insertTestDataForProjectsAndViews();

    Map<String, IndexPermissions> dtos = underTest
      .selectByUuids(dbClient, dbSession, asList(publicProject.uuid(), privateProject1.uuid(), privateProject2.uuid(), view1.uuid(), view2.uuid(), application.uuid()))
      .stream()
      .collect(MoreCollectors.uniqueIndex(IndexPermissions::getProjectUuid, Function.identity()));
    Assertions.assertThat(dtos).hasSize(6);

    IndexPermissions publicProjectAuthorization = dtos.get(publicProject.uuid());
    isPublic(publicProjectAuthorization, PROJECT);

    IndexPermissions view1Authorization = dtos.get(view1.uuid());
    isPublic(view1Authorization, VIEW);

    IndexPermissions applicationAuthorization = dtos.get(application.uuid());
    isPublic(applicationAuthorization, APP);

    IndexPermissions privateProject1Authorization = dtos.get(privateProject1.uuid());
    assertThat(privateProject1Authorization.getGroupIds()).containsOnly(group.getId());
    assertThat(privateProject1Authorization.isAllowAnyone()).isFalse();
    assertThat(privateProject1Authorization.getUserIds()).containsOnly(user1.getId(), user2.getId());
    assertThat(privateProject1Authorization.getQualifier()).isEqualTo(PROJECT);

    IndexPermissions privateProject2Authorization = dtos.get(privateProject2.uuid());
    assertThat(privateProject2Authorization.getGroupIds()).isEmpty();
    assertThat(privateProject2Authorization.isAllowAnyone()).isFalse();
    assertThat(privateProject2Authorization.getUserIds()).containsOnly(user1.getId());
    assertThat(privateProject2Authorization.getQualifier()).isEqualTo(PROJECT);

    IndexPermissions view2Authorization = dtos.get(view2.uuid());
    isPublic(view2Authorization, VIEW);
  }

  @Test
  public void selectByUuids_returns_empty_list_when_project_does_not_exist() {
    insertTestDataForProjectsAndViews();

    List<IndexPermissions> dtos = underTest.selectByUuids(dbClient, dbSession, asList("missing"));
    Assertions.assertThat(dtos).isEmpty();
  }

  @Test
  public void select_by_projects_with_high_number_of_projects() {
    List<String> projectUuids = new ArrayList<>();
    for (int i = 0; i < 350; i++) {
      ComponentDto project = ComponentTesting.newPrivateProjectDto(organization, Integer.toString(i));
      dbClient.componentDao().insert(dbSession, project);
      projectUuids.add(project.uuid());
      GroupPermissionDto dto = new GroupPermissionDto()
        .setOrganizationUuid(group.getOrganizationUuid())
        .setGroupId(group.getId())
        .setRole(USER)
        .setResourceId(project.getId());
      dbClient.groupPermissionDao().insert(dbSession, dto);
    }
    dbSession.commit();

    assertThat(underTest.selectByUuids(dbClient, dbSession, projectUuids))
      .hasSize(350)
      .extracting(IndexPermissions::getProjectUuid)
      .containsAll(projectUuids);
  }

  @Test
  public void return_private_project_without_any_permission_when_no_permission_in_DB() {
    List<IndexPermissions> dtos = underTest.selectByUuids(dbClient, dbSession, singletonList(privateProject1.uuid()));

    // no permissions
    Assertions.assertThat(dtos).hasSize(1);
    IndexPermissions dto = dtos.get(0);
    assertThat(dto.getGroupIds()).isEmpty();
    assertThat(dto.getUserIds()).isEmpty();
    assertThat(dto.isAllowAnyone()).isFalse();
    assertThat(dto.getProjectUuid()).isEqualTo(privateProject1.uuid());
    assertThat(dto.getQualifier()).isEqualTo(privateProject1.qualifier());
  }

  @Test
  public void return_public_project_with_only_AllowAnyone_true_when_no_permission_in_DB() {
    List<IndexPermissions> dtos = underTest.selectByUuids(dbClient, dbSession, singletonList(publicProject.uuid()));

    Assertions.assertThat(dtos).hasSize(1);
    IndexPermissions dto = dtos.get(0);
    assertThat(dto.getGroupIds()).isEmpty();
    assertThat(dto.getUserIds()).isEmpty();
    assertThat(dto.isAllowAnyone()).isTrue();
    assertThat(dto.getProjectUuid()).isEqualTo(publicProject.uuid());
    assertThat(dto.getQualifier()).isEqualTo(publicProject.qualifier());
  }

  @Test
  public void return_private_project_with_AllowAnyone_false_and_user_id_when_user_is_granted_USER_permission_directly() {
    dbTester.users().insertProjectPermissionOnUser(user1, USER, privateProject1);
    List<IndexPermissions> dtos = underTest.selectByUuids(dbClient, dbSession, singletonList(privateProject1.uuid()));

    Assertions.assertThat(dtos).hasSize(1);
    IndexPermissions dto = dtos.get(0);
    assertThat(dto.getGroupIds()).isEmpty();
    assertThat(dto.getUserIds()).containsOnly(user1.getId());
    assertThat(dto.isAllowAnyone()).isFalse();
    assertThat(dto.getProjectUuid()).isEqualTo(privateProject1.uuid());
    assertThat(dto.getQualifier()).isEqualTo(privateProject1.qualifier());
  }

  @Test
  public void return_private_project_with_AllowAnyone_false_and_group_id_but_not_user_id_when_user_is_granted_USER_permission_through_group() {
    dbTester.users().insertMember(group, user1);
    dbTester.users().insertProjectPermissionOnGroup(group, USER, privateProject1);
    List<IndexPermissions> dtos = underTest.selectByUuids(dbClient, dbSession, singletonList(privateProject1.uuid()));

    Assertions.assertThat(dtos).hasSize(1);
    IndexPermissions dto = dtos.get(0);
    assertThat(dto.getGroupIds()).containsOnly(group.getId());
    assertThat(dto.getUserIds()).isEmpty();
    assertThat(dto.isAllowAnyone()).isFalse();
    assertThat(dto.getProjectUuid()).isEqualTo(privateProject1.uuid());
    assertThat(dto.getQualifier()).isEqualTo(privateProject1.qualifier());
  }

  private void isPublic(IndexPermissions view1Authorization, String qualifier) {
    assertThat(view1Authorization.getGroupIds()).isEmpty();
    assertThat(view1Authorization.isAllowAnyone()).isTrue();
    assertThat(view1Authorization.getUserIds()).isEmpty();
    assertThat(view1Authorization.getQualifier()).isEqualTo(qualifier);
  }

  private static IndexPermissions getByProjectUuid(String projectUuid, Collection<IndexPermissions> dtos) {
    return dtos.stream().filter(dto -> dto.getProjectUuid().equals(projectUuid)).findFirst().orElseThrow(IllegalArgumentException::new);
  }

  private void insertTestDataForProjectsAndViews() {
    // user1 has USER access on both private projects
    userDbTester.insertProjectPermissionOnUser(user1, ADMIN, publicProject);
    userDbTester.insertProjectPermissionOnUser(user1, USER, privateProject1);
    userDbTester.insertProjectPermissionOnUser(user1, USER, privateProject2);
    userDbTester.insertProjectPermissionOnUser(user1, ADMIN, view1);
    userDbTester.insertProjectPermissionOnUser(user1, ADMIN, application);

    // user2 has USER access on privateProject1 only
    userDbTester.insertProjectPermissionOnUser(user2, USER, privateProject1);
    userDbTester.insertProjectPermissionOnUser(user2, ADMIN, privateProject2);

    // group1 has USER access on privateProject1 only
    userDbTester.insertProjectPermissionOnGroup(group, USER, privateProject1);
    userDbTester.insertProjectPermissionOnGroup(group, ADMIN, privateProject1);
    userDbTester.insertProjectPermissionOnGroup(group, ADMIN, view1);
    userDbTester.insertProjectPermissionOnGroup(group, ADMIN, application);
  }
}
