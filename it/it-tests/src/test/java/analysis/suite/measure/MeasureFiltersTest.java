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
package analysis.suite.measure;

import analysis.suite.AnalysisTestSuite;
import com.sonar.orchestrator.Orchestrator;
import com.sonar.orchestrator.build.SonarRunner;
import com.sonar.orchestrator.selenium.Selenese;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;
import org.sonar.wsclient.SonarClient;
import org.sonar.wsclient.user.UserParameters;
import selenium.SeleneseTest;

import static util.ItUtils.projectDir;

public class MeasureFiltersTest {

  @ClassRule
  public static Orchestrator orchestrator = AnalysisTestSuite.ORCHESTRATOR;

  @BeforeClass
  public static void scanStruts() {
    orchestrator.resetData();
    orchestrator.executeBuild(SonarRunner.create(projectDir("shared/xoo-multi-modules-sample")));

    createUser("user-measure-filters", "User Measure Filters");
  }

  @AfterClass
  public static void deleteTestUser() {
    deactivateUser("user-measure-filters");
  }

  @Test
  public void execute_measure_filters() {
    Selenese selenese = Selenese.builder().setHtmlTestsInClasspath("execution_of_measure_filters",
      "/measure/suite/measure_filters/link_from_main_header.html",
      "/measure/suite/measure_filters/initial_search_form.html",
      "/measure/suite/measure_filters/search_for_projects.html",
      "/measure/suite/measure_filters/search_for_files.html",
      // SONAR-4195
      "/measure/suite/measure_filters/search-by-key.html",
      "/measure/suite/measure_filters/search-by-name.html",
      "/measure/suite/measure_filters/empty_filter.html"
      ).build();
    new SeleneseTest(selenese).runOn(orchestrator);
  }

  @Test
  public void display_measure_filter_as_list() {
    Selenese selenese = Selenese.builder().setHtmlTestsInClasspath("display_measure_filter_as_list",
      "/measure/suite/measure_filters/list_change_columns.html",
      "/measure/suite/measure_filters/list_delete_column.html",
      "/measure/suite/measure_filters/list_move_columns.html",
      "/measure/suite/measure_filters/list_sort_by_descending_name.html",
      "/measure/suite/measure_filters/list_sort_by_ncloc.html"
      ).build();
    new SeleneseTest(selenese).runOn(orchestrator);
  }

  @Test
  public void share_measure_filters() {
    // SONAR-4099
    String user = "user-measures-filter-with-sharing-perm";
    createUser(user, "User Measure Filters with sharing permission", "shareDashboard");

    try {
      Selenese selenese = Selenese.builder().setHtmlTestsInClasspath("share_measure_filters",
        // SONAR-4469
        "/measure/suite/measure_filters/should-unshare-filter-remove-other-filters-favourite.html"
        ).build();
      new SeleneseTest(selenese).runOn(orchestrator);
    } finally {
      deactivateUser(user);
    }
  }

  /**
   * SONAR-4099
   */
  @Test
  public void should_not_share_filter_when_user_have_no_sharing_permissions() {
    String user = "user-measures-filter-with-no-share-perm";
    createUser(user, "User Measure Filters without sharing permission");

    try {
      new SeleneseTest(Selenese.builder().setHtmlTestsInClasspath("should_not_share_filter_when_user_have_no_sharing_permissions",
        "/measure/suite/measure_filters/should-not-share-filter-when-user-have-no-sharing-permissions.html"
        ).build()).runOn(orchestrator);
    } finally {
      deactivateUser(user);
    }
  }

  @Test
  public void copy_measure_filters() {
    Selenese selenese = Selenese.builder().setHtmlTestsInClasspath("copy_measure_filters",
      "/measure/suite/measure_filters/copy_measure_filter.html",
      "/measure/suite/measure_filters/copy_uniqueness_of_name.html"
      ).build();
    new SeleneseTest(selenese).runOn(orchestrator);
  }

  @Test
  public void manage_measure_filters() {
    Selenese selenese = Selenese.builder().setHtmlTestsInClasspath("manage_measure_filters",
      "/measure/suite/measure_filters/save_with_special_characters.html"
      ).build();
    new SeleneseTest(selenese).runOn(orchestrator);
  }

  @Test
  public void measure_filter_list_widget() {
    Selenese selenese = Selenese.builder().setHtmlTestsInClasspath("measure_filter_list_widget",
      "/measure/suite/measure_filters/list_widget.html",
      "/measure/suite/measure_filters/list_widget_sort.html",
      "/measure/suite/measure_filters/list_widget_warning_if_missing_filter.html"
      ).build();
    new SeleneseTest(selenese).runOn(orchestrator);
  }

  private static void createUser(String login, String name) {
    createUser(login, name, null);
  }

  private static void createUser(String login, String name, String permission) {
    SonarClient client = orchestrator.getServer().adminWsClient();
    UserParameters userCreationParameters = UserParameters.create().login(login).name(name).password("password").passwordConfirmation("password");
    client.userClient().create(userCreationParameters);

    if (permission != null) {
      client.post("api/permissions/add_user",
        "login", login,
        "permission", permission);
    }
  }

  private static void deactivateUser(String user) {
    orchestrator.getServer().adminWsClient().userClient().deactivate(user);
  }
}
