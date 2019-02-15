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
package org.sonar.server.organization.ws;

import java.util.stream.IntStream;
import javax.annotation.Nullable;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.sonar.api.server.ws.WebService;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.db.DbTester;
import org.sonar.db.organization.OrganizationDto;
import org.sonar.db.user.GroupDto;
import org.sonar.db.user.GroupMembershipDto;
import org.sonar.db.user.GroupMembershipQuery;
import org.sonar.db.user.UserDto;
import org.sonar.server.es.EsTester;
import org.sonar.server.exceptions.ForbiddenException;
import org.sonar.server.exceptions.NotFoundException;
import org.sonar.server.issue.ws.AvatarResolverImpl;
import org.sonar.server.organization.MemberUpdater;
import org.sonar.server.organization.OrganizationValidationImpl;
import org.sonar.server.tester.UserSessionRule;
import org.sonar.server.user.index.UserIndexer;
import org.sonar.server.usergroups.DefaultGroupFinder;
import org.sonar.server.ws.TestRequest;
import org.sonar.server.ws.WsActionTester;
import org.sonarqube.ws.Organizations.AddMemberWsResponse;

import static java.lang.String.format;
import static java.util.Optional.ofNullable;
import static org.assertj.core.api.Assertions.assertThat;
import static org.sonar.db.permission.OrganizationPermission.ADMINISTER;
import static org.sonar.db.permission.OrganizationPermission.ADMINISTER_QUALITY_GATES;
import static org.sonar.db.user.GroupMembershipQuery.IN;
import static org.sonar.server.organization.ws.OrganizationsWsSupport.PARAM_ORGANIZATION;
import static org.sonar.test.JsonAssert.assertJson;

public class AddMemberActionTest {
  @Rule
  public ExpectedException expectedException = ExpectedException.none();
  @Rule
  public UserSessionRule userSession = UserSessionRule.standalone().logIn().setRoot();
  @Rule
  public EsTester es = EsTester.create();
  @Rule
  public DbTester db = DbTester.create();

  private DbClient dbClient = db.getDbClient();
  private DbSession dbSession = db.getSession();
  private OrganizationsWsSupport wsSupport = new OrganizationsWsSupport(new OrganizationValidationImpl(), dbClient);
  private WsActionTester ws = new WsActionTester(
    new AddMemberAction(dbClient, userSession, new AvatarResolverImpl(), wsSupport,
      new MemberUpdater(dbClient, new DefaultGroupFinder(dbClient), new UserIndexer(dbClient, es.client()))));

  @Test
  public void add_member() {
    OrganizationDto organization = db.organizations().insert();
    db.users().insertDefaultGroup(organization, "default");
    UserDto user = db.users().insertUser();

    call(organization.getKey(), user.getLogin());

    assertMember(organization.getUuid(), user.getId());
  }

  @Test
  public void user_can_be_member_of_two_organizations() {
    OrganizationDto organization = db.organizations().insert();
    db.users().insertDefaultGroup(organization, "default");
    UserDto user = db.users().insertUser();
    db.organizations().addMember(db.getDefaultOrganization(), user);

    OrganizationDto anotherOrg = db.organizations().insert();
    db.users().insertDefaultGroup(anotherOrg, "default");

    call(organization.getKey(), user.getLogin());
    call(anotherOrg.getKey(), user.getLogin());

    assertMember(organization.getUuid(), user.getId());
    assertMember(anotherOrg.getUuid(), user.getId());
  }

  @Test
  public void add_member_as_organization_admin() {
    OrganizationDto organization = db.organizations().insert();
    db.users().insertDefaultGroup(organization, "default");
    UserDto user = db.users().insertUser();
    db.organizations().addMember(db.getDefaultOrganization(), user);
    userSession.logIn().addPermission(ADMINISTER, organization);

    call(organization.getKey(), user.getLogin());

    assertMember(organization.getUuid(), user.getId());
  }

  @Test
  public void does_not_fail_if_user_already_added_in_organization() {
    OrganizationDto organization = db.organizations().insert();
    GroupDto defaultGroup = db.users().insertDefaultGroup(organization, "default");
    UserDto user = db.users().insertUser();
    db.organizations().addMember(organization, user);
    db.users().insertMember(defaultGroup, user);
    assertMember(organization.getUuid(), user.getId());

    call(organization.getKey(), user.getLogin());

    assertMember(organization.getUuid(), user.getId());
  }

  @Test
  public void return_user_info() {
    OrganizationDto organization = db.organizations().insert();
    db.users().insertDefaultGroup(organization, "default");
    UserDto user = db.users().insertUser(u -> u.setEmail("john@smith.com"));

    AddMemberWsResponse result = call(organization.getKey(), user.getLogin());

    assertThat(result.getUser().getLogin()).isEqualTo(user.getLogin());
    assertThat(result.getUser().getName()).isEqualTo(user.getName());
    assertThat(result.getUser().getAvatar()).isEqualTo("b0d8c6e5ea589e6fc3d3e08afb1873bb");
    assertThat(result.getUser().getGroupCount()).isEqualTo(1);
  }

  @Test
  public void return_user_info_even_when_user_is_already_member_of_organization() {
    OrganizationDto organization = db.organizations().insert();
    db.users().insertDefaultGroup(organization, "default");
    UserDto user = db.users().insertUser(u -> u.setEmail("john@smith.com"));
    IntStream.range(0, 3)
      .mapToObj(i -> db.users().insertGroup(organization))
      .forEach(g -> db.users().insertMembers(g, user));
    db.organizations().addMember(organization, user);

    AddMemberWsResponse result = call(organization.getKey(), user.getLogin());

    assertThat(result.getUser().getLogin()).isEqualTo(user.getLogin());
    assertThat(result.getUser().getName()).isEqualTo(user.getName());
    assertThat(result.getUser().getAvatar()).isEqualTo("b0d8c6e5ea589e6fc3d3e08afb1873bb");
    assertThat(result.getUser().getGroupCount()).isEqualTo(3);
  }

  @Test
  public void fail_when_organization_has_no_default_group() {
    OrganizationDto organization = db.organizations().insert();
    UserDto user = db.users().insertUser();

    expectedException.expect(IllegalStateException.class);
    expectedException.expectMessage(format("Default group cannot be found on organization '%s'", organization.getUuid()));

    call(organization.getKey(), user.getLogin());
  }

  @Test
  public void fail_if_login_does_not_exist() {
    OrganizationDto organization = db.organizations().insert();

    expectedException.expect(NotFoundException.class);
    expectedException.expectMessage("User 'login-42' is not found");

    call(organization.getKey(), "login-42");
  }

  @Test
  public void fail_if_organization_does_not_exist() {
    UserDto user = db.users().insertUser();

    expectedException.expect(NotFoundException.class);
    expectedException.expectMessage("Organization 'org-42' is not found");

    call("org-42", user.getLogin());
  }

  @Test
  public void fail_if_no_login_provided() {
    OrganizationDto organization = db.organizations().insert();

    expectedException.expect(IllegalArgumentException.class);

    call(organization.getKey(), null);
  }

  @Test
  public void fail_if_no_organization_provided() {
    UserDto user = db.users().insertUser();

    expectedException.expect(IllegalArgumentException.class);

    call(null, user.getLogin());
  }

  @Test
  public void fail_if_insufficient_permissions() {
    OrganizationDto organization = db.organizations().insert();
    UserDto user = db.users().insertUser();
    userSession.logIn().addPermission(ADMINISTER_QUALITY_GATES, organization);

    expectedException.expect(ForbiddenException.class);

    call(organization.getKey(), user.getLogin());
  }

  @Test
  public void fail_if_org_is_bind_to_alm_and_members_sync_is_enabled() {
    OrganizationDto organization = db.organizations().insert();
    db.alm().insertOrganizationAlmBinding(organization, db.alm().insertAlmAppInstall(), true);
    UserDto user = db.users().insertUser();

    expectedException.expect(IllegalArgumentException.class);

    call(organization.getKey(), user.getLogin());
  }

  @Test
  public void json_example() {
    OrganizationDto organization = db.organizations().insert();
    db.users().insertDefaultGroup(organization, "default");
    UserDto user = db.users().insertUser(u -> u.setLogin("ada.lovelace").setName("Ada Lovelace").setEmail("ada@lovelace.com"));

    String result = ws.newRequest().setParam(PARAM_ORGANIZATION, organization.getKey()).setParam("login", user.getLogin()).execute().getInput();

    assertJson(result).isSimilarTo(ws.getDef().responseExampleAsString());
  }

  @Test
  public void definition() {
    WebService.Action definition = ws.getDef();

    assertThat(definition.key()).isEqualTo("add_member");
    assertThat(definition.since()).isEqualTo("6.4");
    assertThat(definition.isPost()).isTrue();
    assertThat(definition.isInternal()).isTrue();
    assertThat(definition.responseExampleAsString()).isNotEmpty();
    assertThat(definition.params()).extracting(WebService.Param::key).containsOnly("organization", "login");

    WebService.Param organization = definition.param("organization");
    assertThat(organization.isRequired()).isTrue();

    WebService.Param login = definition.param("login");
    assertThat(login.isRequired()).isTrue();
  }

  private AddMemberWsResponse call(@Nullable String organizationKey, @Nullable String login) {
    TestRequest request = ws.newRequest();
    ofNullable(organizationKey).ifPresent(o -> request.setParam(PARAM_ORGANIZATION, o));
    ofNullable(login).ifPresent(l -> request.setParam("login", l));
    return request.executeProtobuf(AddMemberWsResponse.class);
  }

  private void assertMember(String organizationUuid, int userId) {
    assertThat(dbClient.organizationMemberDao().select(dbSession, organizationUuid, userId)).isPresent();
    Integer defaultGroupId = dbClient.organizationDao().getDefaultGroupId(dbSession, organizationUuid).get();
    assertThat(db.getDbClient().groupMembershipDao().selectGroups(db.getSession(), GroupMembershipQuery.builder()
      .membership(IN)
      .organizationUuid(organizationUuid).build(),
      userId, 0, 10))
        .extracting(GroupMembershipDto::getId)
        .containsOnly(defaultGroupId.longValue());
  }
}
