/*
 * SonarQube
 * Copyright (C) 2009-2018 SonarSource SA
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
package org.sonar.server.permission.ws.template;

import java.util.Optional;
import org.junit.Before;
import org.junit.Test;
import org.sonar.api.utils.System2;
import org.sonar.api.web.UserRole;
import org.sonar.core.permission.GlobalPermissions;
import org.sonar.db.permission.template.PermissionTemplateCharacteristicDto;
import org.sonar.db.permission.template.PermissionTemplateDto;
import org.sonar.server.exceptions.ForbiddenException;
import org.sonar.server.exceptions.NotFoundException;
import org.sonar.server.exceptions.UnauthorizedException;
import org.sonar.server.permission.ws.BasePermissionWsTest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.sonarqube.ws.client.permission.PermissionsWsParameters.PARAM_PERMISSION;
import static org.sonarqube.ws.client.permission.PermissionsWsParameters.PARAM_TEMPLATE_ID;
import static org.sonarqube.ws.client.permission.PermissionsWsParameters.PARAM_TEMPLATE_NAME;

public class RemoveProjectCreatorFromTemplateActionTest extends BasePermissionWsTest<RemoveProjectCreatorFromTemplateAction> {

  private System2 system = mock(System2.class);
  private PermissionTemplateDto template;

  @Override
  protected RemoveProjectCreatorFromTemplateAction buildWsAction() {
    return new RemoveProjectCreatorFromTemplateAction(db.getDbClient(), newPermissionWsSupport(), userSession, system);
  }

  @Before
  public void setUp() {
    loginAsAdmin(db.getDefaultOrganization());
    when(system.now()).thenReturn(2_000_000_000L);
    template = db.permissionTemplates().insertTemplate(db.getDefaultOrganization());
  }

  @Test
  public void update_template_permission() {
    PermissionTemplateCharacteristicDto characteristic = db.getDbClient().permissionTemplateCharacteristicDao().insert(db.getSession(),
      new PermissionTemplateCharacteristicDto()
        .setTemplateId(template.getId())
        .setPermission(UserRole.USER)
        .setWithProjectCreator(false)
        .setCreatedAt(1_000_000_000L)
        .setUpdatedAt(1_000_000_000L));
    db.commit();
    when(system.now()).thenReturn(3_000_000_000L);

    newRequest()
      .setParam(PARAM_PERMISSION, UserRole.USER)
      .setParam(PARAM_TEMPLATE_NAME, template.getName())
      .execute();

    assertWithoutProjectCreatorFor(UserRole.USER);
    PermissionTemplateCharacteristicDto reloaded = reload(characteristic);
    assertThat(reloaded.getCreatedAt()).isEqualTo(1_000_000_000L);
    assertThat(reloaded.getUpdatedAt()).isEqualTo(3_000_000_000L);
  }

  @Test
  public void do_not_fail_when_no_template_permission() {
    newRequest()
      .setParam(PARAM_PERMISSION, UserRole.ADMIN)
      .setParam(PARAM_TEMPLATE_ID, template.getUuid())
      .execute();

    assertNoTemplatePermissionFor(UserRole.ADMIN);
  }

  @Test
  public void fail_when_template_does_not_exist() {
    expectedException.expect(NotFoundException.class);

    newRequest()
      .setParam(PARAM_PERMISSION, UserRole.ADMIN)
      .setParam(PARAM_TEMPLATE_ID, "42")
      .execute();
  }

  @Test
  public void fail_if_permission_is_not_a_project_permission() {
    expectedException.expect(IllegalArgumentException.class);

    newRequest()
      .setParam(PARAM_PERMISSION, GlobalPermissions.QUALITY_GATE_ADMIN)
      .setParam(PARAM_TEMPLATE_ID, template.getUuid())
      .execute();
  }

  @Test
  public void fail_if_not_authenticated() {
    userSession.anonymous();

    expectedException.expect(UnauthorizedException.class);

    newRequest()
      .setParam(PARAM_PERMISSION, UserRole.ADMIN)
      .setParam(PARAM_TEMPLATE_ID, template.getUuid())
      .execute();
  }

  @Test
  public void fail_if_insufficient_privileges() {
    userSession.logIn();

    expectedException.expect(ForbiddenException.class);

    newRequest()
      .setParam(PARAM_PERMISSION, UserRole.ADMIN)
      .setParam(PARAM_TEMPLATE_ID, template.getUuid())
      .execute();
  }

  private void assertWithoutProjectCreatorFor(String permission) {
    Optional<PermissionTemplateCharacteristicDto> templatePermission = db.getDbClient().permissionTemplateCharacteristicDao().selectByPermissionAndTemplateId(db.getSession(),
      permission, template.getId());
    assertThat(templatePermission).isPresent();
    assertThat(templatePermission.get().getWithProjectCreator()).isFalse();
  }

  private void assertNoTemplatePermissionFor(String permission) {
    Optional<PermissionTemplateCharacteristicDto> templatePermission = db.getDbClient().permissionTemplateCharacteristicDao().selectByPermissionAndTemplateId(db.getSession(),
      permission, template.getId());
    assertThat(templatePermission).isNotPresent();
  }

  private PermissionTemplateCharacteristicDto reload(PermissionTemplateCharacteristicDto characteristic) {
    return db.getDbClient().permissionTemplateCharacteristicDao().selectByPermissionAndTemplateId(db.getSession(), characteristic.getPermission(), characteristic.getTemplateId())
      .get();
  }
}
