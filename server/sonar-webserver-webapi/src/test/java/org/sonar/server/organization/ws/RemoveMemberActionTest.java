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

import java.util.HashSet;
import javax.annotation.Nullable;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.sonar.api.server.ws.WebService;
import org.sonar.api.utils.System2;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.db.DbTester;
import org.sonar.db.organization.OrganizationDto;
import org.sonar.db.user.UserDto;
import org.sonar.server.es.EsTester;
import org.sonar.server.es.SearchOptions;
import org.sonar.server.exceptions.ForbiddenException;
import org.sonar.server.exceptions.NotFoundException;
import org.sonar.server.organization.MemberUpdater;
import org.sonar.server.organization.OrganizationValidationImpl;
import org.sonar.server.tester.UserSessionRule;
import org.sonar.server.user.index.UserIndex;
import org.sonar.server.user.index.UserIndexer;
import org.sonar.server.user.index.UserQuery;
import org.sonar.server.usergroups.DefaultGroupFinder;
import org.sonar.server.ws.TestRequest;
import org.sonar.server.ws.TestResponse;
import org.sonar.server.ws.WsActionTester;

import static java.net.HttpURLConnection.HTTP_NO_CONTENT;
import static java.util.Arrays.asList;
import static java.util.Optional.ofNullable;
import static org.assertj.core.api.Assertions.assertThat;
import static org.sonar.db.permission.OrganizationPermission.ADMINISTER;
import static org.sonar.db.permission.OrganizationPermission.ADMINISTER_QUALITY_GATES;
import static org.sonar.server.organization.ws.OrganizationsWsSupport.PARAM_ORGANIZATION;

public class RemoveMemberActionTest {
  @Rule
  public ExpectedException expectedException = ExpectedException.none();
  @Rule
  public UserSessionRule userSession = UserSessionRule.standalone();
  @Rule
  public EsTester es = EsTester.create();
  @Rule
  public DbTester db = DbTester.create();
  private DbClient dbClient = db.getDbClient();
  private DbSession dbSession = db.getSession();

  private UserIndex userIndex = new UserIndex(es.client(), System2.INSTANCE);
  private UserIndexer userIndexer = new UserIndexer(dbClient, es.client());
  private OrganizationsWsSupport wsSupport = new OrganizationsWsSupport(new OrganizationValidationImpl(), dbClient);

  private WsActionTester ws = new WsActionTester(new RemoveMemberAction(dbClient, userSession, wsSupport,
    new MemberUpdater(dbClient, new DefaultGroupFinder(dbClient), new UserIndexer(dbClient, es.client()))));

  @Test
  public void remove_member() {
    OrganizationDto organization = db.organizations().insert();
    UserDto user = db.users().insertUser();
    db.organizations().addMember(organization, user);
    UserDto adminUser = db.users().insertAdminByUserPermission(organization);
    db.organizations().addMember(organization, adminUser);
    userIndexer.indexOnStartup(new HashSet<>());
    assertMember(organization.getUuid(), user);
    userSession.logIn().addPermission(ADMINISTER, organization);

    call(organization.getKey(), user.getLogin());

    assertNotAMember(organization.getUuid(), user);
  }

  @Test
  public void no_content_http_204_returned() {
    OrganizationDto organization = db.organizations().insert();
    UserDto user = db.users().insertUser();
    db.organizations().addMember(organization, user);
    UserDto adminUser = db.users().insertAdminByUserPermission(organization);
    db.organizations().addMember(organization, adminUser);
    userIndexer.indexOnStartup(new HashSet<>());
    assertMember(organization.getUuid(), user);
    userSession.logIn().addPermission(ADMINISTER, organization);

    TestResponse result = call(organization.getKey(), user.getLogin());

    assertThat(result.getStatus()).isEqualTo(HTTP_NO_CONTENT);
    assertThat(result.getInput()).isEmpty();
  }

  @Test
  public void user_is_removed_only_from_designated_organization() {
    OrganizationDto organization = db.organizations().insert();
    UserDto user = db.users().insertUser();
    db.organizations().addMember(organization, user);
    UserDto adminUser = db.users().insertAdminByUserPermission(organization);
    db.organizations().addMember(organization, adminUser);
    userIndexer.indexOnStartup(new HashSet<>());
    assertMember(organization.getUuid(), user);
    OrganizationDto anotherOrg = db.organizations().insert();
    db.organizations().addMember(anotherOrg, user);
    userSession.logIn().addPermission(ADMINISTER, organization);

    call(organization.getKey(), user.getLogin());

    assertMember(anotherOrg.getUuid(), user);
  }

  @Test
  public void do_not_fail_if_user_already_removed_from_organization() {
    OrganizationDto organization = db.organizations().insert();
    UserDto user = db.users().insertUser();
    db.organizations().addMember(organization, user);
    UserDto adminUser = db.users().insertAdminByUserPermission(organization);
    db.organizations().addMember(organization, adminUser);
    userIndexer.indexOnStartup(new HashSet<>());
    assertMember(organization.getUuid(), user);
    userSession.logIn().addPermission(ADMINISTER, organization);
    call(organization.getKey(), user.getLogin());

    call(organization.getKey(), user.getLogin());
  }

  @Test
  public void fail_if_login_does_not_exist() {
    OrganizationDto organization = db.organizations().insert();
    userSession.logIn().addPermission(ADMINISTER, organization);

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
    userSession.logIn().addPermission(ADMINISTER, organization);

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
    db.organizations().addMember(organization, user);
    UserDto adminUser = db.users().insertAdminByUserPermission(organization);
    db.organizations().addMember(organization, adminUser);
    userIndexer.indexOnStartup(new HashSet<>());
    userSession.logIn().addPermission(ADMINISTER_QUALITY_GATES, organization);

    expectedException.expect(ForbiddenException.class);

    call(organization.getKey(), user.getLogin());
  }

  @Test
  public void fail_if_org_is_bind_to_alm_and_members_sync_is_enabled() {
    OrganizationDto organization = db.organizations().insert();
    db.alm().insertOrganizationAlmBinding(organization, db.alm().insertAlmAppInstall(), true);
    UserDto user = db.users().insertUser();
    userSession.logIn().addPermission(ADMINISTER, organization);

    expectedException.expect(IllegalArgumentException.class);

    call(organization.getKey(), user.getLogin());
  }

  @Test
  public void remove_org_admin_is_allowed_when_another_org_admin_exists() {
    OrganizationDto anotherOrganization = db.organizations().insert();
    UserDto admin1 = db.users().insertAdminByUserPermission(anotherOrganization);
    db.organizations().addMember(anotherOrganization, admin1);
    UserDto admin2 = db.users().insertAdminByUserPermission(anotherOrganization);
    db.organizations().addMember(anotherOrganization, admin2);
    userIndexer.commitAndIndex(db.getSession(), asList(admin1, admin2));
    userSession.logIn().addPermission(ADMINISTER, anotherOrganization);

    call(anotherOrganization.getKey(), admin1.getLogin());

    assertNotAMember(anotherOrganization.getUuid(), admin1);
    assertMember(anotherOrganization.getUuid(), admin2);
  }

  @Test
  public void fail_to_remove_last_organization_admin() {
    OrganizationDto anotherOrganization = db.organizations().insert();
    UserDto admin = db.users().insertAdminByUserPermission(anotherOrganization);
    db.organizations().addMember(anotherOrganization, admin);
    UserDto user = db.users().insertUser();
    db.organizations().addMember(anotherOrganization, user);
    userSession.logIn().addPermission(ADMINISTER, anotherOrganization);

    expectedException.expect(IllegalArgumentException.class);
    expectedException.expectMessage("The last administrator member cannot be removed");

    call(anotherOrganization.getKey(), admin.getLogin());
  }

  @Test
  public void definition() {
    WebService.Action definition = ws.getDef();

    assertThat(definition.key()).isEqualTo("remove_member");
    assertThat(definition.since()).isEqualTo("6.4");
    assertThat(definition.isPost()).isTrue();
    assertThat(definition.isInternal()).isTrue();
    assertThat(definition.params()).extracting(WebService.Param::key).containsOnly("organization", "login");

    WebService.Param organization = definition.param("organization");
    assertThat(organization.isRequired()).isTrue();

    WebService.Param login = definition.param("login");
    assertThat(login.isRequired()).isTrue();
  }

  private TestResponse call(@Nullable String organizationKey, @Nullable String login) {
    TestRequest request = ws.newRequest();
    ofNullable(organizationKey).ifPresent(o -> request.setParam(PARAM_ORGANIZATION, o));
    ofNullable(login).ifPresent(l -> request.setParam("login", l));

    return request.execute();
  }

  private void assertNotAMember(String organizationUuid, UserDto user) {
    assertThat(dbClient.organizationMemberDao().select(dbSession, organizationUuid, user.getId())).isNotPresent();
  }

  private void assertMember(String organizationUuid, UserDto user) {
    assertThat(dbClient.organizationMemberDao().select(dbSession, organizationUuid, user.getId())).isPresent();
    assertThat(userIndex.search(UserQuery.builder()
      .setOrganizationUuid(organizationUuid)
      .setTextQuery(user.getLogin())
      .build(),
      new SearchOptions()).getDocs())
        .hasSize(1);
  }

}
