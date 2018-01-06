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
package org.sonarqube.tests.project;

import com.sonar.orchestrator.Orchestrator;
import com.sonar.orchestrator.util.NetworkUtils;
import java.net.InetAddress;
import org.junit.ClassRule;
import org.junit.runner.RunWith;
import org.junit.runners.Suite;

import static util.ItUtils.pluginArtifact;
import static util.ItUtils.xooPlugin;

@RunWith(Suite.class)
@Suite.SuiteClasses({
  ProjectBulkDeletionTest.class,
  ProjectBulkDeletionPageTest.class,
  ProjectDeletionTest.class,
  ProjectFilterTest.class,
  ProjectKeyUpdateTest.class,
  ProjectKeyUpdatePageTest.class,
  ProjectLeakPageTest.class,
  ProjectLinksTest.class,
  ProjectListTest.class,
  ProjectsPageTest.class,
  ProjectProvisioningTest.class,
  ProjectSearchTest.class,
  ProjectSettingsTest.class,
  ProjectVisibilityPageTest.class
})
public class ProjectSuite {
  static final int SEARCH_HTTP_PORT = NetworkUtils.getNextAvailablePort(InetAddress.getLoopbackAddress());

  @ClassRule
  public static final Orchestrator ORCHESTRATOR = Orchestrator.builderEnv()
    // reduce memory for Elasticsearch
    .setServerProperty("sonar.search.javaOpts", "-Xms128m -Xmx128m")
    // for ES resiliency tests
    .setServerProperty("sonar.search.httpPort", "" + SEARCH_HTTP_PORT)
    .setServerProperty("sonar.search.recovery.delayInMs", "1000")
    .setServerProperty("sonar.search.recovery.minAgeInMs", "3000")
    .setServerProperty("sonar.notifications.delay", "1")

    .addPlugin(xooPlugin())

    // for ProjectSettingsTest
    .addPlugin(pluginArtifact("sonar-subcategories-plugin"))

    .build();

}
