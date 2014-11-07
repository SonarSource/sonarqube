/*
 * SonarQube, open source software quality management tool.
 * Copyright (C) 2008-2014 SonarSource
 * mailto:contact AT sonarsource DOT com
 *
 * SonarQube is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * SonarQube is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */

package org.sonar.data.issues;

import com.google.common.collect.Iterables;
import org.junit.*;
import org.junit.rules.TemporaryFolder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sonar.api.config.Settings;
import org.sonar.api.issue.Issue;
import org.sonar.api.rule.RuleKey;
import org.sonar.api.rule.Severity;
import org.sonar.core.component.ComponentDto;
import org.sonar.core.issue.db.IssueDto;
import org.sonar.core.persistence.DbSession;
import org.sonar.core.persistence.TestDatabase;
import org.sonar.core.rule.RuleDto;
import org.sonar.process.NetworkUtils;
import org.sonar.process.ProcessConstants;
import org.sonar.process.Props;
import org.sonar.search.SearchServer;
import org.sonar.server.component.ComponentTesting;
import org.sonar.server.issue.IssueTesting;
import org.sonar.server.issue.index.IssueIndex;
import org.sonar.server.issue.index.IssueNormalizer;
import org.sonar.server.rule.RuleTesting;
import org.sonar.server.search.IndexDefinition;
import org.sonar.server.search.SearchClient;
import org.sonar.server.search.action.InsertDto;
import org.sonar.server.search.action.RefreshIndex;

import java.io.File;
import java.io.IOException;
import java.util.Iterator;
import java.util.List;
import java.util.Properties;

import static com.google.common.collect.Lists.newArrayList;
import static org.fest.assertions.Assertions.assertThat;

public class IssuesIndexInjectionTest extends AbstractTest {

  static final Logger LOGGER = LoggerFactory.getLogger(IssuesIndexInjectionTest.class);

  final static int RULES_NUMBER = 25;
  final static int USERS_NUMBER = 100;

  final static int PROJECTS_NUMBER = 100;
  final static int NUMBER_FILES_PER_PROJECT = 1;
  final static int NUMBER_ISSUES_PER_FILE = 1;

  final static int ISSUE_COUNT = PROJECTS_NUMBER * NUMBER_FILES_PER_PROJECT * NUMBER_ISSUES_PER_FILE;

  @ClassRule
  public static TemporaryFolder temp = new TemporaryFolder();
  static String clusterName;
  static Integer clusterPort;
  private static SearchServer searchServer;
  @Rule
  public TestDatabase db = new TestDatabase();
  SearchClient searchClient;
  DbSession session;

  long ids = 1;

  Iterator<RuleDto> rules;
  Iterator<String> users;
  Iterator<String> severities;
  Iterator<String> statuses;
  Iterator<String> closedResolutions;
  Iterator<String> resolvedResolutions;

  IssueIndex issueIndex;

  @BeforeClass
  public static void setupSearchEngine() {
    clusterName = "sonarqube-test";
    clusterPort = NetworkUtils.freePort();
    Properties properties = new Properties();
    properties.setProperty(ProcessConstants.CLUSTER_NAME, clusterName);
    properties.setProperty(ProcessConstants.CLUSTER_NODE_NAME, "test");
    properties.setProperty(ProcessConstants.SEARCH_PORT, clusterPort.toString());
    properties.setProperty(ProcessConstants.PATH_HOME, temp.getRoot().getAbsolutePath());

    searchServer = new SearchServer(new Props(properties));
    searchServer.start();
  }

  @AfterClass
  public static void tearDownSearchEngine() {
    searchServer.stop();
  }

  @Before
  public void setupSearchClient() throws IOException {
    File dataDir = temp.newFolder();
    Settings settings = new Settings();
    settings.setProperty(ProcessConstants.CLUSTER_ACTIVATE, false);
    settings.setProperty(ProcessConstants.CLUSTER_NAME, clusterName);
    settings.setProperty(ProcessConstants.CLUSTER_NODE_NAME, "test");
    settings.setProperty(ProcessConstants.SEARCH_PORT, clusterPort.toString());
    settings.setProperty(ProcessConstants.PATH_HOME, dataDir.getAbsolutePath());
    searchClient = new SearchClient(settings);

    issueIndex = new IssueIndex(new IssueNormalizer(null), searchClient);
    issueIndex.start();

    session = db.myBatis().openSession(false);

    rules = Iterables.cycle(generateRules()).iterator();
    users = Iterables.cycle(generateUsers()).iterator();
    severities = Iterables.cycle(Severity.ALL).iterator();
    statuses = Iterables.cycle(Issue.STATUS_OPEN, Issue.STATUS_CONFIRMED, Issue.STATUS_REOPENED, Issue.STATUS_RESOLVED, Issue.STATUS_CLOSED).iterator();
    closedResolutions = Iterables.cycle(Issue.RESOLUTION_FALSE_POSITIVE, Issue.RESOLUTION_FIXED, Issue.RESOLUTION_REMOVED).iterator();
    resolvedResolutions = Iterables.cycle(Issue.RESOLUTION_FALSE_POSITIVE, Issue.RESOLUTION_FIXED).iterator();
  }

  @After
  public void after() throws Exception {
    searchClient.stop();
    session.close();
  }

  @Test
  public void inject_issues() throws Exception {

    // Thread.sleep(100000);

    int issueInsertCount = ISSUE_COUNT;

    long start = System.currentTimeMillis();
    for (long projectIndex = 1; projectIndex <= PROJECTS_NUMBER; projectIndex++) {
      ComponentDto project = ComponentTesting.newProjectDto()
        .setId(ids++)
        .setKey("project-" + projectIndex)
        .setName("Project " + projectIndex)
        .setLongName("Project " + projectIndex);

      for (int fileIndex = 0; fileIndex < NUMBER_FILES_PER_PROJECT; fileIndex++) {
        String index = projectIndex * PROJECTS_NUMBER + fileIndex + "";
        ComponentDto file = ComponentTesting.newFileDto(project)
          .setId(ids++)
          .setKey("file-" + index)
          .setName("File " + index)
          .setLongName("File " + index);

        for (int issueIndex = 1; issueIndex < NUMBER_ISSUES_PER_FILE + 1; issueIndex++) {
          String status = statuses.next();
          String resolution = null;
          if (status.equals(Issue.STATUS_CLOSED)) {
            resolution = closedResolutions.next();
          } else if (status.equals(Issue.STATUS_RESOLVED)) {
            resolution = resolvedResolutions.next();
          }
          RuleDto rule = rules.next();
          IssueDto issue = IssueTesting.newDto(rule, file, project)
            .setMessage("Message from rule " + rule.getKey().toString() + " on line " + issueIndex)
            .setLine(issueIndex)
            .setAssignee(users.next())
            .setReporter(users.next())
            .setAuthorLogin(users.next())
            .setSeverity(severities.next())
            .setStatus(status)
            .setResolution(resolution);
          session.enqueue(new InsertDto<IssueDto>(IndexDefinition.ISSUES.getIndexType(), issue, false));
        }
      }
    }
    session.enqueue(new RefreshIndex(IndexDefinition.ISSUES.getIndexType()));
    session.commit();
    long stop = System.currentTimeMillis();

    assertThat(issueIndex.countAll()).isEqualTo(issueInsertCount);

    long time = stop - start;
    LOGGER.info("processed {} Issues in {} ms with avg {} Issue/second", ISSUE_COUNT, time, documentPerSecond(time));
    assertDurationAround(time, Long.parseLong(getProperty("IssuesIndexInjectionTest.inject_issues")));
  }

  protected List<RuleDto> generateRules() {
    List<RuleDto> rules = newArrayList();
    for (int i = 0; i < RULES_NUMBER; i++) {
      rules.add(RuleTesting.newDto(RuleKey.of("rule-repo", "rule-key-" + i)).setId((int) (ids++)));
    }
    return rules;
  }

  protected List<String> generateUsers() {
    List<String> users = newArrayList();
    for (int i = 0; i < USERS_NUMBER; i++) {
      users.add("user-" + i);
    }
    return users;
  }

  private int documentPerSecond(long time) {
    return (int) Math.round(ISSUE_COUNT / (time / 1000.0));
  }

}
