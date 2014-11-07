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

import static org.fest.assertions.Assertions.assertThat;

public class IssuesDbExtractionTest extends AbstractTest {

  static final Logger LOGGER = LoggerFactory.getLogger(IssuesDbExtractionTest.class);

  final static int PROJECTS_NUMBER = 100;
  final static int NUMBER_FILES_PER_PROJECT = 100;
  final static int NUMBER_ISSUES_PER_FILE = 100;

  final static int ISSUE_COUNT = PROJECTS_NUMBER * NUMBER_FILES_PER_PROJECT * NUMBER_ISSUES_PER_FILE;

  @Rule
  public TestDatabase db = new TestDatabase();

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

    for (int i = 0; i < RULES_NUMBER; i++) {
      ruleDao.insert(session, rules.next());
    }
    session.commit();
  }

  @After
  public void closeSession() throws Exception {
    session.close();
  }

  @Test
  public void extract_issues() throws Exception {
    int issueInsertCount = ISSUE_COUNT;

    long start = System.currentTimeMillis();
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
    LOGGER.info("Inserted {} Issues in {} ms", ISSUE_COUNT, System.currentTimeMillis() - start);

    start = System.currentTimeMillis();
    issueDao.synchronizeAfter(session);
    long stop = System.currentTimeMillis();

    assertThat(issueDao.synchronizedIssues).isEqualTo(issueInsertCount);

    long time = stop - start;
    LOGGER.info("Extracted {} Issues in {} ms with avg {} Issue/second", ISSUE_COUNT, time, documentPerSecond(time));
    assertDurationAround(time, Long.parseLong(getProperty("IssuesDbExtractionTest.extract_issues")));
  }

  protected int documentPerSecond(long time) {
    return (int) Math.round(ISSUE_COUNT / (time / 1000.0));
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
        }

        @Override
        public void enqueueCollected() {
        }
      };
    }
  }

}
