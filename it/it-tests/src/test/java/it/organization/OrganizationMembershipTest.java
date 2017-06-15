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

package it.organization;

import com.sonar.orchestrator.Orchestrator;
import it.Category6Suite;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.rules.RuleChain;
import org.junit.rules.TestRule;
import org.sonarqube.ws.Organizations.Organization;
import org.sonarqube.ws.WsUsers.CreateWsResponse.User;
import org.sonarqube.ws.client.HttpException;
import org.sonarqube.ws.client.WsClient;
import org.sonarqube.ws.client.permission.AddUserWsRequest;
import util.OrganizationRule;
import util.user.UserRule;

import static util.ItUtils.newAdminWsClient;
import static util.ItUtils.setServerProperty;

public class OrganizationMembershipTest {

  private static Orchestrator orchestrator = Category6Suite.ORCHESTRATOR;
  private static OrganizationRule organizations = new OrganizationRule(orchestrator);
  private static UserRule users = new UserRule(orchestrator);

  @ClassRule
  public static TestRule chain = RuleChain.outerRule(orchestrator)
    .around(users)
    .around(organizations);

  @Rule
  public ExpectedException expectedException = ExpectedException.none();

  private static WsClient rootWsClient;

  @BeforeClass
  public static void setUp() {
    rootWsClient = newAdminWsClient(orchestrator);
    setServerProperty(orchestrator, "sonar.organizations.anyoneCanCreate", "true");
  }

  @AfterClass
  public static void tearDown() {
    setServerProperty(orchestrator, "sonar.organizations.anyoneCanCreate", null);
  }

  @Test
  public void new_user_should_not_become_member_of_default_organization() {
    User user = users.createUser();
    organizations.assertThatNotMemberOf(null, user);
  }

  @Test
  public void add_and_remove_member() {
    Organization organization = organizations.create();
    User user = users.createUser();

    addMembership(organization, user);
    organizations.assertThatMemberOf(organization, user);

    removeMembership(organization, user);
    organizations.assertThatNotMemberOf(organization, user);
  }

  @Test
  public void remove_organization_admin_member() {
    Organization organization = organizations.create();
    User user = users.createUser();
    addMembership(organization, user);

    rootWsClient.permissions().addUser(new AddUserWsRequest().setLogin(user.getLogin()).setPermission("admin").setOrganization(organization.getKey()));
    organizations.assertThatMemberOf(organization, user);

    removeMembership(organization, user);
    organizations.assertThatNotMemberOf(organization, user);
  }

  @Test
  public void fail_to_remove_organization_admin_member_when_last_admin() {
    Organization organization = organizations.create();
    User user = users.createUser();
    addMembership(organization, user);

    rootWsClient.permissions().addUser(new AddUserWsRequest().setLogin(user.getLogin()).setPermission("admin").setOrganization(organization.getKey()));
    organizations.assertThatMemberOf(organization, user);
    // Admin is the creator of the organization so he was granted with admin permission
    rootWsClient.organizations().removeMember(organization.getKey(), "admin");

    expectedException.expect(HttpException.class);
    expectedException.expectMessage("The last administrator member cannot be removed");
    removeMembership(organization, user);
  }

  @Test
  public void remove_user_remove_its_membership() {
    Organization organization = organizations.create();
    User user = users.createUser();
    addMembership(organization, user);

    users.deactivateUsers(user.getLogin());
    organizations.assertThatNotMemberOf(organization, user);
  }

  @Test
  public void user_creating_an_organization_becomes_member_of_this_organization() {
    String password = "aPassword";
    User user = users.createUser(p -> p.setPassword(password));

    Organization organization = organizations.as(user.getLogin(), password).create();

    organizations.assertThatMemberOf(organization, user);
  }

  private void addMembership(Organization organization, User user) {
    rootWsClient.organizations().addMember(organization.getKey(), user.getLogin());
  }

  private void removeMembership(Organization organization, User user) {
    rootWsClient.organizations().removeMember(organization.getKey(), user.getLogin());
  }
}
