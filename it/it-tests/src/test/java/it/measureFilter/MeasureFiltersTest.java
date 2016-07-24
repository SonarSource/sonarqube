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
package it.measureFilter;

import com.sonar.orchestrator.Orchestrator;
import com.sonar.orchestrator.build.SonarScanner;
import com.sonar.orchestrator.selenium.Selenese;
import it.Category1Suite;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;
import org.sonar.wsclient.SonarClient;
import org.sonar.wsclient.user.UserParameters;
import org.sonarqube.ws.client.WsClient;
import org.sonarqube.ws.client.permission.AddUserWsRequest;
import util.selenium.SeleneseTest;

import static util.ItUtils.newAdminWsClient;
import static util.ItUtils.projectDir;

public class MeasureFiltersTest {

  @ClassRule
  public static Orchestrator orchestrator = Category1Suite.ORCHESTRATOR;
  public static WsClient adminWsClient;

  @BeforeClass
  public static void scanStruts() {
    orchestrator.resetData();
    orchestrator.executeBuild(SonarScanner.create(projectDir("shared/xoo-multi-modules-sample")));
    adminWsClient = newAdminWsClient(orchestrator);

    createUser("user-measure-filters", "User Measure Filters");
  }

  @AfterClass
  public static void deleteTestUser() {
    deactivateUser("user-measure-filters");
  }

  @Test
  public void execute_measure_filters() {
    Selenese selenese = Selenese.builder().setHtmlTestsInClasspath("execution_of_measure_filters",
      "/measureFilter/MeasureFiltersTest/link_from_main_header.html",
      "/measureFilter/MeasureFiltersTest/initial_search_form.html",
      "/measureFilter/MeasureFiltersTest/search_for_projects.html",
      "/measureFilter/MeasureFiltersTest/search_for_files.html",
      // SONAR-4195
      "/measureFilter/MeasureFiltersTest/search-by-key.html",
      "/measureFilter/MeasureFiltersTest/search-by-name.html",
      "/measureFilter/MeasureFiltersTest/empty_filter.html").build();
    new SeleneseTest(selenese).runOn(orchestrator);
  }

  @Test
  public void display_measure_filter_as_list() {
    Selenese selenese = Selenese.builder().setHtmlTestsInClasspath("display_measure_filter_as_list",
      "/measureFilter/MeasureFiltersTest/list_change_columns.html",
      "/measureFilter/MeasureFiltersTest/list_delete_column.html",
      "/measureFilter/MeasureFiltersTest/list_move_columns.html",
      "/measureFilter/MeasureFiltersTest/list_sort_by_descending_name.html",
      "/measureFilter/MeasureFiltersTest/list_sort_by_ncloc.html").build();
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
        "/measureFilter/MeasureFiltersTest/should-unshare-filter-remove-other-filters-favourite.html").build();
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
        "/measureFilter/MeasureFiltersTest/should-not-share-filter-when-user-have-no-sharing-permissions.html").build()).runOn(orchestrator);
    } finally {
      deactivateUser(user);
    }
  }

  @Test
  public void copy_measure_filters() {
    Selenese selenese = Selenese.builder().setHtmlTestsInClasspath("copy_measure_filters",
      "/measureFilter/MeasureFiltersTest/copy_measure_filter.html",
      "/measureFilter/MeasureFiltersTest/copy_uniqueness_of_name.html").build();
    new SeleneseTest(selenese).runOn(orchestrator);
  }

  @Test
  public void manage_measure_filters() {
    Selenese selenese = Selenese.builder().setHtmlTestsInClasspath("manage_measure_filters",
      "/measureFilter/MeasureFiltersTest/save_with_special_characters.html").build();
    new SeleneseTest(selenese).runOn(orchestrator);
  }

  @Test
  public void measure_filter_list_widget() {
    Selenese selenese = Selenese.builder().setHtmlTestsInClasspath("measure_filter_list_widget",
      "/measureFilter/MeasureFiltersTest/list_widget.html",
      "/measureFilter/MeasureFiltersTest/list_widget_sort.html",
      "/measureFilter/MeasureFiltersTest/list_widget_warning_if_missing_filter.html").build();
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
      adminWsClient.permissions().addUser(new AddUserWsRequest()
        .setLogin(login)
        .setPermission(permission));
    }
  }

  private static void deactivateUser(String user) {
    orchestrator.getServer().adminWsClient().userClient().deactivate(user);
  }
}
