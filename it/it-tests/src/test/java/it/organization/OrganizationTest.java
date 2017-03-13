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
import it.Category3Suite;
import java.util.Collections;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Function;
import org.junit.After;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.sonarqube.ws.Organizations;
import org.sonarqube.ws.WsComponents;
import org.sonarqube.ws.client.HttpException;
import org.sonarqube.ws.client.component.ComponentsService;
import org.sonarqube.ws.client.organization.CreateWsRequest;
import org.sonarqube.ws.client.organization.OrganizationService;
import org.sonarqube.ws.client.organization.SearchWsRequest;
import org.sonarqube.ws.client.organization.UpdateWsRequest;
import org.sonarqube.ws.client.permission.AddUserWsRequest;
import org.sonarqube.ws.client.permission.PermissionsService;
import util.ItUtils;
import util.user.GroupManagement;
import util.user.Groups;
import util.user.UserRule;

import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.fail;

public class OrganizationTest {
  private static final String DEFAULT_ORGANIZATION_KEY = "default-organization";
  private static final String NAME = "Foo Company";
  private static final String KEY = "foo-company";
  private static final String DESCRIPTION = "the description of Foo company";
  private static final String URL = "https://www.foo.fr";
  private static final String AVATAR_URL = "https://www.foo.fr/corporate_logo.png";
  private static final String SETTING_ANYONE_CAN_CREATE_ORGANIZATIONS = "sonar.organizations.anyoneCanCreate";

  @ClassRule
  public static Orchestrator orchestrator = Category3Suite.ORCHESTRATOR;
  @ClassRule
  public static UserRule userRule = UserRule.from(orchestrator);
  @Rule
  public ExpectedException expectedException = ExpectedException.none();

  private OrganizationService anonymousOrganizationService = ItUtils.newWsClient(orchestrator).organizations();
  private OrganizationService adminOrganizationService = ItUtils.newAdminWsClient(orchestrator).organizations();

  @Before
  public void setUp() throws Exception {
    orchestrator.resetData();
    userRule.resetUsers();
    ItUtils.resetSettings(orchestrator, null, SETTING_ANYONE_CAN_CREATE_ORGANIZATIONS);
    orchestrator.getServer().post("api/organizations/enable_support", Collections.emptyMap());
  }

  @After
  public void tearDown() throws Exception {
    orchestrator.resetData();
  }

  @Test
  public void create_update_delete_organizations_and_check_security() {
    verifyNoExtraOrganization();

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
    verifyNoExtraOrganization();

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

    // clean-up
    adminOrganizationService.delete("an-org");
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
    verifyNoExtraOrganization();

    String orgKeyAndName = "org-key";
    Organizations.Organization createdOrganization = adminOrganizationService.create(new CreateWsRequest.Builder()
      .setName(orgKeyAndName)
      .setKey(orgKeyAndName)
      .build())
      .getOrganization();
    verifySingleSearchResult(createdOrganization, orgKeyAndName, null, null, null);

    userRule.createUser("bob", "bob");
    userRule.removeGroups("sonar-users");
    adminOrganizationService.addMember(orgKeyAndName, "bob");
    addPermissionsToUser(orgKeyAndName, "bob", "provisioning", "scan");

    ItUtils.runProjectAnalysis(orchestrator, "shared/xoo-sample",
      "sonar.organization", orgKeyAndName, "sonar.login", "bob", "sonar.password", "bob");
    ComponentsService componentsService = ItUtils.newAdminWsClient(orchestrator).components();
    assertThat(searchSampleProject(orgKeyAndName, componentsService).getComponentsList()).hasSize(1);

    adminOrganizationService.delete(orgKeyAndName);
  }

  @Test
  public void by_default_anonymous_cannot_analyse_project_on_organization() {
    verifyNoExtraOrganization();

    String orgKeyAndName = "org-key";
    Organizations.Organization createdOrganization = adminOrganizationService.create(new CreateWsRequest.Builder()
      .setName(orgKeyAndName)
      .setKey(orgKeyAndName)
      .build())
      .getOrganization();
    verifySingleSearchResult(createdOrganization, orgKeyAndName, null, null, null);

    try {
      ItUtils.runProjectAnalysis(orchestrator, "shared/xoo-sample",
        "sonar.organization", orgKeyAndName);
      fail();
    } catch (BuildFailureException e) {
      assertThat(e.getResult().getLogs()).contains("Insufficient privileges");
    }

    ComponentsService componentsService = ItUtils.newAdminWsClient(orchestrator).components();
    assertThat(searchSampleProject(orgKeyAndName, componentsService).getComponentsCount()).isEqualTo(0);
    adminOrganizationService.delete(orgKeyAndName);
  }

  private void addPermissionsToUser(String orgKeyAndName, String login, String permission, String... otherPermissions) {
    PermissionsService permissionsService = ItUtils.newAdminWsClient(orchestrator).permissions();
    permissionsService.addUser(new AddUserWsRequest().setLogin(login).setOrganization(orgKeyAndName).setPermission(permission));
    for (String otherPermission : otherPermissions) {
      permissionsService.addUser(new AddUserWsRequest().setLogin(login).setOrganization(orgKeyAndName).setPermission(otherPermission));
    }
  }

  @Test
  public void deleting_an_organization_also_deletes_group_permissions_and_projects_and_check_security() {
    verifyNoExtraOrganization();

    String orgKeyAndName = "org-key";
    Organizations.Organization createdOrganization = adminOrganizationService.create(new CreateWsRequest.Builder()
      .setName(orgKeyAndName)
      .setKey(orgKeyAndName)
      .build())
      .getOrganization();
    verifySingleSearchResult(createdOrganization, orgKeyAndName, null, null, null);

    GroupManagement groupManagement = userRule.forOrganization(orgKeyAndName);

    userRule.createUser("bob", "bob");
    adminOrganizationService.addMember(orgKeyAndName, "bob");
    groupManagement.createGroup("grp1");
    groupManagement.createGroup("grp2");
    groupManagement.associateGroupsToUser("bob", "grp1", "grp2");
    assertThat(groupManagement.getUserGroups("bob").getGroups())
      .extracting(Groups.Group::getName)
      .contains("grp1", "grp2");
    addPermissionsToUser(orgKeyAndName, "bob", "provisioning", "scan");

    ItUtils.runProjectAnalysis(orchestrator, "shared/xoo-sample",
      "sonar.organization", orgKeyAndName, "sonar.login", "bob", "sonar.password", "bob");
    ComponentsService componentsService = ItUtils.newAdminWsClient(orchestrator).components();
    assertThat(searchSampleProject(orgKeyAndName, componentsService).getComponentsList()).hasSize(1);

    adminOrganizationService.delete(orgKeyAndName);

    expect404HttpError(() -> searchSampleProject(orgKeyAndName, componentsService));
    assertThat(groupManagement.getUserGroups("bob").getGroups())
      .extracting(Groups.Group::getName)
      .doesNotContain("grp1", "grp2");

    verifyNoExtraOrganization();
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

  private void verifyNoExtraOrganization() {
    Organizations.SearchWsResponse searchWsResponse = anonymousOrganizationService.search(new SearchWsRequest.Builder().build());
    List<Organizations.Organization> organizationsList = searchWsResponse.getOrganizationsList();
    assertThat(organizationsList).hasSize(1);
    assertThat(organizationsList.iterator().next().getKey()).isEqualTo(DEFAULT_ORGANIZATION_KEY);
  }

  private void verifySingleSearchResult(Organizations.Organization createdOrganization, String name, String description, String url,
    String avatarUrl) {
    List<Organizations.Organization> organizations = anonymousOrganizationService.search(new SearchWsRequest.Builder()
        .build()).getOrganizationsList();
    assertThat(organizations).hasSize(2);
    Organizations.Organization searchedOrganization = organizations.stream()
      .filter(organization -> !DEFAULT_ORGANIZATION_KEY.equals(organization.getKey()))
      .findFirst()
      .get();
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
