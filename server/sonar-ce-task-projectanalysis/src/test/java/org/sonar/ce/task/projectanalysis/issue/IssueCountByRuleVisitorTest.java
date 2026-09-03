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
package org.sonar.ce.task.projectanalysis.issue;

import java.util.Map;
import javax.annotation.Nullable;
import org.junit.jupiter.api.Test;
import org.sonar.api.rule.RuleKey;
import org.sonar.ce.task.projectanalysis.component.Component;
import org.sonar.core.issue.DefaultIssue;
import org.sonar.core.rule.RuleType;
import org.sonar.db.rule.RuleTesting;

import static org.assertj.core.api.Assertions.assertThat;
import static org.sonar.api.issue.Issue.RESOLUTION_FALSE_POSITIVE;
import static org.sonar.api.issue.Issue.RESOLUTION_FIXED;
import static org.sonar.api.issue.Issue.RESOLUTION_WONT_FIX;
import static org.sonar.api.issue.Issue.STATUS_CLOSED;
import static org.sonar.api.issue.Issue.STATUS_CONFIRMED;
import static org.sonar.api.issue.Issue.STATUS_IN_SANDBOX;
import static org.sonar.api.issue.Issue.STATUS_OPEN;
import static org.sonar.api.issue.Issue.STATUS_REOPENED;
import static org.sonar.api.issue.Issue.STATUS_RESOLVED;
import static org.sonar.api.issue.Issue.STATUS_TO_REVIEW;
import static org.sonar.ce.task.projectanalysis.component.ReportComponent.builder;
import static org.sonar.core.rule.RuleType.SECURITY_HOTSPOT;

class IssueCountByRuleVisitorTest {

  private static final Component FILE = builder(Component.Type.FILE, 1).build();

  private final IssueCountsByRuleHolderImpl holder = new IssueCountsByRuleHolderImpl();
  private final IssueCountByRuleVisitor underTest = new IssueCountByRuleVisitor(holder);
  private static int issueCounter;

  @Test
  void onIssue_shouldIncrementMatchingBucket_forEachTrackedStatus() {
    underTest.onIssue(FILE, createIssue(RuleTesting.XOO_X1, null, STATUS_OPEN));
    underTest.onIssue(FILE, createIssue(RuleTesting.XOO_X1, null, STATUS_REOPENED));
    underTest.onIssue(FILE, createIssue(RuleTesting.XOO_X1, null, STATUS_CONFIRMED));
    underTest.onIssue(FILE, createIssue(RuleTesting.XOO_X1, null, STATUS_IN_SANDBOX));
    underTest.onIssue(FILE, createIssue(RuleTesting.XOO_X1, RESOLUTION_FALSE_POSITIVE, STATUS_RESOLVED));
    underTest.onIssue(FILE, createIssue(RuleTesting.XOO_X1, RESOLUTION_WONT_FIX, STATUS_RESOLVED));

    IssueCounts counts = holder.getCounts().get(RuleTesting.XOO_X1);
    assertThat(counts.open()).isEqualTo(2);
    assertThat(counts.confirmed()).isEqualTo(1);
    assertThat(counts.inSandbox()).isEqualTo(1);
    assertThat(counts.falsePositive()).isEqualTo(1);
    assertThat(counts.accepted()).isEqualTo(1);
  }

  @Test
  void onIssue_shouldExcludeFixedAndHotspotIssues() {
    underTest.onIssue(FILE, createIssue(RuleTesting.XOO_X1, RESOLUTION_FIXED, STATUS_CLOSED));
    underTest.onIssue(FILE, createHotspot(RuleTesting.XOO_X1));

    assertThat(holder.getCounts()).isEmpty();
  }

  @Test
  void onIssue_shouldIncrementRatherThanOverwrite_forRepeatedIssuesOnSameRuleAndBucket() {
    underTest.onIssue(FILE, createIssue(RuleTesting.XOO_X1, null, STATUS_OPEN));
    underTest.onIssue(FILE, createIssue(RuleTesting.XOO_X1, null, STATUS_OPEN));

    assertThat(holder.getCounts().get(RuleTesting.XOO_X1).open()).isEqualTo(2);
  }

  @Test
  void onIssue_shouldTallySeparatelyPerRule() {
    underTest.onIssue(FILE, createIssue(RuleTesting.XOO_X1, null, STATUS_OPEN));
    underTest.onIssue(FILE, createIssue(RuleTesting.XOO_X2, null, STATUS_OPEN));
    underTest.onIssue(FILE, createIssue(RuleTesting.XOO_X2, null, STATUS_OPEN));

    Map<RuleKey, IssueCounts> counts = holder.getCounts();
    assertThat(counts.get(RuleTesting.XOO_X1).open()).isEqualTo(1);
    assertThat(counts.get(RuleTesting.XOO_X2).open()).isEqualTo(2);
  }

  private static DefaultIssue createIssue(RuleKey ruleKey, @Nullable String resolution, String status) {
    return createIssue(ruleKey, resolution, status, RuleType.CODE_SMELL);
  }

  private static DefaultIssue createHotspot(RuleKey ruleKey) {
    return createIssue(ruleKey, null, STATUS_TO_REVIEW, SECURITY_HOTSPOT);
  }

  private static DefaultIssue createIssue(RuleKey ruleKey, @Nullable String resolution, String status, RuleType type) {
    return new DefaultIssue()
      .setKey(String.valueOf(++issueCounter))
      .setRuleKey(ruleKey)
      .setResolution(resolution)
      .setStatus(status)
      .setType(type);
  }
}
