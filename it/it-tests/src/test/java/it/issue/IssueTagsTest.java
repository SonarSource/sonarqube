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
package it.issue;

import com.sonar.orchestrator.Orchestrator;
import com.sonar.orchestrator.build.SonarScanner;
import it.Category6Suite;
import java.util.ArrayList;
import java.util.List;
import javax.annotation.Nullable;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;
import org.sonarqube.ws.client.issue.SearchWsRequest;
import org.sonarqube.ws.client.organization.CreateWsRequest;
import org.sonarqube.ws.client.permission.AddUserWsRequest;
import org.sonarqube.ws.client.project.CreateRequest;
import util.ItUtils;
import util.user.UserRule;

import static it.Category6Suite.enableOrganizationsSupport;
import static java.util.Arrays.asList;
import static org.apache.commons.lang.RandomStringUtils.randomAlphabetic;
import static org.assertj.core.api.Assertions.assertThat;
import static util.ItUtils.newAdminWsClient;
import static util.ItUtils.newProjectKey;
import static util.ItUtils.newUserWsClient;
import static util.ItUtils.projectDir;
import static util.ItUtils.restoreProfile;

/**
 * Tests WS api/issues/tags
 */
public class IssueTagsTest {

  @ClassRule
  public static Orchestrator orchestrator = Category6Suite.ORCHESTRATOR;
  @ClassRule
  public static UserRule userRule = UserRule.from(orchestrator);

  private String organizationKey;

  @BeforeClass
  public static void beforeClass() throws Exception {
    enableOrganizationsSupport();
  }

  @Before
  public void setUp() throws Exception {
    organizationKey = ItUtils.newOrganizationKey();
    newAdminWsClient(orchestrator).organizations().create(CreateWsRequest.builder()
      .setKey(organizationKey)
      .setName(organizationKey)
      .build());
  }

  @Test
  public void getTags() throws Exception {
    restoreProfile(orchestrator, IssueTagsTest.class.getResource("/issue/one-issue-per-line-profile.xml"), organizationKey);
    String projectKey = newProjectKey();
    ItUtils.newAdminWsClient(orchestrator).projects().create(
      CreateRequest.builder()
        .setKey(projectKey)
        .setOrganization(organizationKey)
        .setName(randomAlphabetic(10))
        .setVisibility("private")
        .build());
    analyzeProject(projectKey);

    String issue = newAdminWsClient(orchestrator).issues().search(new SearchWsRequest()).getIssues(0).getKey();
    newAdminWsClient(orchestrator).issues().setTags(issue, "bla", "blubb");

    String[] publicTags = {"bad-practice", "convention", "pitfall"};
    String[] privateTags = {"bad-practice", "bla", "blubb", "convention", "pitfall"};
    String defaultOrganization = null;

    // anonymous must not see custom tags of private project
    {
      String anonymous = null;
      String anonymousPassword = null;
      assertTags(anonymous, anonymousPassword, organizationKey, publicTags);
      assertTags(anonymous, anonymousPassword, defaultOrganization, publicTags);
    }

    // stranger must not see custom tags of private project
    {
      String stranger = randomAlphabetic(10).toLowerCase();
      String strangerPassword = randomAlphabetic(8);
      userRule.createUser(stranger, strangerPassword);
      assertTags(stranger, strangerPassword, organizationKey, publicTags);
      assertTags(stranger, strangerPassword, defaultOrganization, publicTags);
    }

    // member with user permission must be able to see custom tags of private project, if he provides the organization parameter
    {
      String member = randomAlphabetic(10).toLowerCase();
      String memberPassword = randomAlphabetic(8);
      userRule.createUser(member, memberPassword);
      addMember(member);
      grantUserPermission(projectKey, member);
      assertTags(member, memberPassword, organizationKey, privateTags);
      assertTags(member, memberPassword, defaultOrganization, publicTags);
    }
  }

  private void addMember(String member) {
    newAdminWsClient(orchestrator).organizations().addMember(organizationKey, member);
  }

  private void grantUserPermission(String projectKey, String member) {
    newAdminWsClient(orchestrator).permissions().addUser(
      new AddUserWsRequest()
        .setLogin(member)
        .setPermission("user")
        .setProjectKey(projectKey));
  }

  private void assertTags(@Nullable String user, @Nullable String password, @Nullable String organization, String... expectedTags) {
    assertThat(
      (List<String>) ItUtils.jsonToMap(
        newUserWsClient(orchestrator, user, password)
          .issues()
          .getTags(organization)
          .content())
        .get("tags")).containsExactly(
          expectedTags);
  }

  private void analyzeProject(String projectKey) {
    List<String> keyValueProperties = new ArrayList<>(asList(
      "sonar.projectKey", projectKey,
      "sonar.organization", organizationKey,
      "sonar.profile", "one-issue-per-line-profile",
      "sonar.login", "admin", "sonar.password", "admin",
      "sonar.scm.disabled", "false"));
    orchestrator.executeBuild(SonarScanner.create(projectDir("shared/xoo-sample"), keyValueProperties.toArray(new String[0])));
  }
}
