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
package org.sonarqube.tests.authorization;

import com.sonar.orchestrator.Orchestrator;
import java.util.Arrays;
import java.util.Optional;
import org.junit.After;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.DisableOnDebug;
import org.junit.rules.TestRule;
import org.junit.rules.Timeout;
import org.sonarqube.qa.util.Tester;
import org.sonarqube.tests.Category6Suite;
import org.sonarqube.ws.Organizations.Organization;
import org.sonarqube.ws.Permissions;
import org.sonarqube.ws.Permissions.CreateTemplateWsResponse;
import org.sonarqube.ws.Projects.CreateWsResponse.Project;
import org.sonarqube.ws.Users.CreateWsResponse;
import org.sonarqube.ws.client.WsClient;
import org.sonarqube.ws.client.components.SearchProjectsRequest;
import org.sonarqube.ws.client.permissions.*;

import static org.apache.commons.lang.RandomStringUtils.randomAlphabetic;
import static org.assertj.core.api.Assertions.assertThat;

public class PermissionTemplateTest {
  @ClassRule
  public static final Orchestrator orchestrator = Category6Suite.ORCHESTRATOR;

  @Rule
  public TestRule safeguard = new DisableOnDebug(Timeout.seconds(300));
  @Rule
  public Tester tester = new Tester(orchestrator)
    .setElasticsearchHttpPort(Category6Suite.SEARCH_HTTP_PORT);

  @After
  public void tearDown() throws Exception {
    unlockWritesOnProjectIndices();
  }

  @Test
  public void apply_permission_template_on_project() {
    Organization organization = tester.organizations().generate();
    Project project = createPrivateProject(organization);
    CreateWsResponse.User user = tester.users().generateMember(organization);
    CreateWsResponse.User anotherUser = tester.users().generateMember(organization);

    assertThatUserDoesNotHavePermission(user, organization, project);
    assertThatUserDoesNotHavePermission(anotherUser, organization, project);
    assertThat(userHasAccessToIndexedProject(user, organization, project)).isTrue();
    assertThat(userHasAccessToIndexedProject(anotherUser, organization, project)).isTrue();

    // create permission template that gives read permission to "user"
    createAndApplyTemplate(organization, project, user);

    assertThatUserHasPermission(user, organization, project);
    assertThatUserDoesNotHavePermission(anotherUser, organization, project);
    assertThat(userHasAccessToIndexedProject(user, organization, project)).isTrue();
    assertThat(userHasAccessToIndexedProject(anotherUser, organization, project)).isFalse();
  }

  @Test
  public void bulk_apply_template_on_projects() {
    Organization organization = tester.organizations().generate();
    CreateWsResponse.User user = tester.users().generateMember(organization);
    CreateWsResponse.User anotherUser = tester.users().generateMember(organization);
    Permissions.PermissionTemplate template = createTemplate(organization).getPermissionTemplate();
    tester.wsClient().permissions().addUserToTemplate(new AddUserToTemplateRequest()
      .setOrganization(organization.getKey())
      .setTemplateId(template.getId())
      .setLogin(user.getLogin())
      .setPermission("user"));
    Project project1 = createPrivateProject(organization);
    Project project2 = createPrivateProject(organization);
    Project untouchedProject = createPrivateProject(organization);

    tester.wsClient().permissions().bulkApplyTemplate(new BulkApplyTemplateRequest()
      .setOrganization(organization.getKey())
      .setTemplateId(template.getId())
      .setProjects(Arrays.asList(project1.getKey(), project2.getKey())));

    assertThatUserDoesNotHavePermission(anotherUser, organization, untouchedProject);
    assertThatUserDoesNotHavePermission(anotherUser, organization, project1);
    assertThatUserDoesNotHavePermission(anotherUser, organization, project2);
    assertThatUserHasPermission(user, organization, project1);
    assertThatUserHasPermission(user, organization, project2);
    assertThatUserDoesNotHavePermission(user, organization, untouchedProject);
  }

  @Test
  public void indexing_errors_are_recovered_when_applying_permission_template_on_project() throws Exception {
    Organization organization = tester.organizations().generate();
    Project project = createPrivateProject(organization);
    CreateWsResponse.User user = tester.users().generateMember(organization);
    CreateWsResponse.User anotherUser = tester.users().generateMember(organization);

    lockWritesOnProjectIndices();

    createAndApplyTemplate(organization, project, user);

    assertThatUserHasPermission(user, organization, project);
    assertThatUserDoesNotHavePermission(anotherUser, organization, project);
    assertThat(userHasAccessToIndexedProject(user, organization, project)).isTrue();
    // inconsistent, should be false. Waiting for ES to be updated.
    assertThat(userHasAccessToIndexedProject(user, organization, project)).isTrue();

    unlockWritesOnProjectIndices();

    boolean recovered = false;
    while (!recovered) {
      Thread.sleep(1_000L);
      recovered = !userHasAccessToIndexedProject(anotherUser, organization, project);
    }
  }

  private void lockWritesOnProjectIndices() throws Exception {
    tester.elasticsearch().lockWrites("issues");
    tester.elasticsearch().lockWrites("projectmeasures");
    tester.elasticsearch().lockWrites("components");
  }

  private void unlockWritesOnProjectIndices() throws Exception {
    tester.elasticsearch().unlockWrites("issues");
    tester.elasticsearch().unlockWrites("projectmeasures");
    tester.elasticsearch().unlockWrites("components");
  }

  /**
   * Gives the read access only to the specified user. All other users and groups
   * loose their ability to see the project.
   */
  private void createAndApplyTemplate(Organization organization, Project project, CreateWsResponse.User user) {
    String templateName = "For user";
    PermissionsService service = tester.wsClient().permissions();
    service.createTemplate(new CreateTemplateRequest()
      .setOrganization(organization.getKey())
      .setName(templateName)
      .setDescription("Give admin permissions to single user"));
    service.addUserToTemplate(new AddUserToTemplateRequest()
      .setOrganization(organization.getKey())
      .setLogin(user.getLogin())
      .setPermission("user")
      .setTemplateName(templateName));
    service.applyTemplate(new ApplyTemplateRequest()
      .setOrganization(organization.getKey())
      .setProjectKey(project.getKey())
      .setTemplateName(templateName));
  }

  private CreateTemplateWsResponse createTemplate(Organization organization) {
    return tester.wsClient().permissions().createTemplate(new CreateTemplateRequest()
      .setOrganization(organization.getKey())
      .setName(randomAlphabetic(20)));
  }

  private Project createPrivateProject(Organization organization) {
    return tester.projects().provision(organization, p -> p.setVisibility("private"));
  }

  private void assertThatUserHasPermission(CreateWsResponse.User user, Organization organization, Project project) {
    assertThat(hasBrowsePermission(user, organization, project)).isTrue();
  }

  private void assertThatUserDoesNotHavePermission(CreateWsResponse.User user, Organization organization, Project project) {
    assertThat(hasBrowsePermission(user, organization, project)).isFalse();
  }

  private boolean userHasAccessToIndexedProject(CreateWsResponse.User user, Organization organization, Project project) {
    SearchProjectsRequest request = new SearchProjectsRequest().setOrganization(organization.getKey());
    WsClient userSession = tester.as(user.getLogin()).wsClient();
    return userSession.components().searchProjects(request)
      .getComponentsList().stream()
      .anyMatch(c -> c.getKey().equals(project.getKey()));
  }

  private boolean hasBrowsePermission(CreateWsResponse.User user, Organization organization, Project project) {
    UsersRequest request = new UsersRequest()
      .setOrganization(organization.getKey())
      .setProjectKey(project.getKey())
      .setPermission("user");
    Permissions.UsersWsResponse response = tester.wsClient().permissions().users(request);
    Optional<Permissions.User> found = response.getUsersList().stream()
      .filter(u -> user.getLogin().equals(u.getLogin()))
      .findFirst();
    return found.isPresent();
  }
}
