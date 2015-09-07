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

package org.sonar.server.permission.ws.template;

import com.google.common.base.Function;
import java.util.List;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.rules.ExpectedException;
import org.sonar.api.utils.System2;
import org.sonar.core.permission.GlobalPermissions;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.db.DbTester;
import org.sonar.db.permission.GroupWithPermissionDto;
import org.sonar.db.permission.PermissionQuery;
import org.sonar.db.permission.PermissionTemplateDto;
import org.sonar.db.user.GroupDto;
import org.sonar.server.component.ComponentFinder;
import org.sonar.server.exceptions.BadRequestException;
import org.sonar.server.exceptions.ForbiddenException;
import org.sonar.server.exceptions.NotFoundException;
import org.sonar.server.exceptions.UnauthorizedException;
import org.sonar.server.permission.ws.PermissionDependenciesFinder;
import org.sonar.server.permission.ws.WsPermissionParameters;
import org.sonar.server.tester.UserSessionRule;
import org.sonar.server.ws.TestRequest;
import org.sonar.server.ws.WsActionTester;
import org.sonar.test.DbTests;

import static com.google.common.collect.FluentIterable.from;
import static org.assertj.core.api.Assertions.assertThat;
import static org.sonar.api.security.DefaultGroups.ANYONE;
import static org.sonar.api.web.UserRole.CODEVIEWER;
import static org.sonar.db.permission.PermissionTemplateTesting.newPermissionTemplateDto;
import static org.sonar.db.user.GroupMembershipQuery.IN;
import static org.sonar.db.user.GroupTesting.newGroupDto;
import static org.sonar.server.permission.ws.WsPermissionParameters.PARAM_GROUP_ID;
import static org.sonar.server.permission.ws.WsPermissionParameters.PARAM_GROUP_NAME;
import static org.sonar.server.permission.ws.WsPermissionParameters.PARAM_PERMISSION;
import static org.sonar.server.permission.ws.WsPermissionParameters.PARAM_TEMPLATE_ID;
import static org.sonar.server.permission.ws.WsPermissionParameters.PARAM_TEMPLATE_NAME;

@Category(DbTests.class)
public class RemoveGroupFromTemplateActionTest {

  private static final String GROUP_NAME = "group-name";
  private static final String PERMISSION = CODEVIEWER;
  @Rule
  public DbTester db = DbTester.create(System2.INSTANCE);
  @Rule
  public ExpectedException expectedException = ExpectedException.none();
  @Rule
  public UserSessionRule userSession = UserSessionRule.standalone();

  WsActionTester ws;
  DbClient dbClient;
  DbSession dbSession;
  GroupDto group;
  PermissionTemplateDto permissionTemplate;

  @Before
  public void setUp() {
    dbClient = db.getDbClient();
    dbSession = db.getSession();
    userSession.login().setGlobalPermissions(GlobalPermissions.SYSTEM_ADMIN);

    PermissionDependenciesFinder dependenciesFinder = new PermissionDependenciesFinder(dbClient, new ComponentFinder(dbClient));
    ws = new WsActionTester(new RemoveGroupFromTemplateAction(dbClient, dependenciesFinder, userSession));

    group = insertGroup(newGroupDto().setName(GROUP_NAME));
    permissionTemplate = insertPermissionTemplate(newPermissionTemplateDto());
    addGroupToPermissionTemplate(permissionTemplate.getId(), group.getId(), PERMISSION);
    commit();
  }

  @Test
  public void remove_group_from_template() {
    assertThat(getGroupNamesInTemplateAndPermission(permissionTemplate.getId(), PERMISSION)).containsExactly(GROUP_NAME);
    commit();

    newRequest(GROUP_NAME, permissionTemplate.getUuid(), PERMISSION);

    assertThat(getGroupNamesInTemplateAndPermission(permissionTemplate.getId(), PERMISSION)).isEmpty();
  }

  @Test
  public void remove_group_from_template_by_name_case_insensitive() {
    assertThat(getGroupNamesInTemplateAndPermission(permissionTemplate.getId(), PERMISSION)).containsExactly(GROUP_NAME);
    commit();

    ws.newRequest()
      .setParam(PARAM_GROUP_NAME, GROUP_NAME)
      .setParam(PARAM_PERMISSION, PERMISSION)
      .setParam(PARAM_TEMPLATE_NAME, permissionTemplate.getName().toUpperCase())
      .execute();
    commit();

    assertThat(getGroupNamesInTemplateAndPermission(permissionTemplate.getId(), PERMISSION)).isEmpty();
  }

  @Test
  public void remove_group_with_group_id() {
    assertThat(getGroupNamesInTemplateAndPermission(permissionTemplate.getId(), PERMISSION)).containsExactly(GROUP_NAME);
    commit();

    ws.newRequest()
      .setParam(PARAM_TEMPLATE_ID, permissionTemplate.getUuid())
      .setParam(PARAM_PERMISSION, PERMISSION)
      .setParam(PARAM_GROUP_ID, String.valueOf(group.getId()))
      .execute();

    assertThat(getGroupNamesInTemplateAndPermission(permissionTemplate.getId(), PERMISSION)).isEmpty();
  }

  @Test
  public void remove_group_twice_without_error() {
    newRequest(GROUP_NAME, permissionTemplate.getUuid(), PERMISSION);
    newRequest(GROUP_NAME, permissionTemplate.getUuid(), PERMISSION);

    assertThat(getGroupNamesInTemplateAndPermission(permissionTemplate.getId(), PERMISSION)).isEmpty();
  }

  @Test
  public void remove_anyone_group_from_template() {
    addGroupToPermissionTemplate(permissionTemplate.getId(), null, PERMISSION);
    commit();

    newRequest(ANYONE, permissionTemplate.getUuid(), PERMISSION);

    assertThat(getGroupNamesInTemplateAndPermission(permissionTemplate.getId(), PERMISSION)).containsExactly(GROUP_NAME);
  }

  @Test
  public void fail_if_not_a_project_permission() {
    expectedException.expect(BadRequestException.class);

    newRequest(GROUP_NAME, permissionTemplate.getUuid(), GlobalPermissions.PREVIEW_EXECUTION);
  }

  @Test
  public void fail_if_insufficient_privileges() {
    expectedException.expect(ForbiddenException.class);
    userSession.setGlobalPermissions(GlobalPermissions.QUALITY_PROFILE_ADMIN);

    newRequest(GROUP_NAME, permissionTemplate.getUuid(), PERMISSION);
  }

  @Test
  public void fail_if_not_logged_in() {
    expectedException.expect(UnauthorizedException.class);
    userSession.anonymous();

    newRequest(GROUP_NAME, permissionTemplate.getUuid(), PERMISSION);
  }

  @Test
  public void fail_if_group_params_missing() {
    expectedException.expect(BadRequestException.class);

    newRequest(null, permissionTemplate.getUuid(), PERMISSION);
  }

  @Test
  public void fail_if_permission_missing() {
    expectedException.expect(IllegalArgumentException.class);

    newRequest(GROUP_NAME, permissionTemplate.getUuid(), null);
  }

  @Test
  public void fail_if_template_missing() {
    expectedException.expect(BadRequestException.class);

    newRequest(GROUP_NAME, null, PERMISSION);
  }

  @Test
  public void fail_if_group_does_not_exist() {
    expectedException.expect(NotFoundException.class);
    expectedException.expectMessage("Group with name 'unknown-group-name' is not found");

    newRequest("unknown-group-name", permissionTemplate.getUuid(), PERMISSION);
  }

  @Test
  public void fail_if_template_key_does_not_exist() {
    expectedException.expect(NotFoundException.class);
    expectedException.expectMessage("Permission template with id 'unknown-key' is not found");

    newRequest(GROUP_NAME, "unknown-key", PERMISSION);
  }

  private void newRequest(@Nullable String groupName, @Nullable String templateKey, @Nullable String permission) {
    TestRequest request = ws.newRequest();
    if (groupName != null) {
      request.setParam(WsPermissionParameters.PARAM_GROUP_NAME, groupName);
    }
    if (templateKey != null) {
      request.setParam(PARAM_TEMPLATE_ID, templateKey);
    }
    if (permission != null) {
      request.setParam(WsPermissionParameters.PARAM_PERMISSION, permission);
    }

    request.execute();
  }

  private void commit() {
    dbSession.commit();
  }

  private GroupDto insertGroup(GroupDto groupDto) {
    return dbClient.groupDao().insert(dbSession, groupDto);
  }

  private PermissionTemplateDto insertPermissionTemplate(PermissionTemplateDto permissionTemplate) {
    return dbClient.permissionTemplateDao().insert(dbSession, permissionTemplate);
  }

  private void addGroupToPermissionTemplate(long permissionTemplateId, @Nullable Long groupId, String permission) {
    dbClient.permissionTemplateDao().insertGroupPermission(dbSession, permissionTemplateId, groupId, permission);
  }

  private List<String> getGroupNamesInTemplateAndPermission(long templateId, String permission) {
    PermissionQuery permissionQuery = PermissionQuery.builder().permission(permission).membership(IN).build();
    return from(dbClient.permissionTemplateDao()
      .selectGroups(dbSession, permissionQuery, templateId))
      .transform(GroupWithPermissionToGroupName.INSTANCE)
      .toList();
  }

  private enum GroupWithPermissionToGroupName implements Function<GroupWithPermissionDto, String> {
    INSTANCE;

    @Override
    public String apply(@Nonnull GroupWithPermissionDto groupWithPermission) {
      return groupWithPermission.getName();
    }

  }
}
