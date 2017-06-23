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
package org.sonarqube.tests.organization;

import com.sonar.orchestrator.Orchestrator;
import com.sonar.orchestrator.build.BuildFailureException;
import org.sonarqube.tests.Category6Suite;
import java.util.List;
import java.util.Locale;
import org.junit.After;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.sonarqube.tests.OrganizationTester;
import org.sonarqube.tests.Tester;
import org.sonarqube.ws.Organizations.Organization;
import org.sonarqube.ws.QualityProfiles;
import org.sonarqube.ws.Rules;
import org.sonarqube.ws.WsComponents;
import org.sonarqube.ws.WsUserGroups.Group;
import org.sonarqube.ws.WsUsers;
import org.sonarqube.ws.WsUsers.CreateWsResponse.User;
import org.sonarqube.ws.client.component.ComponentsService;
import org.sonarqube.ws.client.organization.CreateWsRequest;
import org.sonarqube.ws.client.organization.OrganizationService;
import org.sonarqube.ws.client.organization.SearchWsRequest;
import org.sonarqube.ws.client.organization.UpdateWsRequest;
import org.sonarqube.ws.client.permission.AddUserWsRequest;
import org.sonarqube.ws.client.permission.PermissionsService;

import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.fail;
import static util.ItUtils.expectBadRequestError;
import static util.ItUtils.expectForbiddenError;
import static util.ItUtils.expectNotFoundError;
import static util.ItUtils.expectUnauthorizedError;
import static util.ItUtils.runProjectAnalysis;
import static util.ItUtils.setServerProperty;

public class OrganizationTest {

  private static final String SETTING_ANYONE_CAN_CREATE_ORGANIZATIONS = "sonar.organizations.anyoneCanCreate";
  private static final String DEFAULT_ORGANIZATION_KEY = "default-organization";
  private static final String NAME = "Foo Company";
  // private static final String KEY = "foo-company";
  private static final String DESCRIPTION = "the description of Foo company";
  private static final String URL = "https://www.foo.fr";
  private static final String AVATAR_URL = "https://www.foo.fr/corporate_logo.png";

  @ClassRule
  public static Orchestrator orchestrator = Category6Suite.ORCHESTRATOR;
  @Rule
  public Tester tester = new Tester(orchestrator);
  @Rule
  public ExpectedException expectedException = ExpectedException.none();

  @After
  public void tearDown() {
    setServerProperty(orchestrator, SETTING_ANYONE_CAN_CREATE_ORGANIZATIONS, null);
  }

  @Test
  public void default_organization_should_exist() {
    Organization defaultOrg = tester.organizations().service().search(SearchWsRequest.builder().build())
      .getOrganizationsList()
      .stream()
      .filter(Organization::getGuarded)
      .findFirst()
      .orElseThrow(IllegalStateException::new);
    assertThat(defaultOrg.getKey()).isEqualTo(DEFAULT_ORGANIZATION_KEY);
    assertThat(defaultOrg.getName()).isEqualTo("Default Organization");
  }

  @Test
  public void default_organization_can_not_be_deleted() {
    expectBadRequestError(() -> tester.organizations().service().delete(DEFAULT_ORGANIZATION_KEY));
  }

  @Test
  public void create_update_and_delete_organizations() {
    OrganizationService service = tester.organizations().service();

    Organization org = tester.organizations().generate(o -> o
      .setName(NAME)
      .setDescription(DESCRIPTION)
      .setUrl(URL)
      .setAvatar(AVATAR_URL)
      .build());
    assertThat(org.getName()).isEqualTo(NAME);
    assertThat(org.getDescription()).isEqualTo(DESCRIPTION);
    assertThat(org.getUrl()).isEqualTo(URL);
    assertThat(org.getAvatar()).isEqualTo(AVATAR_URL);

    verifyOrganization(org, NAME, DESCRIPTION, URL, AVATAR_URL);
    assertThatBuiltInQualityProfilesExist(org);

    // update by key
    service.update(new UpdateWsRequest.Builder()
      .setKey(org.getKey())
      .setName("new name")
      .setDescription("new description")
      .setUrl("new url")
      .setAvatar("new avatar url")
      .build());
    verifyOrganization(org, "new name", "new description", "new url", "new avatar url");

    // remove optional fields
    service.update(new UpdateWsRequest.Builder()
      .setKey(org.getKey())
      .setName("new name 2")
      .setDescription("")
      .setUrl("")
      .setAvatar("")
      .build());
    verifyOrganization(org, "new name 2", null, null, null);

    // delete organization
    service.delete(org.getKey());
    assertThatOrganizationDoesNotExit(org);
    assertThatQualityProfilesDoNotExist(org);

    // create again
    service.create(new CreateWsRequest.Builder()
      .setName(NAME)
      .setKey(org.getKey())
      .build())
      .getOrganization();
    verifyOrganization(org, NAME, null, null, null);
  }

  @Test
  public void create_generates_key_from_name() {
    // create organization without key
    String name = "Foo  Company to keyize";
    String expectedKey = "foo-company-to-keyize";
    Organization createdOrganization = tester.organizations().service().create(new CreateWsRequest.Builder()
      .setName(name)
      .build())
      .getOrganization();
    assertThat(createdOrganization.getKey()).isEqualTo(expectedKey);
    verifyOrganization(createdOrganization, name, null, null, null);
  }

  @Test
  public void anonymous_user_cannot_administrate_organization() {
    Organization org = tester.organizations().generate();
    OrganizationTester anonymousTester = tester.asAnonymous().organizations();

    expectForbiddenError(() -> anonymousTester.generate());
    expectUnauthorizedError(() -> anonymousTester.service().update(new UpdateWsRequest.Builder().setKey(org.getKey()).setName("new name").build()));
    expectUnauthorizedError(() -> anonymousTester.service().delete(org.getKey()));
  }

  @Test
  public void logged_in_user_cannot_administrate_organization() {
    Organization org = tester.organizations().generate();
    User user = tester.users().generate();
    OrganizationTester userTester = tester.as(user.getLogin()).organizations();

    expectForbiddenError(() -> userTester.generate());
    expectForbiddenError(() -> userTester.service().update(new UpdateWsRequest.Builder().setKey(org.getKey()).setName("new name").build()));
    expectForbiddenError(() -> userTester.service().delete(org.getKey()));
  }

  @Test
  public void logged_in_user_can_administrate_organization_if_root() {
    User user = tester.users().generate();
    OrganizationTester asUser = tester.as(user.getLogin()).organizations();

    tester.wsClient().roots().setRoot(user.getLogin());
    Organization org = asUser.generate();

    // delete org, attempt recreate when no root anymore and ensure it can't anymore
    asUser.service().delete(org.getKey());

    tester.wsClient().roots().unsetRoot(user.getLogin());
    expectForbiddenError(() -> asUser.generate());
  }

  @Test
  public void an_organization_member_can_analyze_project() {
    Organization organization = tester.organizations().generate();
    User user = tester.users().generate();
    Group group = tester.groups().generate(organization);
    // users.removeGroups("sonar-users");
    tester.organizations().service().addMember(organization.getKey(), user.getLogin());
    addPermissionsToUser(organization.getKey(), user.getLogin(), "provisioning", "scan");

    runProjectAnalysis(orchestrator, "shared/xoo-sample",
      "sonar.organization", organization.getKey(),
      "sonar.login", user.getLogin(),
      "sonar.password", user.getLogin());
    ComponentsService componentsService = tester.as(user.getLogin()).wsClient().components();
    assertThat(searchSampleProject(organization.getKey(), componentsService).getComponentsList()).hasSize(1);
  }

  @Test
  public void by_default_anonymous_cannot_analyse_project_on_organization() {
    Organization organization = tester.organizations().generate();

    try {
      runProjectAnalysis(orchestrator, "shared/xoo-sample",
        "sonar.organization", organization.getKey());
      fail();
    } catch (BuildFailureException e) {
      assertThat(e.getResult().getLogs()).contains("Insufficient privileges");
    }

    ComponentsService componentsService = tester.wsClient().components();
    assertThat(searchSampleProject(organization.getKey(), componentsService).getComponentsCount()).isEqualTo(0);
  }

  @Test
  public void by_default_anonymous_can_browse_project_on_organization() {
    Organization organization = tester.organizations().generate();

    runProjectAnalysis(orchestrator, "shared/xoo-sample", "sonar.organization", organization.getKey(), "sonar.login", "admin", "sonar.password", "admin");

    ComponentsService componentsService = tester.asAnonymous().wsClient().components();
    assertThat(searchSampleProject(organization.getKey(), componentsService).getComponentsList()).hasSize(1);
  }

  private void addPermissionsToUser(String orgKeyAndName, String login, String permission, String... otherPermissions) {
    PermissionsService permissionsService = tester.wsClient().permissions();
    permissionsService.addUser(new AddUserWsRequest().setLogin(login).setOrganization(orgKeyAndName).setPermission(permission));
    for (String otherPermission : otherPermissions) {
      permissionsService.addUser(new AddUserWsRequest().setLogin(login).setOrganization(orgKeyAndName).setPermission(otherPermission));
    }
  }

  @Test
  public void deleting_an_organization_deletes_its_projects() {
    Organization organization = tester.organizations().generate();

    runProjectAnalysis(orchestrator, "shared/xoo-sample",
      "sonar.organization", organization.getKey(),
      "sonar.login", "admin",
      "sonar.password", "admin");
    ComponentsService componentsService = tester.wsClient().components();
    assertThat(searchSampleProject(organization.getKey(), componentsService).getComponentsList()).hasSize(1);

    tester.organizations().service().delete(organization.getKey());

    expectNotFoundError(() -> searchSampleProject(organization.getKey(), componentsService));
    assertThatOrganizationDoesNotExit(organization);
  }

  @Test
  public void return_groups_belonging_to_a_user_on_an_organization() throws Exception {
    Organization organization = tester.organizations().generate();
    User user = tester.users().generate();
    tester.organizations().service().addMember(organization.getKey(), user.getLogin());

    Group group = tester.groups().generate(organization);
    tester.groups().addMemberToGroups(organization, user.getLogin(), group.getName());

    List<WsUsers.GroupsWsResponse.Group> memberOfGroups = tester.groups().getGroupsOfUser(organization, user.getLogin());

    assertThat(memberOfGroups).extracting(WsUsers.GroupsWsResponse.Group::getName)
      .containsExactlyInAnyOrder(group.getName(), "Members");
  }

  @Test
  public void anonymous_cannot_create_organizations_even_if_anyone_is_allowed_to() {
    setServerProperty(orchestrator, SETTING_ANYONE_CAN_CREATE_ORGANIZATIONS, "true");

    expectUnauthorizedError(() -> tester.asAnonymous().organizations().generate());
  }

  @Test
  public void logged_in_user_can_create_organizations_if_anyone_is_allowed_to() {
    setServerProperty(orchestrator, SETTING_ANYONE_CAN_CREATE_ORGANIZATIONS, "true");
    User user = tester.users().generate();

    Organization organization = tester.as(user.getLogin()).organizations().generate();

    assertThat(organization.getName()).isNotEmpty();
    assertThat(organization.getKey()).isNotEmpty();
    assertThat(organization.getGuarded()).isFalse();

    List<Organization> reloadedOrgs = tester.organizations().service().search(SearchWsRequest.builder().build()).getOrganizationsList();
    assertThat(reloadedOrgs)
      .filteredOn(o -> o.getKey().equals(organization.getKey()))
      .hasSize(1);
  }

  private WsComponents.SearchWsResponse searchSampleProject(String organizationKey, ComponentsService componentsService) {
    return componentsService
      .search(new org.sonarqube.ws.client.component.SearchWsRequest()
        .setOrganization(organizationKey)
        .setQualifiers(singletonList("TRK"))
        .setQuery("sample"));
  }

  private void assertThatOrganizationDoesNotExit(Organization org) {
    SearchWsRequest request = new SearchWsRequest.Builder().setOrganizations(org.getKey()).build();
    assertThat(tester.organizations().service().search(request).getOrganizationsList()).isEmpty();
  }

  private void verifyOrganization(Organization createdOrganization, String name, String description, String url,
    String avatarUrl) {
    SearchWsRequest request = new SearchWsRequest.Builder().setOrganizations(createdOrganization.getKey()).build();
    List<Organization> result = tester.organizations().service().search(request).getOrganizationsList();
    assertThat(result).hasSize(1);
    Organization searchedOrganization = result.get(0);
    assertThat(searchedOrganization.getKey()).isEqualTo(createdOrganization.getKey());
    assertThat(searchedOrganization.getName()).isEqualTo(name);
    if (description == null) {
      assertThat(searchedOrganization.hasDescription()).isFalse();
    } else {
      assertThat(searchedOrganization.getDescription()).isEqualTo(description);
    }
    if (url == null) {
      assertThat(searchedOrganization.hasUrl()).isFalse();
    } else {
      assertThat(searchedOrganization.getUrl()).isEqualTo(url);
    }
    if (avatarUrl == null) {
      assertThat(searchedOrganization.hasAvatar()).isFalse();
    } else {
      assertThat(searchedOrganization.getAvatar()).isEqualTo(avatarUrl);
    }
  }

  private void assertThatBuiltInQualityProfilesExist(Organization org) {
    org.sonarqube.ws.client.qualityprofile.SearchWsRequest profilesRequest = new org.sonarqube.ws.client.qualityprofile.SearchWsRequest()
      .setOrganizationKey(org.getKey());
    QualityProfiles.SearchWsResponse response = tester.wsClient().qualityProfiles().search(profilesRequest);
    assertThat(response.getProfilesCount()).isGreaterThan(0);

    response.getProfilesList().forEach(p -> {
      assertThat(p.getIsInherited()).isFalse();
      assertThat(p.getProjectCount()).isEqualTo(0);
      assertThat(p.getIsBuiltIn()).isTrue();
      if (p.getName().toLowerCase(Locale.ENGLISH).contains("empty")) {
        assertThat(p.getActiveRuleCount()).isEqualTo(0);
      } else {
        assertThat(p.getActiveRuleCount()).isGreaterThan(0);
        // that allows to check the Elasticsearch index of active rules
        Rules.SearchResponse activeRulesResponse = tester.wsClient().rules().search(new org.sonarqube.ws.client.rule.SearchWsRequest().setActivation(true).setQProfile(p.getKey()));
        assertThat(activeRulesResponse.getTotal()).as("profile " + p.getName()).isEqualTo(p.getActiveRuleCount());
        assertThat(activeRulesResponse.getRulesCount()).isEqualTo((int) p.getActiveRuleCount());
      }
    });
  }

  private void assertThatQualityProfilesDoNotExist(Organization org) {
    expectNotFoundError(() -> tester.wsClient().qualityProfiles().search(
      new org.sonarqube.ws.client.qualityprofile.SearchWsRequest().setOrganizationKey(org.getKey())));
  }
}
