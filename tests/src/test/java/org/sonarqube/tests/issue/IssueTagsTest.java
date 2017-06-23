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
package org.sonarqube.tests.issue;

import com.sonar.orchestrator.Orchestrator;
import com.sonar.orchestrator.build.SonarScanner;
import org.sonarqube.tests.Category6Suite;
import java.util.ArrayList;
import java.util.List;
import javax.annotation.Nullable;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.sonarqube.tests.Tester;
import org.sonarqube.ws.Organizations.Organization;
import org.sonarqube.ws.WsUsers.CreateWsResponse.User;
import org.sonarqube.ws.client.issue.SearchWsRequest;
import org.sonarqube.ws.client.permission.AddUserWsRequest;
import org.sonarqube.ws.client.project.CreateRequest;
import util.ItUtils;

import static java.util.Arrays.asList;
import static org.apache.commons.lang.RandomStringUtils.randomAlphabetic;
import static org.assertj.core.api.Assertions.assertThat;
import static util.ItUtils.newProjectKey;
import static util.ItUtils.projectDir;
import static util.ItUtils.restoreProfile;

/**
 * Tests WS api/issues/tags
 */
public class IssueTagsTest {

  @ClassRule
  public static Orchestrator orchestrator = Category6Suite.ORCHESTRATOR;

  @Rule
  public Tester tester = new Tester(orchestrator);

  private Organization organization;

  @Before
  public void setUp() {
    organization = tester.organizations().generate();
  }

  @Test
  public void getTags() {
    restoreProfile(orchestrator, IssueTagsTest.class.getResource("/issue/one-issue-per-line-profile.xml"), organization.getKey());
    String projectKey = newProjectKey();
    tester.wsClient().projects().create(
      CreateRequest.builder()
        .setKey(projectKey)
        .setOrganization(organization.getKey())
        .setName(randomAlphabetic(10))
        .setVisibility("private")
        .build());
    analyzeProject(projectKey);

    String issue = tester.wsClient().issues().search(new SearchWsRequest()).getIssues(0).getKey();
    tester.wsClient().issues().setTags(issue, "bla", "blubb");

    String[] publicTags = {"bad-practice", "convention", "pitfall"};
    String[] privateTags = {"bad-practice", "bla", "blubb", "convention", "pitfall"};
    String defaultOrganization = null;

    // anonymous must not see custom tags of private project
    {
      String anonymous = null;
      assertTags(anonymous, organization.getKey(), publicTags);
      assertTags(anonymous, defaultOrganization, publicTags);
    }

    // stranger must not see custom tags of private project
    {
      User stranger = tester.users().generate();
      assertTags(stranger.getLogin(), organization.getKey(), publicTags);
      assertTags(stranger.getLogin(), defaultOrganization, publicTags);
    }

    // member with user permission must be able to see custom tags of private project, if he provides the organization parameter
    {
      User member = tester.users().generate();
      addMemberToOrganization(member);
      grantUserPermission(projectKey, member);
      assertTags(member.getLogin(), organization.getKey(), privateTags);
      assertTags(member.getLogin(), defaultOrganization, publicTags);
    }
  }

  private void addMemberToOrganization(User member) {
    tester.organizations().service().addMember(organization.getKey(), member.getLogin());
  }

  private void grantUserPermission(String projectKey, User member) {
    tester.wsClient().permissions().addUser(
      new AddUserWsRequest()
        .setLogin(member.getLogin())
        .setPermission("user")
        .setProjectKey(projectKey));
  }

  private void assertTags(@Nullable String userLogin, @Nullable String organization, String... expectedTags) {
    assertThat(
      (List<String>) ItUtils.jsonToMap(
        tester.as(userLogin)
          .wsClient()
          .issues()
          .getTags(organization)
          .content())
        .get("tags")).containsExactly(
          expectedTags);
  }

  private void analyzeProject(String projectKey) {
    List<String> keyValueProperties = new ArrayList<>(asList(
      "sonar.projectKey", projectKey,
      "sonar.organization", organization.getKey(),
      "sonar.profile", "one-issue-per-line-profile",
      "sonar.login", "admin", "sonar.password", "admin",
      "sonar.scm.disabled", "false"));
    orchestrator.executeBuild(SonarScanner.create(projectDir("shared/xoo-sample"), keyValueProperties.toArray(new String[0])));
  }
}
