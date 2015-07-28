/*
 * SonarQube, open source software quality management tool.
 * Copyright (C) 2008-2014 SonarSource
 * mailto:contact AT sonarsource DOT com
 *
 * SonarQube is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * SonarQube is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */

package administration.suite.ui;

import administration.suite.AdministrationTestSuite;
import com.sonar.orchestrator.Orchestrator;
import com.sonar.orchestrator.build.SonarRunner;
import com.sonar.orchestrator.selenium.Selenese;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Test;

import static util.ItUtils.projectDir;

public class I18nTest {

  @ClassRule
  public static Orchestrator orchestrator = AdministrationTestSuite.ORCHESTRATOR;

  @Before
  public void cleanDatabase() {
    orchestrator.resetData();
  }

  /**
   * TODO This test should use a fake widget that display a fake metric with decimals instead of using provided metric
   */
  @Test
  public void test_localization() {
    orchestrator.executeBuild(SonarRunner.create(projectDir("shared/xoo-sample")));

    Selenese selenese = Selenese.builder().setHtmlTestsInClasspath("ui-i18n",
      "/ui/i18n/default-locale-is-english.html",
      "/ui/i18n/french-locale.html",
      "/ui/i18n/french-pack.html",
      "/ui/i18n/locale-with-france-country.html",
      "/ui/i18n/locale-with-swiss-country.html").build();
    orchestrator.executeSelenese(selenese);
  }

}
