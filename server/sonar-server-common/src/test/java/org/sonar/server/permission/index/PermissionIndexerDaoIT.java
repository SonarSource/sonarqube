/*
 * SonarQube
 * Copyright (C) 2009-2025 SonarSource SA
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
import java.util.stream.Collectors;
import org.assertj.core.api.Assertions;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.sonar.api.utils.System2;
import org.sonar.core.util.Uuids;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.db.DbTester;
import org.sonar.db.component.ProjectData;
import org.sonar.db.permission.GroupPermissionDto;
import org.sonar.db.portfolio.PortfolioDto;
import org.sonar.db.project.ProjectDto;
import org.sonar.db.user.GroupDto;
import org.sonar.db.user.UserDbTester;
import org.sonar.db.user.UserDto;

import static java.util.Arrays.asList;
import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.sonar.db.component.ComponentQualifiers.APP;
import static org.sonar.db.component.ComponentQualifiers.PROJECT;
import static org.sonar.db.component.ComponentQualifiers.VIEW;
import static org.sonar.api.web.UserRole.ADMIN;
import static org.sonar.api.web.UserRole.USER;

public class PermissionIndexerDaoIT {

  @Rule
  public DbTester dbTester = DbTester.create(System2.INSTANCE);

  private final DbClient dbClient = dbTester.getDbClient();
  private final DbSession dbSession = dbTester.getSession();
  private final UserDbTester userDbTester = new UserDbTester(dbTester);

  private ProjectDto publicProject;
  private ProjectDto privateProject1;
  private ProjectDto privateProject2;
  private PortfolioDto view1;
  private PortfolioDto view2;
  private ProjectDto application;
  private UserDto user1;
  private UserDto user2;
  private GroupDto group;

  private final PermissionIndexerDao underTest = new PermissionIndexerDao();

  @Before
  public void setUp() {
    publicProject = dbTester.components().insertPublicProject().getProjectDto();
    privateProject1 = dbTester.components().insertPrivateProject().getProjectDto();
    privateProject2 = dbTester.components().insertPrivateProject().getProjectDto();
    view1 = dbTester.components().insertPublicPortfolioDto();
    view2 = dbTester.components().insertPublicPortfolioDto();
    application = dbTester.components().insertPublicApplication().getProjectDto();
    user1 = userDbTester.insertUser();
    user2 = userDbTester.insertUser();
    group = userDbTester.insertGroup();
  }

  @Test
  public void select_all() {
    insertTestDataForProjectsAndViews();

    Collection<IndexPermissions> dtos = underTest.selectAll(dbClient, dbSession);
    Assertions.assertThat(dtos).hasSize(6);

    IndexPermissions publicProjectAuthorization = getByProjectUuid(publicProject.getUuid(), dtos);
    isPublic(publicProjectAuthorization, PROJECT);

    IndexPermissions view1Authorization = getByProjectUuid(view1.getUuid(), dtos);
    isPublic(view1Authorization, VIEW);

    IndexPermissions applicationAuthorization = getByProjectUuid(application.getUuid(), dtos);
    isPublic(applicationAuthorization, APP);

    IndexPermissions privateProject1Authorization = getByProjectUuid(privateProject1.getUuid(), dtos);
    assertThat(privateProject1Authorization.getGroupUuids()).containsOnly(group.getUuid());
    assertThat(privateProject1Authorization.isAllowAnyone()).isFalse();
    assertThat(privateProject1Authorization.getUserUuids()).containsOnly(user1.getUuid(), user2.getUuid());
    assertThat(privateProject1Authorization.getQualifier()).isEqualTo(PROJECT);

    IndexPermissions privateProject2Authorization = getByProjectUuid(privateProject2.getUuid(), dtos);
    assertThat(privateProject2Authorization.getGroupUuids()).isEmpty();
    assertThat(privateProject2Authorization.isAllowAnyone()).isFalse();
    assertThat(privateProject2Authorization.getUserUuids()).containsOnly(user1.getUuid());
    assertThat(privateProject2Authorization.getQualifier()).isEqualTo(PROJECT);

    IndexPermissions view2Authorization = getByProjectUuid(view2.getUuid(), dtos);
    isPublic(view2Authorization, VIEW);
  }

  @Test
  public void selectByUuids() {
    insertTestDataForProjectsAndViews();

    Map<String, IndexPermissions> dtos = underTest
      .selectByUuids(dbClient, dbSession,
        asList(publicProject.getUuid(), privateProject1.getUuid(), privateProject2.getUuid(), view1.getUuid(), view2.getUuid(), application.getUuid()))
      .stream()
      .collect(Collectors.toMap(IndexPermissions::getEntityUuid, Function.identity()));
    Assertions.assertThat(dtos).hasSize(6);

    IndexPermissions publicProjectAuthorization = dtos.get(publicProject.getUuid());
    isPublic(publicProjectAuthorization, PROJECT);

    IndexPermissions view1Authorization = dtos.get(view1.getUuid());
    isPublic(view1Authorization, VIEW);

    IndexPermissions applicationAuthorization = dtos.get(application.getUuid());
    isPublic(applicationAuthorization, APP);

    IndexPermissions privateProject1Authorization = dtos.get(privateProject1.getUuid());
    assertThat(privateProject1Authorization.getGroupUuids()).containsOnly(group.getUuid());
    assertThat(privateProject1Authorization.isAllowAnyone()).isFalse();
    assertThat(privateProject1Authorization.getUserUuids()).containsOnly(user1.getUuid(), user2.getUuid());
    assertThat(privateProject1Authorization.getQualifier()).isEqualTo(PROJECT);

    IndexPermissions privateProject2Authorization = dtos.get(privateProject2.getUuid());
    assertThat(privateProject2Authorization.getGroupUuids()).isEmpty();
    assertThat(privateProject2Authorization.isAllowAnyone()).isFalse();
    assertThat(privateProject2Authorization.getUserUuids()).containsOnly(user1.getUuid());
    assertThat(privateProject2Authorization.getQualifier()).isEqualTo(PROJECT);

    IndexPermissions view2Authorization = dtos.get(view2.getUuid());
    isPublic(view2Authorization, VIEW);
  }

  @Test
  public void selectByUuids_returns_empty_list_when_project_does_not_exist() {
    insertTestDataForProjectsAndViews();

    List<IndexPermissions> dtos = underTest.selectByUuids(dbClient, dbSession, singletonList("missing"));
    Assertions.assertThat(dtos).isEmpty();
  }

  @Test
  public void select_by_projects_with_high_number_of_projects() {
    List<String> projectUuids = new ArrayList<>();
    for (int i = 0; i < 3500; i++) {
      ProjectData project = dbTester.components().insertPrivateProject(Integer.toString(i));
      projectUuids.add(project.projectUuid());
      GroupPermissionDto dto = new GroupPermissionDto()
        .setUuid(Uuids.createFast())
        .setGroupUuid(group.getUuid())
        .setGroupName(group.getName())
        .setRole(USER)
        .setEntityUuid(project.projectUuid())
        .setEntityName(project.getProjectDto().getName());
      dbClient.groupPermissionDao().insert(dbSession, dto, project.getProjectDto(), null);
    }
    dbSession.commit();

    assertThat(underTest.selectByUuids(dbClient, dbSession, projectUuids))
      .hasSize(3500)
      .extracting(IndexPermissions::getEntityUuid)
      .containsAll(projectUuids);
  }

  @Test
  public void return_private_project_without_any_permission_when_no_permission_in_DB() {
    List<IndexPermissions> dtos = underTest.selectByUuids(dbClient, dbSession, singletonList(privateProject1.getUuid()));

    // no permissions
    Assertions.assertThat(dtos).hasSize(1);
    IndexPermissions dto = dtos.get(0);
    assertThat(dto.getGroupUuids()).isEmpty();
    assertThat(dto.getUserUuids()).isEmpty();
    assertThat(dto.isAllowAnyone()).isFalse();
    assertThat(dto.getEntityUuid()).isEqualTo(privateProject1.getUuid());
    assertThat(dto.getQualifier()).isEqualTo(privateProject1.getQualifier());
  }

  @Test
  public void return_public_project_with_only_AllowAnyone_true_when_no_permission_in_DB() {
    List<IndexPermissions> dtos = underTest.selectByUuids(dbClient, dbSession, singletonList(publicProject.getUuid()));

    Assertions.assertThat(dtos).hasSize(1);
    IndexPermissions dto = dtos.get(0);
    assertThat(dto.getGroupUuids()).isEmpty();
    assertThat(dto.getUserUuids()).isEmpty();
    assertThat(dto.isAllowAnyone()).isTrue();
    assertThat(dto.getEntityUuid()).isEqualTo(publicProject.getUuid());
    assertThat(dto.getQualifier()).isEqualTo(publicProject.getQualifier());
  }

  @Test
  public void return_private_project_with_AllowAnyone_false_and_user_id_when_user_is_granted_USER_permission_directly() {
    dbTester.users().insertProjectPermissionOnUser(user1, USER, privateProject1);
    List<IndexPermissions> dtos = underTest.selectByUuids(dbClient, dbSession, singletonList(privateProject1.getUuid()));

    Assertions.assertThat(dtos).hasSize(1);
    IndexPermissions dto = dtos.get(0);
    assertThat(dto.getGroupUuids()).isEmpty();
    assertThat(dto.getUserUuids()).containsOnly(user1.getUuid());
    assertThat(dto.isAllowAnyone()).isFalse();
    assertThat(dto.getEntityUuid()).isEqualTo(privateProject1.getUuid());
    assertThat(dto.getQualifier()).isEqualTo(privateProject1.getQualifier());
  }

  @Test
  public void return_private_project_with_AllowAnyone_false_and_group_id_but_not_user_id_when_user_is_granted_USER_permission_through_group() {
    dbTester.users().insertMember(group, user1);
    dbTester.users().insertEntityPermissionOnGroup(group, USER, privateProject1);
    List<IndexPermissions> dtos = underTest.selectByUuids(dbClient, dbSession, singletonList(privateProject1.getUuid()));

    Assertions.assertThat(dtos).hasSize(1);
    IndexPermissions dto = dtos.get(0);
    assertThat(dto.getGroupUuids()).containsOnly(group.getUuid());
    assertThat(dto.getUserUuids()).isEmpty();
    assertThat(dto.isAllowAnyone()).isFalse();
    assertThat(dto.getEntityUuid()).isEqualTo(privateProject1.getUuid());
    assertThat(dto.getQualifier()).isEqualTo(privateProject1.getQualifier());
  }

  private void isPublic(IndexPermissions view1Authorization, String qualifier) {
    assertThat(view1Authorization.getGroupUuids()).isEmpty();
    assertThat(view1Authorization.isAllowAnyone()).isTrue();
    assertThat(view1Authorization.getUserUuids()).isEmpty();
    assertThat(view1Authorization.getQualifier()).isEqualTo(qualifier);
  }

  private static IndexPermissions getByProjectUuid(String projectUuid, Collection<IndexPermissions> dtos) {
    return dtos.stream().filter(dto -> dto.getEntityUuid().equals(projectUuid)).findFirst().orElseThrow(IllegalArgumentException::new);
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
    userDbTester.insertEntityPermissionOnGroup(group, USER, privateProject1);
    userDbTester.insertEntityPermissionOnGroup(group, ADMIN, privateProject1);
    userDbTester.insertEntityPermissionOnGroup(group, ADMIN, view1);
    userDbTester.insertEntityPermissionOnGroup(group, ADMIN, application);
  }
}
