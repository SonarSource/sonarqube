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
package org.sonarqube.tests.issue;

import com.sonar.orchestrator.Orchestrator;
import com.sonar.orchestrator.build.SonarScanner;
import java.util.ArrayList;
import java.util.List;
import javax.annotation.Nullable;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.sonarqube.qa.util.Tester;
import org.sonarqube.tests.Category6Suite;
import org.sonarqube.ws.Organizations.Organization;
import org.sonarqube.ws.Projects.CreateWsResponse;
import org.sonarqube.ws.Users.CreateWsResponse.User;
import org.sonarqube.ws.client.issues.SearchRequest;
import org.sonarqube.ws.client.issues.SetTagsRequest;
import org.sonarqube.ws.client.issues.TagsRequest;
import org.sonarqube.ws.client.organizations.AddMemberRequest;
import org.sonarqube.ws.client.permissions.AddUserRequest;
import org.sonarqube.ws.client.projects.CreateRequest;

import static java.util.Arrays.asList;
import static java.util.Collections.singletonList;
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
      new CreateRequest()
        .setProject(projectKey)
        .setOrganization(organization.getKey())
        .setName(randomAlphabetic(10))
        .setVisibility("private"));
    analyzeProject(organization.getKey(), projectKey);

    String issue = tester.wsClient().issues().search(new SearchRequest()).getIssues(0).getKey();
    tester.wsClient().issues().setTags(new SetTagsRequest().setIssue(issue).setTags(asList("bla", "blubb")));

    String[] publicTags = {"bad-practice", "convention", "pitfall"};
    String[] privateTags = {"bad-practice", "bla", "blubb", "convention", "pitfall"};
    String defaultOrganization = tester.organizations().getDefaultOrganization().getKey();

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

  @Test
  public void tags_across_organizations() {
    Organization organization = tester.organizations().generate();
    Organization anotherOrganization = tester.organizations().generate();
    restoreProfile(orchestrator, IssueTagsTest.class.getResource("/issue/one-issue-per-line-profile.xml"), organization.getKey());
    restoreProfile(orchestrator, IssueTagsTest.class.getResource("/issue/one-issue-per-line-profile.xml"), anotherOrganization.getKey());
    CreateWsResponse.Project project = tester.projects().provision(organization);
    CreateWsResponse.Project anotherProject = tester.projects().provision(anotherOrganization);
    analyzeProject(organization.getKey(), project.getKey());
    analyzeProject(anotherOrganization.getKey(), anotherProject.getKey());
    String issue = tester.wsClient().issues().search(new SearchRequest().setProjects(singletonList(project.getKey()))).getIssues(0).getKey();
    String anotherIssue = tester.wsClient().issues().search(new SearchRequest().setProjects(singletonList(anotherProject.getKey()))).getIssues(0).getKey();
    tester.wsClient().issues().setTags(new SetTagsRequest().setIssue(issue).setTags(singletonList("first-tag")));
    tester.wsClient().issues().setTags(new SetTagsRequest().setIssue(anotherIssue).setTags(singletonList("another-tag")));

    assertThat(tester.wsClient().issues().tags(new TagsRequest().setOrganization(null)).getTagsList()).contains("first-tag", "another-tag");
  }

  private void addMemberToOrganization(User member) {
    tester.organizations().service().addMember(new AddMemberRequest().setOrganization(organization.getKey()).setLogin(member.getLogin()));
  }

  private void grantUserPermission(String projectKey, User member) {
    tester.wsClient().permissions().addUser(
      new AddUserRequest()
        .setLogin(member.getLogin())
        .setPermission("user")
        .setProjectKey(projectKey));
  }

  private void assertTags(@Nullable String userLogin, @Nullable String organization, String... expectedTags) {
    assertThat(
      tester.as(userLogin)
        .wsClient()
        .issues()
        .tags(new TagsRequest().setOrganization(organization))
        .getTagsList())
      .containsExactly(expectedTags);
  }

  private void analyzeProject(String organizationKey, String projectKey) {
    List<String> keyValueProperties = new ArrayList<>(asList(
      "sonar.projectKey", projectKey,
      "sonar.organization", organizationKey,
      "sonar.profile", "one-issue-per-line-profile",
      "sonar.login", "admin", "sonar.password", "admin",
      "sonar.scm.disabled", "false"));
    orchestrator.executeBuild(SonarScanner.create(projectDir("shared/xoo-sample"), keyValueProperties.toArray(new String[0])));
  }
}
