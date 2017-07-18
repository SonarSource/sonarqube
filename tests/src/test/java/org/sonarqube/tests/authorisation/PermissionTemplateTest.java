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
package org.sonarqube.tests.authorisation;

import com.sonar.orchestrator.Orchestrator;
import java.util.Optional;
import org.junit.After;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.DisableOnDebug;
import org.junit.rules.TestRule;
import org.junit.rules.Timeout;
import org.sonarqube.tests.Category6Suite;
import org.sonarqube.tests.Tester;
import org.sonarqube.ws.Organizations.Organization;
import org.sonarqube.ws.WsPermissions;
import org.sonarqube.ws.WsProjects.CreateWsResponse.Project;
import org.sonarqube.ws.WsUsers;
import org.sonarqube.ws.client.WsClient;
import org.sonarqube.ws.client.component.SearchProjectsRequest;
import org.sonarqube.ws.client.permission.AddUserToTemplateWsRequest;
import org.sonarqube.ws.client.permission.ApplyTemplateWsRequest;
import org.sonarqube.ws.client.permission.CreateTemplateWsRequest;
import org.sonarqube.ws.client.permission.PermissionsService;
import org.sonarqube.ws.client.permission.UsersWsRequest;

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
    Project project = tester.projects().generate(organization, p -> p.setVisibility("private"));
    WsUsers.CreateWsResponse.User user = tester.users().generateMember(organization);
    WsUsers.CreateWsResponse.User anotherUser = tester.users().generateMember(organization);

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
  public void indexing_errors_are_recovered_when_applying_permission_template_on_project() throws Exception {
    Organization organization = tester.organizations().generate();
    Project project = tester.projects().generate(organization, p -> p.setVisibility("private"));
    WsUsers.CreateWsResponse.User user = tester.users().generateMember(organization);
    WsUsers.CreateWsResponse.User anotherUser = tester.users().generateMember(organization);

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
  private void createAndApplyTemplate(Organization organization, Project project, WsUsers.CreateWsResponse.User user) {
    String templateName = "For user";
    PermissionsService service = tester.wsClient().permissions();
    service.createTemplate(new CreateTemplateWsRequest()
      .setOrganization(organization.getKey())
      .setName(templateName)
      .setDescription("Give admin permissions to single user"));
    service.addUserToTemplate(new AddUserToTemplateWsRequest()
      .setOrganization(organization.getKey())
      .setLogin(user.getLogin())
      .setPermission("user")
      .setTemplateName(templateName));
    service.applyTemplate(new ApplyTemplateWsRequest()
      .setOrganization(organization.getKey())
      .setProjectKey(project.getKey())
      .setTemplateName(templateName));
  }

  private void assertThatUserHasPermission(WsUsers.CreateWsResponse.User user, Organization organization, Project project) {
    assertThat(hasAdminPermission(user, organization, project)).isTrue();
  }

  private void assertThatUserDoesNotHavePermission(WsUsers.CreateWsResponse.User user, Organization organization, Project project) {
    assertThat(hasAdminPermission(user, organization, project)).isFalse();
  }

  private boolean userHasAccessToIndexedProject(WsUsers.CreateWsResponse.User user, Organization organization, Project project) {
    SearchProjectsRequest request = SearchProjectsRequest.builder().setOrganization(organization.getKey()).build();
    WsClient userSession = tester.as(user.getLogin()).wsClient();
    return userSession.components().searchProjects(request)
      .getComponentsList().stream()
      .anyMatch(c -> c.getKey().equals(project.getKey()));
  }

  private boolean hasAdminPermission(WsUsers.CreateWsResponse.User user, Organization organization, Project project) {
    UsersWsRequest request = new UsersWsRequest()
      .setOrganization(organization.getKey())
      .setProjectKey(project.getKey())
      .setPermission("user");
    WsPermissions.UsersWsResponse response = tester.wsClient().permissions().users(request);
    Optional<WsPermissions.User> found = response.getUsersList().stream()
      .filter(u -> user.getLogin().equals(u.getLogin()))
      .findFirst();
    return found.isPresent();
  }
}
