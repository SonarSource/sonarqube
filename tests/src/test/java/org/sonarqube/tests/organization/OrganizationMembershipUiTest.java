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
import org.sonarqube.qa.util.Tester;
import org.sonarqube.qa.util.pageobjects.organization.MembersPage;
import org.sonarqube.ws.Organizations.Organization;
import org.sonarqube.ws.Users.CreateWsResponse.User;
import org.sonarqube.ws.client.roots.SetRootRequest;

public class OrganizationMembershipUiTest {

  @ClassRule
  public static Orchestrator orchestrator = OrganizationSuite.ORCHESTRATOR;

  @Rule
  public Tester tester = new Tester(orchestrator);

  private User root;

  @Before
  public void setUp() {
    tester.settings().setGlobalSetting("sonar.organizations.anyoneCanCreate", "true");
    root = tester.users().generate();
    tester.wsClient().roots().setRoot(new SetRootRequest().setLogin(root.getLogin()));
  }

  @After
  public void tearDown() {
    tester.settings().resetSettings("sonar.organizations.anyoneCanCreate");
  }

  @Test
  public void should_display_members_page() {
    Organization organization = tester.organizations().generate();
    User member1 = tester.users().generate(p -> p.setName("foo"));
    addMember(organization, member1);
    User member2 = tester.users().generate(p -> p.setName("bar"));
    addMember(organization, member2);
    tester.users().generate();

    MembersPage page = tester.openBrowser().openOrganizationMembers(organization.getKey());
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
    Organization organization = tester.organizations().generate();
    User member1 = tester.users().generate(p -> p.setName("foo"));
    addMember(organization, member1);
    User member2 = tester.users().generate(p -> p.setName("sameprefixuser1"));
    addMember(organization, member2);
    // Created to verify that only the user part of the org is returned
    tester.users().generate(p -> p.setName(member2.getName() + "sameprefixuser2"));

    MembersPage page = tester.openBrowser().openOrganizationMembers(organization.getKey());
    page
      .searchForMember("sameprefixuser")
      .shouldHaveTotal(1);
    page.getMembersByIdx(0).shouldBeNamed(member2.getLogin(), member2.getName());
    page
      .searchForMember(member1.getLogin())
      .shouldHaveTotal(1);
    page.getMembersByIdx(0).shouldBeNamed(member1.getLogin(), member1.getName());
  }

  @Test
  public void admin_can_add_members() {
    Organization organization = tester.organizations().generate();
    User user1 = tester.users().generate(u -> u.setLogin("foo"));
    tester.users().generate();

    MembersPage page = tester.openBrowser()
      .logIn().submitCredentials(root.getLogin())
      .openOrganizationMembers(organization.getKey());
    page
      .shouldHaveTotal(1)
      .addMember(user1.getLogin())
      .shouldHaveTotal(2);
    page.getMembersByIdx(0).shouldBeNamed("admin", "Administrator").shouldHaveGroups(2);
    page.getMembersByIdx(1).shouldBeNamed(user1.getLogin(), user1.getName()).shouldHaveGroups(1);
  }

  @Test
  public void admin_can_remove_members() {
    Organization organization = tester.organizations().generate();
    User user1 = tester.users().generate();
    addMember(organization, user1);
    User user2 = tester.users().generate();
    addMember(organization, user2);

    MembersPage page = tester.openBrowser()
      .logIn().submitCredentials(root.getLogin())
      .openOrganizationMembers(organization.getKey());
    page.shouldHaveTotal(3)
      .getMembersByIdx(1).removeMembership();
    page.shouldHaveTotal(2);
  }

  @Test
  public void admin_can_manage_groups() {
    Organization organization = tester.organizations().generate();
    User user = tester.users().generate(u -> u.setLogin("foo"));
    addMember(organization, user);

    MembersPage page = tester.openBrowser()
      .logIn().submitCredentials(root.getLogin())
      .openOrganizationMembers(organization.getKey());
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
    Organization organization = tester.organizations().generate();
    User user = tester.users().generate();

    MembersPage page = tester.openBrowser()
      .logIn().submitCredentials(root.getLogin())
      .openOrganizationMembers(organization.getKey());
    page
      .addMember(user.getLogin())
      .getMembersByIdx(1)
      .shouldHaveGroups(1)
      .manageGroupsOpen()
      .manageGroupsSelect("owners")
      .manageGroupsSave()
      .shouldHaveGroups(2);
  }

  private void addMember(Organization organization, User member1) {
    tester.organizations().addMember(organization, member1);
  }
}
