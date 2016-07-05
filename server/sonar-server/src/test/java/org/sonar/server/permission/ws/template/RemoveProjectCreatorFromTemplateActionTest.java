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

package org.sonar.server.permission.ws.template;

import java.util.Optional;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.sonar.api.utils.System2;
import org.sonar.api.web.UserRole;
import org.sonar.core.permission.GlobalPermissions;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.db.DbTester;
import org.sonar.db.component.ResourceTypesRule;
import org.sonar.db.permission.template.PermissionTemplateDto;
import org.sonar.db.permission.template.PermissionTemplateCharacteristicDao;
import org.sonar.db.permission.template.PermissionTemplateCharacteristicDto;
import org.sonar.db.permission.template.PermissionTemplateCharacteristicMapper;
import org.sonar.server.component.ComponentFinder;
import org.sonar.server.exceptions.BadRequestException;
import org.sonar.server.exceptions.ForbiddenException;
import org.sonar.server.exceptions.NotFoundException;
import org.sonar.server.exceptions.UnauthorizedException;
import org.sonar.server.permission.ws.PermissionDependenciesFinder;
import org.sonar.server.tester.UserSessionRule;
import org.sonar.server.usergroups.ws.UserGroupFinder;
import org.sonar.server.ws.TestRequest;
import org.sonar.server.ws.WsActionTester;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.sonar.db.permission.template.PermissionTemplateTesting.newPermissionTemplateDto;
import static org.sonarqube.ws.client.permission.PermissionsWsParameters.PARAM_PERMISSION;
import static org.sonarqube.ws.client.permission.PermissionsWsParameters.PARAM_TEMPLATE_ID;
import static org.sonarqube.ws.client.permission.PermissionsWsParameters.PARAM_TEMPLATE_NAME;

public class RemoveProjectCreatorFromTemplateActionTest {
  @Rule
  public ExpectedException expectedException = ExpectedException.none();
  @Rule
  public UserSessionRule userSession = UserSessionRule.standalone();
  @Rule
  public DbTester db = DbTester.create(System2.INSTANCE);
  DbClient dbClient = db.getDbClient();
  DbSession dbSession = db.getSession();
  PermissionTemplateCharacteristicDao dao = dbClient.permissionTemplateCharacteristicDao();
  PermissionTemplateCharacteristicMapper mapper = dbSession.getMapper(PermissionTemplateCharacteristicMapper.class);
  ResourceTypesRule resourceTypes = new ResourceTypesRule();
  System2 system = mock(System2.class);

  WsActionTester ws;

  PermissionTemplateDto template;

  @Before
  public void setUp() {
    userSession.login().setGlobalPermissions(GlobalPermissions.SYSTEM_ADMIN);
    when(system.now()).thenReturn(2_000_000_000L);

    ws = new WsActionTester(new RemoveProjectCreatorFromTemplateAction(dbClient,
      new PermissionDependenciesFinder(dbClient, new ComponentFinder(dbClient), new UserGroupFinder(dbClient), resourceTypes), userSession, system));

    template = insertTemplate();
  }

  @Test
  public void update_template_permission() {
    PermissionTemplateCharacteristicDto insertedPermissionTemplate = dbClient.permissionTemplateCharacteristicDao().insert(dbSession, new PermissionTemplateCharacteristicDto()
      .setTemplateId(template.getId())
      .setPermission(UserRole.USER)
      .setWithProjectCreator(false)
      .setCreatedAt(1_000_000_000L)
      .setUpdatedAt(1_000_000_000L));
    db.commit();
    when(system.now()).thenReturn(3_000_000_000L);

    call(ws.newRequest()
      .setParam(PARAM_PERMISSION, UserRole.USER)
      .setParam(PARAM_TEMPLATE_NAME, template.getName()));

    assertWithoutProjectCreatorFor(UserRole.USER);
    PermissionTemplateCharacteristicDto updatedPermissionTemplate = mapper.selectById(insertedPermissionTemplate.getId());
    assertThat(updatedPermissionTemplate.getCreatedAt()).isEqualTo(1_000_000_000L);
    assertThat(updatedPermissionTemplate.getUpdatedAt()).isEqualTo(3_000_000_000L);
  }

  @Test
  public void do_not_fail_when_no_template_permission() {
    call(ws.newRequest()
      .setParam(PARAM_PERMISSION, UserRole.ADMIN)
      .setParam(PARAM_TEMPLATE_ID, template.getUuid()));

    assertNoTemplatePermissionFor(UserRole.ADMIN);
  }

  @Test
  public void fail_when_template_does_not_exist() {
    expectedException.expect(NotFoundException.class);

    call(ws.newRequest()
      .setParam(PARAM_PERMISSION, UserRole.ADMIN)
      .setParam(PARAM_TEMPLATE_ID, "42"));
  }

  @Test
  public void fail_if_permission_is_not_a_project_permission() {
    expectedException.expect(BadRequestException.class);

    call(ws.newRequest()
      .setParam(PARAM_PERMISSION, GlobalPermissions.DASHBOARD_SHARING)
      .setParam(PARAM_TEMPLATE_ID, template.getUuid()));
  }

  @Test
  public void fail_if_not_authenticated() {
    expectedException.expect(UnauthorizedException.class);
    userSession.anonymous();

    call(ws.newRequest()
      .setParam(PARAM_PERMISSION, UserRole.ADMIN)
      .setParam(PARAM_TEMPLATE_ID, template.getUuid()));
  }

  @Test
  public void fail_if_insufficient_privileges() {
    expectedException.expect(ForbiddenException.class);
    userSession.login().setGlobalPermissions(GlobalPermissions.QUALITY_GATE_ADMIN);

    call(ws.newRequest()
      .setParam(PARAM_PERMISSION, UserRole.ADMIN)
      .setParam(PARAM_TEMPLATE_ID, template.getUuid()));
  }

  @Test
  public void ws_metadata() {
    assertThat(ws.getDef().key()).isEqualTo("remove_project_creator_from_template");
    assertThat(ws.getDef().isPost()).isTrue();
  }

  private void assertWithoutProjectCreatorFor(String permission) {
    Optional<PermissionTemplateCharacteristicDto> templatePermission = dao.selectByPermissionAndTemplateId(dbSession, permission, template.getId());
    assertThat(templatePermission).isPresent();
    assertThat(templatePermission.get().getWithProjectCreator()).isFalse();
  }

  private void assertNoTemplatePermissionFor(String permission) {
    Optional<PermissionTemplateCharacteristicDto> templatePermission = dao.selectByPermissionAndTemplateId(dbSession, permission, template.getId());
    assertThat(templatePermission).isNotPresent();
  }

  private PermissionTemplateDto insertTemplate() {
    PermissionTemplateDto template = dbClient.permissionTemplateDao().insert(dbSession, newPermissionTemplateDto());
    db.commit();
    return template;
  }

  private void call(TestRequest request) {
    request.execute();
  }
}
