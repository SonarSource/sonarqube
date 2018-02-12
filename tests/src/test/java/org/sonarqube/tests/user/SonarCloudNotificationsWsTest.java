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
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.sonarqube.qa.util.Tester;
import org.sonarqube.ws.Notifications;
import org.sonarqube.ws.Notifications.Notification;
import org.sonarqube.ws.Projects;
import org.sonarqube.ws.Users;
import org.sonarqube.ws.client.notifications.AddRequest;
import org.sonarqube.ws.client.notifications.ListRequest;
import org.sonarqube.ws.client.notifications.RemoveRequest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;
import static util.ItUtils.expectHttpError;

public class SonarCloudNotificationsWsTest {

  @ClassRule
  public static final Orchestrator orchestrator = SonarCloudUserSuite.ORCHESTRATOR;

  @Rule
  public Tester tester = new Tester(orchestrator);

  @Test
  public void list_notifications() {
    Users.CreateWsResponse.User user = tester.users().generate();

    Notifications.ListResponse list = tester.as(user.getLogin()).wsClient().notifications().list(new ListRequest());

    assertThat(list.getNotificationsList()).isEmpty();
    assertThat(list.getChannelsList()).containsExactlyInAnyOrder("EmailNotificationChannel");
    assertThat(list.getGlobalTypesList())
      .containsExactlyInAnyOrder(
        "CeReportTaskFailure", "ChangesOnMyIssue", "SQ-MyNewIssues")
      .doesNotContain("NewAlerts", "NewFalsePositiveIssue", "NewIssues");
    assertThat(list.getPerProjectTypesList()).containsExactlyInAnyOrder(
      "CeReportTaskFailure", "ChangesOnMyIssue", "NewAlerts", "NewFalsePositiveIssue", "NewIssues", "SQ-MyNewIssues");
  }

  @Test
  public void add_global_and_project_notifications() {
    Users.CreateWsResponse.User user = tester.users().generate();
    assertThat(tester.as(user.getLogin()).wsClient().notifications().list(new ListRequest()).getNotificationsList()).isEmpty();
    Projects.CreateWsResponse.Project project = tester.projects().provision();

    tester.as(user.getLogin()).wsClient().notifications().add(new AddRequest().setChannel("EmailNotificationChannel").setType("ChangesOnMyIssue"));
    tester.as(user.getLogin()).wsClient().notifications().add(new AddRequest().setChannel("EmailNotificationChannel").setType("NewIssues").setProject(project.getKey()));

    assertThat(tester.as(user.getLogin()).wsClient().notifications().list(new ListRequest()).getNotificationsList())
      .extracting(Notification::getChannel, Notification::getType, Notification::getProject, Notification::getProjectName)
      .containsExactlyInAnyOrder(
        tuple("EmailNotificationChannel", "ChangesOnMyIssue", "", ""),
        tuple("EmailNotificationChannel", "NewIssues", project.getKey(), project.getName()));
  }

  @Test
  public void remove_global_and_project_notifications() {
    Users.CreateWsResponse.User user = tester.users().generate();
    assertThat(tester.as(user.getLogin()).wsClient().notifications().list(new ListRequest()).getNotificationsList()).isEmpty();
    Projects.CreateWsResponse.Project project = tester.projects().provision();
    tester.as(user.getLogin()).wsClient().notifications().add(new AddRequest().setChannel("EmailNotificationChannel").setType("ChangesOnMyIssue"));
    tester.as(user.getLogin()).wsClient().notifications().add(new AddRequest().setChannel("EmailNotificationChannel").setType("NewIssues").setProject(project.getKey()));

    tester.as(user.getLogin()).wsClient().notifications().remove(new RemoveRequest().setChannel("EmailNotificationChannel").setType("ChangesOnMyIssue"));
    tester.as(user.getLogin()).wsClient().notifications().remove(new RemoveRequest().setChannel("EmailNotificationChannel").setType("NewIssues").setProject(project.getKey()));

    assertThat(tester.as(user.getLogin()).wsClient().notifications().list(new ListRequest()).getNotificationsList()).isEmpty();
  }

  @Test
  public void fail_to_add_global_new_issues_notification() {
    Users.CreateWsResponse.User user = tester.users().generate();

    expectHttpError(400,
      "Value of parameter 'type' (NewIssues) must be one of: [CeReportTaskFailure, ChangesOnMyIssue, SQ-MyNewIssues]",
      () -> tester.as(user.getLogin()).wsClient().notifications().add(new AddRequest().setChannel("EmailNotificationChannel").setType("NewIssues")));
  }

  @Test
  public void fail_to_remove_global_new_issues_notification() {
    Users.CreateWsResponse.User user = tester.users().generate();

    expectHttpError(400,
      "Value of parameter 'type' (NewIssues) must be one of: [CeReportTaskFailure, ChangesOnMyIssue, SQ-MyNewIssues]",
      () -> tester.as(user.getLogin()).wsClient().notifications().remove(new RemoveRequest().setChannel("EmailNotificationChannel").setType("NewIssues")));
  }
}
