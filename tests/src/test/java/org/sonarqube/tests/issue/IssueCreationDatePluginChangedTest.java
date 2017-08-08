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
import java.io.File;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Test;
import org.sonar.wsclient.issue.Issue;
import org.sonar.wsclient.issue.IssueQuery;
import org.sonarqube.ws.client.PostRequest;
import util.ItUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;
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

  @ClassRule
  public static final Orchestrator ORCHESTRATOR = Orchestrator.builderEnv()
    .addPlugin(xooPlugin())
    .addPlugin(ItUtils.pluginArtifact("backdating-plugin-v1"))
    .build();

  @Before
  public void cleanup() {
    ORCHESTRATOR.resetData();
  }

  @Test
  public void should_use_scm_date_for_new_issues_if_plugin_updated() {
    ItUtils.restoreProfile(ORCHESTRATOR, getClass().getResource("/issue/IssueCreationDatePluginChangedTest/one-rule.xml"));

    ORCHESTRATOR.getServer().provisionProject(SAMPLE_PROJECT_KEY, SAMPLE_PROJECT_NAME);
    ORCHESTRATOR.getServer().associateProjectToQualityProfile(SAMPLE_PROJECT_KEY, LANGUAGE_XOO, SAMPLE_QUALITY_PROFILE_NAME);

    // First analysis
    SonarScanner scanner = SonarScanner.create(projectDir("issue/creationDatePluginChanged"))
      .setProperty("sonar.scm.provider", "xoo")
      .setProperty("sonar.scm.disabled", "false");
    ORCHESTRATOR.executeBuild(scanner);

    List<Issue> issues = getIssues(issueQuery().components("creation-date-sample:src/main/xoo/sample/Sample.xoo"));

    // Check that issue is backdated to SCM (because it is the first analysis)
    assertThat(issues)
      .extracting(Issue::line, Issue::creationDate)
      .containsExactly(tuple(1, dateTimeParse("2005-01-01T00:00:00+0000")));

    // Update the plugin
    // uninstall plugin V1
    ItUtils.newAdminWsClient(ORCHESTRATOR).wsConnector().call(new PostRequest("api/plugins/uninstall").setParam("key", "backdating")).failIfNotSuccessful();
    // install plugin V2
    File pluginsDir = new File(ORCHESTRATOR.getServer().getHome() + "/extensions/plugins");
    ORCHESTRATOR.getConfiguration().fileSystem().copyToDirectory(pluginArtifact("backdating-plugin-v2"), pluginsDir);

    ORCHESTRATOR.restartServer();

    // New analysis that should raise a new issue
    ORCHESTRATOR.executeBuild(scanner);
    issues = getIssues(issueQuery().components("creation-date-sample:src/main/xoo/sample/Sample.xoo"));
    assertThat(issues)
      .extracting(Issue::line, Issue::creationDate)
      .containsExactly(tuple(1, dateTimeParse("2005-01-01T00:00:00+0000")),
        tuple(2, dateTimeParse("2005-01-01T00:00:00+0000")));
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

}
