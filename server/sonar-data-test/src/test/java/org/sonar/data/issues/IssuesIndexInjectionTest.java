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

import com.google.common.collect.ArrayListMultimap;
import org.junit.After;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sonar.api.issue.Issue;
import org.sonar.api.security.DefaultGroups;
import org.sonar.core.component.ComponentDto;
import org.sonar.core.issue.db.IssueAuthorizationDto;
import org.sonar.core.issue.db.IssueDto;
import org.sonar.core.persistence.DbSession;
import org.sonar.server.component.ComponentTesting;
import org.sonar.server.db.DbClient;
import org.sonar.server.issue.IssueQuery;
import org.sonar.server.issue.index.IssueAuthorizationIndex;
import org.sonar.server.issue.index.IssueIndex;
import org.sonar.server.search.IndexDefinition;
import org.sonar.server.search.QueryContext;
import org.sonar.server.search.Result;
import org.sonar.server.search.action.InsertDto;
import org.sonar.server.search.action.RefreshIndex;
import org.sonar.server.tester.ServerTester;
import org.sonar.server.user.MockUserSession;

import java.util.List;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.atomic.AtomicLong;

import static com.google.common.collect.Lists.newArrayList;
import static org.fest.assertions.Assertions.assertThat;

public class IssuesIndexInjectionTest extends AbstractTest {

  static final Logger LOGGER = LoggerFactory.getLogger(IssuesIndexInjectionTest.class);
  final static int PROJECTS = 100;
  final static int FILES_PER_PROJECT = 100;
  final static int ISSUES_PER_FILE = 100;
  final static int ISSUE_COUNT = PROJECTS * FILES_PER_PROJECT * ISSUES_PER_FILE;

  @ClassRule
  public static ServerTester tester = new ServerTester();
  AtomicLong counter = new AtomicLong(0L);
  DbSession batchSession;

  IssueIndex issueIndex;
  IssueAuthorizationIndex issueAuthorizationIndex;

  List<ComponentDto> projects = newArrayList();
  ArrayListMultimap<ComponentDto, ComponentDto> componentsByProject = ArrayListMultimap.create();

  protected static int documentPerSecond(long nbIssues, long time) {
    return (int) Math.round(nbIssues / (time / 1000.0));
  }

  @Before
  public void setUp() throws Exception {
    issueIndex = tester.get(IssueIndex.class);
    issueAuthorizationIndex = tester.get(IssueAuthorizationIndex.class);
    batchSession = tester.get(DbClient.class).openSession(true);

    MockUserSession.set().setLogin("test");
  }

  @After
  public void after() throws Exception {
    batchSession.close();
  }

  @Test
  public void inject_issues_and_execute_queries() throws Exception {
    generateData();
    injectIssuesInIndex();
    executeQueries();
  }

  public void injectIssuesInIndex() {
    ProgressTask progressTask = new ProgressTask(counter);
    Timer timer = new Timer("Inject Issues");
    timer.schedule(progressTask, ProgressTask.PERIOD_MS, ProgressTask.PERIOD_MS);
    try {
      long start = System.currentTimeMillis();
      for (ComponentDto project : projects) {
        for (ComponentDto file : componentsByProject.get(project)) {
          for (int issueIndex = 1; issueIndex < ISSUES_PER_FILE + 1; issueIndex++) {
            batchSession.enqueue(new InsertDto<IssueDto>(IndexDefinition.ISSUES.getIndexType(), newIssue(issueIndex, file, project, rules.next()), false));
            counter.getAndIncrement();
          }
        }
      }
      batchSession.enqueue(new RefreshIndex(IndexDefinition.ISSUES.getIndexType()));
      batchSession.commit();
      long stop = System.currentTimeMillis();
      progressTask.log();

      assertThat(issueIndex.countAll()).isEqualTo(ISSUE_COUNT);

      long totalTime = stop - start;
      LOGGER.info("Inserted {} Issues in {} ms with avg {} Issue/second", ISSUE_COUNT, totalTime, documentPerSecond(ISSUE_COUNT, totalTime));
      // assertDurationAround(totalTime, Long.parseLong(getProperty("IssuesIndexInjectionTest.inject_issues")));

    } finally {
      timer.cancel();
      timer.purge();
    }
  }

  public void executeQueries() {
    long start = System.currentTimeMillis();
    Result<Issue> result = issueIndex.search(IssueQuery.builder().build(), new QueryContext());
    LOGGER.info("Search for all issues : returned {} issues in {} ms", result.getTotal(), System.currentTimeMillis() - start);

    start = System.currentTimeMillis();
    ComponentDto project = componentsByProject.keySet().iterator().next();
    result = issueIndex.search(IssueQuery.builder().projectUuids(newArrayList(project.uuid())).build(), new QueryContext());
    LOGGER.info("Search for issues from one project : returned {} issues in {} ms", result.getTotal(), System.currentTimeMillis() - start);

    start = System.currentTimeMillis();
    ComponentDto file = componentsByProject.get(project).get(0);
    result = issueIndex.search(IssueQuery.builder().componentUuids(newArrayList(file.uuid())).build(), new QueryContext());
    LOGGER.info("Search for issues from one file : returned {} issues in {} ms", result.getTotal(), System.currentTimeMillis() - start);
  }

  private void generateData() {
    long ids = 1;

    for (int i = 0; i < RULES_NUMBER; i++) {
      rules.next().setId((int) ids++);
    }

    long start = System.currentTimeMillis();
    for (long projectIndex = 0; projectIndex < PROJECTS; projectIndex++) {
      ComponentDto project = ComponentTesting.newProjectDto()
        .setId(ids++)
        .setKey("project-" + projectIndex)
        .setName("Project " + projectIndex)
        .setLongName("Project " + projectIndex);
      projects.add(project);

      // All project are visible by anyone
      // TODO set different groups/users to test search issues queries with more realistic data
      batchSession.enqueue(new InsertDto<IssueAuthorizationDto>(IndexDefinition.ISSUES_AUTHORIZATION.getIndexType(),
        new IssueAuthorizationDto().setProjectUuid(project.uuid()).setGroups(newArrayList(DefaultGroups.ANYONE)), false));

      for (int fileIndex = 0; fileIndex < FILES_PER_PROJECT; fileIndex++) {
        String index = projectIndex * PROJECTS + fileIndex + "";
        ComponentDto file = ComponentTesting.newFileDto(project)
          .setId(ids++)
          .setKey("file-" + index)
          .setName("File " + index)
          .setLongName("File " + index);
        componentsByProject.put(project, file);
      }
    }
    batchSession.enqueue(new RefreshIndex(IndexDefinition.ISSUES_AUTHORIZATION.getIndexType()));
    batchSession.commit();
    LOGGER.info("Generated data in {} ms", System.currentTimeMillis() - start);
  }

  protected static class ProgressTask extends TimerTask {

    public static final long PERIOD_MS = 60000L;
    private static final Logger LOGGER = LoggerFactory.getLogger("PerformanceTests");
    private final AtomicLong counter;
    private long currentStart;
    private long previousTotal = 0;

    public ProgressTask(AtomicLong counter) {
      this.counter = counter;
      this.currentStart = System.currentTimeMillis();
    }

    @Override
    public void run() {
      log();
    }

    public void log() {
      long currentNumberOfIssues = counter.get() - this.previousTotal;
      LOGGER.info("{} issues inserted with avg {} issue/second", currentNumberOfIssues, documentPerSecond(currentNumberOfIssues, System.currentTimeMillis() - this.currentStart));
      this.previousTotal = counter.get();
      this.currentStart = System.currentTimeMillis();
    }
  }

}
