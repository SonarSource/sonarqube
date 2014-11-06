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
import org.apache.ibatis.session.ResultContext;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sonar.api.issue.Issue;
import org.sonar.api.rule.RuleKey;
import org.sonar.api.rule.Severity;
import org.sonar.api.utils.System2;
import org.sonar.core.component.ComponentDto;
import org.sonar.core.persistence.DbSession;
import org.sonar.core.persistence.TestDatabase;
import org.sonar.core.rule.RuleDto;
import org.sonar.server.component.ComponentTesting;
import org.sonar.server.component.db.ComponentDao;
import org.sonar.server.issue.IssueTesting;
import org.sonar.server.issue.db.IssueDao;
import org.sonar.server.rule.RuleTesting;
import org.sonar.server.rule.db.RuleDao;
import org.sonar.server.search.DbSynchronizationHandler;

import java.util.Iterator;
import java.util.List;
import java.util.Map;

import static com.google.common.collect.Lists.newArrayList;
import static org.fest.assertions.Assertions.assertThat;

public class IssuesDbExtractionTest {

  static final Logger LOGGER = LoggerFactory.getLogger(IssuesDbExtractionTest.class);

  final static int RULES_NUMBER = 25;
  final static int USERS_NUMBER = 100;

  final static int PROJECTS_NUMBER = 10;
  final static int NUMBER_FILES_PER_PROJECT = 100;

  final static int ISSUE_COUNT = PROJECTS_NUMBER * NUMBER_FILES_PER_PROJECT;

  @Rule
  public TestDatabase db = new TestDatabase();

  DbSession session;

  Iterator<RuleDto> rules;
  Iterator<String> users;

  ProxyIssueDao issueDao;
  RuleDao ruleDao;
  ComponentDao componentDao;

  @Before
  public void setUp() throws Exception {
    issueDao = new ProxyIssueDao();
    ruleDao = new RuleDao();
    componentDao = new ComponentDao(System2.INSTANCE);

    session = db.myBatis().openSession(false);

    rules = Iterables.cycle(generateRules(session)).iterator();
    users = Iterables.cycle(generateUsers()).iterator();

    generateUsers();
    session.commit();
  }

  @After
  public void closeSession() throws Exception {
    session.close();
  }

  @Test
  public void extract_issues() throws Exception {
    for (long p = 1; p <= PROJECTS_NUMBER; p++) {
      ComponentDto project = ComponentTesting.newProjectDto()
        .setKey("project-" + p)
        .setName("Project " + p)
        .setLongName("Project " + p);
      componentDao.insert(session, project);

      for (int i = 0; i < NUMBER_FILES_PER_PROJECT; i++) {
        String index = p * PROJECTS_NUMBER + i + "";

        ComponentDto file = ComponentTesting.newFileDto(project)
          .setKey("file-" + index)
          .setName("File " + index)
          .setLongName("File " + index);
        componentDao.insert(session, file);

        issueDao.insert(session, IssueTesting.newDto(rules.next(), file, project)
          .setMessage("Message on " + index)
          .setAssignee(users.next())
          .setReporter(users.next())
          .setAuthorLogin(users.next())
          // Change Severity
          .setSeverity(Severity.BLOCKER)
          // Change status & resolution
          .setStatus(Issue.STATUS_RESOLVED)
          .setResolution(Issue.RESOLUTION_FIXED));
      }
      session.commit();
    }

    long start = System.currentTimeMillis();
    int issueInsertCount = ISSUE_COUNT;
    issueDao.synchronizeAfter(session);
    long stop = System.currentTimeMillis();

    // TODO add performance assertions here
    assertThat(issueDao.synchronizedIssues).isEqualTo(issueInsertCount);

    long time = stop - start;
    LOGGER.info("Processed {} Issues in {} ms with avg {} Issue/second", ISSUE_COUNT, time, documentPerSecond(time));
  }

  protected List<RuleDto> generateRules(DbSession session) {
    List<RuleDto> rules = newArrayList();
    for (int i = 0; i < RULES_NUMBER; i++) {
      rules.add(RuleTesting.newDto(RuleKey.of("rule-repo", "rule-key-" + i)));
    }
    ruleDao.insert(this.session, rules);
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
