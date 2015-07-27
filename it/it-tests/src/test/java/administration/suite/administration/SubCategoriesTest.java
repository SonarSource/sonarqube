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
package administration.suite.administration;

import administration.suite.AdministrationTestSuite;
import com.sonar.orchestrator.Orchestrator;
import com.sonar.orchestrator.build.SonarRunner;
import com.sonar.orchestrator.selenium.Selenese;
import org.junit.ClassRule;
import org.junit.Test;
import org.sonar.wsclient.services.PropertyQuery;

import static org.assertj.core.api.Assertions.assertThat;
import static util.ItUtils.projectDir;

public class SubCategoriesTest {

  @ClassRule
  public static Orchestrator orchestrator = AdministrationTestSuite.ORCHESTRATOR;

  /**
   * SONAR-3159
   */
  @Test
  public void should_support_global_subcategories() {
    Selenese selenese = Selenese.builder().setHtmlTestsInClasspath("subcategories",
      "/administration/suite/SubCategoriesTest/subcategories/global-subcategories.html",
      // SONAR-4495
      "/administration/suite/SubCategoriesTest/subcategories/global-subcategories-no-default.html"
      ).build();
    orchestrator.executeSelenese(selenese);
    assertThat(getProperty("prop3", null)).isEqualTo("myValue");
  }

  /**
   * SONAR-3159
   */
  @Test
  public void should_support_project_subcategories() {
    orchestrator.executeBuild(SonarRunner.create(projectDir("shared/xoo-sample")));

    Selenese selenese = Selenese.builder().setHtmlTestsInClasspath("subcategories",
      "/administration/suite/SubCategoriesTest/subcategories/project-subcategories.html",
      // SONAR-4495
      "/administration/suite/SubCategoriesTest/subcategories/project-subcategories-no-default.html"
      ).build();
    orchestrator.executeSelenese(selenese);
    assertThat(getProperty("prop3", "sample")).isEqualTo("myValue2");
  }

  static String getProperty(String key, String resourceKeyOrId) {
    return orchestrator.getServer().getAdminWsClient().find(new PropertyQuery().setKey(key).setResourceKeyOrId(resourceKeyOrId)).getValue();
  }
}
