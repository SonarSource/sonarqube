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
package org.sonarqube.tests.organization;

import com.sonar.orchestrator.Orchestrator;
import org.junit.After;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.sonarqube.qa.util.Tester;
import org.sonarqube.ws.Organizations.Organization;
import org.sonarqube.ws.Users.CreateWsResponse.User;
import org.sonarqube.ws.client.HttpException;
import org.sonarqube.ws.client.organizations.RemoveMemberRequest;
import org.sonarqube.ws.client.permissions.AddUserRequest;
import org.sonarqube.ws.client.users.DeactivateRequest;

public class OrganizationMembershipTest {

  @ClassRule
  public static Orchestrator orchestrator = OrganizationSuite.ORCHESTRATOR;

  @Rule
  public Tester tester = new Tester(orchestrator);

  @Rule
  public ExpectedException expectedException = ExpectedException.none();

  @Before
  public void setUp() {
    tester.settings().setGlobalSettings("sonar.organizations.anyoneCanCreate", "true");
  }

  @After
  public void tearDown() {
    tester.settings().resetSettings("sonar.organizations.anyoneCanCreate");
  }

  @Test
  public void new_user_should_not_become_member_of_default_organization() {
    User user = tester.users().generate();
    tester.organizations().assertThatNotMemberOf(null, user);
  }

  @Test
  public void add_and_remove_member() {
    Organization organization = tester.organizations().generate();
    User user = tester.users().generate();

    addMembership(organization, user);
    tester.organizations().assertThatMemberOf(organization, user);

    removeMembership(organization, user);
    tester.organizations().assertThatNotMemberOf(organization, user);
  }

  @Test
  public void remove_organization_admin_member() {
    Organization organization = tester.organizations().generate();
    User user = tester.users().generate();
    addMembership(organization, user);

    tester.wsClient().permissions().addUser(new AddUserRequest().setLogin(user.getLogin()).setPermission("admin").setOrganization(organization.getKey()));
    tester.organizations().assertThatMemberOf(organization, user);

    removeMembership(organization, user);
    tester.organizations().assertThatNotMemberOf(organization, user);
  }

  @Test
  public void fail_to_remove_organization_admin_member_when_last_admin() {
    Organization organization = tester.organizations().generate();
    User user = tester.users().generate();
    addMembership(organization, user);

    tester.wsClient().permissions().addUser(new AddUserRequest().setLogin(user.getLogin()).setPermission("admin").setOrganization(organization.getKey()));
    tester.organizations().assertThatMemberOf(organization, user);
    // Admin is the creator of the organization so he was granted with admin permission
    tester.wsClient().organizations().removeMember(new RemoveMemberRequest().setOrganization(organization.getKey()).setLogin("admin"));

    expectedException.expect(HttpException.class);
    expectedException.expectMessage("The last administrator member cannot be removed");
    removeMembership(organization, user);
  }

  @Test
  public void remove_user_remove_its_membership() {
    Organization organization = tester.organizations().generate();
    User user = tester.users().generate();
    addMembership(organization, user);

    tester.users().service().deactivate(new DeactivateRequest().setLogin(user.getLogin()));
    tester.organizations().assertThatNotMemberOf(organization, user);
  }

  @Test
  public void user_creating_an_organization_becomes_member_of_this_organization() {
    User user = tester.users().generate();

    Organization organization = tester.as(user.getLogin()).organizations().generate();

    tester.organizations().assertThatMemberOf(organization, user);
  }

  private void addMembership(Organization organization, User user) {
    tester.organizations().addMember(organization, user);
  }

  private void removeMembership(Organization organization, User user) {
    tester.wsClient().organizations().removeMember(new RemoveMemberRequest().setOrganization(organization.getKey()).setLogin(user.getLogin()));
  }
}
