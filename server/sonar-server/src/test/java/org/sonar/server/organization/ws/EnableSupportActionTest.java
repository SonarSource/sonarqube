/*
 * SonarQube
 * Copyright (C) 2009-2017 SonarSource SA
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
package org.sonar.server.organization.ws;

import java.net.HttpURLConnection;
import java.util.Optional;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.sonar.api.server.ws.WebService;
import org.sonar.db.DbTester;
import org.sonar.db.organization.OrganizationDto;
import org.sonar.db.user.GroupDto;
import org.sonar.db.user.UserDto;
import org.sonar.server.exceptions.ForbiddenException;
import org.sonar.server.exceptions.UnauthorizedException;
import org.sonar.server.organization.DefaultOrganizationProvider;
import org.sonar.server.organization.OrganizationFlags;
import org.sonar.server.organization.OrganizationFlagsImpl;
import org.sonar.server.organization.TestDefaultOrganizationProvider;
import org.sonar.server.tester.UserSessionRule;
import org.sonar.server.ws.TestResponse;
import org.sonar.server.ws.WsActionTester;

import static org.assertj.core.api.Assertions.assertThat;
import static org.sonar.db.permission.OrganizationPermission.ADMINISTER;

public class EnableSupportActionTest {

  @Rule
  public ExpectedException expectedException = ExpectedException.none();
  @Rule
  public UserSessionRule userSession = UserSessionRule.standalone();
  @Rule
  public DbTester db = DbTester.create();

  private DefaultOrganizationProvider defaultOrganizationProvider = TestDefaultOrganizationProvider.from(db);
  private OrganizationFlags organizationFlags = new OrganizationFlagsImpl(db.getDbClient());
  private EnableSupportAction underTest = new EnableSupportAction(userSession, db.getDbClient(), defaultOrganizationProvider, organizationFlags);
  private WsActionTester tester = new WsActionTester(underTest);

  @Test
  public void enabling_support_saves_internal_property_and_flags_caller_as_root() {
    UserDto user = db.users().insertUser();
    UserDto otherUser = db.users().insertUser();
    verifyFeatureEnabled(false);
    verifyRoot(user, false);
    verifyRoot(otherUser, false);
    logInAsSystemAdministrator(user.getLogin());

    call();

    verifyFeatureEnabled(true);
    verifyRoot(user, true);
    verifyRoot(otherUser, false);
  }

  @Test
  public void enabling_support_creates_default_members_group_and_associate_org_members() throws Exception {
    OrganizationDto defaultOrganization = db.getDefaultOrganization();
    OrganizationDto anotherOrganization = db.organizations().insert();
    UserDto user1 = db.users().insertUser();
    UserDto user2 = db.users().insertUser();
    UserDto userInAnotherOrganization = db.users().insertUser();
    db.organizations().addMember(defaultOrganization, user1);
    db.organizations().addMember(defaultOrganization, user2);
    db.organizations().addMember(anotherOrganization, userInAnotherOrganization);
    logInAsSystemAdministrator(user1.getLogin());

    call();

    Optional<Integer> defaultGroupId = db.getDbClient().organizationDao().getDefaultGroupId(db.getSession(), defaultOrganization.getUuid());
    assertThat(defaultGroupId).isPresent();
    GroupDto membersGroup = db.getDbClient().groupDao().selectById(db.getSession(), defaultGroupId.get());
    assertThat(membersGroup).isNotNull();
    assertThat(membersGroup.getName()).isEqualTo("Members");
    assertThat(db.getDbClient().groupMembershipDao().selectGroupIdsByUserId(db.getSession(), user1.getId())).containsOnly(defaultGroupId.get());
    assertThat(db.getDbClient().groupMembershipDao().selectGroupIdsByUserId(db.getSession(), user2.getId())).containsOnly(defaultGroupId.get());
    assertThat(db.getDbClient().groupMembershipDao().selectGroupIdsByUserId(db.getSession(), userInAnotherOrganization.getId())).isEmpty();
  }

  @Test
  public void throw_IAE_when_members_group_already_exists() throws Exception {
    UserDto user = db.users().insertUser();
    db.users().insertGroup(db.getDefaultOrganization(), "Members");
    logInAsSystemAdministrator(user.getLogin());

    expectedException.expect(IllegalArgumentException.class);
    expectedException.expectMessage("The group 'Members' already exist");

    call();
  }

  @Test
  public void throw_UnauthorizedException_if_not_logged_in() {
    userSession.anonymous();

    expectedException.expect(UnauthorizedException.class);
    expectedException.expectMessage("Authentication is required");

    call();
  }

  @Test
  public void throw_ForbiddenException_if_not_system_administrator() {
    userSession.logIn();

    expectedException.expect(ForbiddenException.class);
    expectedException.expectMessage("Insufficient privileges");

    call();
  }

  @Test
  public void do_nothing_if_support_is_already_enabled() {
    logInAsSystemAdministrator("foo");

    call();
    verifyFeatureEnabled(true);

    // the test could be improved to verify that
    // the caller user is not flagged as root
    // if he was not already root
    call();
    verifyFeatureEnabled(true);
  }

  @Test
  public void test_definition() {
    WebService.Action def = tester.getDef();
    assertThat(def.key()).isEqualTo("enable_support");
    assertThat(def.isPost()).isTrue();
    assertThat(def.isInternal()).isTrue();
    assertThat(def.params()).isEmpty();
  }

  private void logInAsSystemAdministrator(String login) {
    userSession.logIn(login).addPermission(ADMINISTER, db.getDefaultOrganization());
  }

  private void call() {
    TestResponse response = tester.newRequest().setMethod("POST").execute();
    assertThat(response.getStatus()).isEqualTo(HttpURLConnection.HTTP_NO_CONTENT);
  }

  private void verifyFeatureEnabled(boolean enabled) {
    assertThat(organizationFlags.isEnabled(db.getSession())).isEqualTo(enabled);
  }

  private void verifyRoot(UserDto user, boolean root) {
    db.rootFlag().verify(user.getLogin(), root);
  }
}
