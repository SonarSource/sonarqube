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
package org.sonarqube.tests;

import org.junit.runner.RunWith;
import org.junit.runners.Suite;
import org.sonarqube.tests.analysis.AnalysisEsResilienceTest;
import org.sonarqube.tests.ce.CeShutdownTest;
import org.sonarqube.tests.ce.CeWorkersTest;
import org.sonarqube.tests.issue.IssueCreationDatePluginChangedTest;
import org.sonarqube.tests.marketplace.UpdateCenterTest;
import org.sonarqube.tests.qualityProfile.ActiveRuleEsResilienceTest;
import org.sonarqube.tests.qualityProfile.BuiltInQualityProfilesNotificationTest;
import org.sonarqube.tests.rule.RuleEsResilienceTest;
import org.sonarqube.tests.serverSystem.RestartTest;
import org.sonarqube.tests.serverSystem.ServerSystemRestartingOrchestrator;
import org.sonarqube.tests.serverSystem.SystemStateTest;
import org.sonarqube.tests.settings.SettingsTestRestartingOrchestrator;
import org.sonarqube.tests.startup.StartupIndexationTest;
import org.sonarqube.tests.telemetry.TelemetryOptOutTest;
import org.sonarqube.tests.telemetry.TelemetryUploadTest;
import org.sonarqube.tests.user.OnboardingTest;
import org.sonarqube.tests.user.RealmAuthenticationTest;
import org.sonarqube.tests.user.SsoAuthenticationTest;
import org.sonarqube.tests.user.UserEsResilienceTest;

/**
 * This suite is reserved to the tests that start their own instance of Orchestrator.
 * Indeed multiple instances of Orchestrator can't be started in parallel, so this
 * suite does not declare a shared Orchestrator.
 *
 * @deprecated use dedicated suites in each package (see {@link org.sonarqube.tests.measure.MeasureSuite}
 * for instance)
 */
@Deprecated
@RunWith(Suite.class)
@Suite.SuiteClasses({
  ServerSystemRestartingOrchestrator.class,
  RestartTest.class,
  SettingsTestRestartingOrchestrator.class,
  SystemStateTest.class,
  // update center
  UpdateCenterTest.class,
  RealmAuthenticationTest.class,
  SsoAuthenticationTest.class,
  OnboardingTest.class,
  BuiltInQualityProfilesNotificationTest.class,
  ActiveRuleEsResilienceTest.class,
  AnalysisEsResilienceTest.class,
  RuleEsResilienceTest.class,
  UserEsResilienceTest.class,
  TelemetryUploadTest.class,
  TelemetryOptOutTest.class,
  // ce
  CeShutdownTest.class,
  CeWorkersTest.class,
  // issues
  IssueCreationDatePluginChangedTest.class,

  // elasticsearch
  StartupIndexationTest.class
})
public class Category5Suite {

}
