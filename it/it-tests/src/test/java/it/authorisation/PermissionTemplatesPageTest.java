/*
 * SonarQube
 * Copyright (C) 2009-2016 SonarSource SA
 * mailto:contact AT sonarsource DOT com
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
package it.authorisation;

import com.sonar.orchestrator.Orchestrator;
import com.sonar.orchestrator.selenium.Selenese;
import it.Category1Suite;
import org.junit.ClassRule;
import org.junit.Test;
import util.selenium.SeleneseTest;

public class PermissionTemplatesPageTest {

  @ClassRule
  public static Orchestrator orchestrator = Category1Suite.ORCHESTRATOR;

  @Test
  public void should_display_page() throws Exception {
    Selenese selenese = Selenese.builder().setHtmlTestsInClasspath("should_display_page",
      "/authorisation/PermissionTemplatesPageTest/should_display_page.html",
      "/authorisation/PermissionTemplatesPageTest/should_create.html"
    ).build();
    new SeleneseTest(selenese).runOn(orchestrator);
  }

  @Test
  public void should_manage_project_creators() throws Exception {
    Selenese selenese = Selenese.builder().setHtmlTestsInClasspath("should_manage_project_creators",
      "/authorisation/PermissionTemplatesPageTest/should_manage_project_creators.html"
    ).build();
    new SeleneseTest(selenese).runOn(orchestrator);
  }
}
