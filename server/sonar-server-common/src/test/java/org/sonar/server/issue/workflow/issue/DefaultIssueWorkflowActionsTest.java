/*
 * SonarQube
 * Copyright (C) SonarSource Sàrl
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
package org.sonar.server.issue.workflow.issue;

import java.util.Date;
import org.junit.jupiter.api.Test;
import org.sonar.core.issue.DefaultIssue;
import org.sonar.core.issue.IssueChangeContext;
import org.sonar.server.issue.IssueFieldsSetter;

import static org.assertj.core.api.Assertions.assertThat;
import static org.sonar.api.issue.Issue.STATUS_CONFIRMED;
import static org.sonar.api.issue.Issue.STATUS_IN_SANDBOX;
import static org.sonar.api.issue.Issue.STATUS_OPEN;
import static org.sonar.core.issue.IssueChangeContext.issueChangeContextByScanBuilder;

class DefaultIssueWorkflowActionsTest {

  private static final long NOW = 1_704_067_200_000L;

  private final IssueFieldsSetter updater = new IssueFieldsSetter();
  private final IssueChangeContext changeContext = issueChangeContextByScanBuilder(new Date(NOW)).build();

  @Test
  void setStatus_whenNewStatusIsInSandbox_shouldPreserveDeferralDate() {
    DefaultIssue issue = new DefaultIssue()
      .setKey("ISSUE-1")
      .setStatus(STATUS_OPEN)
      .setDeferralDate(NOW);
    DefaultIssueWorkflowActions underTest = new DefaultIssueWorkflowActions(updater, issue, changeContext);

    underTest.setStatus(issue.issueStatus(), STATUS_IN_SANDBOX);

    assertThat(issue.status()).isEqualTo(STATUS_IN_SANDBOX);
    assertThat(issue.deferralDate()).isEqualTo(NOW);
  }

  @Test
  void setStatus_whenNewStatusIsNotInSandbox_shouldClearDeferralDate() {
    DefaultIssue issue = new DefaultIssue()
      .setKey("ISSUE-1")
      .setStatus(STATUS_IN_SANDBOX)
      .setDeferralDate(NOW);
    DefaultIssueWorkflowActions underTest = new DefaultIssueWorkflowActions(updater, issue, changeContext);

    underTest.setStatus(issue.issueStatus(), STATUS_CONFIRMED);

    assertThat(issue.status()).isEqualTo(STATUS_CONFIRMED);
    assertThat(issue.deferralDate()).isNull();
  }
}
