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

package org.sonar.server.computation.step;

import org.junit.*;
import org.junit.rules.TemporaryFolder;
import org.sonar.api.issue.Issue;
import org.sonar.api.issue.internal.DefaultIssue;
import org.sonar.api.issue.internal.DefaultIssueComment;
import org.sonar.api.issue.internal.FieldDiffs;
import org.sonar.api.rule.RuleKey;
import org.sonar.api.rule.Severity;
import org.sonar.api.utils.System2;
import org.sonar.core.issue.db.UpdateConflictResolver;
import org.sonar.core.persistence.DbSession;
import org.sonar.core.persistence.DbTester;
import org.sonar.server.computation.issue.IssueCache;
import org.sonar.server.computation.issue.RuleCache;
import org.sonar.server.computation.issue.RuleCacheLoader;
import org.sonar.server.db.DbClient;
import org.sonar.server.issue.db.IssueDao;
import org.sonar.server.rule.db.RuleDao;

import java.io.IOException;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class PersistIssuesStepTest extends BaseStepTest {

  @Rule
  public TemporaryFolder temp = new TemporaryFolder();

  @ClassRule
  public static DbTester dbTester = new DbTester();

  DbSession session;

  DbClient dbClient;

  System2 system2;

  IssueCache issueCache;

  ComputationStep step;

  @Override
  protected ComputationStep step() throws IOException {
    return step;
  }

  @Before
  public void setup() throws Exception {
    dbTester.truncateTables();
    session = dbTester.myBatis().openSession(false);
    dbClient = new DbClient(dbTester.database(), dbTester.myBatis(), new IssueDao(dbTester.myBatis()), new RuleDao());

    issueCache = new IssueCache(temp.newFile(), System2.INSTANCE);
    system2 = mock(System2.class);
    when(system2.now()).thenReturn(1400000000000L);
    step = new PersistIssuesStep(dbClient, system2, new UpdateConflictResolver(), new RuleCache(new RuleCacheLoader(dbClient)), issueCache);
  }

  @After
  public void tearDown() throws Exception {
    session.close();
  }

  @Test
  public void insert_new_issue() throws Exception {
    dbTester.prepareDbUnit(getClass(), "insert_new_issue.xml");

    issueCache.newAppender().append(new DefaultIssue()
        .setKey("ISSUE")
        .setRuleKey(RuleKey.of("xoo", "S01"))
        .setComponentUuid("COMPONENT")
        .setProjectUuid("PROJECT")
        .setSeverity(Severity.BLOCKER)
        .setStatus(Issue.STATUS_OPEN)
        .setNew(true)
    ).close();

    step.execute(null);

    dbTester.assertDbUnit(getClass(), "insert_new_issue-result.xml", new String[]{"id"}, "issues");
  }

  @Test
  public void close_issue() throws Exception {
    dbTester.prepareDbUnit(getClass(), "shared.xml");

    issueCache.newAppender().append(new DefaultIssue()
        .setKey("ISSUE")
        .setRuleKey(RuleKey.of("xoo", "S01"))
        .setComponentUuid("COMPONENT")
        .setProjectUuid("PROJECT")
        .setSeverity(Severity.BLOCKER)
        .setStatus(Issue.STATUS_CLOSED)
        .setResolution(Issue.RESOLUTION_FIXED)
        .setNew(false)
        .setChanged(true)
    ).close();

    step.execute(null);

    dbTester.assertDbUnit(getClass(), "close_issue-result.xml", "issues");
  }

  @Test
  public void add_comment() throws Exception {
    dbTester.prepareDbUnit(getClass(), "shared.xml");

    issueCache.newAppender().append(new DefaultIssue()
        .setKey("ISSUE")
        .setRuleKey(RuleKey.of("xoo", "S01"))
        .setComponentUuid("COMPONENT")
        .setProjectUuid("PROJECT")
        .setSeverity(Severity.BLOCKER)
        .setStatus(Issue.STATUS_CLOSED)
        .setResolution(Issue.RESOLUTION_FIXED)
        .setNew(false)
        .setChanged(true)
        .addComment(new DefaultIssueComment()
            .setKey("COMMENT")
            .setIssueKey("ISSUE")
            .setUserLogin("john")
            .setMarkdownText("Some text")
            .setNew(true)
        )
    ).close();

    step.execute(null);

    dbTester.assertDbUnit(getClass(), "add_comment-result.xml", new String[]{"id", "created_at", "updated_at"}, "issue_changes");
  }

  @Test
  public void add_change() throws Exception {
    dbTester.prepareDbUnit(getClass(), "shared.xml");

    issueCache.newAppender().append(new DefaultIssue()
        .setKey("ISSUE")
        .setRuleKey(RuleKey.of("xoo", "S01"))
        .setComponentUuid("COMPONENT")
        .setProjectUuid("PROJECT")
        .setSeverity(Severity.BLOCKER)
        .setStatus(Issue.STATUS_CLOSED)
        .setResolution(Issue.RESOLUTION_FIXED)
        .setNew(false)
        .setChanged(true)
        .setCurrentChange(new FieldDiffs()
            .setIssueKey("ISSUE")
            .setUserLogin("john")
            .setDiff("technicalDebt", null, 1L)
        )
    ).close();

    step.execute(null);

    dbTester.assertDbUnit(getClass(), "add_change-result.xml", new String[]{"id", "created_at", "updated_at"}, "issue_changes");
  }

}
