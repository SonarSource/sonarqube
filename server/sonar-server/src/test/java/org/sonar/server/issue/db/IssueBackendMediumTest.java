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

import com.google.common.collect.ImmutableMap;
import org.junit.After;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Test;
import org.sonar.api.issue.Issue;
import org.sonar.api.rule.RuleKey;
import org.sonar.api.security.DefaultGroups;
import org.sonar.api.utils.KeyValueFormat;
import org.sonar.api.web.UserRole;
import org.sonar.core.component.ComponentDto;
import org.sonar.core.issue.db.IssueDto;
import org.sonar.core.permission.GlobalPermissions;
import org.sonar.core.persistence.DbSession;
import org.sonar.core.rule.RuleDto;
import org.sonar.server.component.ComponentTesting;
import org.sonar.server.component.db.ComponentDao;
import org.sonar.server.db.DbClient;
import org.sonar.server.issue.IssueTesting;
import org.sonar.server.issue.index.IssueIndex;
import org.sonar.server.permission.InternalPermissionService;
import org.sonar.server.permission.PermissionChange;
import org.sonar.server.rule.RuleTesting;
import org.sonar.server.rule.db.RuleDao;
import org.sonar.server.search.IndexClient;
import org.sonar.server.tester.ServerTester;
import org.sonar.server.user.MockUserSession;

import static org.fest.assertions.Assertions.assertThat;

public class IssueBackendMediumTest {

  @ClassRule
  public static ServerTester tester = new ServerTester();

  DbClient dbClient;
  IndexClient indexClient;
  DbSession dbSession;

  @Before
  public void setUp() throws Exception {
    dbClient = tester.get(DbClient.class);
    indexClient = tester.get(IndexClient.class);
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

    ComponentDto project = ComponentTesting.newProjectDto();
    tester.get(ComponentDao.class).insert(dbSession, project);

    // project can be seen by anyone
    dbSession.commit();
    MockUserSession.set().setLogin("admin").setGlobalPermissions(GlobalPermissions.SYSTEM_ADMIN);
    tester.get(InternalPermissionService.class).addPermission(new PermissionChange().setComponentKey(project.getKey()).setGroup(DefaultGroups.ANYONE).setPermission(UserRole.USER));

    ComponentDto file = ComponentTesting.newFileDto(project);
    tester.get(ComponentDao.class).insert(dbSession, file);

    IssueDto issue = IssueTesting.newDto(rule, file, project).setIssueAttributes(KeyValueFormat.format(ImmutableMap.of("key", "value")));
    dbClient.issueDao().insert(dbSession, issue);

    dbSession.commit();

    // Check that Issue is in Index
    assertThat(indexClient.get(IssueIndex.class).countAll()).isEqualTo(1);

    // should find by key
    Issue issueDoc = indexClient.get(IssueIndex.class).getByKey(issue.getKey());

    // Check all normalized fields
    assertThat(issueDoc.actionPlanKey()).isEqualTo(issue.getActionPlanKey());
    assertThat(issueDoc.assignee()).isEqualTo(issue.getAssignee());
    assertThat(issueDoc.authorLogin()).isEqualTo(issue.getAuthorLogin());
    assertThat(issueDoc.closeDate()).isEqualTo(issue.getIssueCloseDate());
    assertThat(issueDoc.effortToFix()).isEqualTo(issue.getEffortToFix());
    assertThat(issueDoc.resolution()).isEqualTo(issue.getResolution());
    assertThat(issueDoc.ruleKey()).isEqualTo(RuleKey.of(issue.getRuleRepo(), issue.getRule()));
    assertThat(issueDoc.line()).isEqualTo(issue.getLine());
    assertThat(issueDoc.message()).isEqualTo(issue.getMessage());
    assertThat(issueDoc.reporter()).isEqualTo(issue.getReporter());
    assertThat(issueDoc.key()).isEqualTo(issue.getKey());
    assertThat(issueDoc.updateDate()).isEqualTo(issue.getIssueUpdateDate());
    assertThat(issueDoc.status()).isEqualTo(issue.getStatus());
    assertThat(issueDoc.severity()).isEqualTo(issue.getSeverity());
    assertThat(issueDoc.attributes()).isEqualTo(KeyValueFormat.parse(issue.getIssueAttributes()));
    assertThat(issueDoc.attribute("key")).isEqualTo("value");
  }

}
