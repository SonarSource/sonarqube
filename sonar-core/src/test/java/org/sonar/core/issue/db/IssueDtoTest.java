/*
 * SonarQube, open source software quality management tool.
 * Copyright (C) 2008-2013 SonarSource
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
package org.sonar.core.issue.db;

import org.apache.commons.lang.time.DateUtils;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.sonar.api.issue.Issue;
import org.sonar.api.issue.internal.DefaultIssue;
import org.sonar.api.utils.Duration;

import java.util.Calendar;
import java.util.Date;

import static org.fest.assertions.Assertions.assertThat;

public class IssueDtoTest {

  @Rule
  public ExpectedException thrown = ExpectedException.none();

  @Test
  public void set_data_check_maximal_length() {
    thrown.expect(IllegalArgumentException.class);
    thrown.expectMessage("Issue attributes must not exceed 4000 characters: ");

    StringBuilder s = new StringBuilder(4500);
    for (int i = 0; i < 4500; i++) {
      s.append('a');
    }
    new IssueDto().setIssueAttributes(s.toString());
  }

  @Test
  public void set_issue_fields() {
    Date createdAt = DateUtils.addDays(new Date(), -5);
    Date updatedAt = DateUtils.addDays(new Date(), -3);
    Date closedAt = DateUtils.addDays(new Date(), -1);

    IssueDto dto = new IssueDto()
      .setKee("100")
      .setRuleId(1)
      .setRuleKey_unit_test_only("squid", "AvoidCycle")
      .setComponentKey_unit_test_only("org.sonar.sample:Sample")
      .setRootComponentKey_unit_test_only("org.sonar.sample")
      .setComponentId(1l)
      .setRootComponentId(1l)
      .setStatus(Issue.STATUS_CLOSED)
      .setResolution(Issue.RESOLUTION_FALSE_POSITIVE)
      .setEffortToFix(15.0)
      .setDebt(10L)
      .setLine(6)
      .setSeverity("BLOCKER")
      .setMessage("message")
      .setManualSeverity(true)
      .setReporter("arthur")
      .setAssignee("perceval")
      .setIssueAttributes("key=value")
      .setAuthorLogin("pierre")
      .setIssueCreationDate(createdAt)
      .setIssueUpdateDate(updatedAt)
      .setIssueCloseDate(closedAt);

    DefaultIssue issue = dto.toDefaultIssue();
    assertThat(issue.key()).isEqualTo("100");
    assertThat(issue.ruleKey().toString()).isEqualTo("squid:AvoidCycle");
    assertThat(issue.componentKey()).isEqualTo("org.sonar.sample:Sample");
    assertThat(issue.componentId()).isEqualTo(1L);
    assertThat(issue.projectKey()).isEqualTo("org.sonar.sample");
    assertThat(issue.status()).isEqualTo(Issue.STATUS_CLOSED);
    assertThat(issue.resolution()).isEqualTo(Issue.RESOLUTION_FALSE_POSITIVE);
    assertThat(issue.effortToFix()).isEqualTo(15.0);
    assertThat(issue.debt()).isEqualTo(Duration.create(10L));
    assertThat(issue.line()).isEqualTo(6);
    assertThat(issue.severity()).isEqualTo("BLOCKER");
    assertThat(issue.message()).isEqualTo("message");
    assertThat(issue.manualSeverity()).isTrue();
    assertThat(issue.reporter()).isEqualTo("arthur");
    assertThat(issue.assignee()).isEqualTo("perceval");
    assertThat(issue.attribute("key")).isEqualTo("value");
    assertThat(issue.authorLogin()).isEqualTo("pierre");
    assertThat(issue.creationDate()).isEqualTo(DateUtils.truncate(createdAt, Calendar.SECOND));
    assertThat(issue.updateDate()).isEqualTo(DateUtils.truncate(updatedAt, Calendar.SECOND));
    assertThat(issue.closeDate()).isEqualTo(DateUtils.truncate(closedAt, Calendar.SECOND));
    assertThat(issue.isNew()).isFalse();
  }

}
