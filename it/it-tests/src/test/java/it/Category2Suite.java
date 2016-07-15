/*
 * SonarQube
 * Copyright (C) 2009-2016 SonarSource SA
 * mailto:contact AT sonarsource DOT com
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
package it;

import com.sonar.orchestrator.Orchestrator;
import it.issue.AutoAssignTest;
import it.issue.CommonRulesTest;
import it.issue.CustomRulesTest;
import it.issue.IssueActionTest;
import it.issue.IssueBulkChangeTest;
import it.issue.IssueChangelogTest;
import it.issue.IssueCreationTest;
import it.issue.IssueFilterExtensionTest;
import it.issue.IssueFilterOnCommonRulesTest;
import it.issue.IssueFilterTest;
import it.issue.IssueMeasureTest;
import it.issue.IssueNotificationsTest;
import it.issue.IssuePurgeTest;
import it.issue.IssueSearchTest;
import it.issue.IssueTrackingTest;
import it.issue.IssueWorkflowTest;
import it.issue.IssuesPageTest;
import it.issue.NewIssuesMeasureTest;
import it.qualityModel.MaintainabilityMeasureTest;
import it.qualityModel.MaintainabilityRatingMeasureTest;
import it.qualityModel.NewDebtRatioMeasureTest;
import it.qualityModel.ReliabilityMeasureTest;
import it.qualityModel.SecurityMeasureTest;
import it.qualityModel.TechnicalDebtInIssueChangelogTest;
import it.qualityModel.TechnicalDebtMeasureVariationTest;
import it.qualityModel.TechnicalDebtTest;
import it.qualityModel.TechnicalDebtWidgetTest;
import it.scm.ScmTest;
import it.test.CoverageTest;
import it.test.CoverageTrackingTest;
import it.test.NewCoverageTest;
import it.test.TestExecutionTest;
import org.junit.ClassRule;
import org.junit.runner.RunWith;
import org.junit.runners.Suite;

import static util.ItUtils.pluginArtifact;
import static util.ItUtils.xooPlugin;

@RunWith(Suite.class)
@Suite.SuiteClasses({
  // test
  CoverageTrackingTest.class,
  CoverageTest.class,
  NewCoverageTest.class,
  TestExecutionTest.class,
  // scm
  ScmTest.class,
  // issue
  AutoAssignTest.class,
  CommonRulesTest.class,
  CustomRulesTest.class,
  IssueActionTest.class,
  IssueBulkChangeTest.class,
  IssueChangelogTest.class,
  IssueCreationTest.class,
  IssueFilterOnCommonRulesTest.class,
  IssueFilterTest.class,
  IssueFilterExtensionTest.class,
  IssueMeasureTest.class,
  IssueNotificationsTest.class,
  IssuePurgeTest.class,
  IssueSearchTest.class,
  IssueTrackingTest.class,
  IssueWorkflowTest.class,
  IssuesPageTest.class,
  NewIssuesMeasureTest.class,
  // debt
  MaintainabilityMeasureTest.class,
  MaintainabilityRatingMeasureTest.class,
  NewDebtRatioMeasureTest.class,
  ReliabilityMeasureTest.class,
  SecurityMeasureTest.class,
  TechnicalDebtInIssueChangelogTest.class,
  TechnicalDebtMeasureVariationTest.class,
  TechnicalDebtTest.class,
  TechnicalDebtWidgetTest.class
})
public class Category2Suite {

  @ClassRule
  public static final Orchestrator ORCHESTRATOR = Orchestrator.builderEnv()
    .addPlugin(xooPlugin())

    // issue
    .addPlugin(pluginArtifact("issue-filter-plugin"))

    // 1 second. Required for notification test.
    .setServerProperty("sonar.notifications.delay", "1")

    .build();

}
