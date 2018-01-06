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

import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.sonar.api.issue.Issue;
import org.sonar.api.rule.RuleKey;
import org.sonar.api.rule.Severity;
import org.sonar.api.rules.RuleType;
import org.sonar.api.utils.DateUtils;
import org.sonar.core.issue.DefaultIssue;
import org.sonar.db.issue.IssueDto;
import org.sonar.db.issue.IssueMapper;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class UpdateConflictResolverTest {

  @Test
  public void should_reload_issue_and_resolve_conflict() {
    DefaultIssue issue = new DefaultIssue()
      .setKey("ABCDE")
      .setType(RuleType.CODE_SMELL)
      .setRuleKey(RuleKey.of("squid", "AvoidCycles"))
      .setProjectUuid("U1")
      .setComponentUuid("U2")
      .setNew(false)
      .setStatus(Issue.STATUS_OPEN);

    // Issue as seen and changed by end-user
    IssueMapper mapper = mock(IssueMapper.class);
    when(mapper.selectByKey("ABCDE")).thenReturn(
      new IssueDto()
        .setKee("ABCDE")
        .setType(RuleType.CODE_SMELL)
        .setRuleId(10)
        .setRuleKey("squid", "AvoidCycles")
        .setProjectUuid("U1")
        .setComponentUuid("U2")
        .setLine(10)
        .setStatus(Issue.STATUS_OPEN)

        // field changed by user
        .setAssignee("arthur")
      );

    new UpdateConflictResolver().resolve(issue, mapper);

    ArgumentCaptor<IssueDto> argument = ArgumentCaptor.forClass(IssueDto.class);
    verify(mapper).update(argument.capture());
    IssueDto updatedIssue = argument.getValue();
    assertThat(updatedIssue.getKee()).isEqualTo("ABCDE");
    assertThat(updatedIssue.getAssignee()).isEqualTo("arthur");
  }

  @Test
  public void should_keep_changes_made_by_user() {
    DefaultIssue issue = new DefaultIssue()
      .setKey("ABCDE")
      .setRuleKey(RuleKey.of("squid", "AvoidCycles"))
      .setComponentKey("struts:org.apache.struts.Action")
      .setNew(false);

    // Before starting scan
    issue.setAssignee(null);
    issue.setCreationDate(DateUtils.parseDate("2012-01-01"));
    issue.setUpdateDate(DateUtils.parseDate("2012-02-02"));

    // Changed by scan
    issue.setLine(200);
    issue.setSeverity(Severity.BLOCKER);
    issue.setManualSeverity(false);
    issue.setAuthorLogin("simon");
    issue.setChecksum("CHECKSUM-ABCDE");
    issue.setResolution(null);
    issue.setStatus(Issue.STATUS_REOPENED);

    // Issue as seen and changed by end-user
    IssueDto dbIssue = new IssueDto()
      .setKee("ABCDE")
      .setRuleId(10)
      .setRuleKey("squid", "AvoidCycles")
      .setComponentUuid("100")
      .setComponentKey("struts:org.apache.struts.Action")
      .setLine(10)
      .setResolution(Issue.RESOLUTION_FALSE_POSITIVE)
      .setStatus(Issue.STATUS_RESOLVED)
      .setAssignee("arthur")
      .setSeverity(Severity.MAJOR)
      .setManualSeverity(false);

    new UpdateConflictResolver().mergeFields(dbIssue, issue);

    assertThat(issue.key()).isEqualTo("ABCDE");
    assertThat(issue.componentKey()).isEqualTo("struts:org.apache.struts.Action");

    // Scan wins on :
    assertThat(issue.line()).isEqualTo(200);
    assertThat(issue.severity()).isEqualTo(Severity.BLOCKER);
    assertThat(issue.manualSeverity()).isFalse();

    // User wins on :
    assertThat(issue.assignee()).isEqualTo("arthur");
    assertThat(issue.resolution()).isEqualTo(Issue.RESOLUTION_FALSE_POSITIVE);
    assertThat(issue.status()).isEqualTo(Issue.STATUS_RESOLVED);
  }

  @Test
  public void severity_changed_by_user_should_be_kept() {
    DefaultIssue issue = new DefaultIssue()
      .setKey("ABCDE")
      .setRuleKey(RuleKey.of("squid", "AvoidCycles"))
      .setComponentKey("struts:org.apache.struts.Action")
      .setNew(false)
      .setStatus(Issue.STATUS_OPEN);

    // Changed by scan
    issue.setSeverity(Severity.BLOCKER);
    issue.setManualSeverity(false);

    // Issue as seen and changed by end-user
    IssueDto dbIssue = new IssueDto()
      .setKee("ABCDE")
      .setStatus(Issue.STATUS_OPEN)
      .setSeverity(Severity.INFO)
      .setManualSeverity(true);

    new UpdateConflictResolver().mergeFields(dbIssue, issue);

    assertThat(issue.severity()).isEqualTo(Severity.INFO);
    assertThat(issue.manualSeverity()).isTrue();
  }
}
