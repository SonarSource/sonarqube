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
package org.sonarqube.tests.analysis;

import com.sonar.orchestrator.Orchestrator;
import com.sonar.orchestrator.build.MavenBuild;
import org.sonarqube.tests.Category3Suite;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Test;
import util.ItUtils;

public class ExtensionLifecycleTest {

  @ClassRule
  public static Orchestrator orchestrator = Category3Suite.ORCHESTRATOR;

  @Before
  public void cleanup() {
    orchestrator.resetData();
  }

  @Test
  public void testInstantiationStrategyAndLifecycleOfBatchExtensions() {
    MavenBuild build = MavenBuild.create(ItUtils.projectPom("analysis/extension-lifecycle"))
      .setCleanSonarGoals()
      .setProperty("extension.lifecycle", "true");

    // Build fails if the extensions provided in the extension-lifecycle-plugin are not correctly
    // managed.
    orchestrator.executeBuild(build);
  }
}
