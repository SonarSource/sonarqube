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
package org.sonar.ce.task.projectanalysis.issue.fixedissues;

import java.time.Instant;
import java.util.Date;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.sonar.api.issue.impact.Severity;
import org.sonar.api.issue.impact.SoftwareQuality;
import org.sonar.ce.task.projectanalysis.component.Component;
import org.sonar.core.issue.DefaultIssue;
import org.sonar.core.issue.FieldDiffs;
import org.sonar.core.rule.RuleType;
import org.sonarsource.history.model.FixedIssueForHistory;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

class FixedIssueVisitorTest {
  private static final Date CREATION_DATE = Date.from(Instant.parse("2026-01-05T00:00:00.00Z"));
  private static final Date DETECTION_DATE = Date.from(Instant.parse("2026-03-05T00:00:00.00Z"));
  private static final Date CLOSE_DATE = Date.from(Instant.parse("2026-08-05T00:00:00.00Z"));

  private final FixedIssueForHistoryRepository fixedIssueForHistoryRepository = new FixedIssueForHistoryRepository();
  private final FixedIssueVisitor underTest = new FixedIssueVisitor(fixedIssueForHistoryRepository);

  private final Component component = mock();

  @Test
  void onIssue_whenIssueIsBeingClosed_shouldRecordIssueInRepository() {
    var issue = getIssue()
      .setCreationDate(CREATION_DATE)
      .setBeingClosed(true);
    var expected = new FixedIssueForHistory(
      issue.key(),
      issue.projectUuid(),
      CREATION_DATE.getTime(),
      DETECTION_DATE.getTime(),
      CLOSE_DATE.getTime(),
      issue.type().name(),
      issue.getBranchUuid(),
      issue.severity(),
      Map.of(
        SoftwareQuality.MAINTAINABILITY.name(), Severity.HIGH.name(),
        SoftwareQuality.SECURITY.name(), Severity.MEDIUM.name()
      ),
      "OPEN"
    );

    underTest.onIssue(component, issue);

    assertThat(fixedIssueForHistoryRepository.getFixedIssues())
      .containsExactly(expected);
  }

  @Test
  void onIssue_whenIssueHasImpacts_shouldRecordImpactsInHistory() {
    var issue = getIssue()
      .setDefaultRuleImpacts(Map.of())
      .setOverriddenImpacts(Map.of())
      .addImpact(SoftwareQuality.SECURITY, Severity.BLOCKER)
      .setCreationDate(CREATION_DATE)
      .setBeingClosed(true);

    underTest.onIssue(component, issue);

    assertThat(fixedIssueForHistoryRepository.getFixedIssues())
      .singleElement()
      .satisfies(fixedIssue -> assertThat(fixedIssue.impacts())
        .containsExactlyEntriesOf(Map.of(SoftwareQuality.SECURITY.name(), Severity.BLOCKER.name())));
  }

  @Test
  void onIssue_whenIssueIsBeingClosed_shouldSkipIssueWithNullCreationDate() {
    var issue = getIssue()
      .setCreationDate(null)
      .setBeingClosed(true);

    underTest.onIssue(component, issue);

    assertThat(fixedIssueForHistoryRepository.getFixedIssues())
      .isEmpty();
  }

  @Test
  void onIssue_whenIssueIsNotBeingClosed_shouldNotRecordIssueInRepository() {
    var issue = getIssue()
      .setCreationDate(CREATION_DATE)
      .setBeingClosed(false);

    underTest.onIssue(component, issue);

    assertThat(fixedIssueForHistoryRepository.getFixedIssues())
      .isEmpty();
  }

  private DefaultIssue getIssue() {
    return new DefaultIssue()
      .setKey("kee")
      .setProjectUuid("project-uuid")
      .setDetectionDate(DETECTION_DATE)
      .setCloseDate(CLOSE_DATE)
      .setType(RuleType.BUG)
      .setSeverity("MAJOR")
      .setBranchUuid("branch-uuid")
      .addChange(new FieldDiffs().setDiff("status", "OPEN", "CLOSED"))
      .setDefaultRuleImpacts(Map.of(SoftwareQuality.MAINTAINABILITY, Severity.HIGH, SoftwareQuality.SECURITY, Severity.LOW))
      .setOverriddenImpacts(Map.of(SoftwareQuality.SECURITY, Severity.MEDIUM));
  }
}
