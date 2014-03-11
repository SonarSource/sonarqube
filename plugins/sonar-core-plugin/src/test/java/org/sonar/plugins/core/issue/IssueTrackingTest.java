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

package org.sonar.plugins.core.issue;

import org.sonar.api.batch.SonarIndex;

import com.google.common.base.Charsets;
import com.google.common.io.Resources;
import org.junit.Before;
import org.junit.Test;
import org.sonar.api.issue.Issue;
import org.sonar.api.issue.internal.DefaultIssue;
import org.sonar.api.resources.Project;
import org.sonar.api.resources.Resource;
import org.sonar.api.rule.RuleKey;
import org.sonar.batch.scan.LastSnapshots;
import org.sonar.core.issue.db.IssueDto;

import java.io.IOException;
import java.util.Arrays;

import static com.google.common.collect.Lists.newArrayList;
import static org.fest.assertions.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class IssueTrackingTest {

  IssueTracking tracking;
  Resource project;
  SourceHashHolder sourceHashHolder;
  SonarIndex index;
  LastSnapshots lastSnapshots;
  long violationId = 0;

  @Before
  public void before() {
    index = mock(SonarIndex.class);
    lastSnapshots = mock(LastSnapshots.class);

    project = mock(Project.class);
    tracking = new IssueTracking();
  }

  @Test
  public void key_should_be_the_prioritary_field_to_check() {
    IssueDto referenceIssue1 = newReferenceIssue("message", 10, "squid", "AvoidCycle", "checksum1").setKee("100");
    IssueDto referenceIssue2 = newReferenceIssue("message", 10, "squid", "AvoidCycle", "checksum2").setKee("200");

    // exactly the fields of referenceIssue1 but not the same key
    DefaultIssue newIssue = newDefaultIssue("message", 10, RuleKey.of("squid", "AvoidCycle"), "checksum1").setKey("200");

    IssueTrackingResult result = new IssueTrackingResult();
    tracking.mapIssues(newArrayList(newIssue), newArrayList(referenceIssue1, referenceIssue2), null, result);
    // same key
    assertThat(result.matching(newIssue)).isSameAs(referenceIssue2);
  }

  @Test
  public void checksum_should_have_greater_priority_than_line() {
    sourceHashHolder = new SourceHashHolder(index, lastSnapshots, null);

    IssueDto referenceIssue1 = newReferenceIssue("message", 1, "squid", "AvoidCycle", "checksum1");
    IssueDto referenceIssue2 = newReferenceIssue("message", 3, "squid", "AvoidCycle", "checksum2");

    DefaultIssue newIssue1 = newDefaultIssue("message", 3, RuleKey.of("squid", "AvoidCycle"), "checksum1");
    DefaultIssue newIssue2 = newDefaultIssue("message", 5, RuleKey.of("squid", "AvoidCycle"), "checksum2");

    IssueTrackingResult result = new IssueTrackingResult();
    tracking.mapIssues(newArrayList(newIssue1, newIssue2), newArrayList(referenceIssue1, referenceIssue2), sourceHashHolder, result);
    assertThat(result.matching(newIssue1)).isSameAs(referenceIssue1);
    assertThat(result.matching(newIssue2)).isSameAs(referenceIssue2);
  }

  /**
   * SONAR-2928
   */
  @Test
  public void same_rule_and_null_line_and_checksum_but_different_messages() {
    sourceHashHolder = new SourceHashHolder(index, lastSnapshots, null);

    DefaultIssue newIssue = newDefaultIssue("new message", null, RuleKey.of("squid", "AvoidCycle"), "checksum1");
    IssueDto referenceIssue = newReferenceIssue("old message", null, "squid", "AvoidCycle", "checksum1");

    IssueTrackingResult result = new IssueTrackingResult();
    tracking.mapIssues(newArrayList(newIssue), newArrayList(referenceIssue), sourceHashHolder, result);
    assertThat(result.matching(newIssue)).isSameAs(referenceIssue);
  }

  @Test
  public void same_rule_and_line_and_checksum_but_different_messages() {
    sourceHashHolder = new SourceHashHolder(index, lastSnapshots, null);

    DefaultIssue newIssue = newDefaultIssue("new message", 1, RuleKey.of("squid", "AvoidCycle"), "checksum1");
    IssueDto referenceIssue = newReferenceIssue("old message", 1, "squid", "AvoidCycle", "checksum1");

    IssueTrackingResult result = new IssueTrackingResult();
    tracking.mapIssues(newArrayList(newIssue), newArrayList(referenceIssue), sourceHashHolder, result);
    assertThat(result.matching(newIssue)).isSameAs(referenceIssue);
  }

  @Test
  public void same_rule_and_line_message() {
    sourceHashHolder = new SourceHashHolder(index, lastSnapshots, null);

    DefaultIssue newIssue = newDefaultIssue("message", 1, RuleKey.of("squid", "AvoidCycle"), "checksum1");
    IssueDto referenceIssue = newReferenceIssue("message", 1, "squid", "AvoidCycle", "checksum2");

    IssueTrackingResult result = new IssueTrackingResult();
    tracking.mapIssues(newArrayList(newIssue), newArrayList(referenceIssue), sourceHashHolder, result);
    assertThat(result.matching(newIssue)).isSameAs(referenceIssue);
  }

  @Test
  public void should_ignore_reference_measure_without_checksum() {
    sourceHashHolder = new SourceHashHolder(index, lastSnapshots, null);

    DefaultIssue newIssue = newDefaultIssue("message", 1, RuleKey.of("squid", "AvoidCycle"), null);
    IssueDto referenceIssue = newReferenceIssue("message", 1, "squid", "NullDeref", null);

    IssueTrackingResult result = new IssueTrackingResult();
    tracking.mapIssues(newArrayList(newIssue), newArrayList(referenceIssue), sourceHashHolder, result);
    assertThat(result.matching(newIssue)).isNull();
  }

  @Test
  public void same_rule_and_message_and_checksum_but_different_line() {
    sourceHashHolder = new SourceHashHolder(index, lastSnapshots, null);

    DefaultIssue newIssue = newDefaultIssue("message", 1, RuleKey.of("squid", "AvoidCycle"), "checksum1");
    IssueDto referenceIssue = newReferenceIssue("message", 2, "squid", "AvoidCycle", "checksum1");

    IssueTrackingResult result = new IssueTrackingResult();
    tracking.mapIssues(newArrayList(newIssue), newArrayList(referenceIssue), sourceHashHolder, result);
    assertThat(result.matching(newIssue)).isSameAs(referenceIssue);
  }

  /**
   * SONAR-2812
   */
  @Test
  public void same_checksum_and_rule_but_different_line_and_different_message() {
    sourceHashHolder = new SourceHashHolder(index, lastSnapshots, null);

    DefaultIssue newIssue = newDefaultIssue("new message", 1, RuleKey.of("squid", "AvoidCycle"), "checksum1");
    IssueDto referenceIssue = newReferenceIssue("old message", 2, "squid", "AvoidCycle", "checksum1");

    IssueTrackingResult result = new IssueTrackingResult();
    tracking.mapIssues(newArrayList(newIssue), newArrayList(referenceIssue), sourceHashHolder, result);
    assertThat(result.matching(newIssue)).isSameAs(referenceIssue);
  }

  @Test
  public void should_create_new_issue_when_same_rule_same_message_but_different_line_and_checksum() {
    sourceHashHolder = new SourceHashHolder(index, lastSnapshots, null);

    DefaultIssue newIssue = newDefaultIssue("message", 1, RuleKey.of("squid", "AvoidCycle"), "checksum1");
    IssueDto referenceIssue = newReferenceIssue("message", 2, "squid", "AvoidCycle", "checksum2");

    IssueTrackingResult result = new IssueTrackingResult();
    tracking.mapIssues(newArrayList(newIssue), newArrayList(referenceIssue), sourceHashHolder, result);
    assertThat(result.matching(newIssue)).isNull();
  }

  @Test
  public void should_not_track_issue_if_different_rule() {
    sourceHashHolder = new SourceHashHolder(index, lastSnapshots, null);

    DefaultIssue newIssue = newDefaultIssue("message", 1, RuleKey.of("squid", "AvoidCycle"), "checksum1");
    IssueDto referenceIssue = newReferenceIssue("message", 1, "squid", "NullDeref", "checksum1");

    IssueTrackingResult result = new IssueTrackingResult();
    tracking.mapIssues(newArrayList(newIssue), newArrayList(referenceIssue), sourceHashHolder, result);
    assertThat(result.matching(newIssue)).isNull();
  }

  @Test
  public void should_compare_issues_with_database_format() {
    sourceHashHolder = new SourceHashHolder(index, lastSnapshots, null);

    // issue messages are trimmed and can be abbreviated when persisted in database.
    // Comparing issue messages must use the same format.
    DefaultIssue newIssue = newDefaultIssue("      message    ", 1, RuleKey.of("squid", "AvoidCycle"), "checksum1");
    IssueDto referenceIssue = newReferenceIssue("message", 1, "squid", "AvoidCycle", "checksum2");

    IssueTrackingResult result = new IssueTrackingResult();
    tracking.mapIssues(newArrayList(newIssue), newArrayList(referenceIssue), sourceHashHolder, result);
    assertThat(result.matching(newIssue)).isSameAs(referenceIssue);
  }

  @Test
  public void past_issue_not_associated_with_line_should_not_cause_npe() throws Exception {
    when(lastSnapshots.getSource(project)).thenReturn(load("example2-v1"));
    when(index.getSource(project)).thenReturn(load("example2-v2"));
    sourceHashHolder = new SourceHashHolder(index, lastSnapshots, project);

    DefaultIssue newIssue = newDefaultIssue("Indentation", 9, RuleKey.of("squid", "AvoidCycle"), "foo");
    IssueDto referenceIssue = newReferenceIssue("2 branches need to be covered", null, "squid", "AvoidCycle", null);

    IssueTrackingResult result = new IssueTrackingResult();
    tracking.mapIssues(newArrayList(newIssue), newArrayList(referenceIssue), sourceHashHolder, result);

    assertThat(result.matched()).isEmpty();
  }

  @Test
  public void new_issue_not_associated_with_line_should_not_cause_npe() throws Exception {
    when(lastSnapshots.getSource(project)).thenReturn(load("example2-v1"));
    when(index.getSource(project)).thenReturn(load("example2-v2"));
    sourceHashHolder = new SourceHashHolder(index, lastSnapshots, project);

    DefaultIssue newIssue = newDefaultIssue("1 branch need to be covered", null, RuleKey.of("squid", "AvoidCycle"), "foo");
    IssueDto referenceIssue = newReferenceIssue("Indentationd", 7, "squid", "AvoidCycle", null);

    IssueTrackingResult result = new IssueTrackingResult();
    tracking.mapIssues(newArrayList(newIssue), newArrayList(referenceIssue), sourceHashHolder, result);

    assertThat(result.matched()).isEmpty();
  }

  /**
   * SONAR-2928
   */
  @Test
  public void issue_not_associated_with_line() throws Exception {
    when(lastSnapshots.getSource(project)).thenReturn(load("example2-v1"));
    when(index.getSource(project)).thenReturn(load("example2-v2"));
    sourceHashHolder = new SourceHashHolder(index, lastSnapshots, project);

    DefaultIssue newIssue = newDefaultIssue("1 branch need to be covered", null, RuleKey.of("squid", "AvoidCycle"), null);
    IssueDto referenceIssue = newReferenceIssue("2 branches need to be covered", null, "squid", "AvoidCycle", null);

    IssueTrackingResult result = new IssueTrackingResult();
    tracking.mapIssues(newArrayList(newIssue), newArrayList(referenceIssue), sourceHashHolder, result);

    assertThat(result.matching(newIssue)).isEqualTo(referenceIssue);
  }

  /**
   * SONAR-3072
   */
  @Test
  public void should_track_issues_based_on_blocks_recognition_on_example1() throws Exception {
    when(lastSnapshots.getSource(project)).thenReturn(load("example1-v1"));
    when(index.getSource(project)).thenReturn(load("example1-v2"));
    sourceHashHolder = new SourceHashHolder(index, lastSnapshots, project);

    IssueDto referenceIssue1 = newReferenceIssue("Indentation", 7, "squid", "AvoidCycle", null);
    IssueDto referenceIssue2 = newReferenceIssue("Indentation", 11, "squid", "AvoidCycle", null);

    DefaultIssue newIssue1 = newDefaultIssue("Indentation", 9, RuleKey.of("squid", "AvoidCycle"), null);
    DefaultIssue newIssue2 = newDefaultIssue("Indentation", 13, RuleKey.of("squid", "AvoidCycle"), null);
    DefaultIssue newIssue3 = newDefaultIssue("Indentation", 17, RuleKey.of("squid", "AvoidCycle"), null);
    DefaultIssue newIssue4 = newDefaultIssue("Indentation", 21, RuleKey.of("squid", "AvoidCycle"), null);

    IssueTrackingResult result = new IssueTrackingResult();
    tracking.mapIssues(Arrays.asList(newIssue1, newIssue2, newIssue3, newIssue4), Arrays.asList(referenceIssue1, referenceIssue2), sourceHashHolder, result);

    assertThat(result.matching(newIssue1)).isNull();
    assertThat(result.matching(newIssue2)).isNull();
    assertThat(result.matching(newIssue3)).isSameAs(referenceIssue1);
    assertThat(result.matching(newIssue4)).isSameAs(referenceIssue2);
  }

  /**
   * SONAR-3072
   */
  @Test
  public void should_track_issues_based_on_blocks_recognition_on_example2() throws Exception {
    when(lastSnapshots.getSource(project)).thenReturn(load("example2-v1"));
    when(index.getSource(project)).thenReturn(load("example2-v2"));
    sourceHashHolder = new SourceHashHolder(index, lastSnapshots, project);

    IssueDto referenceIssue1 = newReferenceIssue("SystemPrintln", 5, "squid", "AvoidCycle", null);

    DefaultIssue newIssue1 = newDefaultIssue("SystemPrintln", 6, RuleKey.of("squid", "AvoidCycle"), null);
    DefaultIssue newIssue2 = newDefaultIssue("SystemPrintln", 10, RuleKey.of("squid", "AvoidCycle"), null);
    DefaultIssue newIssue3 = newDefaultIssue("SystemPrintln", 14, RuleKey.of("squid", "AvoidCycle"), null);

    IssueTrackingResult result = new IssueTrackingResult();
    tracking.mapIssues(
      Arrays.asList(newIssue1, newIssue2, newIssue3),
      Arrays.asList(referenceIssue1),
      sourceHashHolder, result);

    assertThat(result.matching(newIssue1)).isNull();
    assertThat(result.matching(newIssue2)).isSameAs(referenceIssue1);
    assertThat(result.matching(newIssue3)).isNull();
  }

  @Test
  public void should_track_issues_based_on_blocks_recognition_on_example3() throws Exception {
    when(lastSnapshots.getSource(project)).thenReturn(load("example3-v1"));
    when(index.getSource(project)).thenReturn(load("example3-v2"));
    sourceHashHolder = new SourceHashHolder(index, lastSnapshots, project);

    IssueDto referenceIssue1 = newReferenceIssue("Avoid unused local variables such as 'j'.", 6, "squid", "AvoidCycle", "63c11570fc0a76434156be5f8138fa03");
    IssueDto referenceIssue2 = newReferenceIssue("Avoid unused private methods such as 'myMethod()'.", 13, "squid", "NullDeref", "ef23288705d1ef1e512448ace287586e");
    IssueDto referenceIssue3 = newReferenceIssue("Method 'avoidUtilityClass' is not designed for extension - needs to be abstract, final or empty.", 9, "pmd", "UnusedLocalVariable", "ed5cdd046fda82727d6fedd1d8e3a310");

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

    IssueTrackingResult result = new IssueTrackingResult();
    tracking.mapIssues(
      Arrays.asList(newIssue1, newIssue2, newIssue3, newIssue4, newIssue5),
      Arrays.asList(referenceIssue1, referenceIssue2, referenceIssue3),
      sourceHashHolder, result);

    assertThat(result.matching(newIssue1)).isNull();
    assertThat(result.matching(newIssue2)).isSameAs(referenceIssue2);
    assertThat(result.matching(newIssue3)).isSameAs(referenceIssue3);
    assertThat(result.matching(newIssue4)).isNull();
    assertThat(result.matching(newIssue5)).isSameAs(referenceIssue1);
  }

  private static String load(String name) throws IOException {
    return Resources.toString(IssueTrackingTest.class.getResource("IssueTrackingTest/" + name + ".txt"), Charsets.UTF_8);
  }

  private DefaultIssue newDefaultIssue(String message, Integer line, RuleKey ruleKey, String checksum) {
    return new DefaultIssue().setMessage(message).setLine(line).setRuleKey(ruleKey).setChecksum(checksum).setStatus(Issue.STATUS_OPEN);
  }

  private IssueDto newReferenceIssue(String message, Integer lineId, String ruleRepo, String ruleKey, String lineChecksum) {
    IssueDto referenceIssue = new IssueDto();
    Long id = violationId++;
    referenceIssue.setId(id);
    referenceIssue.setKee(Long.toString(id));
    referenceIssue.setLine(lineId);
    referenceIssue.setMessage(message);
    referenceIssue.setRuleKey_unit_test_only(ruleRepo, ruleKey);
    referenceIssue.setChecksum(lineChecksum);
    referenceIssue.setResolution(null);
    referenceIssue.setStatus(Issue.STATUS_OPEN);
    return referenceIssue;
  }
}
