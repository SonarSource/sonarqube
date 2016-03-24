/*
 * SonarQube
 * Copyright (C) 2009-2016 SonarSource SA
 * mailto:contact AT sonarsource DOT com
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
package org.sonar.server.computation.issue;

import com.google.common.collect.ImmutableMap;
import java.util.Date;
import org.junit.Test;
import org.sonar.api.utils.Duration;
import org.sonar.core.issue.DefaultIssue;
import org.sonar.core.issue.IssueChangeContext;
import org.sonar.db.protobuf.DbCommons;
import org.sonar.db.protobuf.DbIssues;
import org.sonar.server.issue.IssueUpdater;
import org.sonar.server.issue.workflow.IssueWorkflow;

import static com.google.common.collect.Lists.newArrayList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
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

  IssueUpdater updater = mock(IssueUpdater.class);

  DebtCalculator debtCalculator = mock(DebtCalculator.class);

  IssueLifecycle underTest = new IssueLifecycle(issueChangeContext, workflow, updater, debtCalculator);

  @Test
  public void initNewOpenIssue() throws Exception {
    DefaultIssue issue = new DefaultIssue();
    when(debtCalculator.calculate(issue)).thenReturn(DEFAULT_DURATION);

    underTest.initNewOpenIssue(issue);

    assertThat(issue.key()).isNotNull();
    assertThat(issue.creationDate()).isNotNull();
    assertThat(issue.updateDate()).isNotNull();
    assertThat(issue.status()).isEqualTo(STATUS_OPEN);
    assertThat(issue.debt()).isEqualTo(DEFAULT_DURATION);
  }

  @Test
  public void doAutomaticTransition() throws Exception {
    DefaultIssue issue = new DefaultIssue();

    underTest.doAutomaticTransition(issue);

    verify(workflow).doAutomaticTransition(issue, issueChangeContext);
  }

  @Test
  public void mergeExistingOpenIssue() throws Exception {
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
  public void mergeExistingOpenIssue_with_manual_severity() throws Exception {
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
  public void mergeExistingOpenIssue_with_attributes() throws Exception {
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
