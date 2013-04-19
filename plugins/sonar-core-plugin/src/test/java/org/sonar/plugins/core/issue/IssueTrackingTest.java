/*
 * Sonar, open source software quality management tool.
 * Copyright (C) 2008-2012 SonarSource
 * mailto:contact AT sonarsource DOT com
 *
 * Sonar is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * Sonar is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with Sonar; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02
 */

package org.sonar.plugins.core.issue;

import com.google.common.collect.Lists;
import org.junit.Before;
import org.junit.Test;
import org.sonar.api.resources.Project;
import org.sonar.api.rule.RuleKey;
import org.sonar.api.rules.Rule;
import org.sonar.api.rules.RuleFinder;
import org.sonar.api.utils.DateUtils;
import org.sonar.core.issue.DefaultIssue;
import org.sonar.core.issue.IssueDto;

import java.util.Date;
import java.util.Map;

import static com.google.common.collect.Lists.newArrayList;
import static org.fest.assertions.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class IssueTrackingTest {

  private final Date analysisDate = DateUtils.parseDate("2013-04-11");
  private IssueTracking decorator;
  private long violationId = 0;

  @Before
  public void before() {
    Rule rule1 = Rule.create("squid", "AvoidCycle");
    rule1.setId(1);
    Rule rule2 = Rule.create("squid", "NullDeref");
    rule2.setId(2);

    RuleFinder ruleFinder = mock(RuleFinder.class);
    when(ruleFinder.findById(1)).thenReturn(rule1);
    when(ruleFinder.findById(2)).thenReturn(rule2);
    when(ruleFinder.findByKey(RuleKey.of("squid", "AvoidCycle"))).thenReturn(rule1);
    when(ruleFinder.findByKey(RuleKey.of("squid", "NullDeref"))).thenReturn(rule2);

    Project project = mock(Project.class);
    when(project.getAnalysisDate()).thenReturn(analysisDate);
    decorator = new IssueTracking(project, ruleFinder, null, null);
  }

  @Test
  public void key_should_be_the_prioritary_field_to_check() {
    IssueDto referenceIssue1 = newReferenceIssue("message", 10, 1, "checksum1").setUuid("100");
    IssueDto referenceIssue2 = newReferenceIssue("message", 10, 1, "checksum2").setUuid("200");

    // exactly the fields of referenceIssue1 but not the same key
    DefaultIssue newIssue = newDefaultIssue("message", 10, RuleKey.of("squid", "AvoidCycle"), "checksum1").setKey("200");

    decorator.mapIssues(newArrayList(newIssue), newArrayList(referenceIssue1, referenceIssue2));
    // same key
    assertThat(decorator.getReferenceIssue(newIssue)).isSameAs(referenceIssue2);
    assertThat(newIssue.isNew()).isFalse();
  }

  @Test
  public void checksum_should_have_greater_priority_than_line() {
    IssueDto referenceIssue1 = newReferenceIssue("message", 1, 1, "checksum1");
    IssueDto referenceIssue2 = newReferenceIssue("message", 3, 1, "checksum2");

    DefaultIssue newIssue1 = newDefaultIssue("message", 3, RuleKey.of("squid", "AvoidCycle"), "checksum1");
    DefaultIssue newIssue2 = newDefaultIssue("message", 5, RuleKey.of("squid", "AvoidCycle"), "checksum2");

    decorator.mapIssues(newArrayList(newIssue1, newIssue2), newArrayList(referenceIssue1, referenceIssue2));
    assertThat(decorator.getReferenceIssue(newIssue1)).isSameAs(referenceIssue1);
    assertThat(newIssue1.isNew()).isFalse();
    assertThat(decorator.getReferenceIssue(newIssue2)).isSameAs(referenceIssue2);
    assertThat(newIssue2.isNew()).isFalse();
  }

  /**
   * SONAR-2928
   */
  @Test
  public void same_rule_and_null_line_and_checksum_but_different_messages() {
    DefaultIssue newIssue = newDefaultIssue("new message", null, RuleKey.of("squid", "AvoidCycle"), "checksum1");
    IssueDto referenceIssue = newReferenceIssue("old message", null, 1, "checksum1");

    decorator.mapIssues(newArrayList(newIssue), newArrayList(referenceIssue));
    assertThat(decorator.getReferenceIssue(newIssue)).isSameAs(referenceIssue);
    assertThat(newIssue.isNew()).isFalse();
  }

  @Test
  public void same_rule_and_line_and_checksum_but_different_messages() {
    DefaultIssue newIssue = newDefaultIssue("new message", 1, RuleKey.of("squid", "AvoidCycle"), "checksum1");
    IssueDto referenceIssue = newReferenceIssue("old message", 1, 1, "checksum1");

    decorator.mapIssues(newArrayList(newIssue), newArrayList(referenceIssue));
    assertThat(decorator.getReferenceIssue(newIssue)).isSameAs(referenceIssue);
    assertThat(newIssue.isNew()).isFalse();
  }

  @Test
  public void same_rule_and_line_message() {
    DefaultIssue newIssue = newDefaultIssue("message", 1, RuleKey.of("squid", "AvoidCycle"), "checksum1");
    IssueDto referenceIssue = newReferenceIssue("message", 1, 1, "checksum2");

    decorator.mapIssues(newArrayList(newIssue), newArrayList(referenceIssue));
    assertThat(decorator.getReferenceIssue(newIssue)).isSameAs(referenceIssue);
    assertThat(newIssue.isNew()).isFalse();
  }

  @Test
  public void should_ignore_reference_measure_without_checksum() {
    DefaultIssue newIssue = newDefaultIssue("message", 1, RuleKey.of("squid", "AvoidCycle"), null);
    IssueDto referenceIssue = newReferenceIssue("message", 1, 2, null);

    decorator.mapIssues(newArrayList(newIssue), newArrayList(referenceIssue));
    assertThat(decorator.getReferenceIssue(newIssue)).isNull();
    assertThat(newIssue.isNew()).isTrue();
  }

  @Test
  public void same_rule_and_message_and_checksum_but_different_line() {
    DefaultIssue newIssue = newDefaultIssue("message", 1, RuleKey.of("squid", "AvoidCycle"), "checksum1");
    IssueDto referenceIssue = newReferenceIssue("message", 2, 1, "checksum1");

    decorator.mapIssues(newArrayList(newIssue), newArrayList(referenceIssue));
    assertThat(decorator.getReferenceIssue(newIssue)).isSameAs(referenceIssue);
    assertThat(newIssue.isNew()).isFalse();
  }

  /**
   * SONAR-2812
   */
  @Test
  public void same_checksum_and_rule_but_different_line_and_different_message() {
    DefaultIssue newIssue = newDefaultIssue("new message", 1, RuleKey.of("squid", "AvoidCycle"), "checksum1");
    IssueDto referenceIssue = newReferenceIssue("old message", 2, 1, "checksum1");

    decorator.mapIssues(newArrayList(newIssue), newArrayList(referenceIssue));
    assertThat(decorator.getReferenceIssue(newIssue)).isSameAs(referenceIssue);
    assertThat(newIssue.isNew()).isFalse();
  }

  @Test
  public void should_create_new_issue_when_same_rule_same_message_but_different_line_and_checksum() {
    DefaultIssue newIssue = newDefaultIssue("message", 1, RuleKey.of("squid", "AvoidCycle"), "checksum1");
    IssueDto referenceIssue = newReferenceIssue("message", 2, 1, "checksum2");

    decorator.mapIssues(newArrayList(newIssue), newArrayList(referenceIssue));
    assertThat(decorator.getReferenceIssue(newIssue)).isNull();
    assertThat(newIssue.isNew()).isTrue();
  }

  @Test
  public void should_not_track_issue_if_different_rule() {
    DefaultIssue newIssue = newDefaultIssue("message", 1, RuleKey.of("squid", "AvoidCycle"), "checksum1");
    IssueDto referenceIssue = newReferenceIssue("message", 1, 2, "checksum1");

    decorator.mapIssues(newArrayList(newIssue), newArrayList(referenceIssue));
    assertThat(decorator.getReferenceIssue(newIssue)).isNull();
    assertThat(newIssue.isNew()).isTrue();
  }

  @Test
  public void should_compare_issues_with_database_format() {
    // issue messages are trimmed and can be abbreviated when persisted in database.
    // Comparing issue messages must use the same format.
    DefaultIssue newIssue = newDefaultIssue(" message ", 1, RuleKey.of("squid", "AvoidCycle"), "checksum1");
    IssueDto referenceIssue = newReferenceIssue("       message       ", 1, 1, "checksum2");

    decorator.mapIssues(newArrayList(newIssue), newArrayList(referenceIssue));
    assertThat(decorator.getReferenceIssue(newIssue)).isSameAs(referenceIssue);
    assertThat(newIssue.isNew()).isFalse();
  }

  @Test
  public void should_set_date_of_new_issues() {
    DefaultIssue newIssue = newDefaultIssue("message", 1, RuleKey.of("squid", "AvoidCycle"), "checksum");
    assertThat(newIssue.createdAt()).isNull();

    Map<DefaultIssue, IssueDto> mapping = decorator.mapIssues(newArrayList(newIssue), Lists.<IssueDto>newArrayList());
    assertThat(mapping.size()).isEqualTo(0);
    assertThat(newIssue.createdAt()).isEqualTo(analysisDate);
    assertThat(newIssue.isNew()).isTrue();
  }

  @Test
  public void should_set_severity_if_severity_has_been_changed_by_user() {
    DefaultIssue newIssue = newDefaultIssue("message", 1, RuleKey.of("squid", "AvoidCycle"), "checksum").setSeverity("MAJOR");
    IssueDto referenceIssue = newReferenceIssue("message", 1, 1, "checksum").setSeverity("MINOR").setManualSeverity(true);

    decorator.mapIssues(newArrayList(newIssue), newArrayList(referenceIssue));
    assertThat(newIssue.severity()).isEqualTo("MINOR");
  }

  @Test
  public void should_copy_date_when_not_new() {
    DefaultIssue newIssue = newDefaultIssue("message", 1, RuleKey.of("squid", "AvoidCycle"), "checksum");
    IssueDto referenceIssue = newReferenceIssue("", 1, 1, "checksum");
    Date referenceDate = DateUtils.parseDate("2009-05-18");
    referenceIssue.setCreatedAt(referenceDate);
    assertThat(newIssue.createdAt()).isNull();

    Map<DefaultIssue, IssueDto> mapping = decorator.mapIssues(newArrayList(newIssue), newArrayList(referenceIssue));
    assertThat(mapping.size()).isEqualTo(1);
    assertThat(newIssue.createdAt()).isEqualTo(referenceDate);
    assertThat(newIssue.isNew()).isFalse();
  }

  private DefaultIssue newDefaultIssue(String description, Integer line, RuleKey ruleKey, String checksum) {
    return new DefaultIssue().setDescription(description).setLine(line).setRuleKey(ruleKey).setChecksum(checksum);
  }

  private IssueDto newReferenceIssue(String description, Integer lineId, int ruleId, String lineChecksum) {
    IssueDto referenceIssue = new IssueDto();
    Long id = violationId++;
    referenceIssue.setId(id);
    referenceIssue.setUuid(Long.toString(id));
    referenceIssue.setLine(lineId);
    referenceIssue.setDescription(description);
    referenceIssue.setRuleId(ruleId);
    referenceIssue.setChecksum(lineChecksum);
    return referenceIssue;
  }

}
