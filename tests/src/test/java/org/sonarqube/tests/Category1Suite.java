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
import org.junit.ClassRule;
import org.junit.runner.RunWith;
import org.junit.runners.Suite;
import org.sonarqube.tests.ce.BackgroundTasksTest;
import org.sonarqube.tests.settings.DeprecatedPropertiesWsTest;
import org.sonarqube.tests.settings.EmailsTest;
import org.sonarqube.tests.settings.PropertySetsTest;
import org.sonarqube.tests.settings.SettingsTest;

import static util.ItUtils.pluginArtifact;
import static util.ItUtils.xooPlugin;

/**
 * @deprecated use dedicated suites in each package (see {@link org.sonarqube.tests.measure.MeasureSuite}
 * for instance)
 */
@Deprecated
@RunWith(Suite.class)
@Suite.SuiteClasses({
  BackgroundTasksTest.class,
  DeprecatedPropertiesWsTest.class,
  EmailsTest.class,
  PropertySetsTest.class,
  SettingsTest.class
})
public class Category1Suite {

  @ClassRule
  public static final Orchestrator ORCHESTRATOR = Orchestrator.builderEnv()
    .setServerProperty("sonar.notifications.delay", "1")
    .addPlugin(pluginArtifact("property-sets-plugin"))
    .addPlugin(pluginArtifact("sonar-subcategories-plugin"))

    // Used in SettingsTest.global_property_change_extension_point
    .addPlugin(pluginArtifact("global-property-change-plugin"))

    // Used in SettingsTest.should_get_settings_default_value
    .addPlugin(pluginArtifact("server-plugin"))

    // Used in I18nTest
    .addPlugin(pluginArtifact("l10n-fr-pack"))

    // 1 second. Required for notification test.
    .setServerProperty("sonar.notifications.delay", "1")

    .addPlugin(pluginArtifact("posttask-plugin"))

    // reduce memory for Elasticsearch to 128M
    .setServerProperty("sonar.search.javaOpts", "-Xms128m -Xmx128m")

    .addPlugin(xooPlugin())
    .build();

}
