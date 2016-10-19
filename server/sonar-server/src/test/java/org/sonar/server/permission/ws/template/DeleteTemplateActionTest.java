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

import java.util.Collections;
import java.util.Date;
import javax.annotation.Nullable;
import org.junit.Before;
import org.junit.Test;
import org.sonar.api.web.UserRole;
import org.sonar.core.permission.GlobalPermissions;
import org.sonar.db.permission.template.PermissionTemplateCharacteristicDto;
import org.sonar.db.permission.template.PermissionTemplateDto;
import org.sonar.db.user.GroupDto;
import org.sonar.db.user.GroupTesting;
import org.sonar.db.user.UserDto;
import org.sonar.db.user.UserTesting;
import org.sonar.server.exceptions.BadRequestException;
import org.sonar.server.exceptions.ForbiddenException;
import org.sonar.server.exceptions.NotFoundException;
import org.sonar.server.exceptions.UnauthorizedException;
import org.sonar.server.permission.ws.BasePermissionWsTest;
import org.sonar.server.ws.TestRequest;
import org.sonar.server.ws.TestResponse;

import static com.google.common.primitives.Longs.asList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.mockito.internal.util.collections.Sets.newSet;
import static org.sonarqube.ws.client.permission.PermissionsWsParameters.PARAM_TEMPLATE_ID;
import static org.sonarqube.ws.client.permission.PermissionsWsParameters.PARAM_TEMPLATE_NAME;

public class DeleteTemplateActionTest extends BasePermissionWsTest<DeleteTemplateAction> {

  private DefaultPermissionTemplateFinder defaultTemplatePermissionFinder = mock(DefaultPermissionTemplateFinder.class);
  private PermissionTemplateDto template;

  @Override
  protected DeleteTemplateAction buildWsAction() {
    return new DeleteTemplateAction(db.getDbClient(), userSession, newPermissionWsSupport(), defaultTemplatePermissionFinder);
  }

  @Before
  public void setUp() {
    loginAsAdminOnDefaultOrganization();
    when(defaultTemplatePermissionFinder.getDefaultTemplateUuids()).thenReturn(Collections.emptySet());
    template = insertTemplateAndAssociatedPermissions();
  }

  @Test
  public void delete_template_in_db() throws Exception {
    TestResponse result = newRequest(template.getUuid());

    assertThat(result.getInput()).isEmpty();
    assertThat(db.getDbClient().permissionTemplateDao().selectByUuid(db.getSession(), template.getUuid())).isNull();
  }

  @Test
  public void delete_template_by_name_case_insensitive() throws Exception {
    newRequest()
      .setParam(PARAM_TEMPLATE_NAME, template.getName().toUpperCase())
      .execute();

    assertThat(db.getDbClient().permissionTemplateDao().selectByUuid(db.getSession(), template.getUuid())).isNull();
  }

  @Test
  public void fail_if_uuid_is_not_known() throws Exception {
    expectedException.expect(NotFoundException.class);

    newRequest("unknown-template-uuid");
  }

  @Test
  public void fail_if_template_is_default() throws Exception {
    when(defaultTemplatePermissionFinder.getDefaultTemplateUuids()).thenReturn(newSet(template.getUuid()));

    expectedException.expect(BadRequestException.class);
    expectedException.expectMessage("It is not possible to delete a default template");

    newRequest(template.getUuid());
  }

  @Test
  public void fail_if_not_logged_in() throws Exception {
    expectedException.expect(UnauthorizedException.class);
    userSession.anonymous();

    newRequest(template.getUuid());
  }

  @Test
  public void fail_if_not_admin() throws Exception {
    expectedException.expect(ForbiddenException.class);
    userSession.login().setGlobalPermissions(GlobalPermissions.QUALITY_PROFILE_ADMIN);

    newRequest(template.getUuid());
  }

  @Test
  public void fail_if_uuid_is_not_provided() throws Exception {
    expectedException.expect(BadRequestException.class);

    newRequest(null);
  }

  @Test
  public void delete_perm_tpl_characteristic_when_delete_template() throws Exception {
    db.getDbClient().permissionTemplateCharacteristicDao().insert(db.getSession(), new PermissionTemplateCharacteristicDto()
      .setPermission(UserRole.USER)
      .setTemplateId(template.getId())
      .setWithProjectCreator(true)
      .setCreatedAt(new Date().getTime())
      .setUpdatedAt(new Date().getTime()));
    db.commit();

    newRequest(template.getUuid());

    assertThat(db.getDbClient().permissionTemplateCharacteristicDao().selectByTemplateIds(db.getSession(), asList(template.getId()))).isEmpty();
  }

  private PermissionTemplateDto insertTemplateAndAssociatedPermissions() {
    PermissionTemplateDto dto = addTemplateToDefaultOrganization();
    UserDto user = db.getDbClient().userDao().insert(db.getSession(), UserTesting.newUserDto().setActive(true));
    GroupDto group = db.getDbClient().groupDao().insert(db.getSession(), GroupTesting.newGroupDto());
    db.getDbClient().permissionTemplateDao().insertUserPermission(db.getSession(), dto.getId(), user.getId(), UserRole.ADMIN);
    db.getDbClient().permissionTemplateDao().insertGroupPermission(db.getSession(), dto.getId(), group.getId(), UserRole.CODEVIEWER);
    db.commit();
    return dto;
  }

  private TestResponse newRequest(@Nullable String id) throws Exception {
    TestRequest request = newRequest();
    if (id != null) {
      request.setParam(PARAM_TEMPLATE_ID, id);
    }

    return request.execute();
  }

}
