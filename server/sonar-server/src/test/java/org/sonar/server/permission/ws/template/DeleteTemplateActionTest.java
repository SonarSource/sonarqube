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
import org.sonar.server.ws.WsTester;

import static com.google.common.primitives.Longs.asList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.mockito.internal.util.collections.Sets.newSet;
import static org.sonar.db.permission.template.PermissionTemplateTesting.newPermissionTemplateDto;
import static org.sonarqube.ws.client.permission.PermissionsWsParameters.CONTROLLER;
import static org.sonarqube.ws.client.permission.PermissionsWsParameters.PARAM_TEMPLATE_ID;
import static org.sonarqube.ws.client.permission.PermissionsWsParameters.PARAM_TEMPLATE_NAME;

public class DeleteTemplateActionTest extends BasePermissionWsTest<DeleteTemplateAction> {

  private static final String TEMPLATE_UUID = "permission-template-uuid";
  private static final String ACTION = "delete_template";

  private DefaultPermissionTemplateFinder defaultTemplatePermissionFinder = mock(DefaultPermissionTemplateFinder.class);
  private PermissionTemplateDto permissionTemplate;

  @Override
  protected DeleteTemplateAction buildWsAction() {
    return new DeleteTemplateAction(db.getDbClient(), userSession, newPermissionWsSupport(), defaultTemplatePermissionFinder);
  }

  @Before
  public void setUp() {
    userSession.login().setGlobalPermissions(GlobalPermissions.SYSTEM_ADMIN);
    when(defaultTemplatePermissionFinder.getDefaultTemplateUuids()).thenReturn(Collections.emptySet());
    permissionTemplate = insertTemplateAndAssociatedPermissions(newPermissionTemplateDto().setUuid(TEMPLATE_UUID));
  }

  @Test
  public void delete_template_in_db() throws Exception {
    WsTester.Result result = newRequest(TEMPLATE_UUID);

    assertThat(result.outputAsString()).isEmpty();
    assertThat(db.getDbClient().permissionTemplateDao().selectByUuidWithUserAndGroupPermissions(db.getSession(), TEMPLATE_UUID)).isNull();
  }

  @Test
  public void delete_template_by_name_case_insensitive() throws Exception {
    wsTester.newPostRequest(CONTROLLER, ACTION)
      .setParam(PARAM_TEMPLATE_NAME, permissionTemplate.getName().toUpperCase())
      .execute();

    assertThat(db.getDbClient().permissionTemplateDao().selectByUuidWithUserAndGroupPermissions(db.getSession(), TEMPLATE_UUID)).isNull();
  }

  @Test
  public void fail_if_uuid_is_not_known() throws Exception {
    expectedException.expect(NotFoundException.class);

    newRequest("unknown-template-uuid");
  }

  @Test
  public void fail_if_template_is_default() throws Exception {
    expectedException.expect(BadRequestException.class);
    expectedException.expectMessage("It is not possible to delete a default template");
    when(defaultTemplatePermissionFinder.getDefaultTemplateUuids()).thenReturn(newSet(TEMPLATE_UUID));

    newRequest(TEMPLATE_UUID);
  }

  @Test
  public void fail_if_not_logged_in() throws Exception {
    expectedException.expect(UnauthorizedException.class);
    userSession.anonymous();

    newRequest(TEMPLATE_UUID);
  }

  @Test
  public void fail_if_not_admin() throws Exception {
    expectedException.expect(ForbiddenException.class);
    userSession.login().setGlobalPermissions(GlobalPermissions.QUALITY_PROFILE_ADMIN);

    newRequest(TEMPLATE_UUID);
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
      .setTemplateId(permissionTemplate.getId())
      .setWithProjectCreator(true)
      .setCreatedAt(new Date().getTime())
      .setUpdatedAt(new Date().getTime()));
    db.commit();

    newRequest(TEMPLATE_UUID);

    assertThat(db.getDbClient().permissionTemplateCharacteristicDao().selectByTemplateIds(db.getSession(), asList(permissionTemplate.getId()))).isEmpty();
  }

  private PermissionTemplateDto insertTemplateAndAssociatedPermissions(PermissionTemplateDto template) {
    db.getDbClient().permissionTemplateDao().insert(db.getSession(), template);
    UserDto user = db.getDbClient().userDao().insert(db.getSession(), UserTesting.newUserDto().setActive(true));
    GroupDto group = db.getDbClient().groupDao().insert(db.getSession(), GroupTesting.newGroupDto());
    db.getDbClient().permissionTemplateDao().insertUserPermission(db.getSession(), template.getId(), user.getId(), UserRole.ADMIN);
    db.getDbClient().permissionTemplateDao().insertGroupPermission(db.getSession(), template.getId(), group.getId(), UserRole.CODEVIEWER);
    db.commit();

    return template;
  }

  private WsTester.Result newRequest(@Nullable String id) throws Exception {
    WsTester.TestRequest request = wsTester.newPostRequest(CONTROLLER, ACTION);
    if (id != null) {
      request.setParam(PARAM_TEMPLATE_ID, id);
    }

    return request.execute();
  }

}
