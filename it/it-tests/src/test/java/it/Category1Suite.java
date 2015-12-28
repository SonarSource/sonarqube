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
import it.actionPlan.ActionPlanTest;
import it.actionPlan.ActionPlanUiTest;
import it.administration.UsersUITest;
import it.authorisation.AuthenticationTest;
import it.authorisation.IssuePermissionTest;
import it.authorisation.PermissionTest;
import it.authorisation.ScanPermissionTest;
import it.i18n.I18nTest;
import it.measureHistory.DifferentialPeriodsTest;
import it.measureHistory.HistoryUiTest;
import it.measureHistory.SincePreviousVersionHistoryTest;
import it.measureHistory.SinceXDaysHistoryTest;
import it.measureHistory.TimeMachineTest;
import it.projectAdministration.BackgroundTasksTest;
import it.projectAdministration.BulkDeletionTest;
import it.projectAdministration.ProjectAdministrationTest;
import it.projectServices.ProjectCodeTest;
import it.projectServices.ProjectOverviewTest;
import it.qualityGate.QualityGateNotificationTest;
import it.qualityGate.QualityGateTest;
import it.qualityGate.QualityGateUiTest;
import it.settings.PropertySetsTest;
import it.settings.SettingsTest;
import it.settings.SettingsTestRestartingOrchestrator;
import it.settings.SubCategoriesTest;
import org.junit.ClassRule;
import org.junit.runner.RunWith;
import org.junit.runners.Suite;

import static util.ItUtils.pluginArtifact;
import static util.ItUtils.xooPlugin;

@RunWith(Suite.class)
@Suite.SuiteClasses({
  // administration
  UsersUITest.class,
  // project administration
  BulkDeletionTest.class,
  ProjectAdministrationTest.class,
  BackgroundTasksTest.class,
  // project pages
  ProjectOverviewTest.class,
  ProjectCodeTest.class,
  // settings
  PropertySetsTest.class,
  SubCategoriesTest.class,
  SettingsTest.class,
  SettingsTestRestartingOrchestrator.class,
  // i18n
  I18nTest.class,
  // quality gate
  QualityGateTest.class,
  QualityGateUiTest.class,
  QualityGateNotificationTest.class,
  // permission
  AuthenticationTest.class,
  PermissionTest.class,
  IssuePermissionTest.class,
  ScanPermissionTest.class,
  // measure history
  DifferentialPeriodsTest.class,
  HistoryUiTest.class,
  SincePreviousVersionHistoryTest.class,
  SinceXDaysHistoryTest.class,
  TimeMachineTest.class,
  // action plan
  ActionPlanTest.class,
  ActionPlanUiTest.class
})
public class Category1Suite {

  @ClassRule
  public static final Orchestrator ORCHESTRATOR = Orchestrator.builderEnv()
    .setServerProperty("sonar.notifications.delay", "1")
    .addPlugin(pluginArtifact("property-sets-plugin"))
    .addPlugin(pluginArtifact("sonar-subcategories-plugin"))

    // Used in I18nTest
    .addPlugin(pluginArtifact("l10n-fr-pack"))

    // 1 second. Required for notification test.
    .setServerProperty("sonar.notifications.delay", "1")

    // Used in SettingsTest.global_property_change_extension_point
    .addPlugin(pluginArtifact("global-property-change-plugin"))

    // Used in SettingsTest.should_get_settings_default_value
    .addPlugin(pluginArtifact("server-plugin"))

    .addPlugin(xooPlugin())
    .build();

}
