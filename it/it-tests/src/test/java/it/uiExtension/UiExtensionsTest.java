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
package it.uiExtension;

import com.sonar.orchestrator.Orchestrator;
import it.Category4Suite;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Ignore;
import org.junit.Test;

import static util.selenium.Selenese.runSelenese;


public class UiExtensionsTest {

  @ClassRule
  public static Orchestrator orchestrator = Category4Suite.ORCHESTRATOR;

  @BeforeClass
  public static void setUp() throws Exception {
    orchestrator.resetData();
    orchestrator.getServer().provisionProject("sample", "Sample");
  }

  @Test
  public void test_static_files() {
    runSelenese(orchestrator, "/uiExtension/UiExtensionsTest/static-files.html");
  }

  /**
   * SONAR-2376
   */
  @Test
  @Ignore("page extensions are not reimplemented yet")
  public void test_page_decoration() {
    runSelenese(orchestrator, "/uiExtension/UiExtensionsTest/page-decoration.html");
  }

  /**
   * SONAR-4173
   */
  @Test
  @Ignore("page extensions are not reimplemented yet")
  public void test_resource_configuration_extension() {
    runSelenese(orchestrator, "/uiExtension/UiExtensionsTest/resource-configuration-extension.html");
  }

}
