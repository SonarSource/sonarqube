/*
 * SonarQube
 * Copyright (C) 2009-2022 SonarSource SA
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
package org.sonar.api.ce.measure.test;

import org.junit.Test;
import org.sonar.api.ce.measure.Issue;
import org.sonar.api.rule.RuleKey;
import org.sonar.api.rule.Severity;
import org.sonar.api.rules.RuleType;
import org.sonar.api.utils.Duration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

public class TestIssueTest {

  @Test
  public void create_issue() {
    Issue issue = new TestIssue.Builder()
      .setKey("ABCD")
      .setRuleKey(RuleKey.of("xoo", "S01"))
      .setSeverity(Severity.BLOCKER)
      .setStatus(org.sonar.api.issue.Issue.STATUS_RESOLVED)
      .setResolution(org.sonar.api.issue.Issue.RESOLUTION_FIXED)
      .setEffort(Duration.create(10L))
      .setType(RuleType.BUG)
      .build();

    assertThat(issue.key()).isEqualTo("ABCD");
    assertThat(issue.ruleKey()).isEqualTo(RuleKey.of("xoo", "S01"));
    assertThat(issue.severity()).isEqualTo(Severity.BLOCKER);
    assertThat(issue.status()).isEqualTo(org.sonar.api.issue.Issue.STATUS_RESOLVED);
    assertThat(issue.resolution()).isEqualTo(org.sonar.api.issue.Issue.RESOLUTION_FIXED);
    assertThat(issue.effort()).isEqualTo(Duration.create(10L));
  }

  @Test
  public void create_issue_with_minimal_fields() {
    Issue issue = new TestIssue.Builder()
      .setKey("ABCD")
      .setRuleKey(RuleKey.of("xoo", "S01"))
      .setSeverity(Severity.BLOCKER)
      .setStatus(org.sonar.api.issue.Issue.STATUS_RESOLVED)
      .setType(RuleType.BUG)
      .build();

    assertThat(issue.resolution()).isNull();
    assertThat(issue.effort()).isNull();
  }

  @Test
  public void fail_with_NPE_when_building_issue_without_key() {
    assertThatThrownBy(() -> new TestIssue.Builder()
      .setRuleKey(RuleKey.of("xoo", "S01"))
      .setSeverity(Severity.BLOCKER)
      .setStatus(org.sonar.api.issue.Issue.STATUS_RESOLVED)
      .setResolution(org.sonar.api.issue.Issue.RESOLUTION_FIXED)
      .setType(RuleType.BUG)
      .build())
      .isInstanceOf(NullPointerException.class)
      .hasMessage("key cannot be null");
  }

  @Test
  public void fail_with_NPE_when_creating_issue_with_null_key() {
    assertThatThrownBy(() -> new TestIssue.Builder().setKey(null))
      .isInstanceOf(NullPointerException.class)
      .hasMessage("key cannot be null");
  }

  @Test
  public void fail_with_NPE_when_building_issue_without_rule_key() {
    assertThatThrownBy(() -> new TestIssue.Builder()
      .setKey("ABCD")
      .setSeverity(Severity.BLOCKER)
      .setStatus(org.sonar.api.issue.Issue.STATUS_RESOLVED)
      .setResolution(org.sonar.api.issue.Issue.RESOLUTION_FIXED)
      .setType(RuleType.BUG)
      .build())
      .isInstanceOf(NullPointerException.class)
      .hasMessage("ruleKey cannot be null");
  }

  @Test
  public void fail_with_NPE_when_creating_issue_with_null_rule_key() {
    assertThatThrownBy(() -> new TestIssue.Builder().setRuleKey(null))
      .isInstanceOf(NullPointerException.class)
      .hasMessage("ruleKey cannot be null");
  }

  @Test
  public void fail_with_IAE_when_building_issue_with_invalid_resolution() {
    assertThatThrownBy(() -> new TestIssue.Builder()
      .setKey("ABCD")
      .setRuleKey(RuleKey.of("xoo", "S01"))
      .setSeverity(Severity.BLOCKER)
      .setStatus(org.sonar.api.issue.Issue.STATUS_RESOLVED)
      .setResolution("unknown")
      .setType(RuleType.BUG)
      .build())
      .isInstanceOf(IllegalArgumentException.class)
      .hasMessage("resolution 'unknown' is invalid");
  }

  @Test
  public void fail_with_NPE_when_building_issue_without_severity() {
    assertThatThrownBy(() -> new TestIssue.Builder()
      .setKey("ABCD")
      .setRuleKey(RuleKey.of("xoo", "S01"))
      .setStatus(org.sonar.api.issue.Issue.STATUS_RESOLVED)
      .setResolution(org.sonar.api.issue.Issue.RESOLUTION_FIXED)
      .setType(RuleType.BUG)
      .build())
      .isInstanceOf(NullPointerException.class)
      .hasMessage("severity cannot be null");
  }

  @Test
  public void fail_with_NPE_when_creating_issue_with_null_severity() {
    assertThatThrownBy(() -> new TestIssue.Builder().setSeverity(null))
      .isInstanceOf(NullPointerException.class)
      .hasMessage("severity cannot be null");
  }

  @Test
  public void fail_with_IAE_when_building_issue_with_invalid_severity() {
    assertThatThrownBy(() -> new TestIssue.Builder()
      .setKey("ABCD")
      .setRuleKey(RuleKey.of("xoo", "S01"))
      .setSeverity("unknown")
      .setStatus(org.sonar.api.issue.Issue.STATUS_RESOLVED)
      .setResolution(org.sonar.api.issue.Issue.RESOLUTION_FIXED)
      .setType(RuleType.BUG)
      .build())
      .isInstanceOf(IllegalArgumentException.class)
      .hasMessage("severity 'unknown' is invalid");
  }

  @Test
  public void fail_with_NPE_when_building_issue_without_status() {
    assertThatThrownBy(() -> new TestIssue.Builder()
      .setKey("ABCD")
      .setRuleKey(RuleKey.of("xoo", "S01"))
      .setSeverity(Severity.BLOCKER)
      .setResolution(org.sonar.api.issue.Issue.RESOLUTION_FIXED)
      .setType(RuleType.BUG)
      .build())
      .isInstanceOf(NullPointerException.class)
      .hasMessage("status cannot be null");
  }

  @Test
  public void fail_with_NPE_when_creating_issue_with_null_status() {
    assertThatThrownBy(() -> new TestIssue.Builder().setStatus(null))
      .isInstanceOf(NullPointerException.class)
      .hasMessage("status cannot be null");
  }

  @Test
  public void fail_with_IAE_when_building_issue_with_invalid_status() {
    assertThatThrownBy(() -> new TestIssue.Builder()
      .setKey("ABCD")
      .setRuleKey(RuleKey.of("xoo", "S01"))
      .setSeverity(Severity.BLOCKER)
      .setStatus("unknown")
      .setResolution(org.sonar.api.issue.Issue.RESOLUTION_FIXED)
      .setType(RuleType.BUG)
      .build())
      .isInstanceOf(IllegalArgumentException.class)
      .hasMessage("status 'unknown' is invalid");
  }
}
