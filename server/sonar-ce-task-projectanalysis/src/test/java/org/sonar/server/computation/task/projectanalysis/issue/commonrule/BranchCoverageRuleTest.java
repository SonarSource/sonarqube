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
package org.sonar.server.computation.task.projectanalysis.issue.commonrule;

import org.sonar.api.measures.CoreMetrics;
import org.sonar.api.rule.RuleKey;
import org.sonar.server.rule.CommonRuleKeys;

public class BranchCoverageRuleTest extends CoverageRuleTest {

  @Override
  protected CommonRule createRule() {
    return new BranchCoverageRule(activeRuleHolder, metricRepository, measureRepository);
  }

  @Override
  protected RuleKey getRuleKey() {
    return RuleKey.of("common-java", CommonRuleKeys.INSUFFICIENT_BRANCH_COVERAGE);
  }

  @Override
  protected String getMinPropertyKey() {
    return CommonRuleKeys.INSUFFICIENT_BRANCH_COVERAGE_PROPERTY;
  }

  @Override
  protected String getCoverageMetricKey() {
    return CoreMetrics.BRANCH_COVERAGE_KEY;
  }

  @Override
  protected String getToCoverMetricKey() {
    return CoreMetrics.CONDITIONS_TO_COVER_KEY;
  }

  @Override
  protected String getUncoveredMetricKey() {
    return CoreMetrics.UNCOVERED_CONDITIONS_KEY;
  }

  @Override
  protected String getExpectedIssueMessage() {
    return "23 more branches need to be covered by tests to reach the minimum threshold of 65.0% branch coverage.";
  }
}
