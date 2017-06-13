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
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.RuleChain;
import org.junit.rules.TestRule;
import org.sonarqube.ws.Organizations.Organization;
import org.sonarqube.ws.WsUsers.CreateWsResponse.User;
import org.sonarqube.ws.client.WsClient;
import pageobjects.Navigation;
import pageobjects.organization.MembersPage;
import util.OrganizationRule;
import util.user.UserRule;

import static util.ItUtils.newAdminWsClient;
import static util.ItUtils.setServerProperty;

public class OrganizationMembershipUiTest {

  private static Orchestrator orchestrator = Category6Suite.ORCHESTRATOR;
  private static OrganizationRule organizations = new OrganizationRule(orchestrator);
  private static UserRule users = new UserRule(orchestrator);

  @ClassRule
  public static TestRule chain = RuleChain.outerRule(orchestrator)
    .around(users)
    .around(organizations);

  @Rule
  public Navigation nav = Navigation.get(orchestrator);

  private static WsClient rootWsClient;
  private String adminUser;

  @BeforeClass
  public static void setUp() {
    rootWsClient = newAdminWsClient(orchestrator);
    setServerProperty(orchestrator, "sonar.organizations.anyoneCanCreate", "true");
  }

  @AfterClass
  public static void tearDown() {
    setServerProperty(orchestrator, "sonar.organizations.anyoneCanCreate", null);
  }

  @Before
  public void createRootUser() {
    adminUser = users.createRootUser();
  }

  @After
  public void purgeData() throws Exception {
    organizations.deleteOrganizations();
    users.deactivateAllUsers();
  }

  @Test
  public void should_display_members_page() {
    Organization organization = organizations.create();
    User member1 = users.createUser(p -> p.setName("foo"));
    addMembership(organization, member1);
    User member2 = users.createUser(p -> p.setName("bar"));
    addMembership(organization, member2);
    users.createUser();

    MembersPage page = nav.openOrganizationMembers(organization.getKey());
    page
      .canNotAddMember()
      .shouldHaveTotal(3);
    page.getMembersByIdx(0).shouldBeNamed("admin", "Administrator");
    page.getMembersByIdx(1)
      .shouldBeNamed(member2.getLogin(), member2.getName());
    page.getMembersByIdx(2)
      .shouldBeNamed(member1.getLogin(), member1.getName())
      .shouldNotHaveActions();
  }

  @Test
  public void search_for_members() {
    Organization organization = organizations.create();

    User user1 = users.createUser();
    rootWsClient.organizations().addMember(organization.getKey(), user1.getLogin());

    User user2 = users.createUser(p -> p.setLogin("sameprefixuser1"));
    rootWsClient.organizations().addMember(organization.getKey(), user2.getLogin());

    // Created to verify that only the user part of the org is returned
    users.createUser(p -> p.setLogin("sameprefixuser2"));

    MembersPage page = nav.openOrganizationMembers(organization.getKey());
    page
      .searchForMember("sameprefixuser")
      .shouldHaveTotal(1);
    page.getMembersByIdx(0).shouldBeNamed(user2.getLogin(), user2.getName());
    page
      .searchForMember(user1.getLogin())
      .shouldHaveTotal(1);
    page.getMembersByIdx(0).shouldBeNamed(user1.getLogin(), user1.getName());
  }

  @Test
  public void admin_can_add_members() {
    Organization organization = organizations.create();
    String orgKey = organization.getKey();
    User user = users.createUser(p -> p.setLogin("foo"));
    users.createUser();

    MembersPage page = nav.logIn().submitCredentials(adminUser).openOrganizationMembers(orgKey);
    page
      .shouldHaveTotal(1)
      .addMember(user.getLogin())
      .shouldHaveTotal(2);
    page.getMembersByIdx(0).shouldBeNamed("admin", "Administrator").shouldHaveGroups(2);
    page.getMembersByIdx(1).shouldBeNamed(user.getLogin(), user.getName()).shouldHaveGroups(1);
  }

  @Test
  public void admin_can_remove_members() {
    Organization organization = organizations.create();
    String orgKey = organization.getKey();

    User user1 = users.createUser();
    rootWsClient.organizations().addMember(orgKey, user1.getLogin());

    User user2 = users.createUser();
    rootWsClient.organizations().addMember(orgKey, user2.getLogin());

    MembersPage page = nav.logIn().submitCredentials(adminUser).openOrganizationMembers(orgKey);
    page.shouldHaveTotal(3)
      .getMembersByIdx(1).removeMembership();
    page.shouldHaveTotal(2);
  }

  @Test
  public void admin_can_manage_groups() {
    Organization organization = organizations.create();
    String orgKey = organization.getKey();

    User user = users.createUser(p -> p.setLogin("foo"));
    rootWsClient.organizations().addMember(orgKey, user.getLogin());

    MembersPage page = nav.logIn().submitCredentials(adminUser).openOrganizationMembers(orgKey);
    // foo user
    page.getMembersByIdx(1)
      .manageGroupsOpen()
      .manageGroupsSelect("owners")
      .manageGroupsSave()
      .shouldHaveGroups(2);
    // admin user
    page.getMembersByIdx(0)
      .manageGroupsOpen()
      .manageGroupsSelect("owners")
      .manageGroupsSave()
      .shouldHaveGroups(1);
  }

  @Test
  public void groups_count_should_be_updated_when_a_member_was_just_added() {
    Organization organization = organizations.create();
    String orgKey = organization.getKey();
    User user = users.createUser(p -> p.setLogin("foo"));

    MembersPage page = nav.logIn().submitCredentials(adminUser).openOrganizationMembers(orgKey);
    page
      .addMember(user.getLogin())
      .getMembersByIdx(1)
      .shouldHaveGroups(1)
      .manageGroupsOpen()
      .manageGroupsSelect("owners")
      .manageGroupsSave()
      .shouldHaveGroups(2);
  }

  private void addMembership(Organization organization, User user) {
    rootWsClient.organizations().addMember(organization.getKey(), user.getLogin());
  }
}
