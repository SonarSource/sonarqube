/*
 * SonarQube
 * Copyright (C) 2009-2017 SonarSource SA
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
package org.sonarqube.tests;

import com.sonar.orchestrator.Orchestrator;
import org.sonarqube.tests.issue.AutoAssignTest;
import org.sonarqube.tests.issue.CommonRulesTest;
import org.sonarqube.tests.issue.CustomRulesTest;
import org.sonarqube.tests.issue.IssueActionTest;
import org.sonarqube.tests.issue.IssueBulkChangeTest;
import org.sonarqube.tests.issue.IssueChangelogTest;
import org.sonarqube.tests.issue.IssueCreationTest;
import org.sonarqube.tests.issue.IssueFilterExtensionTest;
import org.sonarqube.tests.issue.IssueFilterOnCommonRulesTest;
import org.sonarqube.tests.issue.IssueFilterTest;
import org.sonarqube.tests.issue.IssueMeasureTest;
import org.sonarqube.tests.issue.IssueNotificationsTest;
import org.sonarqube.tests.issue.IssuePurgeTest;
import org.sonarqube.tests.issue.IssueSearchTest;
import org.sonarqube.tests.issue.IssueTrackingTest;
import org.sonarqube.tests.issue.IssueWorkflowTest;
import org.sonarqube.tests.issue.IssuesPageTest;
import org.sonarqube.tests.issue.NewIssuesMeasureTest;
import org.sonarqube.tests.qualityModel.MaintainabilityMeasureTest;
import org.sonarqube.tests.qualityModel.MaintainabilityRatingMeasureTest;
import org.sonarqube.tests.qualityModel.NewDebtRatioMeasureTest;
import org.sonarqube.tests.qualityModel.ReliabilityMeasureTest;
import org.sonarqube.tests.qualityModel.SecurityMeasureTest;
import org.sonarqube.tests.qualityModel.TechnicalDebtInIssueChangelogTest;
import org.sonarqube.tests.qualityModel.TechnicalDebtMeasureVariationTest;
import org.sonarqube.tests.qualityModel.TechnicalDebtTest;
import org.sonarqube.tests.scm.ScmTest;
import org.sonarqube.tests.test.CoverageTest;
import org.sonarqube.tests.test.CoverageTrackingTest;
import org.sonarqube.tests.test.NewCoverageTest;
import org.sonarqube.tests.test.TestExecutionTest;
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
  // ui
  IssuesPageTest.class
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
