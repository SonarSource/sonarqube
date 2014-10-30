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
package org.sonar.server.search;

import org.junit.After;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Test;
import org.sonar.api.rule.RuleKey;
import org.sonar.api.security.DefaultGroups;
import org.sonar.api.web.UserRole;
import org.sonar.core.component.ComponentDto;
import org.sonar.core.permission.PermissionFacade;
import org.sonar.core.persistence.BatchSession;
import org.sonar.core.persistence.DbSession;
import org.sonar.core.rule.RuleDto;
import org.sonar.server.component.ComponentTesting;
import org.sonar.server.db.DbClient;
import org.sonar.server.issue.IssueTesting;
import org.sonar.server.issue.index.IssueAuthorizationIndex;
import org.sonar.server.issue.index.IssueIndex;
import org.sonar.server.rule.RuleTesting;
import org.sonar.server.rule.db.RuleDao;
import org.sonar.server.rule.index.RuleIndex;
import org.sonar.server.tester.ServerTester;

import static org.fest.assertions.Assertions.assertThat;

public class IndexSynchronizerMediumTest {

  @ClassRule
  public static ServerTester tester = new ServerTester();

  IndexSynchronizer synchronizer;
  DbClient dbClient;
  IndexClient indexClient;
  DbSession dbSession;

  @Before
  public void setUp() throws Exception {
    dbClient = tester.get(DbClient.class);
    indexClient = tester.get(IndexClient.class);
    dbSession = dbClient.openSession(false);
    synchronizer = new IndexSynchronizer(dbClient, indexClient);
    tester.clearDbAndIndexes();
  }

  @After
  public void tearDown() throws Exception {
    dbSession.close();
  }

  @Test
  public void can_synchronize() throws Exception {
    int numberOfRules = 1000;
    int batchSize = BatchSession.MAX_BATCH_SIZE;

    int count = 0;
    for (int step = 0; (step * batchSize) < numberOfRules; step++) {
      for (int i = 0; i < batchSize; i++) {
        dbClient.ruleDao().insert(dbSession, RuleTesting.newDto(RuleKey.of("test", "x" + (count++))));
      }
      dbSession.commit();
    }

    assertThat(indexClient.get(RuleIndex.class).countAll()).isEqualTo(numberOfRules);
    tester.clearIndexes();
    assertThat(indexClient.get(RuleIndex.class).countAll()).isEqualTo(0);

    synchronizer.execute();
    assertThat(indexClient.get(RuleIndex.class).countAll()).isEqualTo(numberOfRules);
  }

  @Test
  public void synchronize_issues_from_empty_index() throws Exception {
    ComponentDto project = ComponentTesting.newProjectDto();
    ComponentDto file = ComponentTesting.newFileDto(project);
    dbClient.componentDao().insert(dbSession, project, file);

    RuleDto rule = RuleTesting.newXooX1();
    tester.get(RuleDao.class).insert(dbSession, rule);
    dbClient.issueDao().insert(dbSession, IssueTesting.newDto(rule, file, project));

    dbSession.commit();

    assertThat(indexClient.get(IssueIndex.class).countAll()).isEqualTo(1);
    tester.clearIndexes();
    assertThat(indexClient.get(IssueIndex.class).countAll()).isEqualTo(0);

    synchronizer.execute();
    assertThat(indexClient.get(IssueIndex.class).countAll()).isEqualTo(1);
  }

  @Test
  public void synchronize_issues_from_not_empty_index() throws Exception {
    RuleDto rule = RuleTesting.newXooX1();
    tester.get(RuleDao.class).insert(dbSession, rule);

    ComponentDto project1 = ComponentTesting.newProjectDto();
    ComponentDto file1 = ComponentTesting.newFileDto(project1);
    dbClient.componentDao().insert(dbSession, project1, file1);
    dbClient.issueDao().insert(dbSession, IssueTesting.newDto(rule, file1, project1));

    ComponentDto project2 = ComponentTesting.newProjectDto();
    ComponentDto file2 = ComponentTesting.newFileDto(project2);
    dbClient.componentDao().insert(dbSession, project2, file2);
    dbClient.issueDao().insert(dbSession, IssueTesting.newDto(rule, file2, project2));

    dbSession.commit();

    // Remove second issue to simulate that this issue has not been synchronized
    indexClient.get(IssueIndex.class).deleteByProjectUuid(project2.uuid());

    assertThat(indexClient.get(IssueIndex.class).countAll()).isEqualTo(1);
    synchronizer.execute();
    assertThat(indexClient.get(IssueIndex.class).countAll()).isEqualTo(2);
  }

  @Test
  public void synchronize_issues_authorization() throws Exception {
    ComponentDto project = ComponentTesting.newProjectDto();
    ComponentDto file = ComponentTesting.newFileDto(project);
    dbClient.componentDao().insert(dbSession, project, file);

    RuleDto rule = RuleTesting.newXooX1();
    tester.get(RuleDao.class).insert(dbSession, rule);
    dbClient.issueDao().insert(dbSession, IssueTesting.newDto(rule, file, project));

    tester.get(PermissionFacade.class).insertGroupPermission(project.getId(), DefaultGroups.ANYONE, UserRole.USER, dbSession);
    dbSession.commit();

    assertThat(indexClient.get(IssueAuthorizationIndex.class).countAll()).isEqualTo(0);
    synchronizer.execute();
    assertThat(indexClient.get(IssueAuthorizationIndex.class).countAll()).isEqualTo(1);
  }
}
