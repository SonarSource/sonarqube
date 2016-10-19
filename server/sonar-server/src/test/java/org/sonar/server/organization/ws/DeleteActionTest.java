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
package org.sonar.server.organization.ws;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.sonar.api.server.ws.WebService;
import org.sonar.api.utils.System2;
import org.sonar.db.DbTester;
import org.sonar.db.organization.OrganizationDto;
import org.sonar.db.permission.template.PermissionTemplateDto;
import org.sonar.db.user.GroupDto;
import org.sonar.db.user.UserDto;
import org.sonar.server.exceptions.ForbiddenException;
import org.sonar.server.exceptions.NotFoundException;
import org.sonar.server.exceptions.UnauthorizedException;
import org.sonar.server.organization.TestDefaultOrganizationProvider;
import org.sonar.server.tester.UserSessionRule;
import org.sonar.server.ws.WsActionTester;

import static org.assertj.core.api.Assertions.assertThat;
import static org.sonar.core.permission.GlobalPermissions.SYSTEM_ADMIN;
import static org.sonar.server.organization.ws.OrganizationsWsSupport.PARAM_KEY;

public class DeleteActionTest {

  @Rule
  public DbTester dbTester = DbTester.create(System2.INSTANCE);
  @Rule
  public UserSessionRule userSession = UserSessionRule.standalone();
  @Rule
  public ExpectedException expectedException = ExpectedException.none();

  private DeleteAction underTest = new DeleteAction(userSession, dbTester.getDbClient(), TestDefaultOrganizationProvider.from(dbTester));
  private WsActionTester wsTester = new WsActionTester(underTest);

  @Test
  public void verify_define() {
    WebService.Action action = wsTester.getDef();
    assertThat(action.key()).isEqualTo("delete");
    assertThat(action.isPost()).isTrue();
    assertThat(action.description()).isEqualTo("Delete an organization.<br/>" +
      "Require 'Administer System' permission on the specified organization.");
    assertThat(action.isInternal()).isTrue();
    assertThat(action.since()).isEqualTo("6.2");
    assertThat(action.handler()).isEqualTo(underTest);
    assertThat(action.params()).hasSize(1);
    assertThat(action.responseExample()).isNull();

    assertThat(action.param("key"))
      .matches(param -> param.isRequired())
      .matches(param -> "foo-company".equals(param.exampleValue()))
      .matches(param -> "Organization key".equals(param.description()));
  }

  @Test
  public void request_fails_with_UnauthorizedException_if_user_is_not_logged_in() {
    expectedException.expect(UnauthorizedException.class);
    expectedException.expectMessage("Authentication is required");

    wsTester.newRequest()
      .execute();
  }

  @Test
  public void request_fails_with_IAE_if_key_param_is_missing() {
    userSession.login();

    expectedException.expect(IllegalArgumentException.class);
    expectedException.expectMessage("The 'key' parameter is missing");

    wsTester.newRequest()
      .execute();
  }

  @Test
  public void request_fails_with_IAE_if_key_is_the_one_of_default_organization() {
    userSession.login();

    expectedException.expect(IllegalArgumentException.class);
    expectedException.expectMessage("Default Organization can't be deleted");

    sendRequest(dbTester.getDefaultOrganization());
  }

  @Test
  public void request_fails_with_NotFoundException_if_organization_with_specified_key_does_not_exist() {
    userSession.login();

    expectedException.expect(NotFoundException.class);
    expectedException.expectMessage("Organization with key 'foo' not found");

    sendRequest("foo");
  }

  @Test
  public void request_fails_with_ForbiddenException_when_user_has_no_System_Administer_permission() {
    OrganizationDto organization = dbTester.organizations().insert();
    userSession.login();

    expectedException.expect(ForbiddenException.class);
    expectedException.expectMessage("Insufficient privileges");

    sendRequest(organization);
  }

  @Test
  public void request_fails_with_ForbiddenException_when_user_does_not_have_System_Administer_permission_on_specified_organization() {
    OrganizationDto organization = dbTester.organizations().insert();
    userSession.login().addOrganizationPermission(dbTester.getDefaultOrganization().getUuid(), SYSTEM_ADMIN);

    expectedException.expect(ForbiddenException.class);
    expectedException.expectMessage("Insufficient privileges");

    sendRequest(organization);
  }

  @Test
  public void request_deletes_specified_organization_if_exists_and_user_has_Admin_permission_on_it() {
    OrganizationDto organization = dbTester.organizations().insert();
    userSession.login().addOrganizationPermission(organization.getUuid(), SYSTEM_ADMIN);

    sendRequest(organization);

    verifyOrganizationDoesNotExist(organization);
  }

  @Test
  public void request_deletes_specified_organization_if_exists_and_user_is_root() {
    OrganizationDto organization = dbTester.organizations().insert();
    userSession.login().setRoot();

    sendRequest(organization);

    verifyOrganizationDoesNotExist(organization);
  }

  private void verifyOrganizationDoesNotExist(OrganizationDto organization) {
    assertThat(dbTester.getDbClient().organizationDao().selectByKey(dbTester.getSession(), organization.getKey()))
      .isEmpty();
  }

  private void sendRequest(OrganizationDto organization) {
    sendRequest(organization.getKey());
  }

  private void sendRequest(String organizationKey) {
    wsTester.newRequest()
      .setParam(PARAM_KEY, organizationKey)
      .execute();
  }
}
