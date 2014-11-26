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

import org.apache.ibatis.session.ResultContext;
import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sonar.api.utils.System2;
import org.sonar.core.component.ComponentDto;
import org.sonar.core.persistence.DbSession;
import org.sonar.core.persistence.TestDatabase;
import org.sonar.server.component.ComponentTesting;
import org.sonar.server.component.db.ComponentDao;
import org.sonar.server.issue.db.IssueDao;
import org.sonar.server.rule.db.RuleDao;
import org.sonar.server.search.DbSynchronizationHandler;

import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.atomic.AtomicLong;

import static org.fest.assertions.Assertions.assertThat;

@Ignore("Waiting for usage of IssueIndexer")
public class IssuesDbExtractionTest extends AbstractTest {

  static final Logger LOGGER = LoggerFactory.getLogger(IssuesDbExtractionTest.class);
  final static int PROJECTS_NUMBER = 100;
  final static int NUMBER_FILES_PER_PROJECT = 100;
  final static int NUMBER_ISSUES_PER_FILE = 100;
  final static int ISSUE_COUNT = PROJECTS_NUMBER * NUMBER_FILES_PER_PROJECT * NUMBER_ISSUES_PER_FILE;
  @Rule
  public TestDatabase db = new TestDatabase();
  AtomicLong counter = new AtomicLong(0L);
  DbSession session;

  ProxyIssueDao issueDao;
  RuleDao ruleDao;
  ComponentDao componentDao;

  @Before
  public void setUp() throws Exception {
    issueDao = new ProxyIssueDao();
    ruleDao = new RuleDao();
    componentDao = new ComponentDao(System2.INSTANCE);

    session = db.myBatis().openSession(false);
  }

  @After
  public void closeSession() throws Exception {
    session.close();
  }

  @Test
  public void extract_issues() throws Exception {
    insertReferentials();

    ProgressTask progressTask = new ProgressTask(counter);
    Timer timer = new Timer("Extract Issues");
    timer.schedule(progressTask, ProgressTask.PERIOD_MS, ProgressTask.PERIOD_MS);
    try {
      long start = System.currentTimeMillis();
      issueDao.synchronizeAfter(session);
      long stop = System.currentTimeMillis();
      progressTask.log();

      assertThat(issueDao.synchronizedIssues).isEqualTo(ISSUE_COUNT);

      long time = stop - start;
      LOGGER.info("Iterated over {} issues in {} ms with avg of {} issues/second", ISSUE_COUNT, time, rowsPerSecond(time));
      // assertDurationAround(time, Long.parseLong(getProperty("IssuesDbExtractionTest.extract_issues")));

    } finally {
      timer.cancel();
      timer.purge();
    }
  }

  private void insertReferentials() {
    long start = System.currentTimeMillis();

    for (int i = 0; i < RULES_NUMBER; i++) {
      ruleDao.insert(session, rules.next());
    }
    session.commit();

    for (long projectIndex = 0; projectIndex < PROJECTS_NUMBER; projectIndex++) {
      ComponentDto project = ComponentTesting.newProjectDto()
        .setKey("project-" + projectIndex)
        .setName("Project " + projectIndex)
        .setLongName("Project " + projectIndex);
      componentDao.insert(session, project);

      for (int fileIndex = 0; fileIndex < NUMBER_FILES_PER_PROJECT; fileIndex++) {
        String index = projectIndex * PROJECTS_NUMBER + fileIndex + "";
        ComponentDto file = ComponentTesting.newFileDto(project)
          .setKey("file-" + index)
          .setName("File " + index)
          .setLongName("File " + index);
        componentDao.insert(session, file);

        for (int issueIndex = 1; issueIndex < NUMBER_ISSUES_PER_FILE + 1; issueIndex++) {
          issueDao.insert(session, newIssue(issueIndex, file, project, rules.next()));
        }
        session.commit();
      }
    }
    LOGGER.info("Referentials inserted in {} ms", System.currentTimeMillis() - start);
  }

  protected int rowsPerSecond(long time) {
    return (int) Math.round(ISSUE_COUNT / (time / 1000.0));
  }

  protected static class ProgressTask extends TimerTask {

    public static final long PERIOD_MS = 60000L;
    private static final Logger LOGGER = LoggerFactory.getLogger("PerformanceTests");
    private final AtomicLong counter;

    public ProgressTask(AtomicLong counter) {
      this.counter = counter;
    }

    @Override
    public void run() {
      log();
    }

    public void log() {
      LOGGER.info(String.format("%d issues processed", counter.get()));
    }
  }

  class ProxyIssueDao extends IssueDao {
    public Integer synchronizedIssues = 0;

    @Override
    protected boolean hasIndex() {
      return false;
    }

    @Override
    protected DbSynchronizationHandler getSynchronizationResultHandler(DbSession session, Map<String, String> params) {
      return new DbSynchronizationHandler(session, params) {

        @Override
        public void handleResult(ResultContext context) {
          synchronizedIssues++;
          counter.getAndIncrement();
        }

        @Override
        public void enqueueCollected() {

        }
      };
    }
  }

}
