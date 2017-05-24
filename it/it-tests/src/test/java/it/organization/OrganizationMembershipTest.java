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
import java.util.List;
import org.junit.After;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.sonarqube.ws.Organizations;
import org.sonarqube.ws.WsUsers;
import org.sonarqube.ws.client.HttpException;
import org.sonarqube.ws.client.WsClient;
import org.sonarqube.ws.client.organization.CreateWsRequest;
import org.sonarqube.ws.client.organization.SearchMembersWsRequest;
import org.sonarqube.ws.client.permission.AddUserWsRequest;
import org.sonarqube.ws.client.user.GroupsRequest;
import pageobjects.Navigation;
import pageobjects.organization.MembersPage;
import util.user.UserRule;

import static it.Category6Suite.enableOrganizationsSupport;
import static org.apache.commons.lang.RandomStringUtils.randomAlphabetic;
import static org.assertj.core.api.Assertions.assertThat;
import static util.ItUtils.deleteOrganizationsIfExists;
import static util.ItUtils.newAdminWsClient;
import static util.ItUtils.newOrganizationKey;
import static util.ItUtils.newUserWsClient;
import static util.ItUtils.setServerProperty;

public class OrganizationMembershipTest {

  private static final String KEY = newOrganizationKey();

  @ClassRule
  public static Orchestrator orchestrator = Category6Suite.ORCHESTRATOR;

  @ClassRule
  public static UserRule userRule = UserRule.from(orchestrator);

  @Rule
  public ExpectedException expectedException = ExpectedException.none();

  @Rule
  public Navigation nav = Navigation.get(orchestrator);

  private static WsClient adminClient;

  @BeforeClass
  public static void setUp() throws Exception {
    adminClient = newAdminWsClient(orchestrator);
    enableOrganizationsSupport();
    setServerProperty(orchestrator, "sonar.organizations.anyoneCanCreate", "true");
    deleteOrganizationsIfExists(orchestrator, KEY);
  }

  @After
  public void tearDown() throws Exception {
    deleteOrganizationsIfExists(orchestrator, KEY);
  }

  @Test
  public void new_user_should_not_become_member_of_default_organization() throws Exception {
    String login = createUser();
    verifyMembership(login, "default-organization", false);
  }

  @Test
  public void add_and_remove_member() throws Exception {
    String organizationKey = createOrganization();
    String login = createUser();
    adminClient.organizations().addMember(organizationKey, login);
    verifyMembership(login, organizationKey, true);

    adminClient.organizations().removeMember(organizationKey, login);
    verifyMembership(login, organizationKey, false);
  }

  @Test
  public void remove_organization_admin_member() throws Exception {
    String organizationKey = createOrganization();
    String login = createUser();
    adminClient.organizations().addMember(organizationKey, login);
    adminClient.permissions().addUser(new AddUserWsRequest().setLogin(login).setPermission("admin").setOrganization(organizationKey));
    verifyMembership(login, organizationKey, true);

    adminClient.organizations().removeMember(organizationKey, login);
    verifyMembership(login, organizationKey, false);
  }

  @Test
  public void fail_to_remove_organization_admin_member_when_last_admin() throws Exception {
    String organizationKey = createOrganization();
    String login = createUser();
    adminClient.organizations().addMember(organizationKey, login);
    adminClient.permissions().addUser(new AddUserWsRequest().setLogin(login).setPermission("admin").setOrganization(organizationKey));
    verifyMembership(login, organizationKey, true);
    // Admin is the creator of the organization so he was granted with admin permission
    adminClient.organizations().removeMember(organizationKey, "admin");

    expectedException.expect(HttpException.class);
    expectedException.expectMessage("The last administrator member cannot be removed");

    adminClient.organizations().removeMember(organizationKey, login);
  }

  @Test
  public void remove_user_remove_its_membership() throws Exception {
    String organizationKey = createOrganization();
    String login = createUser();
    adminClient.organizations().addMember(organizationKey, login);
    verifyMembership(login, organizationKey, true);

    userRule.deactivateUsers(login);
    verifyOrganizationMembership(login, organizationKey, false);
  }

  @Test
  public void user_creating_an_organization_becomes_member_of_this_organization() throws Exception {
    String keyAndName = newOrganizationKey();
    String login = createUser();

    newUserWsClient(orchestrator, login, login).organizations().create(
      new CreateWsRequest.Builder().setKey(keyAndName).setName(keyAndName).build()).getOrganization();

    verifyMembership(login, keyAndName, true);
  }

  @Test
  public void should_display_members_page() {
    String orgKey = createOrganization();

    String userFoo = createUser("foo");
    adminClient.organizations().addMember(orgKey, userFoo);

    String userBar = createUser("bar");
    adminClient.organizations().addMember(orgKey, userBar);

    createUser();

    MembersPage page = nav.openOrganizationMembers(orgKey);
    page
      .canNotAddMember()
      .shouldHaveTotal(3);
    page.getMembersByIdx(0).shouldBeNamed("admin", "Administrator");
    page.getMembersByIdx(1).shouldBeNamed(userBar, userBar);
    page.getMembersByIdx(2)
      .shouldBeNamed(userFoo, userFoo)
      .shouldNotHaveActions();
  }

  @Test
  public void search_for_members() {
    String orgKey = createOrganization();

    String user1 = createUser();
    adminClient.organizations().addMember(orgKey, user1);

    String user2 = createUser("sameprefixuser");
    adminClient.organizations().addMember(orgKey, user2);

    // Created to verify that only the user part of the org is returned
    createUser("sameprefixuser");

    MembersPage page = nav.openOrganizationMembers(orgKey);
    page
      .searchForMember("sameprefixuser")
      .shouldHaveTotal(1);
    page.getMembersByIdx(0).shouldBeNamed(user2, user2);
    page
      .searchForMember(user1)
      .shouldHaveTotal(1);
    page.getMembersByIdx(0).shouldBeNamed(user1, user1);
  }

  @Test
  public void admin_can_add_members() {
    String orgKey = createOrganization();
    String userFoo = createUser("foo");
    createUser();

    MembersPage page = nav.logIn().asAdmin().openOrganizationMembers(orgKey);
    page
      .shouldHaveTotal(1)
      .addMember(userFoo)
      .shouldHaveTotal(2);
    page.getMembersByIdx(0).shouldBeNamed("admin", "Administrator").shouldHaveGroups(2);
    page.getMembersByIdx(1).shouldBeNamed(userFoo, userFoo).shouldHaveGroups(1);
  }

  @Test
  public void admin_can_remove_members() {
    String orgKey = createOrganization();

    String user1 = createUser();
    adminClient.organizations().addMember(orgKey, user1);

    String user2 = createUser();
    adminClient.organizations().addMember(orgKey, user2);

    MembersPage page = nav.logIn().asAdmin().openOrganizationMembers(orgKey);
    page.shouldHaveTotal(3)
      .getMembersByIdx(1).removeMembership();
    page.shouldHaveTotal(2);
  }

  @Test
  public void admin_can_manage_groups() {
    String orgKey = createOrganization();

    String userFoo = createUser("foo");
    adminClient.organizations().addMember(orgKey, userFoo);

    MembersPage page = nav.logIn().asAdmin().openOrganizationMembers(orgKey);
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
    String orgKey = createOrganization();
    String userFoo = createUser("foo");

    MembersPage page = nav.logIn().asAdmin().openOrganizationMembers(orgKey);
    page
      .addMember(userFoo)
      .getMembersByIdx(1)
      .shouldHaveGroups(1)
      .manageGroupsOpen()
      .manageGroupsSelect("owners")
      .manageGroupsSave()
      .shouldHaveGroups(2);
  }

  private void verifyMembership(String login, String organizationKey, boolean isMember) {
    verifyOrganizationMembership(login, organizationKey, isMember);
    verifyMembersGroupMembership(login, organizationKey, isMember);
  }

  private void verifyOrganizationMembership(String login, String organizationKey, boolean isMember) {
    List<Organizations.User> users = adminClient.organizations().searchMembers(new SearchMembersWsRequest()
      .setQuery(login)
      .setSelected("selected")
      .setOrganization(organizationKey))
      .getUsersList();
    assertThat(users).hasSize(isMember ? 1 : 0);
  }

  private void verifyMembersGroupMembership(String login, String organizationKey, boolean isMember) {
    List<WsUsers.GroupsWsResponse.Group> groups = adminClient.users().groups(GroupsRequest.builder()
      .setLogin(login)
      .setOrganization(organizationKey)
      .setQuery("Members")
      .setSelected("selected")
      .build())
      .getGroupsList();
    assertThat(groups).hasSize(isMember ? 1 : 0);
  }

  private static String createOrganization() {
    adminClient.organizations().create(new CreateWsRequest.Builder().setKey(KEY).setName(KEY).build()).getOrganization();
    return KEY;
  }

  private static String createUser() {
    return createUser("");
  }

  private static String createUser(String prefix) {
    String login = prefix + randomAlphabetic(10).toLowerCase();
    userRule.createUser(login, login);
    return login;
  }
}
