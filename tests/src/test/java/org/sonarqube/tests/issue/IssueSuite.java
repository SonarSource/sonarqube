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
package org.sonarqube.tests.issue;

import com.sonar.orchestrator.Orchestrator;
import org.junit.ClassRule;
import org.junit.runner.RunWith;
import org.junit.runners.Suite;

import static util.ItUtils.pluginArtifact;
import static util.ItUtils.xooPlugin;

@RunWith(Suite.class)
@Suite.SuiteClasses({
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
  IssuePurgeTest.class,
  IssueSearchTest.class,
  IssueTrackingTest.class,
  IssueWorkflowTest.class,
  NewIssuesMeasureTest.class,
  IssueCreationDateQPChangedTest.class,
  IssuesPageTest.class
})
public class IssueSuite {

  @ClassRule
  public static final Orchestrator ORCHESTRATOR = Orchestrator.builderEnv()
    .setServerProperty("sonar.search.httpPort", "9025")
    .addPlugin(xooPlugin())

    // issue
    .addPlugin(pluginArtifact("issue-filter-plugin"))

    // 1 second. Required for notification test.
    .setServerProperty("sonar.notifications.delay", "1")

    .setServerProperty("organization.enabled", "true")

    // reduce memory for Elasticsearch to 128M
    .setServerProperty("sonar.search.javaOpts", "-Xms128m -Xmx128m")

    .build();

}
