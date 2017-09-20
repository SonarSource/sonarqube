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

import com.google.common.collect.ObjectArrays;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import javax.mail.internet.MimeMessage;
import org.apache.commons.lang.RandomStringUtils;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameter;
import org.junit.runners.Parameterized.Parameters;
import org.sonarqube.ws.Issues.Issue;
import org.sonarqube.ws.Issues.SearchWsResponse;
import org.sonarqube.ws.Organizations.Organization;
import org.sonarqube.ws.QualityProfiles;
import org.sonarqube.ws.WsProjects.CreateWsResponse.Project;
import org.sonarqube.ws.WsUsers.CreateWsResponse.User;
import org.sonarqube.ws.client.issue.AssignRequest;
import org.sonarqube.ws.client.issue.BulkChangeRequest;
import org.sonarqube.ws.client.issue.SearchWsRequest;
import org.sonarqube.ws.client.permission.AddUserWsRequest;

import static java.lang.String.format;
import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Fail.fail;
import static util.ItUtils.runProjectAnalysis;

@RunWith(Parameterized.class)
public class IssueNotificationsTest extends AbstractIssueSMTPTest {

  private final static String PROJECT_KEY = "sample";

  private Organization organization;

  private User userWithUserRole;
  private User userWithUserRoleThroughGroups;
  private User userNotInOrganization;

  @Parameters
  public static List<Boolean> data() {
    return Arrays.asList(true, false);
  }

  @Parameter
  public boolean privateProject;

  @Before
  public void createOrganization() throws Exception {
    organization = tester.organizations().generate();
  }

  @Test
  public void notifications_for_new_issues_and_issue_changes() throws Exception {
    String version = RandomStringUtils.randomAlphanumeric(10);
    createSampleProject(privateProject ? "private" : "public");
    createUsers();
    runAnalysis(version, "shared/xoo-sample"
      , "sonar.projectDate", "2015-12-15");
    checkEmailAfterAnalysis(version);

    // change assignee
    SearchWsResponse issues = tester.wsClient().issues().search(new SearchWsRequest().setProjectKeys(singletonList(PROJECT_KEY)));
    Issue issue = issues.getIssuesList().get(0);
    tester.wsClient().issues().assign(new AssignRequest(issue.getKey(), userWithUserRole.getLogin()));

    waitUntilAllNotificationsAreDelivered(1);
    assertThat(smtpServer.getMessages()).hasSize(1);

    MimeMessage message = smtpServer.getMessages().get(0).getMimeMessage();
    assertThat(message.getHeader("To", null)).isEqualTo(format("<%s>", userWithUserRole.getEmail()));
    assertThat((String) message.getContent())
      .contains("sample/Sample.xoo")
      .contains("Assignee changed to userWithUserRole")
      .contains("More details at: http://localhost:9000/project/issues?id=sample&issues=" + issue.getKey() + "&open=" + issue.getKey());
  }

  @Test
  public void notifications_for_personalized_emails() throws Exception {
    String version = RandomStringUtils.randomAlphanumeric(10);
    // 1st analysis without any issue (because no file is analyzed)
    createSampleProject(privateProject ? "private" : "public");
    createUsers();
    tester.settings().setGlobalSettings("sonar.issues.defaultAssigneeLogin", userWithUserRole.getLogin());

    runAnalysis(version, "issue/xoo-with-scm",
       "sonar.scm.provider", "xoo",
      "sonar.scm.disabled", "false",
      "sonar.exclusions", "**/*"
      );

    waitUntilAllNotificationsAreDelivered(1);
    assertThat(smtpServer.getMessages()).isEmpty();

    // run 2nd analysis which will generate issues on the leak period
    runAnalysis(version, "issue/xoo-with-scm",
       "sonar.scm.provider", "xoo",
      "sonar.scm.disabled", "false"
    );

    waitUntilAllNotificationsAreDelivered(3);

    assertThat(smtpServer.getMessages()).hasSize(privateProject ? 3 : 4);

    // the last email sent is the personalized one
    MimeMessage message = smtpServer.getMessages().get(privateProject ? 2 : 3).getMimeMessage();

    assertThat(message.getHeader("To", null)).isEqualTo( format("<%s>", userWithUserRole.getEmail()));
    assertThat(message.getSubject()).contains("You have 13 new issues");
    assertThat((String) message.getContent())
      .contains("Project: Sample")
      .contains("Version: " + version);
  }

  /**
   * SONAR-4606
   */
  @Test
  public void notifications_for_bulk_change_ws() throws Exception {
    String version = RandomStringUtils.randomAlphanumeric(10);
    createSampleProject(privateProject ? "private" : "public");
    createUsers();
    runAnalysis(version, "shared/xoo-sample",
       "sonar.projectDate", "2015-12-15");
    checkEmailAfterAnalysis(version);

    SearchWsResponse issues = tester.wsClient().issues().search(new SearchWsRequest().setProjectKeys(singletonList(PROJECT_KEY)));
    Issue issue = issues.getIssuesList().get(0);

    // bulk change without notification by default
    tester.wsClient().issues().bulkChange(BulkChangeRequest.builder()
      .setIssues(singletonList(issue.getKey()))
      .setAssign(userWithUserRole.getLogin())
      .setSetSeverity("MINOR")
      .build());

    // bulk change with notification
    tester.wsClient().issues().bulkChange(BulkChangeRequest.builder()
      .setIssues(singletonList(issue.getKey()))
      .setSetSeverity("BLOCKER")
      .setSendNotifications(true)
      .build());

    // We are waiting for a single notification for userWithUserRole
    // for a change on MyIssues
    waitUntilAllNotificationsAreDelivered(1);

    assertThat(smtpServer.getMessages()).hasSize(1);

    MimeMessage message = smtpServer.getMessages().get(0).getMimeMessage();
    assertThat(message.getHeader("To", null))
      .isEqualTo(format("<%s>", userWithUserRole.getEmail()));
    assertThat((String) message.getContent()).contains("sample/Sample.xoo");
    assertThat((String) message.getContent()).contains("Severity: BLOCKER (was MINOR)");
    assertThat((String) message.getContent()).contains(
      "More details at: http://localhost:9000/project/issues?id=sample&issues=" + issue.getKey() + "&open=" + issue.getKey());
  }

  private void runAnalysis(String version, String projectRelativePath, String... extraParameters) throws Exception {
    String[] parameters = {
      "sonar.login", "admin",
      "sonar.password", "admin",
      "sonar.projectVersion", version,
      "sonar.organization", organization.getKey() };
    runProjectAnalysis(ORCHESTRATOR, projectRelativePath,
      ObjectArrays.concat(parameters, extraParameters, String.class));

    // Two emails should be sent for subscribers of "New issues"
    waitUntilAllNotificationsAreDelivered(2);
  }

  private void checkEmailAfterAnalysis(String version) {
    assertThat(smtpServer.getMessages()).hasSize(privateProject ? 2 : 3);

    final List<String> recipients = new ArrayList<>();

    smtpServer.getMessages().forEach(
      m -> {
        try {
          MimeMessage message = m.getMimeMessage();
          assertThat((String) message.getContent())
            .contains("Project: Sample")
            .contains("Version: " + version)
            .contains("17 new issues (new debt: 17min)")
            .contains("Type")
            .contains("One Issue Per Line (xoo): 17")
            .contains("More details at: http://localhost:9000/project/issues?id=sample&createdAt=2015-12-15T00%3A00%3A00%2B");
          recipients.add(message.getHeader("To", null));
        } catch (Exception e) {
          fail(e.getMessage());
        }
      }
    );

    if (privateProject) {
      assertThat(recipients).containsExactlyInAnyOrder(
        format("<%s>", userWithUserRole.getEmail()),
        format("<%s>", userWithUserRoleThroughGroups.getEmail()));
    } else {
      assertThat(recipients).containsExactlyInAnyOrder(
        format("<%s>", userWithUserRole.getEmail()),
        format("<%s>", userWithUserRoleThroughGroups.getEmail()),
        format("<%s>", userNotInOrganization.getEmail()));
    }
    clearSmtpMessages();
  }

  private void createSampleProject(String visibility) {
    // Create project
    QualityProfiles.CreateWsResponse.QualityProfile profile = tester.qProfiles().createXooProfile(organization);
    Project project = tester.projects().generate(organization, p -> p.setKey(PROJECT_KEY)
      .setName("Sample")
      .setVisibility(visibility));
    tester.qProfiles()
      .activateRule(profile,"xoo:OneIssuePerLine")
      .assignQProfileToProject(profile, project);
  }

  private void createUsers() {
    // Create a user with User role
    userWithUserRole = tester.users().generateMember(organization,
      u -> u.setLogin("userWithUserRole")
        .setPassword("userWithUserRole")
        .setName("userWithUserRole")
        .setEmail("userWithUserRole@nowhere.com"));
    tester.organizations().addMember(organization, userWithUserRole);
    tester.wsClient().permissions().addUser(
      new AddUserWsRequest()
        .setLogin(userWithUserRole.getLogin())
        .setProjectKey(PROJECT_KEY)
        .setPermission("user"));
    addNotificationsTo(userWithUserRole);

    // Create a user that have User role through Members group
    userWithUserRoleThroughGroups = tester.users().generate(
      u -> u.setLogin("userWithUserRoleThroughGroups")
        .setPassword("userWithUserRoleThroughGroups")
        .setName("userWithUserRoleThroughGroups")
        .setEmail("userWithUserRoleThroughGroups@nowhere.com"));
    tester.organizations().addMember(organization, userWithUserRoleThroughGroups);
    addNotificationsTo(userWithUserRoleThroughGroups);

    // Create a user that does not belongs to organization
    userNotInOrganization = tester.users().generate(
      u -> u.setLogin("userNotInOrganization")
        .setPassword("userNotInOrganization")
        .setName("userNotInOrganization")
        .setEmail("userNotInOrganization@nowhere.com"));
    addNotificationsTo(userNotInOrganization);
  }
}
