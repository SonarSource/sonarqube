/*
 * SonarQube
 * Copyright (C) 2009-2024 SonarSource SA
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
import org.sonar.api.resources.Qualifiers;
import org.sonar.api.resources.ResourceTypes;
import org.sonar.api.utils.System2;
import org.sonar.api.web.UserRole;
import org.sonar.core.util.Uuids;
import org.sonar.db.component.ResourceTypesRule;
import org.sonar.db.permission.template.PermissionTemplateCharacteristicDto;
import org.sonar.db.permission.template.PermissionTemplateDto;
import org.sonar.server.exceptions.ForbiddenException;
import org.sonar.server.exceptions.NotFoundException;
import org.sonar.server.permission.PermissionService;
import org.sonar.server.permission.PermissionServiceImpl;
import org.sonar.server.permission.RequestValidator;
import org.sonar.server.permission.ws.BasePermissionWsIT;
import org.sonar.server.permission.ws.WsParameters;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;
import static org.sonar.db.permission.GlobalPermission.ADMINISTER_QUALITY_GATES;
import static org.sonarqube.ws.client.permission.PermissionsWsParameters.PARAM_PERMISSION;
import static org.sonarqube.ws.client.permission.PermissionsWsParameters.PARAM_TEMPLATE_ID;
import static org.sonarqube.ws.client.permission.PermissionsWsParameters.PARAM_TEMPLATE_NAME;

public class AddProjectCreatorToTemplateActionIT extends BasePermissionWsIT<AddProjectCreatorToTemplateAction> {

  private System2 system = spy(System2.INSTANCE);
  private PermissionTemplateDto template;
  private ResourceTypes resourceTypes = new ResourceTypesRule().setRootQualifiers(Qualifiers.PROJECT);
  private PermissionService permissionService = new PermissionServiceImpl(resourceTypes);
  private WsParameters wsParameters = new WsParameters(permissionService);
  private RequestValidator requestValidator = new RequestValidator(permissionService);

  @Override
  protected AddProjectCreatorToTemplateAction buildWsAction() {
    return new AddProjectCreatorToTemplateAction(db.getDbClient(), newPermissionWsSupport(), userSession, system, wsParameters, requestValidator);
  }

  @Before
  public void setUp() {
    template = db.permissionTemplates().insertTemplate();
    when(system.now()).thenReturn(2_000_000_000L);
  }

  @Test
  public void insert_row_when_no_template_permission() {
    loginAsAdmin();

    newRequest()
      .setParam(PARAM_PERMISSION, UserRole.ADMIN)
      .setParam(PARAM_TEMPLATE_ID, template.getUuid())
      .execute();

    assertThatProjectCreatorIsPresentFor(UserRole.ADMIN, template.getUuid());
  }

  @Test
  public void update_row_when_existing_template_permission() {
    loginAsAdmin();
    PermissionTemplateCharacteristicDto characteristic = db.getDbClient().permissionTemplateCharacteristicDao().insert(db.getSession(),
      new PermissionTemplateCharacteristicDto()
        .setUuid(Uuids.createFast())
        .setTemplateUuid(template.getUuid())
        .setPermission(UserRole.USER)
        .setWithProjectCreator(false)
        .setCreatedAt(1_000_000_000L)
        .setUpdatedAt(1_000_000_000L),
      template.getName());
    db.commit();
    when(system.now()).thenReturn(3_000_000_000L);

    newRequest()
      .setParam(PARAM_PERMISSION, UserRole.USER)
      .setParam(PARAM_TEMPLATE_NAME, template.getName())
      .execute();

    assertThatProjectCreatorIsPresentFor(UserRole.USER, template.getUuid());
    PermissionTemplateCharacteristicDto reloaded = reload(characteristic);
    assertThat(reloaded.getCreatedAt()).isEqualTo(1_000_000_000L);
    assertThat(reloaded.getUpdatedAt()).isEqualTo(3_000_000_000L);
  }

  @Test
  public void fail_when_template_does_not_exist() {
    loginAsAdmin();

    assertThatThrownBy(() -> {
      newRequest()
        .setParam(PARAM_PERMISSION, UserRole.ADMIN)
        .setParam(PARAM_TEMPLATE_ID, "42")
        .execute();
    })
      .isInstanceOf(NotFoundException.class);
  }

  @Test
  public void fail_if_permission_is_not_a_project_permission() {
    loginAsAdmin();

    assertThatThrownBy(() -> {
      newRequest()
        .setParam(PARAM_PERMISSION, ADMINISTER_QUALITY_GATES.getKey())
        .setParam(PARAM_TEMPLATE_ID, template.getUuid())
        .execute();
    })
      .isInstanceOf(IllegalArgumentException.class);
  }

  @Test
  public void fail_if_not_admin() {
    userSession.logIn().addPermission(ADMINISTER_QUALITY_GATES);

    assertThatThrownBy(() -> {
      newRequest()
        .setParam(PARAM_PERMISSION, UserRole.ADMIN)
        .setParam(PARAM_TEMPLATE_ID, template.getUuid())
        .execute();
    })
      .isInstanceOf(ForbiddenException.class);
  }

  private void assertThatProjectCreatorIsPresentFor(String permission, String templateUuid) {
    Optional<PermissionTemplateCharacteristicDto> templatePermission = db.getDbClient().permissionTemplateCharacteristicDao().selectByPermissionAndTemplateId(db.getSession(),
      permission,
      templateUuid);
    assertThat(templatePermission).isPresent();
    assertThat(templatePermission.get().getWithProjectCreator()).isTrue();
  }

  private PermissionTemplateCharacteristicDto reload(PermissionTemplateCharacteristicDto characteristic) {
    return db.getDbClient().permissionTemplateCharacteristicDao().selectByPermissionAndTemplateId(db.getSession(), characteristic.getPermission(), characteristic.getTemplateUuid())
      .get();
  }
}
