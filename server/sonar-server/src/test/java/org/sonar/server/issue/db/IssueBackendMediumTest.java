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
package org.sonar.server.issue.db;

import org.junit.After;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Ignore;
import org.junit.Test;
import org.sonar.api.resources.Project;
import org.sonar.core.persistence.DbSession;
import org.sonar.core.rule.RuleDto;
import org.sonar.server.db.DbClient;
import org.sonar.server.platform.Platform;
import org.sonar.server.rule.RuleTesting;
import org.sonar.server.search.IndexClient;
import org.sonar.server.tester.ServerTester;

public class IssueBackendMediumTest {


  @ClassRule
  public static ServerTester tester = new ServerTester();

  DbClient dbClient;
  IndexClient indexClient;
  Platform platform;
  DbSession dbSession;

  @Before
  public void setUp() throws Exception {
    dbClient = tester.get(DbClient.class);
    indexClient = tester.get(IndexClient.class);
    platform = tester.get(Platform.class);
    dbSession = dbClient.openSession(false);
    tester.clearDbAndIndexes();
  }

  @After
  public void tearDown() throws Exception {
    if (dbSession != null) {
      dbSession.close();
    }
  }

  @Test
  @Ignore("work in progress")
  public void insert_select_issue() throws Exception {
    Project project = new Project("my:project");
    RuleDto rule = RuleTesting.newXooX1();
//    //IssueDto issue = IssueDto.createFor(project, rule);
//    System.out.println("issue.getKey() = " + issue.getKey());
//    dbClient.issueDao().insert(dbSession, issue);
//
//    assertThat(issue.getId()).isNotNull();
  }
}