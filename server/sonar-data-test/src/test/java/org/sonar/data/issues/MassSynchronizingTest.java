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
import org.junit.Before;
import org.junit.Test;
import org.sonar.core.persistence.DbSession;
import org.sonar.server.issue.db.IssueDao;
import org.sonar.server.search.DbSynchronizationHandler;

import java.util.Date;
import java.util.Map;

import static org.fest.assertions.Assertions.assertThat;

public class MassSynchronizingTest extends IssueData {

  MyIssueDao myIssueDao;

  @Before
  public void setUp() throws Exception {

    myIssueDao = new MyIssueDao();

    DbSession setupSession = db.openSession(false);
    generateRules(setupSession);
    generateProjects(setupSession);


    // Inserting Issues now (finally)
    for (int i = 0; i < ISSUE_COUNT; i++) {
      myIssueDao.insert(setupSession, getIssue(i));
      if (i % 100 == 0) {
        setupSession.commit();
      }
    }
    setupSession.commit();
    setupSession.close();
  }

  @Test
  public void synchronize_issues() throws Exception {
    long start = System.currentTimeMillis();
    int issueInsertCount = ISSUE_COUNT;
    myIssueDao.synchronizeAfter(session, new Date(0));
    long stop = System.currentTimeMillis();

    // TODO add performance assertions here
    assertThat(myIssueDao.synchronizedIssues).isEqualTo(issueInsertCount);

    long time = stop-start;
    LOGGER.info("processed {} Issues in {}ms with avg {} Issue/second", ISSUE_COUNT, time, this.documentPerSecond(time));
  }

  class MyIssueDao extends IssueDao {
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
