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

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.sonar.api.issue.Issue;
import org.sonar.api.rule.RuleKey;
import org.sonar.api.rule.Severity;
import org.sonar.api.utils.System2;
import org.sonar.batch.protocol.output.BatchReport;
import org.sonar.core.issue.DefaultIssue;
import org.sonar.core.issue.DefaultIssueComment;
import org.sonar.core.issue.FieldDiffs;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.db.DbTester;
import org.sonar.server.computation.batch.BatchReportReaderRule;
import org.sonar.server.computation.issue.IssueCache;
import org.sonar.server.computation.issue.RuleCacheLoader;
import org.sonar.server.computation.issue.RuleRepositoryImpl;
import org.sonar.server.computation.issue.UpdateConflictResolver;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class PersistIssuesStepTest extends BaseStepTest {

  public static final long NOW = 1400000000000L;

  @Rule
  public TemporaryFolder temp = new TemporaryFolder();

  @Rule
  public DbTester dbTester = DbTester.create(System2.INSTANCE);

  @Rule
  public BatchReportReaderRule reportReader = new BatchReportReaderRule();

  DbSession session = dbTester.getSession();

  DbClient dbClient = dbTester.getDbClient();

  System2 system2;

  IssueCache issueCache;

  ComputationStep step;

  @Override
  protected ComputationStep step() {
    return step;
  }

  @Before
  public void setup() throws Exception {
    dbTester.truncateTables();
    issueCache = new IssueCache(temp.newFile(), System2.INSTANCE);
    system2 = mock(System2.class);
    when(system2.now()).thenReturn(NOW);
    reportReader.setMetadata(BatchReport.Metadata.getDefaultInstance());

    step = new PersistIssuesStep(dbClient, system2, new UpdateConflictResolver(), new RuleRepositoryImpl(new RuleCacheLoader(dbClient)), issueCache);
  }

  @After
  public void tearDown() {
    session.close();
  }

  @Test
  public void insert_new_issue() {
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

    step.execute();

    dbTester.assertDbUnit(getClass(), "insert_new_issue-result.xml", new String[] {"id"}, "issues");
  }

  @Test
  public void close_issue() {
    dbTester.prepareDbUnit(getClass(), "shared.xml");

    issueCache.newAppender().append(new DefaultIssue()
      .setKey("ISSUE")
      .setRuleKey(RuleKey.of("xoo", "S01"))
      .setComponentUuid("COMPONENT")
      .setProjectUuid("PROJECT")
      .setSeverity(Severity.BLOCKER)
      .setStatus(Issue.STATUS_CLOSED)
      .setResolution(Issue.RESOLUTION_FIXED)
      .setSelectedAt(NOW)
      .setNew(false)
      .setChanged(true)
      ).close();

    step.execute();

    dbTester.assertDbUnit(getClass(), "close_issue-result.xml", "issues");
  }

  @Test
  public void add_comment() {
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

    step.execute();

    dbTester.assertDbUnit(getClass(), "add_comment-result.xml", new String[] {"id", "created_at", "updated_at"}, "issue_changes");
  }

  @Test
  public void add_change() {
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

    step.execute();

    dbTester.assertDbUnit(getClass(), "add_change-result.xml", new String[] {"id", "created_at", "updated_at"}, "issue_changes");
  }

}
