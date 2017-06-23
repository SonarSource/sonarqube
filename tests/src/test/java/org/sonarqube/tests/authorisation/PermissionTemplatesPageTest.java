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
package org.sonarqube.tests.authorisation;

import com.sonar.orchestrator.Orchestrator;
import org.sonarqube.tests.Category1Suite;
import org.junit.ClassRule;
import org.junit.Test;

import static util.selenium.Selenese.runSelenese;

public class PermissionTemplatesPageTest {

  @ClassRule
  public static Orchestrator orchestrator = Category1Suite.ORCHESTRATOR;

  @Test
  public void should_display_page() throws Exception {
    runSelenese(orchestrator,
      "/authorisation/PermissionTemplatesPageTest/should_display_page.html",
      "/authorisation/PermissionTemplatesPageTest/should_create.html");
  }

  @Test
  public void should_manage_project_creators() throws Exception {
    runSelenese(orchestrator, "/authorisation/PermissionTemplatesPageTest/should_manage_project_creators.html");
  }
}
