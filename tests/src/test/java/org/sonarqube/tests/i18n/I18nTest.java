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
package org.sonarqube.tests.i18n;

import com.sonar.orchestrator.Orchestrator;
import com.sonar.orchestrator.build.SonarScanner;
import org.sonarqube.tests.Category1Suite;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Ignore;
import org.junit.Test;

import static util.ItUtils.projectDir;
import static util.selenium.Selenese.runSelenese;

public class I18nTest {

  @ClassRule
  public static Orchestrator orchestrator = Category1Suite.ORCHESTRATOR;

  @Before
  public void cleanDatabase() {
    orchestrator.resetData();
  }

  /**
   * TODO This test should use a fake widget that display a fake metric with decimals instead of using provided metric
   * Ignored because there is not a good idea to force a display language by GET parameter
   * The displayed language is based on browser/system locale
   */
  @Test
  @Ignore
  public void test_localization() {
    orchestrator.executeBuild(SonarScanner.create(projectDir("shared/xoo-sample")));

    runSelenese(orchestrator,
      "/i18n/default-locale-is-english.html",
      "/i18n/french-locale.html",
      "/i18n/french-pack.html",
      "/i18n/locale-with-france-country.html",
      "/i18n/locale-with-swiss-country.html");
  }

}
