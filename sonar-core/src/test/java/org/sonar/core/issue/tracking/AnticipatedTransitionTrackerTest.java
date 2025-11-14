/*
 * SonarQube
 * Copyright (C) 2009-2025 SonarSource Sàrl
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
package org.sonar.core.issue.tracking;

import java.util.List;
import java.util.stream.Collectors;
import org.junit.Test;
import org.sonar.api.rule.RuleKey;
import org.sonar.core.issue.AnticipatedTransition;
import org.sonar.core.issue.DefaultIssue;

import static org.assertj.core.api.Assertions.assertThat;

public class AnticipatedTransitionTrackerTest {

  private final AnticipatedTransitionTracker<DefaultIssue, AnticipatedTransition> underTest = new AnticipatedTransitionTracker<>();

  @Test
  public void givenIssuesAndAnticipatedTransitions_trackerShouldReturnTheExpectedMatching() {

    List<DefaultIssue> issues = getIssues();
    List<AnticipatedTransition> anticipatedTransitions = getAnticipatedTransitions();

    Tracking<DefaultIssue, AnticipatedTransition> tracking = underTest.track(issues, anticipatedTransitions);

    var matchedRaws = tracking.getMatchedRaws();
    var unmatchedRaws = tracking.getUnmatchedRaws().collect(Collectors.toList());

    assertThat(matchedRaws).hasSize(5);
    assertThat(unmatchedRaws).hasSize(2);

    assertThat(matchedRaws.keySet()).containsExactlyInAnyOrder(issues.get(0), issues.get(1), issues.get(2), issues.get(3), issues.get(4));
    assertThat(unmatchedRaws).containsExactlyInAnyOrder(issues.get(5), issues.get(6));

    assertThat(matchedRaws).containsEntry(issues.get(0), anticipatedTransitions.get(1))
      .containsEntry(issues.get(1), anticipatedTransitions.get(0))
      .containsEntry(issues.get(2), anticipatedTransitions.get(3))
      .containsEntry(issues.get(3), anticipatedTransitions.get(2))
      .containsEntry(issues.get(4), anticipatedTransitions.get(6));
  }

  private List<DefaultIssue> getIssues() {
    return List.of(
      getIssue(1, "message1", "hash1", "rule:key1"), //should match transition 2 due to lvl 1 matching
      getIssue(2, "message2", "hash2", "rule:key2"), //should match transition 1 due to lvl 2 matching
      getIssue(3, "message3", "hash3", "rule:key3"), //should match transition 4 due to lvl 3 matching
      getIssue(4, "message4", "hash4", "rule:key4"), //should match transition 3 due to lvl 4 matching
      getIssue(5, "message5", "hash5", "rule:key5"), //should match transition 7 due to lvl 5 matching
      getIssue(6, "message6", "hash6", "rule:key6"), //should not match
      getIssue(7, "message7", "hash7", "rule:key7")  //should not match
    );
  }

  private List<AnticipatedTransition> getAnticipatedTransitions() {
    //Anticipated Transitions with random order
    return List.of(
      getAnticipatedTransition(2, "message a bit different 2", "hash2", "rule:key2"),
      getAnticipatedTransition(1, "message1", "hash1", "rule:key1"),
      getAnticipatedTransition(4, "message4", "different hash", "rule:key4"),
      getAnticipatedTransition(13, "message3", "hash3", "rule:key3"),
      getAnticipatedTransition(16, "different message", "different hash", "rule:key6"),
      getAnticipatedTransition(7, "different message", "different hash", "rule:key17"),
      getAnticipatedTransition(15, "different message", "hash5", "rule:key5")

    );
  }

  private DefaultIssue getIssue(Integer line, String message, String hash, String ruleKey) {
    return new DefaultIssue()
      .setKey("key" + line)
      .setLine(line)
      .setMessage(message)
      .setChecksum(hash)
      .setRuleKey(RuleKey.parse(ruleKey));
  }

  private AnticipatedTransition getAnticipatedTransition(Integer line, String message, String hash, String ruleKey) {
    return new AnticipatedTransition(
      null,
      "projectKey",
      "userUuid",
      RuleKey.parse(ruleKey),
      message,
      "filePath",
      line,
      hash,
      "transition",
      null);
  }

}
