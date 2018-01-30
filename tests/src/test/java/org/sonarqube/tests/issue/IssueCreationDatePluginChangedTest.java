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

import com.google.common.collect.ImmutableList;
import com.sonar.orchestrator.Orchestrator;
import com.sonar.orchestrator.build.SonarScanner;
import java.io.File;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.sonar.wsclient.issue.Issue;
import org.sonar.wsclient.issue.IssueQuery;
import org.sonarqube.qa.util.Tester;
import org.sonarqube.ws.client.PostRequest;
import org.sonarqube.ws.client.WsClient;
import org.subethamail.wiser.Wiser;
import util.ItUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;
import static util.ItUtils.newUserWsClient;
import static util.ItUtils.pluginArtifact;
import static util.ItUtils.projectDir;
import static util.ItUtils.xooPlugin;

/**
 * @see <a href="https://jira.sonarsource.com/browse/MMF-766">MMF-766</a>
 */
public class IssueCreationDatePluginChangedTest {

  private static final String ISSUE_STATUS_OPEN = "OPEN";

  private static final String LANGUAGE_XOO = "xoo";

  private static final String DATETIME_FORMAT = "yyyy-MM-dd'T'HH:mm:ssZ";

  private static final String SAMPLE_PROJECT_KEY = "creation-date-sample";
  private static final String SAMPLE_PROJECT_NAME = "Creation date sample";
  private static final String SAMPLE_QUALITY_PROFILE_NAME = "creation-date-plugin";

  private final static String USER_LOGIN = "tester";
  private final static String USER_PASSWORD = "tester";
  private static final String USER_EMAIL = "tester@example.org";

  @ClassRule
  public static final Orchestrator ORCHESTRATOR = Orchestrator.builderEnv()
    .addPlugin(xooPlugin())
    .addPlugin(ItUtils.pluginArtifact("backdating-plugin-v1"))
    .addPlugin(ItUtils.pluginArtifact("backdating-customplugin"))
    .build();
  @Rule
  public Tester tester = new Tester(ORCHESTRATOR);

  private static Wiser smtpServer;

  @BeforeClass
  public static void setUp() {
    smtpServer = new Wiser(0);
    smtpServer.start();
  }

  @Before
  public void before() {
    ORCHESTRATOR.resetData();

    // Configure Sonar
    tester.settings().setGlobalSetting("email.smtp_host.secured", "localhost");
    tester.settings().setGlobalSetting("email.smtp_port.secured", Integer.toString(smtpServer.getServer().getPort()));

    // Create a user and register her to receive notification on NewIssues
    tester.users().generate(t -> t.setLogin(USER_LOGIN).setPassword(USER_PASSWORD).setEmail(USER_EMAIL)
      .setScmAccounts(ImmutableList.of("jhenry")));
    // Add notifications to the test user
    WsClient wsClient = newUserWsClient(ORCHESTRATOR, USER_LOGIN, USER_PASSWORD);
    wsClient.wsConnector().call(new PostRequest("api/notifications/add")
      .setParam("type", "NewIssues")
      .setParam("channel", "EmailNotificationChannel"))
      .failIfNotSuccessful();
    wsClient.wsConnector().call(new PostRequest("api/notifications/add")
      .setParam("type", "SQ-MyNewIssues")
      .setParam("channel", "EmailNotificationChannel"))
      .failIfNotSuccessful();
  }

  @AfterClass
  public static void stop() {
    if (smtpServer != null) {
      smtpServer.stop();
    }
  }

  @Test
  public void should_use_scm_date_for_new_issues_if_plugin_updated() throws InterruptedException {
    ItUtils.restoreProfile(ORCHESTRATOR, getClass().getResource("/issue/IssueCreationDatePluginChangedTest/profile.xml"));

    ORCHESTRATOR.getServer().provisionProject(SAMPLE_PROJECT_KEY, SAMPLE_PROJECT_NAME);
    ORCHESTRATOR.getServer().associateProjectToQualityProfile(SAMPLE_PROJECT_KEY, LANGUAGE_XOO, SAMPLE_QUALITY_PROFILE_NAME);

    // First analysis
    SonarScanner scanner = SonarScanner.create(projectDir("issue/creationDatePluginChanged"))
      .setProperty("sonar.scm.provider", "xoo")
      .setProperty("sonar.scm.disabled", "false");
    ORCHESTRATOR.executeBuild(scanner);

    // Check that issue is backdated to SCM (because it is the first analysis)
    List<Issue> issues = getIssues(issueQuery().components("creation-date-sample:src/main/xoo/sample/Sample.xoo"));
    assertThat(issues)
      .extracting(Issue::line, Issue::creationDate)
      .containsExactly(tuple(1, dateTimeParse("2005-01-01T00:00:00+0000")));

    // ensure no notification is sent as all issues are off the leak period
    waitUntilAllNotificationsAreDelivered();
    assertThat(smtpServer.getMessages()).isEmpty();

    // Update the plugin
    // uninstall plugin V1
    ItUtils.newAdminWsClient(ORCHESTRATOR).wsConnector().call(new PostRequest("api/plugins/uninstall").setParam("key", "backdating")).failIfNotSuccessful();
    // install plugin V2
    File pluginsDir = new File(ORCHESTRATOR.getServer().getHome() + "/extensions/plugins");
    ORCHESTRATOR.getConfiguration().fileSystem().copyToDirectory(pluginArtifact("backdating-plugin-v2"), pluginsDir);

    ORCHESTRATOR.restartServer();

    // New analysis that should raise 2 new issues that will be backdated
    ORCHESTRATOR.executeBuild(scanner);
    issues = getIssues(issueQuery()
      .components("creation-date-sample:src/main/xoo/sample/Sample.xoo")
      .sort("FILE_LINE"));
    assertThat(issues)
      .extracting(Issue::line, Issue::creationDate)
      .containsExactly(tuple(1, dateTimeParse("2005-01-01T00:00:00+0000")),
        tuple(2, dateTimeParse("2005-01-01T00:00:00+0000")),
        tuple(3, dateTimeParse("2005-01-01T00:00:00+0000")));

    // ensure no notification is sent as all issues are off the leak period
    waitUntilAllNotificationsAreDelivered();
    assertThat(smtpServer.getMessages()).isEmpty();
  }

  private static List<Issue> getIssues(IssueQuery query) {
    return ORCHESTRATOR.getServer().wsClient().issueClient().find(query).list();
  }

  private static IssueQuery issueQuery() {
    return IssueQuery.create().statuses(ISSUE_STATUS_OPEN);
  }

  private static Date dateTimeParse(String expectedDate) {
    try {
      return new SimpleDateFormat(DATETIME_FORMAT).parse(expectedDate);
    } catch (ParseException e) {
      throw new RuntimeException(e);
    }
  }

  private static void waitUntilAllNotificationsAreDelivered() throws InterruptedException {
    for (int i = 0; i < 10; i++) {
      if (smtpServer.getMessages().size() == 3) {
        break;
      }
      Thread.sleep(1_000);
    }
  }

}
