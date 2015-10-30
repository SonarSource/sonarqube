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

package it;/*
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

import com.sonar.orchestrator.Orchestrator;
import it.customMeasure.CustomMeasuresTest;
import it.debt.SqaleRatingMeasureTest;
import it.debt.TechnicalDebtInIssueChangelogTest;
import it.debt.TechnicalDebtMeasureTest;
import it.debt.TechnicalDebtPurgeTest;
import it.debt.TechnicalDebtTest;
import it.debt.TechnicalDebtWidgetTest;
import it.issue.CommonRulesTest;
import it.issue.CustomRulesTest;
import it.issue.IssueActionTest;
import it.issue.IssueBulkChangeTest;
import it.issue.IssueChangelogTest;
import it.issue.IssueFilterExtensionTest;
import it.issue.IssuePurgeTest;
import it.issue.IssueWorkflowTest;
import it.issue.ManualRulesTest;
import it.issue.NewIssuesMeasureTest;
import it.measure.NewDebtRatioMeasureTest;
import it.measure.TechnicalDebtMeasureVariationTest;
import it.measureFilter.MeasureFiltersTest;
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
  // custom measure
  CustomMeasuresTest.class,
  // measure
  TechnicalDebtMeasureVariationTest.class,
  NewDebtRatioMeasureTest.class,
  MeasureFiltersTest.class,
  // test
  CoverageTrackingTest.class,
  CoverageTest.class,
  NewCoverageTest.class,
  TestExecutionTest.class,
  // issue
  CommonRulesTest.class,
  CustomRulesTest.class,
  IssueActionTest.class,
  IssueBulkChangeTest.class,
  IssueChangelogTest.class,
  IssueFilterExtensionTest.class,
  IssuePurgeTest.class,
  IssueWorkflowTest.class,
  ManualRulesTest.class,
  NewIssuesMeasureTest.class,
  // debt
  SqaleRatingMeasureTest.class,
  TechnicalDebtInIssueChangelogTest.class,
  TechnicalDebtMeasureTest.class,
  TechnicalDebtPurgeTest.class,
  TechnicalDebtTest.class,
  TechnicalDebtWidgetTest.class
})
public class Category2Suite {

  @ClassRule
  public static final Orchestrator ORCHESTRATOR = Orchestrator.builderEnv()
    .addPlugin(xooPlugin())

    // issue
    .addPlugin(pluginArtifact("issue-action-plugin"))
    .addPlugin(pluginArtifact("issue-filter-plugin"))

    .build();

}
