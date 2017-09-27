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
package org.sonarqube.tests.qualityProfile;

import com.sonar.orchestrator.Orchestrator;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.sonarqube.tests.Category6Suite;
import org.sonarqube.tests.Tester;
import org.sonarqube.ws.Common;
import org.sonarqube.ws.Organizations.Organization;
import org.sonarqube.ws.QualityProfiles;
import org.sonarqube.ws.QualityProfiles.CreateWsResponse;
import org.sonarqube.ws.QualityProfiles.SearchGroupsResponse;
import org.sonarqube.ws.QualityProfiles.SearchUsersResponse;
import org.sonarqube.ws.WsUserGroups;
import org.sonarqube.ws.WsUsers;
import org.sonarqube.ws.client.permission.AddUserWsRequest;
import org.sonarqube.ws.client.qualityprofile.AddGroupRequest;
import org.sonarqube.ws.client.qualityprofile.AddUserRequest;
import org.sonarqube.ws.client.qualityprofile.RemoveGroupRequest;
import org.sonarqube.ws.client.qualityprofile.RemoveUserRequest;
import org.sonarqube.ws.client.qualityprofile.SearchGroupsRequest;
import org.sonarqube.ws.client.qualityprofile.SearchUsersRequest;
import org.sonarqube.ws.client.qualityprofile.SearchWsRequest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;
import static org.sonarqube.ws.QualityProfiles.SearchGroupsResponse.Group;

public class QualityProfilesEditTest {

  @ClassRule
  public static Orchestrator orchestrator = Category6Suite.ORCHESTRATOR;

  @Rule
  public Tester tester = new Tester(orchestrator);

  @Test
  public void search_users_allowed_to_edit_a_profile() {
    Organization organization = tester.organizations().generate();
    WsUsers.CreateWsResponse.User user1 = tester.users().generateMember(organization, u -> u.setEmail("user1@email.com"));
    WsUsers.CreateWsResponse.User user2 = tester.users().generateMember(organization, u -> u.setEmail("user2@email.com"));
    CreateWsResponse.QualityProfile xooProfile = tester.qProfiles().createXooProfile(organization);
    addUserPermission(organization, user1, xooProfile);

    SearchUsersResponse users = tester.qProfiles().service().searchUsers(SearchUsersRequest.builder()
      .setOrganization(organization.getKey())
      .setQualityProfile(xooProfile.getName())
      .setLanguage(xooProfile.getLanguage())
      .setSelected("all")
      .build());

    assertThat(users.getUsersList()).extracting(SearchUsersResponse.User::getLogin, SearchUsersResponse.User::getName, SearchUsersResponse.User::getAvatar, SearchUsersResponse.User::getSelected)
      .containsExactlyInAnyOrder(
        tuple(user1.getLogin(), user1.getName(), "3acc837f898bdaa338b7cd7a9ab6dd5b", true),
        tuple(user2.getLogin(), user2.getName(), "fd6926c24d76d650a365ae350784e048", false),
        tuple("admin", "Administrator", "d41d8cd98f00b204e9800998ecf8427e", false));
    assertThat(users.getPaging()).extracting(Common.Paging::getPageIndex, Common.Paging::getPageSize, Common.Paging::getTotal)
      .containsExactlyInAnyOrder(1, 25, 3);
  }

  @Test
  public void add_and_remove_user() {
    Organization organization = tester.organizations().generate();
    WsUsers.CreateWsResponse.User user1 = tester.users().generateMember(organization);
    WsUsers.CreateWsResponse.User user2 = tester.users().generateMember(organization);
    CreateWsResponse.QualityProfile xooProfile = tester.qProfiles().createXooProfile(organization);

    // No user added
    assertThat(tester.qProfiles().service().searchUsers(SearchUsersRequest.builder()
      .setOrganization(organization.getKey())
      .setQualityProfile(xooProfile.getName())
      .setLanguage(xooProfile.getLanguage())
      .setSelected("selected")
      .build()).getUsersList())
      .extracting(SearchUsersResponse.User::getLogin)
      .isEmpty();

    // Add user 1
    addUserPermission(organization, user1, xooProfile);
    assertThat(tester.qProfiles().service().searchUsers(SearchUsersRequest.builder()
      .setOrganization(organization.getKey())
      .setQualityProfile(xooProfile.getName())
      .setLanguage(xooProfile.getLanguage())
      .setSelected("selected")
      .build()).getUsersList())
      .extracting(SearchUsersResponse.User::getLogin)
      .containsExactlyInAnyOrder(user1.getLogin());

    // Remove user 1
    tester.qProfiles().service().removeUser(RemoveUserRequest.builder()
      .setOrganization(organization.getKey())
      .setQualityProfile(xooProfile.getName())
      .setLanguage(xooProfile.getLanguage())
      .setUserLogin(user1.getLogin())
      .build());
    assertThat(tester.qProfiles().service().searchUsers(SearchUsersRequest.builder()
      .setOrganization(organization.getKey())
      .setQualityProfile(xooProfile.getName())
      .setLanguage(xooProfile.getLanguage())
      .setSelected("selected")
      .build()).getUsersList())
      .extracting(SearchUsersResponse.User::getLogin)
      .isEmpty();
  }

  @Test
  public void search_groups_allowed_to_edit_a_profile() {
    Organization organization = tester.organizations().generate();
    WsUserGroups.Group group1 = tester.groups().generate(organization);
    WsUserGroups.Group group2 = tester.groups().generate(organization);
    WsUserGroups.Group group3 = tester.groups().generate(organization);
    CreateWsResponse.QualityProfile xooProfile = tester.qProfiles().createXooProfile(organization);
    addGroupPermission(organization, group1, xooProfile);
    addGroupPermission(organization, group2, xooProfile);

    SearchGroupsResponse groups = tester.qProfiles().service().searchGroups(SearchGroupsRequest.builder()
      .setOrganization(organization.getKey())
      .setQualityProfile(xooProfile.getName())
      .setLanguage(xooProfile.getLanguage())
      .build());

    assertThat(groups.getGroupsList()).extracting(Group::getName, Group::getDescription, Group::getSelected)
      .containsExactlyInAnyOrder(
        tuple(group1.getName(), group1.getDescription(), true),
        tuple(group2.getName(), group2.getDescription(), true));
    assertThat(groups.getPaging()).extracting(Common.Paging::getPageIndex, Common.Paging::getPageSize, Common.Paging::getTotal)
      .containsExactlyInAnyOrder(1, 25, 2);
  }

  @Test
  public void add_and_remove_group() {
    Organization organization = tester.organizations().generate();
    WsUserGroups.Group group1 = tester.groups().generate(organization);
    WsUserGroups.Group group2 = tester.groups().generate(organization);
    CreateWsResponse.QualityProfile xooProfile = tester.qProfiles().createXooProfile(organization);

    // No group added
    assertThat(tester.qProfiles().service().searchGroups(SearchGroupsRequest.builder()
      .setOrganization(organization.getKey())
      .setQualityProfile(xooProfile.getName())
      .setLanguage(xooProfile.getLanguage())
      .setSelected("selected")
      .build()).getGroupsList())
      .extracting(Group::getName)
      .isEmpty();

    // Add group 1
    addGroupPermission(organization, group1, xooProfile);
    assertThat(tester.qProfiles().service().searchGroups(SearchGroupsRequest.builder()
      .setOrganization(organization.getKey())
      .setQualityProfile(xooProfile.getName())
      .setLanguage(xooProfile.getLanguage())
      .setSelected("selected")
      .build()).getGroupsList())
      .extracting(Group::getName)
      .containsExactlyInAnyOrder(group1.getName());

    // Remove group 1
    tester.qProfiles().service().removeGroup(RemoveGroupRequest.builder()
      .setOrganization(organization.getKey())
      .setQualityProfile(xooProfile.getName())
      .setLanguage(xooProfile.getLanguage())
      .setGroup(group1.getName())
      .build());
    assertThat(tester.qProfiles().service().searchGroups(SearchGroupsRequest.builder()
      .setOrganization(organization.getKey())
      .setQualityProfile(xooProfile.getName())
      .setLanguage(xooProfile.getLanguage())
      .setSelected("selected")
      .build()).getGroupsList())
      .extracting(Group::getName)
      .isEmpty();
  }

  @Test
  public void actions_when_user_can_edit_profiles() {
    Organization organization = tester.organizations().generate();
    WsUsers.CreateWsResponse.User user = tester.users().generateMember(organization);
    CreateWsResponse.QualityProfile xooProfile1 = tester.qProfiles().createXooProfile(organization);
    addUserPermission(organization, user, xooProfile1);
    CreateWsResponse.QualityProfile xooProfile2 = tester.qProfiles().createXooProfile(organization);
    WsUserGroups.Group group = tester.groups().generate(organization);
    tester.groups().addMemberToGroups(organization, user.getLogin(), group.getName());
    addGroupPermission(organization, group, xooProfile2);
    CreateWsResponse.QualityProfile xooProfile3 = tester.qProfiles().createXooProfile(organization);

    QualityProfiles.SearchWsResponse result = tester.as(user.getLogin())
      .qProfiles().service().search(new SearchWsRequest().setOrganizationKey(organization.getKey()));
    assertThat(result.getActions().getCreate()).isFalse();
    assertThat(result.getProfilesList())
      .extracting(QualityProfiles.SearchWsResponse.QualityProfile::getKey, qp -> qp.getActions().getEdit(), qp -> qp.getActions().getCopy(), qp -> qp.getActions().getSetAsDefault())
    .contains(
      tuple(xooProfile1.getKey(), true, false, false),
      tuple(xooProfile2.getKey(), true, false, false),
      tuple(xooProfile3.getKey(), false, false, false));
  }

  @Test
  public void actions_when_user_is_global_qprofile_administer() {
    Organization organization = tester.organizations().generate();
    WsUsers.CreateWsResponse.User user = tester.users().generateMember(organization);
    CreateWsResponse.QualityProfile xooProfile = tester.qProfiles().createXooProfile(organization);
    tester.wsClient().permissions().addUser(new AddUserWsRequest().setOrganization(organization.getKey()).setLogin(user.getLogin()).setPermission("profileadmin"));

    QualityProfiles.SearchWsResponse result = tester.as(user.getLogin())
      .qProfiles().service().search(new SearchWsRequest().setOrganizationKey(organization.getKey()));
    assertThat(result.getActions().getCreate()).isTrue();
    assertThat(result.getProfilesList())
      .extracting(QualityProfiles.SearchWsResponse.QualityProfile::getKey, qp -> qp.getActions().getEdit(), qp -> qp.getActions().getCopy(), qp -> qp.getActions().getSetAsDefault())
      .contains(
        tuple(xooProfile.getKey(), true, true, true));
  }

  private void addUserPermission(Organization organization, WsUsers.CreateWsResponse.User user, CreateWsResponse.QualityProfile qProfile){
    tester.qProfiles().service().addUser(AddUserRequest.builder()
      .setOrganization(organization.getKey())
      .setQualityProfile(qProfile.getName())
      .setLanguage(qProfile.getLanguage())
      .setUserLogin(user.getLogin())
      .build());
  }

  private void addGroupPermission(Organization organization, WsUserGroups.Group group, CreateWsResponse.QualityProfile qProfile){
    tester.qProfiles().service().addGroup(AddGroupRequest.builder()
      .setOrganization(organization.getKey())
      .setQualityProfile(qProfile.getName())
      .setLanguage(qProfile.getLanguage())
      .setGroup(group.getName())
      .build());
  }
}
