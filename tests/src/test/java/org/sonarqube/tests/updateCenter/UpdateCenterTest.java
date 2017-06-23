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
package org.sonarqube.tests.updateCenter;

import com.sonar.orchestrator.Orchestrator;
import org.junit.After;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import util.user.UserRule;

import static util.ItUtils.pluginArtifact;
import static util.selenium.Selenese.runSelenese;

/**
 * This class start its own orchestrator
 */
public class UpdateCenterTest {

  @ClassRule
  public static final Orchestrator orchestrator = Orchestrator.builderEnv()
    .setServerProperty("sonar.updatecenter.url", UpdateCenterTest.class.getResource("/updateCenter/UpdateCenterTest/update-center.properties").toString())
    .addPlugin(pluginArtifact("sonar-fake-plugin"))
    .build();
  @Rule
  public UserRule userRule = UserRule.from(orchestrator);

  @After
  public void tearDown() throws Exception {
    userRule.resetUsers();
  }

  @Test
  public void test_console() {
    runSelenese(orchestrator, "/updateCenter/installed-plugins.html");
  }

}
