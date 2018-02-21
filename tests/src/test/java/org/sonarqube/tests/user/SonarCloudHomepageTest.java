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

package org.sonarqube.tests.user;

import com.sonar.orchestrator.Orchestrator;
import javax.annotation.Nullable;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.sonarqube.qa.util.Tester;
import org.sonarqube.ws.Organizations.Organization;
import org.sonarqube.ws.Projects.CreateWsResponse.Project;
import org.sonarqube.ws.Users;
import org.sonarqube.ws.Users.CreateWsResponse.User;
import org.sonarqube.ws.Users.CurrentWsResponse.HomepageType;
import org.sonarqube.ws.client.PostRequest;
import org.sonarqube.ws.client.projects.DeleteRequest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.sonarqube.ws.Users.CurrentWsResponse.HomepageType.MY_ISSUES;
import static org.sonarqube.ws.Users.CurrentWsResponse.HomepageType.MY_PROJECTS;
import static org.sonarqube.ws.Users.CurrentWsResponse.HomepageType.ORGANIZATION;
import static org.sonarqube.ws.Users.CurrentWsResponse.HomepageType.PROJECT;

public class SonarCloudHomepageTest {

  @ClassRule
  public static final Orchestrator orchestrator = SonarCloudUserSuite.ORCHESTRATOR;

  @Rule
  public Tester tester = new Tester(orchestrator);

  @Test
  public void default_homepage() {
    Organization organization = tester.organizations().generate();
    User user = tester.users().generateMember(organization);

    checkHomepage(user, MY_PROJECTS, null, null);
  }

  @Test
  public void set_and_get_homepage() {
    Organization organization = tester.organizations().generate();
    User user = tester.users().generateMember(organization);

    setHomepage(user, "MY_ISSUES", null, null);

    checkHomepage(user, MY_ISSUES, null, null);
  }


  @Test
  public void set_and_get_homepage_on_organizations() {
    Organization organization = tester.organizations().generate();
    User user = tester.users().generateMember(organization);

    setHomepage(user, "ORGANIZATION", organization.getKey(), null);

    checkHomepage(user, ORGANIZATION, organization, null);
  }

  @Test
  public void fallback_to_my_projects_when_homepage_was_set_to_a_removed_project() {
    Organization organization = tester.organizations().generate();
    User user = tester.users().generateMember(organization);
    Project project = tester.projects().provision(organization);
    setHomepage(user, "PROJECT", null, project.getKey());
    checkHomepage(user, PROJECT, null, project);

    tester.wsClient().projects().delete(new DeleteRequest().setProject(project.getKey()));

    checkHomepage(user, MY_PROJECTS, null, null);
  }

  @Test
  public void fallback_to_my_projects_when_homepage_was_set_to_a_removed_organization() {
    Organization organization = tester.organizations().generate();
    User user = tester.users().generateMember(organization);
    setHomepage(user, "ORGANIZATION", organization.getKey(), null);
    checkHomepage(user, ORGANIZATION, organization, null);

    tester.wsClient().organizations().delete(new org.sonarqube.ws.client.organizations.DeleteRequest().setOrganization(organization.getKey()));

    checkHomepage(user, MY_PROJECTS, null, null);
  }

  private void setHomepage(User user, String type, @Nullable String organization, @Nullable String component) {
    // Do not call the java user client as it's only generated for SonarQube, not for SonarCloud
    tester.as(user.getLogin()).wsClient().wsConnector().call(new PostRequest("api/users/set_homepage")
      .setParam("type", type)
      .setParam("organization", organization)
      .setParam("component", component))
      .failIfNotSuccessful();
  }

  private void checkHomepage(User user, HomepageType type, @Nullable Organization organization, @Nullable Project project) {
    Users.CurrentWsResponse current = tester.as(user.getLogin()).wsClient().users().current();
    assertThat(current.getHomepage().getType()).isEqualTo(type);
    if (organization != null) {
      assertThat(current.getHomepage().getOrganization()).isEqualTo(organization.getKey());
    } else {
      assertThat(current.getHomepage().hasOrganization()).isFalse();
    }
    if (project != null) {
      assertThat(current.getHomepage().getComponent()).isEqualTo(project.getKey());
    } else {
      assertThat(current.getHomepage().hasComponent()).isFalse();
    }
  }
}
