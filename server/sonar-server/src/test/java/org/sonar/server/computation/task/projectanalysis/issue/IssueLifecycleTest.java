/*
 * SonarQube
 * Copyright (C) 2009-2018 SonarSource SA
 * mailto:info AT sonarsource DOT com
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */
package org.sonar.server.computation.task.projectanalysis.issue;

import com.google.common.collect.ImmutableMap;
import java.util.Date;
import org.junit.Rule;
import org.junit.Test;
import org.sonar.api.issue.IssueComment;
import org.sonar.api.utils.Duration;
import org.sonar.core.issue.DefaultIssue;
import org.sonar.core.issue.DefaultIssueComment;
import org.sonar.core.issue.FieldDiffs;
import org.sonar.core.issue.IssueChangeContext;
import org.sonar.db.protobuf.DbCommons;
import org.sonar.db.protobuf.DbIssues;
import org.sonar.server.computation.task.projectanalysis.analysis.AnalysisMetadataHolderRule;
import org.sonar.server.computation.task.projectanalysis.analysis.Branch;
import org.sonar.server.issue.IssueFieldsSetter;
import org.sonar.server.issue.workflow.IssueWorkflow;

import static com.google.common.collect.Lists.newArrayList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.groups.Tuple.tuple;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;
import static org.sonar.api.issue.Issue.RESOLUTION_FIXED;
import static org.sonar.api.issue.Issue.STATUS_CLOSED;
import static org.sonar.api.issue.Issue.STATUS_OPEN;
import static org.sonar.api.rule.Severity.BLOCKER;
import static org.sonar.api.utils.DateUtils.parseDate;

public class IssueLifecycleTest {

  static final Date DEFAULT_DATE = new Date();

  static final Duration DEFAULT_DURATION = Duration.create(10);

  IssueChangeContext issueChangeContext = IssueChangeContext.createUser(DEFAULT_DATE, "julien");

  IssueWorkflow workflow = mock(IssueWorkflow.class);

  IssueFieldsSetter updater = mock(IssueFieldsSetter.class);

  DebtCalculator debtCalculator = mock(DebtCalculator.class);

  @Rule
  public AnalysisMetadataHolderRule analysisMetadataHolder = new AnalysisMetadataHolderRule();

  IssueLifecycle underTest = new IssueLifecycle(analysisMetadataHolder, issueChangeContext, workflow, updater, debtCalculator);

  @Test
  public void initNewOpenIssue() {
    DefaultIssue issue = new DefaultIssue();
    when(debtCalculator.calculate(issue)).thenReturn(DEFAULT_DURATION);

    underTest.initNewOpenIssue(issue);

    assertThat(issue.key()).isNotNull();
    assertThat(issue.creationDate()).isNotNull();
    assertThat(issue.updateDate()).isNotNull();
    assertThat(issue.status()).isEqualTo(STATUS_OPEN);
    assertThat(issue.debt()).isEqualTo(DEFAULT_DURATION);
    assertThat(issue.isNew()).isTrue();
    assertThat(issue.isCopied()).isFalse();
  }

  @Test
  public void mergeIssueFromShortLivingBranch() {
    DefaultIssue raw = new DefaultIssue()
      .setKey("raw");
    DefaultIssue fromShort = new DefaultIssue()
      .setKey("short");
    fromShort.setResolution("resolution");
    fromShort.setStatus("status");

    Date commentDate = new Date();
    fromShort.addComment(new DefaultIssueComment()
      .setIssueKey("short")
      .setCreatedAt(commentDate)
      .setUserLogin("user")
      .setMarkdownText("A comment"));

    Date diffDate = new Date();
    // file diff alone
    fromShort.addChange(new FieldDiffs()
      .setCreationDate(diffDate)
      .setIssueKey("short")
      .setUserLogin("user")
      .setDiff("file", "uuidA1", "uuidB1"));
    // file diff with another field
    fromShort.addChange(new FieldDiffs()
      .setCreationDate(diffDate)
      .setIssueKey("short")
      .setUserLogin("user")
      .setDiff("severity", "MINOR", "MAJOR")
      .setDiff("file", "uuidA2", "uuidB2"));

    Branch branch = mock(Branch.class);
    when(branch.getName()).thenReturn("master");
    analysisMetadataHolder.setBranch(branch);

    underTest.mergeConfirmedOrResolvedFromShortLivingBranch(raw, fromShort, "feature/foo");

    assertThat(raw.resolution()).isEqualTo("resolution");
    assertThat(raw.status()).isEqualTo("status");
    assertThat(raw.comments()).extracting(IssueComment::issueKey, IssueComment::createdAt, IssueComment::userLogin, IssueComment::markdownText)
      .containsOnly(tuple("raw", commentDate, "user", "A comment"));
    assertThat(raw.changes()).hasSize(2);
    assertThat(raw.changes().get(0).creationDate()).isEqualTo(diffDate);
    assertThat(raw.changes().get(0).userLogin()).isEqualTo("user");
    assertThat(raw.changes().get(0).issueKey()).isEqualTo("raw");
    assertThat(raw.changes().get(0).diffs()).containsOnlyKeys("severity");
    assertThat(raw.changes().get(1).userLogin()).isEqualTo("julien");
    assertThat(raw.changes().get(1).diffs()).containsOnlyKeys(IssueFieldsSetter.FROM_SHORT_BRANCH);
    assertThat(raw.changes().get(1).get(IssueFieldsSetter.FROM_SHORT_BRANCH).oldValue()).isEqualTo("feature/foo");
    assertThat(raw.changes().get(1).get(IssueFieldsSetter.FROM_SHORT_BRANCH).newValue()).isEqualTo("master");
  }

  @Test
  public void copiedIssue() {
    DefaultIssue raw = new DefaultIssue()
      .setNew(true)
      .setKey("RAW_KEY")
      .setCreationDate(parseDate("2015-10-01"))
      .setUpdateDate(parseDate("2015-10-02"))
      .setCloseDate(parseDate("2015-10-03"));

    DbIssues.Locations issueLocations = DbIssues.Locations.newBuilder()
      .setTextRange(DbCommons.TextRange.newBuilder()
        .setStartLine(10)
        .setEndLine(12)
        .build())
      .build();
    DefaultIssue base = new DefaultIssue()
      .setKey("BASE_KEY")
      .setCreationDate(parseDate("2015-01-01"))
      .setUpdateDate(parseDate("2015-01-02"))
      .setCloseDate(parseDate("2015-01-03"))
      .setResolution(RESOLUTION_FIXED)
      .setStatus(STATUS_CLOSED)
      .setSeverity(BLOCKER)
      .setAssignee("base assignee")
      .setAuthorLogin("base author")
      .setTags(newArrayList("base tag"))
      .setOnDisabledRule(true)
      .setSelectedAt(1000L)
      .setLine(10)
      .setMessage("message")
      .setGap(15d)
      .setEffort(Duration.create(15L))
      .setManualSeverity(false)
      .setLocations(issueLocations);

    when(debtCalculator.calculate(raw)).thenReturn(DEFAULT_DURATION);

    Branch branch = mock(Branch.class);
    when(branch.getName()).thenReturn("release-2.x");
    analysisMetadataHolder.setBranch(branch);

    underTest.copyExistingOpenIssueFromLongLivingBranch(raw, base, "master");

    assertThat(raw.isNew()).isFalse();
    assertThat(raw.isCopied()).isTrue();
    assertThat(raw.key()).isNotNull();
    assertThat(raw.key()).isNotEqualTo(base.key());
    assertThat(raw.creationDate()).isEqualTo(base.creationDate());
    assertThat(raw.updateDate()).isEqualTo(base.updateDate());
    assertThat(raw.closeDate()).isEqualTo(base.closeDate());
    assertThat(raw.resolution()).isEqualTo(RESOLUTION_FIXED);
    assertThat(raw.status()).isEqualTo(STATUS_CLOSED);
    assertThat(raw.assignee()).isEqualTo("base assignee");
    assertThat(raw.authorLogin()).isEqualTo("base author");
    assertThat(raw.tags()).containsOnly("base tag");
    assertThat(raw.debt()).isEqualTo(DEFAULT_DURATION);
    assertThat(raw.isOnDisabledRule()).isTrue();
    assertThat(raw.selectedAt()).isEqualTo(1000L);
    assertThat(raw.changes().get(0).get(IssueFieldsSetter.FROM_LONG_BRANCH).oldValue()).isEqualTo("master");
    assertThat(raw.changes().get(0).get(IssueFieldsSetter.FROM_LONG_BRANCH).newValue()).isEqualTo("release-2.x");

    verifyZeroInteractions(updater);
  }

  @Test
  public void doAutomaticTransition() {
    DefaultIssue issue = new DefaultIssue();

    underTest.doAutomaticTransition(issue);

    verify(workflow).doAutomaticTransition(issue, issueChangeContext);
  }

  @Test
  public void mergeExistingOpenIssue() {
    DefaultIssue raw = new DefaultIssue()
      .setNew(true)
      .setKey("RAW_KEY")
      .setCreationDate(parseDate("2015-10-01"))
      .setUpdateDate(parseDate("2015-10-02"))
      .setCloseDate(parseDate("2015-10-03"));

    DbIssues.Locations issueLocations = DbIssues.Locations.newBuilder()
      .setTextRange(DbCommons.TextRange.newBuilder()
        .setStartLine(10)
        .setEndLine(12)
        .build())
      .build();
    DefaultIssue base = new DefaultIssue()
      .setKey("BASE_KEY")
      .setCreationDate(parseDate("2015-01-01"))
      .setUpdateDate(parseDate("2015-01-02"))
      .setCloseDate(parseDate("2015-01-03"))
      .setResolution(RESOLUTION_FIXED)
      .setStatus(STATUS_CLOSED)
      .setSeverity(BLOCKER)
      .setAssignee("base assignee")
      .setAuthorLogin("base author")
      .setTags(newArrayList("base tag"))
      .setOnDisabledRule(true)
      .setSelectedAt(1000L)
      .setLine(10)
      .setMessage("message")
      .setGap(15d)
      .setEffort(Duration.create(15L))
      .setManualSeverity(false)
      .setLocations(issueLocations);

    when(debtCalculator.calculate(raw)).thenReturn(DEFAULT_DURATION);

    underTest.mergeExistingOpenIssue(raw, base);

    assertThat(raw.isNew()).isFalse();
    assertThat(raw.key()).isEqualTo("BASE_KEY");
    assertThat(raw.creationDate()).isEqualTo(base.creationDate());
    assertThat(raw.updateDate()).isEqualTo(base.updateDate());
    assertThat(raw.closeDate()).isEqualTo(base.closeDate());
    assertThat(raw.resolution()).isEqualTo(RESOLUTION_FIXED);
    assertThat(raw.status()).isEqualTo(STATUS_CLOSED);
    assertThat(raw.assignee()).isEqualTo("base assignee");
    assertThat(raw.authorLogin()).isEqualTo("base author");
    assertThat(raw.tags()).containsOnly("base tag");
    assertThat(raw.debt()).isEqualTo(DEFAULT_DURATION);
    assertThat(raw.isOnDisabledRule()).isTrue();
    assertThat(raw.selectedAt()).isEqualTo(1000L);

    verify(updater).setPastSeverity(raw, BLOCKER, issueChangeContext);
    verify(updater).setPastLine(raw, 10);
    verify(updater).setPastMessage(raw, "message", issueChangeContext);
    verify(updater).setPastEffort(raw, Duration.create(15L), issueChangeContext);
    verify(updater).setPastLocations(raw, issueLocations);
  }

  @Test
  public void mergeExistingOpenIssue_with_manual_severity() {
    DefaultIssue raw = new DefaultIssue()
      .setNew(true)
      .setKey("RAW_KEY");
    DefaultIssue base = new DefaultIssue()
      .setKey("BASE_KEY")
      .setResolution(RESOLUTION_FIXED)
      .setStatus(STATUS_CLOSED)
      .setSeverity(BLOCKER)
      .setManualSeverity(true);

    underTest.mergeExistingOpenIssue(raw, base);

    assertThat(raw.manualSeverity()).isTrue();
    assertThat(raw.severity()).isEqualTo(BLOCKER);

    verify(updater, never()).setPastSeverity(raw, BLOCKER, issueChangeContext);
  }

  @Test
  public void mergeExistingOpenIssue_with_attributes() {
    DefaultIssue raw = new DefaultIssue()
      .setNew(true)
      .setKey("RAW_KEY");
    DefaultIssue base = new DefaultIssue()
      .setKey("BASE_KEY")
      .setResolution(RESOLUTION_FIXED)
      .setStatus(STATUS_CLOSED)
      .setSeverity(BLOCKER)
      .setAttributes(ImmutableMap.of("JIRA", "SONAR-01"));

    underTest.mergeExistingOpenIssue(raw, base);

    assertThat(raw.attributes()).containsEntry("JIRA", "SONAR-01");
  }
}
