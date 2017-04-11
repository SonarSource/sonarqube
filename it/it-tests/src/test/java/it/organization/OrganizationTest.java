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
import util.ItUtils;
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
import static util.ItUtils.resetSettings;

public class OrganizationTest {
  private static final String DEFAULT_ORGANIZATION_KEY = "default-organization";
  private static final String NAME = "Foo Company";
  private static final String KEY = "foo-company";
  private static final String DESCRIPTION = "the description of Foo company";
  private static final String URL = "https://www.foo.fr";
  private static final String AVATAR_URL = "https://www.foo.fr/corporate_logo.png";
  private static final String SETTING_ANYONE_CAN_CREATE_ORGANIZATIONS = "sonar.organizations.anyoneCanCreate";

  @ClassRule
  public static Orchestrator orchestrator = Category6Suite.ORCHESTRATOR;
  @ClassRule
  public static UserRule userRule = UserRule.from(orchestrator);
  @Rule
  public ExpectedException expectedException = ExpectedException.none();

  private WsClient adminClient = newAdminWsClient(orchestrator);
  private OrganizationService anonymousOrganizationService = ItUtils.newWsClient(orchestrator).organizations();
  private OrganizationService adminOrganizationService = adminClient.organizations();

  @BeforeClass
  public static void enableOrganizations() throws Exception {
    enableOrganizationsSupport();
  }

  @Before
  public void setUp() throws Exception {
    resetSettings(orchestrator, null, SETTING_ANYONE_CAN_CREATE_ORGANIZATIONS);
    deleteOrganizationsIfExists(orchestrator, KEY, "an-org");
  }

  @After
  public void tearDown() throws Exception {
    deleteOrganizationsIfExists(orchestrator, KEY, "an-org");
  }

  @Test
  public void create_update_delete_organizations_and_check_security() {
    verifyOrganizationDoesNotExit(KEY);

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
    verifyOrganizationDoesNotExit(KEY);

    adminOrganizationService.create(new CreateWsRequest.Builder()
      .setName(NAME)
      .setKey(KEY)
      .build())
      .getOrganization();
    verifySingleSearchResult(createdOrganization, NAME, null, null, null);

    // verify anonymous can't create update nor delete an organization by default
    verifyAnonymousNotAuthorized(service -> service.create(new CreateWsRequest.Builder().setName("An org").build()));
    verifyUserNotAuthenticated(service -> service.update(new UpdateWsRequest.Builder().setKey(KEY).setName("new name").build()));
    verifyUserNotAuthenticated(service -> service.delete(KEY));

    // verify logged in user without any permission can't create update nor delete an organization by default
    userRule.createUser("john", "doh");
    verifyUserNotAuthorized("john", "doh", service -> service.create(new CreateWsRequest.Builder().setName("An org").build()));
    verifyUserNotAuthorized("john", "doh", service -> service.update(new UpdateWsRequest.Builder().setKey(KEY).setName("new name").build()));
    verifyUserNotAuthorized("john", "doh", service -> service.delete(KEY));

    ItUtils.setServerProperty(orchestrator, SETTING_ANYONE_CAN_CREATE_ORGANIZATIONS, "true");
    // verify anonymous still can't create update nor delete an organization if property is true
    verifyUserNotAuthenticated(service -> service.create(new CreateWsRequest.Builder().setName("An org").build()));
    verifyUserNotAuthenticated(service -> service.update(new UpdateWsRequest.Builder().setKey(KEY).setName("new name").build()));
    verifyUserNotAuthenticated(service -> service.delete(KEY));

    // verify logged in user without any permission can't create nor update nor delete an organization if property is true
    verifyUserNotAuthorized("john", "doh", service -> service.update(new UpdateWsRequest.Builder().setKey(KEY).setName("new name").build()));
    verifyUserNotAuthorized("john", "doh", service -> service.delete(KEY));
    // clean-up
    adminOrganizationService.delete(KEY);
    verifySingleSearchResult(
      verifyUserAuthorized("john", "doh", service -> service.create(new CreateWsRequest.Builder().setName("An org").build())).getOrganization(),
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

  private void verifyUserNotAuthenticated(Consumer<OrganizationService> consumer) {
    try {
      consumer.accept(anonymousOrganizationService);
      fail("An HttpException should have been raised");
    } catch (HttpException e) {
      assertThat(e.code()).isEqualTo(401);
    }
  }

  private void verifyUserNotAuthorized(String login, String password, Consumer<OrganizationService> consumer) {
    try {
      OrganizationService organizationService = ItUtils.newUserWsClient(orchestrator, login, password).organizations();
      consumer.accept(organizationService);
      fail("An HttpException should have been raised");
    } catch (HttpException e) {
      assertThat(e.code()).isEqualTo(403);
    }
  }

  private <T> T verifyUserAuthorized(String login, String password, Function<OrganizationService, T> consumer) {
    OrganizationService organizationService = ItUtils.newUserWsClient(orchestrator, login, password).organizations();
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
    userRule.createUser("foo", "bar");

    CreateWsRequest createWsRequest = new CreateWsRequest.Builder()
      .setName("bla bla")
      .build();
    OrganizationService fooUserOrganizationService = ItUtils.newUserWsClient(orchestrator, "foo", "bar").organizations();

    expect403HttpError(() -> fooUserOrganizationService.create(createWsRequest));

    userRule.setRoot("foo");
    assertThat(fooUserOrganizationService.create(createWsRequest).getOrganization().getKey()).isEqualTo("bla-bla");

    // delete org, attempt recreate when no root anymore and ensure it can't anymore
    fooUserOrganizationService.delete("bla-bla");
    userRule.unsetRoot("foo");
    expect403HttpError(() -> fooUserOrganizationService.create(createWsRequest));
  }

  @Test
  public void an_organization_member_can_analyze_project() {
    verifyOrganizationDoesNotExit(KEY);

    Organizations.Organization createdOrganization = adminOrganizationService.create(new CreateWsRequest.Builder()
      .setName(KEY)
      .setKey(KEY)
      .build())
      .getOrganization();
    verifySingleSearchResult(createdOrganization, KEY, null, null, null);

    userRule.createUser("bob", "bob");
    userRule.removeGroups("sonar-users");
    adminOrganizationService.addMember(KEY, "bob");
    addPermissionsToUser(KEY, "bob", "provisioning", "scan");

    ItUtils.runProjectAnalysis(orchestrator, "shared/xoo-sample",
      "sonar.organization", KEY, "sonar.login", "bob", "sonar.password", "bob");
    ComponentsService componentsService = ItUtils.newAdminWsClient(orchestrator).components();
    assertThat(searchSampleProject(KEY, componentsService).getComponentsList()).hasSize(1);
  }

  @Test
  public void by_default_anonymous_cannot_analyse_project_on_organization() {
    verifyOrganizationDoesNotExit(KEY);

    Organizations.Organization createdOrganization = adminOrganizationService.create(new CreateWsRequest.Builder()
      .setName(KEY)
      .setKey(KEY)
      .build())
      .getOrganization();
    verifySingleSearchResult(createdOrganization, KEY, null, null, null);

    try {
      ItUtils.runProjectAnalysis(orchestrator, "shared/xoo-sample",
        "sonar.organization", KEY);
      fail();
    } catch (BuildFailureException e) {
      assertThat(e.getResult().getLogs()).contains("Insufficient privileges");
    }

    ComponentsService componentsService = ItUtils.newAdminWsClient(orchestrator).components();
    assertThat(searchSampleProject(KEY, componentsService).getComponentsCount()).isEqualTo(0);
  }

  private void addPermissionsToUser(String orgKeyAndName, String login, String permission, String... otherPermissions) {
    PermissionsService permissionsService = ItUtils.newAdminWsClient(orchestrator).permissions();
    permissionsService.addUser(new AddUserWsRequest().setLogin(login).setOrganization(orgKeyAndName).setPermission(permission));
    for (String otherPermission : otherPermissions) {
      permissionsService.addUser(new AddUserWsRequest().setLogin(login).setOrganization(orgKeyAndName).setPermission(otherPermission));
    }
  }

  @Test
  public void deleting_an_organization_also_deletes_projects_and_check_security() {
    verifyOrganizationDoesNotExit(KEY);

    Organizations.Organization createdOrganization = adminOrganizationService.create(new CreateWsRequest.Builder()
      .setName(KEY)
      .setKey(KEY)
      .build())
      .getOrganization();
    verifySingleSearchResult(createdOrganization, KEY, null, null, null);

    GroupManagement groupManagement = userRule.forOrganization(KEY);

    userRule.createUser("bob", "bob");
    adminOrganizationService.addMember(KEY, "bob");
    groupManagement.createGroup("grp1");
    groupManagement.createGroup("grp2");
    groupManagement.associateGroupsToUser("bob", "grp1", "grp2");
    assertThat(groupManagement.getUserGroups("bob").getGroups())
      .extracting(Groups.Group::getName)
      .contains("grp1", "grp2");
    addPermissionsToUser(KEY, "bob", "provisioning", "scan");

    ItUtils.runProjectAnalysis(orchestrator, "shared/xoo-sample",
      "sonar.organization", KEY, "sonar.login", "bob", "sonar.password", "bob");
    ComponentsService componentsService = ItUtils.newAdminWsClient(orchestrator).components();
    assertThat(searchSampleProject(KEY, componentsService).getComponentsList()).hasSize(1);

    adminOrganizationService.delete(KEY);

    expect404HttpError(() -> searchSampleProject(KEY, componentsService));
    verifyOrganizationDoesNotExit(KEY);
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

  private void verifyOrganizationDoesNotExit(String organizationKey) {
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
}
