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
import com.sonar.orchestrator.build.BuildFailureException;
import it.Category6Suite;
import java.util.List;
import java.util.Locale;
import java.util.function.Consumer;
import java.util.function.Function;
import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.sonarqube.ws.Organizations;
import org.sonarqube.ws.QualityProfiles;
import org.sonarqube.ws.Rules;
import org.sonarqube.ws.WsComponents;
import org.sonarqube.ws.WsUsers;
import org.sonarqube.ws.client.HttpException;
import org.sonarqube.ws.client.PostRequest;
import org.sonarqube.ws.client.WsClient;
import org.sonarqube.ws.client.component.ComponentsService;
import org.sonarqube.ws.client.organization.CreateWsRequest;
import org.sonarqube.ws.client.organization.OrganizationService;
import org.sonarqube.ws.client.organization.SearchWsRequest;
import org.sonarqube.ws.client.organization.UpdateWsRequest;
import org.sonarqube.ws.client.permission.AddUserWsRequest;
import org.sonarqube.ws.client.permission.PermissionsService;
import org.sonarqube.ws.client.user.GroupsRequest;
import util.user.GroupManagement;
import util.user.Groups;
import util.user.UserRule;

import static it.Category6Suite.enableOrganizationsSupport;
import static java.util.Collections.singletonList;
import static org.apache.commons.lang.RandomStringUtils.randomAlphabetic;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.fail;
import static util.ItUtils.deleteOrganizationsIfExists;
import static util.ItUtils.newAdminWsClient;
import static util.ItUtils.newUserWsClient;
import static util.ItUtils.newWsClient;
import static util.ItUtils.resetSettings;
import static util.ItUtils.runProjectAnalysis;
import static util.ItUtils.setServerProperty;

public class OrganizationTest {
  private static final String DEFAULT_ORGANIZATION_KEY = "default-organization";
  private static final String NAME = "Foo Company";
  private static final String KEY = "foo-company";
  private static final String DESCRIPTION = "the description of Foo company";
  private static final String URL = "https://www.foo.fr";
  private static final String AVATAR_URL = "https://www.foo.fr/corporate_logo.png";
  private static final String SETTING_ANYONE_CAN_CREATE_ORGANIZATIONS = "sonar.organizations.anyoneCanCreate";
  private static final String USER_LOGIN = "foo";

  @ClassRule
  public static Orchestrator orchestrator = Category6Suite.ORCHESTRATOR;
  @ClassRule
  public static UserRule userRule = UserRule.from(orchestrator);
  @Rule
  public ExpectedException expectedException = ExpectedException.none();

  private WsClient adminClient = newAdminWsClient(orchestrator);
  private OrganizationService anonymousOrganizationService = newWsClient(orchestrator).organizations();
  private OrganizationService adminOrganizationService = adminClient.organizations();

  @BeforeClass
  public static void enableOrganizations() throws Exception {
    enableOrganizationsSupport();
  }

  @Before
  public void setUp() throws Exception {
    resetSettings(orchestrator, null, SETTING_ANYONE_CAN_CREATE_ORGANIZATIONS);
    deleteOrganizationsIfExists(orchestrator, KEY, "an-org");
    userRule.deactivateUsers(USER_LOGIN);
  }

  @After
  public void tearDown() throws Exception {
    deleteOrganizationsIfExists(orchestrator, KEY, "an-org");
  }

  @Test
  public void create_update_delete_organizations_and_check_security() {
    assertThatOrganizationDoesNotExit(KEY);

    Organizations.Organization createdOrganization = adminOrganizationService.create(new CreateWsRequest.Builder()
      .setName(NAME)
      .setKey(KEY)
      .setDescription(DESCRIPTION)
      .setUrl(URL)
      .setAvatar(AVATAR_URL)
      .build())
      .getOrganization();
    assertThat(createdOrganization.getName()).isEqualTo(NAME);
    assertThat(createdOrganization.getKey()).isEqualTo(KEY);
    assertThat(createdOrganization.getDescription()).isEqualTo(DESCRIPTION);
    assertThat(createdOrganization.getUrl()).isEqualTo(URL);
    assertThat(createdOrganization.getAvatar()).isEqualTo(AVATAR_URL);

    verifySingleSearchResult(createdOrganization, NAME, DESCRIPTION, URL, AVATAR_URL);
    assertThatBuiltInQualityProfilesExist(createdOrganization.getKey());

    // update by id
    adminOrganizationService.update(new UpdateWsRequest.Builder()
      .setKey(createdOrganization.getKey())
      .setName("new name")
      .setDescription("new description")
      .setUrl("new url")
      .setAvatar("new avatar url")
      .build());
    verifySingleSearchResult(createdOrganization, "new name", "new description", "new url", "new avatar url");

    // update by key
    adminOrganizationService.update(new UpdateWsRequest.Builder()
      .setKey(createdOrganization.getKey())
      .setName("new name 2")
      .setDescription("new description 2")
      .setUrl("new url 2")
      .setAvatar("new avatar url 2")
      .build());
    verifySingleSearchResult(createdOrganization, "new name 2", "new description 2", "new url 2", "new avatar url 2");

    // remove optional fields
    adminOrganizationService.update(new UpdateWsRequest.Builder()
      .setKey(createdOrganization.getKey())
      .setName("new name 3")
      .setDescription("")
      .setUrl("")
      .setAvatar("")
      .build());
    verifySingleSearchResult(createdOrganization, "new name 3", null, null, null);

    // delete organization
    adminOrganizationService.delete(createdOrganization.getKey());
    assertThatOrganizationDoesNotExit(createdOrganization.getKey());
    assertThatQualityProfilesDoNotExist(createdOrganization.getKey());

    adminOrganizationService.create(new CreateWsRequest.Builder()
      .setName(NAME)
      .setKey(KEY)
      .build())
      .getOrganization();
    verifySingleSearchResult(createdOrganization, NAME, null, null, null);

    // verify anonymous can't create update nor delete an organization by default
    verifyAnonymousNotAuthorized(service -> service.create(new CreateWsRequest.Builder().setName("An org").build()));
    assertThatUserNotAuthenticated(service -> service.update(new UpdateWsRequest.Builder().setKey(KEY).setName("new name").build()));
    assertThatUserNotAuthenticated(service -> service.delete(KEY));

    // verify logged in user without any permission can't create update nor delete an organization by default
    userRule.createUser(USER_LOGIN, USER_LOGIN);
    assertThatUserNotAuthorized(USER_LOGIN, USER_LOGIN, service -> service.create(new CreateWsRequest.Builder().setName("An org").build()));
    assertThatUserNotAuthorized(USER_LOGIN, USER_LOGIN, service -> service.update(new UpdateWsRequest.Builder().setKey(KEY).setName("new name").build()));
    assertThatUserNotAuthorized(USER_LOGIN, USER_LOGIN, service -> service.delete(KEY));

    setServerProperty(orchestrator, SETTING_ANYONE_CAN_CREATE_ORGANIZATIONS, "true");
    // verify anonymous still can't create update nor delete an organization if property is true
    assertThatUserNotAuthenticated(service -> service.create(new CreateWsRequest.Builder().setName("An org").build()));
    assertThatUserNotAuthenticated(service -> service.update(new UpdateWsRequest.Builder().setKey(KEY).setName("new name").build()));
    assertThatUserNotAuthenticated(service -> service.delete(KEY));

    // verify logged in user without any permission can't create nor update nor delete an organization if property is true
    assertThatUserNotAuthorized(USER_LOGIN, USER_LOGIN, service -> service.update(new UpdateWsRequest.Builder().setKey(KEY).setName("new name").build()));
    assertThatUserNotAuthorized(USER_LOGIN, USER_LOGIN, service -> service.delete(KEY));
    // clean-up
    adminOrganizationService.delete(KEY);
    verifySingleSearchResult(
      verifyUserAuthorized(USER_LOGIN, USER_LOGIN, service -> service.create(new CreateWsRequest.Builder().setName("An org").build())).getOrganization(),
      "An org", null, null, null);
  }

  private void verifyAnonymousNotAuthorized(Consumer<OrganizationService> consumer) {
    try {
      consumer.accept(anonymousOrganizationService);
      fail("An HttpException should have been raised");
    } catch (HttpException e) {
      assertThat(e.code()).isEqualTo(403);
    }
  }

  private void assertThatUserNotAuthenticated(Consumer<OrganizationService> consumer) {
    try {
      consumer.accept(anonymousOrganizationService);
      fail("An HttpException should have been raised");
    } catch (HttpException e) {
      assertThat(e.code()).isEqualTo(401);
    }
  }

  private void assertThatUserNotAuthorized(String login, String password, Consumer<OrganizationService> consumer) {
    try {
      OrganizationService organizationService = newUserWsClient(orchestrator, login, password).organizations();
      consumer.accept(organizationService);
      fail("An HttpException should have been raised");
    } catch (HttpException e) {
      assertThat(e.code()).isEqualTo(403);
    }
  }

  private <T> T verifyUserAuthorized(String login, String password, Function<OrganizationService, T> consumer) {
    OrganizationService organizationService = newUserWsClient(orchestrator, login, password).organizations();
    return consumer.apply(organizationService);
  }

  @Test
  public void create_generates_key_from_name() {
    // create organization without key
    String name = "Foo  Company to keyize";
    String expectedKey = "foo-company-to-keyize";
    Organizations.Organization createdOrganization = adminOrganizationService.create(new CreateWsRequest.Builder()
      .setName(name)
      .build())
      .getOrganization();
    assertThat(createdOrganization.getKey()).isEqualTo(expectedKey);
    verifySingleSearchResult(createdOrganization, name, null, null, null);

    // clean-up
    adminOrganizationService.delete(expectedKey);
  }

  @Test
  public void default_organization_can_not_be_deleted() {
    try {
      adminOrganizationService.delete(DEFAULT_ORGANIZATION_KEY);
      fail("a HttpException should have been raised");
    } catch (HttpException e) {
      assertThat(e.code()).isEqualTo(400);
    }
  }

  @Test
  public void create_fails_if_user_is_not_root() {
    userRule.createUser(USER_LOGIN, USER_LOGIN);

    CreateWsRequest createWsRequest = new CreateWsRequest.Builder()
      .setName("bla bla")
      .build();
    OrganizationService fooUserOrganizationService = newUserWsClient(orchestrator, USER_LOGIN, USER_LOGIN).organizations();

    expect403HttpError(() -> fooUserOrganizationService.create(createWsRequest));

    userRule.setRoot(USER_LOGIN);
    assertThat(fooUserOrganizationService.create(createWsRequest).getOrganization().getKey()).isEqualTo("bla-bla");

    // delete org, attempt recreate when no root anymore and ensure it can't anymore
    fooUserOrganizationService.delete("bla-bla");
    userRule.unsetRoot(USER_LOGIN);
    expect403HttpError(() -> fooUserOrganizationService.create(createWsRequest));
  }

  @Test
  public void an_organization_member_can_analyze_project() {
    assertThatOrganizationDoesNotExit(KEY);

    Organizations.Organization createdOrganization = adminOrganizationService.create(new CreateWsRequest.Builder()
      .setName(KEY)
      .setKey(KEY)
      .build())
      .getOrganization();
    verifySingleSearchResult(createdOrganization, KEY, null, null, null);

    userRule.createUser(USER_LOGIN, USER_LOGIN);
    userRule.removeGroups("sonar-users");
    adminOrganizationService.addMember(KEY, USER_LOGIN);
    addPermissionsToUser(KEY, USER_LOGIN, "provisioning", "scan");

    runProjectAnalysis(orchestrator, "shared/xoo-sample",
      "sonar.organization", KEY, "sonar.login", USER_LOGIN, "sonar.password", USER_LOGIN);
    ComponentsService componentsService = newUserWsClient(orchestrator, USER_LOGIN, USER_LOGIN).components();
    assertThat(searchSampleProject(KEY, componentsService).getComponentsList()).hasSize(1);
  }

  @Test
  public void by_default_anonymous_cannot_analyse_project_on_organization() {
    assertThatOrganizationDoesNotExit(KEY);

    Organizations.Organization createdOrganization = adminOrganizationService.create(new CreateWsRequest.Builder()
      .setName(KEY)
      .setKey(KEY)
      .build())
      .getOrganization();
    verifySingleSearchResult(createdOrganization, KEY, null, null, null);

    try {
      runProjectAnalysis(orchestrator, "shared/xoo-sample",
        "sonar.organization", KEY);
      fail();
    } catch (BuildFailureException e) {
      assertThat(e.getResult().getLogs()).contains("Insufficient privileges");
    }

    ComponentsService componentsService = newAdminWsClient(orchestrator).components();
    assertThat(searchSampleProject(KEY, componentsService).getComponentsCount()).isEqualTo(0);
  }

  @Test
  public void by_default_anonymous_can_browse_project_on_organization() {
    adminOrganizationService.create(new CreateWsRequest.Builder()
      .setName(KEY)
      .setKey(KEY)
      .build())
      .getOrganization();

    runProjectAnalysis(orchestrator, "shared/xoo-sample", "sonar.organization", KEY, "sonar.login", "admin", "sonar.password", "admin");

    ComponentsService componentsService = newWsClient(orchestrator).components();
    assertThat(searchSampleProject(KEY, componentsService).getComponentsList()).hasSize(1);
  }

  private void addPermissionsToUser(String orgKeyAndName, String login, String permission, String... otherPermissions) {
    PermissionsService permissionsService = newAdminWsClient(orchestrator).permissions();
    permissionsService.addUser(new AddUserWsRequest().setLogin(login).setOrganization(orgKeyAndName).setPermission(permission));
    for (String otherPermission : otherPermissions) {
      permissionsService.addUser(new AddUserWsRequest().setLogin(login).setOrganization(orgKeyAndName).setPermission(otherPermission));
    }
  }

  @Test
  public void deleting_an_organization_also_deletes_projects_and_check_security() {
    assertThatOrganizationDoesNotExit(KEY);

    Organizations.Organization createdOrganization = adminOrganizationService.create(new CreateWsRequest.Builder()
      .setName(KEY)
      .setKey(KEY)
      .build())
      .getOrganization();
    verifySingleSearchResult(createdOrganization, KEY, null, null, null);

    GroupManagement groupManagement = userRule.forOrganization(KEY);

    userRule.createUser(USER_LOGIN, USER_LOGIN);
    adminOrganizationService.addMember(KEY, USER_LOGIN);
    groupManagement.createGroup("grp1");
    groupManagement.createGroup("grp2");
    groupManagement.associateGroupsToUser(USER_LOGIN, "grp1", "grp2");
    assertThat(groupManagement.getUserGroups(USER_LOGIN).getGroups())
      .extracting(Groups.Group::getName)
      .contains("grp1", "grp2");
    addPermissionsToUser(KEY, USER_LOGIN, "provisioning", "scan");

    runProjectAnalysis(orchestrator, "shared/xoo-sample",
      "sonar.organization", KEY, "sonar.login", USER_LOGIN, "sonar.password", USER_LOGIN);
    ComponentsService componentsService = newAdminWsClient(orchestrator).components();
    assertThat(searchSampleProject(KEY, componentsService).getComponentsList()).hasSize(1);

    adminOrganizationService.delete(KEY);

    expect404HttpError(() -> searchSampleProject(KEY, componentsService));
    assertThatOrganizationDoesNotExit(KEY);
  }

  @Test
  public void return_groups_belonging_to_a_user_on_an_organization() throws Exception {
    String userLogin = randomAlphabetic(10);
    String groupName = randomAlphabetic(10);
    adminClient.organizations().create(new CreateWsRequest.Builder().setKey(KEY).setName(KEY).build()).getOrganization();
    userRule.createUser(userLogin, userLogin);
    adminOrganizationService.addMember(KEY, userLogin);
    adminClient.wsConnector().call(new PostRequest("api/user_groups/create")
      .setParam("name", groupName)
      .setParam("description", groupName)
      .setParam("organization", KEY)).failIfNotSuccessful();
    adminClient.wsConnector().call(new PostRequest("api/user_groups/add_user")
      .setParam("login", userLogin)
      .setParam("name", groupName)
      .setParam("organization", KEY)).failIfNotSuccessful();

    List<WsUsers.GroupsWsResponse.Group> result = adminClient.users().groups(
      GroupsRequest.builder().setLogin(userLogin).setOrganization(KEY).build()).getGroupsList();

    assertThat(result).extracting(WsUsers.GroupsWsResponse.Group::getName).containsOnly(groupName, "Members");
  }

  private WsComponents.SearchWsResponse searchSampleProject(String organizationKey, ComponentsService componentsService) {
    return componentsService
      .search(new org.sonarqube.ws.client.component.SearchWsRequest()
        .setOrganization(organizationKey)
        .setQualifiers(singletonList("TRK"))
        .setQuery("sample"));
  }

  private void expect403HttpError(Runnable runnable) {
    try {
      runnable.run();
      fail("Ws call should have failed");
    } catch (HttpException e) {
      assertThat(e.code()).isEqualTo(403);
    }
  }

  private void expect404HttpError(Runnable runnable) {
    try {
      runnable.run();
      fail("Ws call should have failed");
    } catch (HttpException e) {
      assertThat(e.code()).isEqualTo(404);
    }
  }

  private void assertThatOrganizationDoesNotExit(String organizationKey) {
    Organizations.SearchWsResponse searchWsResponse = anonymousOrganizationService.search(new SearchWsRequest.Builder().setOrganizations(organizationKey).build());
    assertThat(searchWsResponse.getOrganizationsList()).isEmpty();
  }

  private void verifySingleSearchResult(Organizations.Organization createdOrganization, String name, String description, String url,
    String avatarUrl) {
    List<Organizations.Organization> organizations = anonymousOrganizationService.search(new SearchWsRequest.Builder().setOrganizations(createdOrganization.getKey())
      .build()).getOrganizationsList();
    assertThat(organizations).hasSize(1);
    Organizations.Organization searchedOrganization = organizations.get(0);
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

  private void assertThatBuiltInQualityProfilesExist(String organizationKey) {
    org.sonarqube.ws.client.qualityprofile.SearchWsRequest profilesRequest = new org.sonarqube.ws.client.qualityprofile.SearchWsRequest().setOrganizationKey(organizationKey);
    QualityProfiles.SearchWsResponse response = adminClient.qualityProfiles().search(profilesRequest);
    assertThat(response.getProfilesCount()).isGreaterThan(0);
    for (QualityProfiles.SearchWsResponse.QualityProfile profile : response.getProfilesList()) {
      assertThat(profile.getIsInherited()).isFalse();
      assertThat(profile.getProjectCount()).isEqualTo(0);
      if (profile.getName().toLowerCase(Locale.ENGLISH).contains("empty")) {
        assertThat(profile.getActiveRuleCount()).isEqualTo(0);
      } else {
        assertThat(profile.getActiveRuleCount()).isGreaterThan(0);
        // that allows to check the Elasticsearch index of active rules
        Rules.SearchResponse activeRulesResponse = adminClient.rules().search(new org.sonarqube.ws.client.rule.SearchWsRequest().setActivation(true).setQProfile(profile.getKey()));
        assertThat(activeRulesResponse.getTotal()).isEqualTo(profile.getActiveRuleCount());
        assertThat(activeRulesResponse.getRulesCount()).isEqualTo((int)profile.getActiveRuleCount());
      }
    }
  }

  private void assertThatQualityProfilesDoNotExist(String organizationKey) {
    org.sonarqube.ws.client.qualityprofile.SearchWsRequest profilesRequest = new org.sonarqube.ws.client.qualityprofile.SearchWsRequest().setOrganizationKey(organizationKey);
    try {
      adminClient.qualityProfiles().search(profilesRequest);
      fail();
    } catch (HttpException e) {
      assertThat(e.code()).isEqualTo(404);
      assertThat(e.getMessage()).contains("No organization with key '" + organizationKey + "'");
    }
  }
}
