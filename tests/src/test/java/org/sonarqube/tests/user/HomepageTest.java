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
import org.sonarqube.ws.Projects.CreateWsResponse.Project;
import org.sonarqube.ws.Users;
import org.sonarqube.ws.Users.CreateWsResponse.User;
import org.sonarqube.ws.Users.CurrentWsResponse.HomepageType;
import org.sonarqube.ws.client.PostRequest;
import org.sonarqube.ws.client.projects.DeleteRequest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.sonarqube.ws.Users.CurrentWsResponse.HomepageType.PROJECT;
import static org.sonarqube.ws.Users.CurrentWsResponse.HomepageType.PROJECTS;

public class HomepageTest {

  @ClassRule
  public static final Orchestrator orchestrator = UserSuite.ORCHESTRATOR;

  @Rule
  public Tester tester = new Tester(orchestrator).disableOrganizations();

  @Test
  public void default_homepage() {
    User user = tester.users().generate();

    checkHomepage(user, PROJECTS, null);
  }

  @Test
  public void set_and_get_homepage() {
    Project project = tester.projects().provision();
    User user = tester.users().generate();

    setHomepage(user, "PROJECT", project.getKey());

    checkHomepage(user, PROJECT, project);
  }

  @Test
  public void fallback_to_projects_when_homepage_was_set_to_a_removed_project() {
    User user = tester.users().generate();
    Project project = tester.projects().provision();
    setHomepage(user, "PROJECT", project.getKey());
    checkHomepage(user, PROJECT, project);

    tester.wsClient().projects().delete(new DeleteRequest().setProject(project.getKey()));

    checkHomepage(user, PROJECTS, null);
  }

  private void setHomepage(User user, String type, @Nullable String component) {
    tester.as(user.getLogin()).wsClient().wsConnector().call(new PostRequest("api/users/set_homepage")
      .setParam("type", type)
      .setParam("component", component))
      .failIfNotSuccessful();
  }

  private void checkHomepage(User user, HomepageType type, @Nullable Project project) {
    Users.CurrentWsResponse current = tester.as(user.getLogin()).wsClient().users().current();
    assertThat(current.getHomepage().getType()).isEqualTo(type);
    if (project != null) {
      assertThat(current.getHomepage().getComponent()).isEqualTo(project.getKey());
    } else {
      assertThat(current.getHomepage().hasComponent()).isFalse();
    }
  }
}
