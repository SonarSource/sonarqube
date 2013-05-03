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

package org.sonar.plugins.core.issue;

import com.google.common.base.Charsets;
import com.google.common.io.Resources;
import org.junit.Before;
import org.junit.Test;
import org.sonar.api.issue.Issue;
import org.sonar.api.resources.Project;
import org.sonar.api.rule.RuleKey;
import org.sonar.api.rules.Rule;
import org.sonar.api.rules.RuleFinder;
import org.sonar.api.utils.DateUtils;
import org.sonar.batch.scan.LastSnapshots;
import org.sonar.core.issue.DefaultIssue;
import org.sonar.core.issue.db.IssueDto;

import java.io.IOException;
import java.util.Arrays;
import java.util.Date;
import java.util.Map;

import static com.google.common.collect.Lists.newArrayList;
import static org.fest.assertions.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class IssueTrackingTest {

  private final Date analysisDate = DateUtils.parseDate("2013-04-11");

  IssueTracking tracking;
  Project project;
  RuleFinder ruleFinder;
  LastSnapshots lastSnapshots;
  long violationId = 0;

  @Before
  public void before() {
    Rule rule1 = Rule.create("squid", "AvoidCycle");
    rule1.setId(1);
    Rule rule2 = Rule.create("squid", "NullDeref");
    rule2.setId(2);
    Rule rule3 = Rule.create("pmd", "UnusedLocalVariable");
    rule3.setId(3);

    ruleFinder = mock(RuleFinder.class);
    when(ruleFinder.findById(1)).thenReturn(rule1);
    when(ruleFinder.findById(2)).thenReturn(rule2);
    when(ruleFinder.findById(3)).thenReturn(rule3);
    when(ruleFinder.findByKey(RuleKey.of("squid", "AvoidCycle"))).thenReturn(rule1);
    when(ruleFinder.findByKey(RuleKey.of("squid", "NullDeref"))).thenReturn(rule2);
    when(ruleFinder.findByKey(RuleKey.of("pmd", "UnusedLocalVariable"))).thenReturn(rule3);

    lastSnapshots = mock(LastSnapshots.class);

    project = mock(Project.class);
    when(project.getAnalysisDate()).thenReturn(analysisDate);
    tracking = new IssueTracking(project, ruleFinder, lastSnapshots, null);
  }

  @Test
  public void key_should_be_the_prioritary_field_to_check() {
    IssueDto referenceIssue1 = newReferenceIssue("message", 10, 1, "checksum1").setKey("100");
    IssueDto referenceIssue2 = newReferenceIssue("message", 10, 1, "checksum2").setKey("200");

    // exactly the fields of referenceIssue1 but not the same key
    DefaultIssue newIssue = newDefaultIssue("message", 10, RuleKey.of("squid", "AvoidCycle"), "checksum1").setKey("200");

    tracking.mapIssues(newArrayList(newIssue), newArrayList(referenceIssue1, referenceIssue2));
    // same key
    assertThat(tracking.getReferenceIssue(newIssue)).isSameAs(referenceIssue2);
    assertThat(newIssue.isNew()).isFalse();
  }

  @Test
  public void checksum_should_have_greater_priority_than_line() {
    IssueDto referenceIssue1 = newReferenceIssue("message", 1, 1, "checksum1");
    IssueDto referenceIssue2 = newReferenceIssue("message", 3, 1, "checksum2");

    DefaultIssue newIssue1 = newDefaultIssue("message", 3, RuleKey.of("squid", "AvoidCycle"), "checksum1");
    DefaultIssue newIssue2 = newDefaultIssue("message", 5, RuleKey.of("squid", "AvoidCycle"), "checksum2");

    tracking.mapIssues(newArrayList(newIssue1, newIssue2), newArrayList(referenceIssue1, referenceIssue2));
    assertThat(tracking.getReferenceIssue(newIssue1)).isSameAs(referenceIssue1);
    assertThat(newIssue1.isNew()).isFalse();
    assertThat(tracking.getReferenceIssue(newIssue2)).isSameAs(referenceIssue2);
    assertThat(newIssue2.isNew()).isFalse();
  }

  /**
   * SONAR-2928
   */
  @Test
  public void same_rule_and_null_line_and_checksum_but_different_messages() {
    DefaultIssue newIssue = newDefaultIssue("new message", null, RuleKey.of("squid", "AvoidCycle"), "checksum1");
    IssueDto referenceIssue = newReferenceIssue("old message", null, 1, "checksum1");

    tracking.mapIssues(newArrayList(newIssue), newArrayList(referenceIssue));
    assertThat(tracking.getReferenceIssue(newIssue)).isSameAs(referenceIssue);
    assertThat(newIssue.isNew()).isFalse();
  }

  @Test
  public void same_rule_and_line_and_checksum_but_different_messages() {
    DefaultIssue newIssue = newDefaultIssue("new message", 1, RuleKey.of("squid", "AvoidCycle"), "checksum1");
    IssueDto referenceIssue = newReferenceIssue("old message", 1, 1, "checksum1");

    tracking.mapIssues(newArrayList(newIssue), newArrayList(referenceIssue));
    assertThat(tracking.getReferenceIssue(newIssue)).isSameAs(referenceIssue);
    assertThat(newIssue.isNew()).isFalse();
  }

  @Test
  public void same_rule_and_line_message() {
    DefaultIssue newIssue = newDefaultIssue("message", 1, RuleKey.of("squid", "AvoidCycle"), "checksum1");
    IssueDto referenceIssue = newReferenceIssue("message", 1, 1, "checksum2");

    tracking.mapIssues(newArrayList(newIssue), newArrayList(referenceIssue));
    assertThat(tracking.getReferenceIssue(newIssue)).isSameAs(referenceIssue);
    assertThat(newIssue.isNew()).isFalse();
  }

  @Test
  public void should_ignore_reference_measure_without_checksum() {
    DefaultIssue newIssue = newDefaultIssue("message", 1, RuleKey.of("squid", "AvoidCycle"), null);
    IssueDto referenceIssue = newReferenceIssue("message", 1, 2, null);

    tracking.mapIssues(newArrayList(newIssue), newArrayList(referenceIssue));
    assertThat(tracking.getReferenceIssue(newIssue)).isNull();
    assertThat(newIssue.isNew()).isTrue();
  }

  @Test
  public void same_rule_and_message_and_checksum_but_different_line() {
    DefaultIssue newIssue = newDefaultIssue("message", 1, RuleKey.of("squid", "AvoidCycle"), "checksum1");
    IssueDto referenceIssue = newReferenceIssue("message", 2, 1, "checksum1");

    tracking.mapIssues(newArrayList(newIssue), newArrayList(referenceIssue));
    assertThat(tracking.getReferenceIssue(newIssue)).isSameAs(referenceIssue);
    assertThat(newIssue.isNew()).isFalse();
  }

  /**
   * SONAR-2812
   */
  @Test
  public void same_checksum_and_rule_but_different_line_and_different_message() {
    DefaultIssue newIssue = newDefaultIssue("new message", 1, RuleKey.of("squid", "AvoidCycle"), "checksum1");
    IssueDto referenceIssue = newReferenceIssue("old message", 2, 1, "checksum1");

    tracking.mapIssues(newArrayList(newIssue), newArrayList(referenceIssue));
    assertThat(tracking.getReferenceIssue(newIssue)).isSameAs(referenceIssue);
    assertThat(newIssue.isNew()).isFalse();
  }

  @Test
  public void should_create_new_issue_when_same_rule_same_message_but_different_line_and_checksum() {
    DefaultIssue newIssue = newDefaultIssue("message", 1, RuleKey.of("squid", "AvoidCycle"), "checksum1");
    IssueDto referenceIssue = newReferenceIssue("message", 2, 1, "checksum2");

    tracking.mapIssues(newArrayList(newIssue), newArrayList(referenceIssue));
    assertThat(tracking.getReferenceIssue(newIssue)).isNull();
    assertThat(newIssue.isNew()).isTrue();
  }

  @Test
  public void should_not_track_issue_if_different_rule() {
    DefaultIssue newIssue = newDefaultIssue("message", 1, RuleKey.of("squid", "AvoidCycle"), "checksum1");
    IssueDto referenceIssue = newReferenceIssue("message", 1, 2, "checksum1");

    tracking.mapIssues(newArrayList(newIssue), newArrayList(referenceIssue));
    assertThat(tracking.getReferenceIssue(newIssue)).isNull();
    assertThat(newIssue.isNew()).isTrue();
  }

  @Test
  public void should_compare_issues_with_database_format() {
    // issue messages are trimmed and can be abbreviated when persisted in database.
    // Comparing issue messages must use the same format.
    DefaultIssue newIssue = newDefaultIssue("      message    ", 1, RuleKey.of("squid", "AvoidCycle"), "checksum1");
    IssueDto referenceIssue = newReferenceIssue("message", 1, 1, "checksum2");

    tracking.mapIssues(newArrayList(newIssue), newArrayList(referenceIssue));
    assertThat(tracking.getReferenceIssue(newIssue)).isSameAs(referenceIssue);
    assertThat(newIssue.isNew()).isFalse();
  }

  @Test
  public void should_set_severity_if_severity_has_been_changed_by_user() {
    DefaultIssue newIssue = newDefaultIssue("message", 1, RuleKey.of("squid", "AvoidCycle"), "checksum").setSeverity("MAJOR");
    IssueDto referenceIssue = newReferenceIssue("message", 1, 1, "checksum").setSeverity("MINOR").setManualSeverity(true);

    tracking.mapIssues(newArrayList(newIssue), newArrayList(referenceIssue));
    assertThat(newIssue.severity()).isEqualTo("MINOR");
  }

  @Test
  public void should_copy_some_fields_when_not_new() {
    DefaultIssue newIssue = newDefaultIssue("message", 1, RuleKey.of("squid", "AvoidCycle"), "checksum");
    IssueDto referenceIssue = newReferenceIssue("", 1, 1, "checksum").setAuthorLogin("arthur").setAssignee("perceval");
    Date referenceDate = DateUtils.parseDate("2009-05-18");
    referenceIssue.setIssueCreationDate(referenceDate);
    assertThat(newIssue.creationDate()).isNull();

    Map<DefaultIssue, IssueDto> mapping = tracking.mapIssues(newArrayList(newIssue), newArrayList(referenceIssue));
    assertThat(mapping.size()).isEqualTo(1);
    assertThat(newIssue.isNew()).isFalse();

    assertThat(newIssue.creationDate()).isEqualTo(referenceDate);
    assertThat(newIssue.assignee()).isEqualTo("perceval");
    assertThat(newIssue.authorLogin()).isEqualTo("arthur");
  }

  @Test
  public void past_issue_not_associated_with_line_should_not_cause_npe() throws Exception {
    when(lastSnapshots.getSource(project)).thenReturn(load("example2-v1"));
    String source = load("example2-v2");

    DefaultIssue newIssue = newDefaultIssue("Indentation", 9, RuleKey.of("squid", "AvoidCycle"), "foo");
    IssueDto referenceIssue = newReferenceIssue("2 branches need to be covered", null, 1, null);


    Map<DefaultIssue, IssueDto> mapping = tracking.mapIssues(
      newArrayList(newIssue),
      newArrayList(referenceIssue),
      source, project);

    assertThat(mapping.isEmpty()).isTrue();
    assertThat(newIssue.isNew()).isTrue();
  }

  @Test
  public void new_issue_not_associated_with_line_should_not_cause_npe() throws Exception {
    when(lastSnapshots.getSource(project)).thenReturn(load("example2-v1"));
    String source = load("example2-v2");

    DefaultIssue newIssue = newDefaultIssue("1 branch need to be covered", null, RuleKey.of("squid", "AvoidCycle"), "foo");
    IssueDto referenceIssue = newReferenceIssue("Indentationd", 7, 1, null);

    Map<DefaultIssue, IssueDto> mapping = tracking.mapIssues(
      newArrayList(newIssue),
      newArrayList(referenceIssue),
      source, project);

    assertThat(mapping.isEmpty()).isTrue();
    assertThat(newIssue.isNew()).isTrue();
  }

  /**
   * SONAR-2928
   */
  @Test
  public void issue_not_associated_with_line() throws Exception {
    when(lastSnapshots.getSource(project)).thenReturn(load("example2-v1"));
    String source = load("example2-v2");

    DefaultIssue newIssue = newDefaultIssue("1 branch need to be covered", null, RuleKey.of("squid", "AvoidCycle"), null);
    IssueDto referenceIssue = newReferenceIssue("2 branches need to be covered", null, 1, null);

    Map<DefaultIssue, IssueDto> mapping = tracking.mapIssues(
      newArrayList(newIssue),
      newArrayList(referenceIssue),
      source, project);

    assertThat(newIssue.isNew()).isFalse();
    assertThat(mapping.get(newIssue)).isEqualTo(referenceIssue);
  }

  /**
   * SONAR-3072
   */
  @Test
  public void should_track_issues_based_on_blocks_recognition_on_example1() throws Exception {
    when(lastSnapshots.getSource(project)).thenReturn(load("example1-v1"));
    String source = load("example1-v2");

    IssueDto referenceIssue1 = newReferenceIssue("Indentation", 7, 1, null);
    IssueDto referenceIssue2 = newReferenceIssue("Indentation", 11, 1, null);

    DefaultIssue newIssue1 = newDefaultIssue("Indentation", 9, RuleKey.of("squid", "AvoidCycle"), null);
    DefaultIssue newIssue2 = newDefaultIssue("Indentation", 13, RuleKey.of("squid", "AvoidCycle"), null);
    DefaultIssue newIssue3 = newDefaultIssue("Indentation", 17, RuleKey.of("squid", "AvoidCycle"), null);
    DefaultIssue newIssue4 = newDefaultIssue("Indentation", 21, RuleKey.of("squid", "AvoidCycle"), null);

    Map<DefaultIssue, IssueDto> mapping = tracking.mapIssues(
      Arrays.asList(newIssue1, newIssue2, newIssue3, newIssue4),
      Arrays.asList(referenceIssue1, referenceIssue2),
      source, project);

    assertThat(newIssue1.isNew()).isTrue();
    assertThat(newIssue2.isNew()).isTrue();
    assertThat(newIssue3.isNew()).isFalse();
    assertThat(mapping.get(newIssue3)).isEqualTo(referenceIssue1);
    assertThat(newIssue4.isNew()).isFalse();
    assertThat(mapping.get(newIssue4)).isEqualTo(referenceIssue2);
  }

  /**
   * SONAR-3072
   */
  @Test
  public void should_track_issues_based_on_blocks_recognition_on_example2() throws Exception {
    when(lastSnapshots.getSource(project)).thenReturn(load("example2-v1"));
    String source = load("example2-v2");

    IssueDto referenceIssue1 = newReferenceIssue("SystemPrintln", 5, 1, null);

    DefaultIssue newIssue1 = newDefaultIssue("SystemPrintln", 6, RuleKey.of("squid", "AvoidCycle"), null);
    DefaultIssue newIssue2 = newDefaultIssue("SystemPrintln", 10, RuleKey.of("squid", "AvoidCycle"), null);
    DefaultIssue newIssue3 = newDefaultIssue("SystemPrintln", 14, RuleKey.of("squid", "AvoidCycle"), null);

    Map<DefaultIssue, IssueDto> mapping = tracking.mapIssues(
      Arrays.asList(newIssue1, newIssue2, newIssue3),
      Arrays.asList(referenceIssue1),
      source, project);

    assertThat(newIssue1.isNew()).isTrue();
    assertThat(newIssue2.isNew()).isFalse();
    assertThat(mapping.get(newIssue2)).isEqualTo(referenceIssue1);
    assertThat(newIssue3.isNew()).isTrue();
  }

  @Test
  public void should_track_issues_based_on_blocks_recognition_on_example3() throws Exception {
    when(lastSnapshots.getSource(project)).thenReturn(load("example3-v1"));
    String source = load("example3-v2");

    IssueDto referenceIssue1 = newReferenceIssue("Avoid unused local variables such as 'j'.", 6, 1, "63c11570fc0a76434156be5f8138fa03");
    IssueDto referenceIssue2 = newReferenceIssue("Avoid unused private methods such as 'myMethod()'.", 13, 2, "ef23288705d1ef1e512448ace287586e");
    IssueDto referenceIssue3 = newReferenceIssue("Method 'avoidUtilityClass' is not designed for extension - needs to be abstract, final or empty.", 9, 3, "ed5cdd046fda82727d6fedd1d8e3a310");

    // New issue
    DefaultIssue newIssue1 = newDefaultIssue("Avoid unused local variables such as 'msg'.", 18, RuleKey.of("squid", "AvoidCycle"), "a24254126be2bf1a9b9a8db43f633733");
    // Same as referenceIssue2
    DefaultIssue newIssue2 = newDefaultIssue("Avoid unused private methods such as 'myMethod()'.", 13, RuleKey.of("squid", "NullDeref"), "ef23288705d1ef1e512448ace287586e");
    // Same as referenceIssue3
    DefaultIssue newIssue3 = newDefaultIssue("Method 'avoidUtilityClass' is not designed for extension - needs to be abstract, final or empty.", 9, RuleKey.of("pmd", "UnusedLocalVariable"), "ed5cdd046fda82727d6fedd1d8e3a310");
    // New issue
    DefaultIssue newIssue4 = newDefaultIssue("Method 'newViolation' is not designed for extension - needs to be abstract, final or empty.", 17, RuleKey.of("pmd", "UnusedLocalVariable"), "7d58ac9040c27e4ca2f11a0269e251e2");
    // Same as referenceIssue1
    DefaultIssue newIssue5 = newDefaultIssue("Avoid unused local variables such as 'j'.", 6, RuleKey.of("squid", "AvoidCycle"), "4432a2675ec3e1620daefe38386b51ef");

    Map<DefaultIssue, IssueDto> mapping = tracking.mapIssues(
      Arrays.asList(newIssue1, newIssue2, newIssue3, newIssue4, newIssue5),
      Arrays.asList(referenceIssue1, referenceIssue2, referenceIssue3),
      source, project);

    assertThat(newIssue1.isNew()).isTrue();
    assertThat(newIssue2.isNew()).isFalse();
    assertThat(newIssue3.isNew()).isFalse();
    assertThat(newIssue4.isNew()).isTrue();
    assertThat(newIssue5.isNew()).isFalse();
    assertThat(mapping.get(newIssue2)).isEqualTo(referenceIssue2);
    assertThat(mapping.get(newIssue3)).isEqualTo(referenceIssue3);
    assertThat(mapping.get(newIssue5)).isEqualTo(referenceIssue1);
  }

  private static String load(String name) throws IOException {
    return Resources.toString(IssueTrackingTest.class.getResource("IssueTrackingTest/" + name + ".txt"), Charsets.UTF_8);
  }

  private DefaultIssue newDefaultIssue(String message, Integer line, RuleKey ruleKey, String checksum) {
    return new DefaultIssue().setDescription(message).setLine(line).setRuleKey(ruleKey).setChecksum(checksum).setStatus(Issue.STATUS_OPEN);
  }

  private IssueDto newReferenceIssue(String message, Integer lineId, int ruleId, String lineChecksum) {
    IssueDto referenceIssue = new IssueDto();
    Long id = violationId++;
    referenceIssue.setId(id);
    referenceIssue.setKey(Long.toString(id));
    referenceIssue.setLine(lineId);
    referenceIssue.setDescription(message);
    referenceIssue.setRuleId(ruleId);
    referenceIssue.setChecksum(lineChecksum);
    referenceIssue.setResolution(null);
    referenceIssue.setStatus(Issue.STATUS_OPEN);
    return referenceIssue;
  }

}
