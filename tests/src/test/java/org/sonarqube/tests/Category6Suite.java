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

import com.sonar.orchestrator.Orchestrator;
import com.sonar.orchestrator.util.NetworkUtils;
import java.net.InetAddress;
import org.junit.ClassRule;
import org.junit.runner.RunWith;
import org.junit.runners.Suite;
import org.sonarqube.tests.authorization.PermissionTemplateTest;
import org.sonarqube.tests.ce.ReportFailureNotificationTest;
import org.sonarqube.tests.issue.IssueNotificationsTest;
import org.sonarqube.tests.issue.IssueTagsTest;
import org.sonarqube.tests.issue.OrganizationIssuesPageTest;
import org.sonarqube.tests.qualityProfile.BuiltInQualityProfilesTest;
import org.sonarqube.tests.qualityProfile.CustomQualityProfilesTest;
import org.sonarqube.tests.qualityProfile.OrganizationQualityProfilesUiTest;
import org.sonarqube.tests.qualityProfile.QualityProfilesEditTest;
import org.sonarqube.tests.qualityProfile.QualityProfilesWsTest;
import org.sonarqube.tests.rule.RulesMarkdownTest;
import org.sonarqube.tests.rule.RulesWsTest;
import org.sonarqube.tests.user.OrganizationIdentityProviderTest;

import static util.ItUtils.pluginArtifact;
import static util.ItUtils.xooPlugin;

/**
 * This category is used only when organizations feature is activated
 *
 * @deprecated use dedicated suites in each package (see {@link org.sonarqube.tests.measure.MeasureSuite}
 * for instance)
 */
@Deprecated
@RunWith(Suite.class)
@Suite.SuiteClasses({
  OrganizationIdentityProviderTest.class,
  OrganizationIssuesPageTest.class,
  OrganizationQualityProfilesUiTest.class,
  BuiltInQualityProfilesTest.class,
  QualityProfilesEditTest.class,
  QualityProfilesWsTest.class,
  CustomQualityProfilesTest.class,
  IssueTagsTest.class,
  RulesWsTest.class,
  RulesMarkdownTest.class,
  PermissionTemplateTest.class,
  ReportFailureNotificationTest.class,
  IssueNotificationsTest.class
})
public class Category6Suite {

  public static final int SEARCH_HTTP_PORT = NetworkUtils.getNextAvailablePort(InetAddress.getLoopbackAddress());

  @ClassRule
  public static final Orchestrator ORCHESTRATOR = Orchestrator.builderEnv()

    // for ES resiliency tests
    .setServerProperty("sonar.search.httpPort", "" + SEARCH_HTTP_PORT)
    .setServerProperty("sonar.search.recovery.delayInMs", "1000")
    .setServerProperty("sonar.search.recovery.minAgeInMs", "3000")
    .setServerProperty("sonar.notifications.delay", "1")

    .addPlugin(xooPlugin())
    .addPlugin(pluginArtifact("base-auth-plugin"))
    .addPlugin(pluginArtifact("ui-extensions-plugin"))

    // reduce memory for Elasticsearch to 128M
    .setServerProperty("sonar.search.javaOpts", "-Xms128m -Xmx128m")

    .build();
}
