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
import org.junit.Test;
import org.sonar.core.component.ComponentDto;
import org.sonar.core.issue.db.IssueDto;
import org.sonar.core.persistence.DbSession;
import org.sonar.core.rule.RuleDto;
import org.sonar.server.component.persistence.ComponentDao;
import org.sonar.server.db.DbClient;
import org.sonar.server.issue.index.IssueDoc;
import org.sonar.server.issue.index.IssueIndex;
import org.sonar.server.platform.Platform;
import org.sonar.server.rule.RuleTesting;
import org.sonar.server.rule.db.RuleDao;
import org.sonar.server.search.IndexClient;
import org.sonar.server.tester.ServerTester;

import java.util.Date;
import java.util.UUID;

import static org.fest.assertions.Assertions.assertThat;

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
  public void insert_and_find_by_key() throws Exception {
    RuleDto rule = RuleTesting.newXooX1();
    tester.get(RuleDao.class).insert(dbSession, rule);

    ComponentDto project = new ComponentDto()
      .setId(1L)
      .setProjectId(1L);
    tester.get(ComponentDao.class).insert(dbSession, project);

    ComponentDto resource = new ComponentDto()
      .setProjectId(1L)
      .setId(2L);
    tester.get(ComponentDao.class).insert(dbSession, resource);

    IssueDto issue = new IssueDto().setId(1L).setRuleId(50).setComponentId(123l).setRootComponentId(100l)
      .setRuleId(rule.getId())
      .setRootComponentId(project.getId())
      .setComponentId(resource.getId())
      .setStatus("OPEN").setResolution("OPEN")
      .setKee(UUID.randomUUID().toString());
    dbClient.issueDao().insert(dbSession, issue);

    dbSession.commit();
    assertThat(issue.getId()).isNotNull();

    // Check that Issue is in Index
    assertThat(indexClient.get(IssueIndex.class).countAll()).isEqualTo(1);

    // should find by key
    IssueDoc issueDoc = indexClient.get(IssueIndex.class).getByKey(issue.getKey());
    assertThat(issueDoc).isNotNull();
    assertThat(issueDoc.key()).isEqualTo(issue.getKey());
  }

  @Test
 public void insert_and_find_after_date() throws Exception {

    RuleDto rule = RuleTesting.newXooX1();
    tester.get(RuleDao.class).insert(dbSession, rule);

    ComponentDto project = new ComponentDto()
      .setId(1L)
      .setProjectId(1L);
    tester.get(ComponentDao.class).insert(dbSession, project);

    ComponentDto resource = new ComponentDto()
      .setProjectId(1L)
      .setId(2L);
    tester.get(ComponentDao.class).insert(dbSession, resource);

    IssueDto issue = new IssueDto().setId(1L).setRuleId(50).setComponentId(123l).setRootComponentId(100l)
      .setRuleId(rule.getId())
      .setRootComponentId(project.getId())
      .setComponentId(resource.getId())
      .setStatus("OPEN").setResolution("OPEN")
      .setKee(UUID.randomUUID().toString());
    dbClient.issueDao().insert(dbSession, issue);

    dbSession.commit();
    assertThat(issue.getId()).isNotNull();

    // Find Issues since forever
    Date t0 = new Date(0);
    assertThat(dbClient.issueDao().findAfterDate(dbSession, t0)).hasSize(1);

    // Should not find any new issues
    Date t1 = new Date();
    assertThat(dbClient.issueDao().findAfterDate(dbSession, t1)).hasSize(0);

    // Should synchronise
    tester.clearIndexes();
    assertThat(indexClient.get(IssueIndex.class).countAll()).isEqualTo(0);
    tester.get(Platform.class).executeStartupTasks();
    assertThat(indexClient.get(IssueIndex.class).countAll()).isEqualTo(1);

  }
}
