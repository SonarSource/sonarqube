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
package org.sonarqube.tests.projectAdministration;

import com.sonar.orchestrator.Orchestrator;
import com.sonar.orchestrator.build.SonarScanner;
import org.sonarqube.tests.Category1Suite;
import java.io.UnsupportedEncodingException;
import java.sql.SQLException;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import javax.annotation.Nullable;
import org.apache.commons.lang.time.DateFormatUtils;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.sonar.wsclient.SonarClient;
import org.sonar.wsclient.base.HttpException;
import org.sonar.wsclient.user.UserParameters;
import org.sonarqube.pageobjects.Navigation;
import org.sonarqube.pageobjects.settings.SettingsPage;
import util.user.UserRule;

import static org.apache.commons.lang.time.DateUtils.addDays;
import static org.assertj.core.api.Assertions.assertThat;
import static util.ItUtils.getComponent;
import static util.ItUtils.projectDir;
import static util.selenium.Selenese.runSelenese;

public class ProjectAdministrationTest {
  private static final String DELETE_WS_ENDPOINT = "api/projects/bulk_delete";

  // take some day in the past
  private static final String ANALYSIS_DATE = DateFormatUtils.ISO_DATE_FORMAT.format(addDays(new Date(), -1));

  @ClassRule
  public static Orchestrator orchestrator = Category1Suite.ORCHESTRATOR;

  @Rule
  public ExpectedException expectedException = ExpectedException.none();

  @Rule
  public UserRule userRule = UserRule.from(orchestrator);

  private Navigation nav = Navigation.create(orchestrator);

  private static final String PROJECT_KEY = "sample";
  private static final String FILE_KEY = "sample:src/main/xoo/sample/Sample.xoo";
  private String adminUser;

  @Before
  public void deleteAnalysisData() throws SQLException {
    orchestrator.resetData();
    adminUser = userRule.createAdminUser();
  }

  @Test
  public void delete_project_by_web_service() {
    scanSampleWithDate(ANALYSIS_DATE);

    assertThat(getComponent(orchestrator, PROJECT_KEY)).isNotNull();
    assertThat(getComponent(orchestrator, FILE_KEY)).isNotNull();

    orchestrator.getServer().adminWsClient().post(DELETE_WS_ENDPOINT, "keys", PROJECT_KEY);

    assertThat(getComponent(orchestrator, PROJECT_KEY)).isNull();
    assertThat(getComponent(orchestrator, FILE_KEY)).isNull();
  }

  @Test
  public void fail_when_trying_to_delete_a_file() {
    expectedException.expect(HttpException.class);
    scanSampleWithDate(ANALYSIS_DATE);

    assertThat(getComponent(orchestrator, PROJECT_KEY)).isNotNull();
    assertThat(getComponent(orchestrator, FILE_KEY)).isNotNull();

    // it's forbidden to delete only some files
    orchestrator.getServer().adminWsClient().post(DELETE_WS_ENDPOINT, "keys", FILE_KEY);
  }

  @Test
  public void fail_when_insufficient_privilege() {
    expectedException.expect(HttpException.class);
    scanSampleWithDate(ANALYSIS_DATE);

    assertThat(getComponent(orchestrator, PROJECT_KEY)).isNotNull();

    // use wsClient() instead of adminWsClient()
    orchestrator.getServer().wsClient().post(DELETE_WS_ENDPOINT, "keys", PROJECT_KEY);
  }

  /**
   * Test updated for SONAR-3570 and SONAR-5923
   */
  @Test
  public void project_deletion() {
    String projectAdminUser = "project-deletion-with-admin-permission-on-project";
    SonarClient wsClient = orchestrator.getServer().adminWsClient();
    try {
      SonarScanner scan = SonarScanner.create(projectDir("shared/xoo-sample"));
      orchestrator.executeBuild(scan);

      // Create user having admin permission on previously analysed project
      wsClient.userClient().create(
        UserParameters.create().login(projectAdminUser).name(projectAdminUser).password("password").passwordConfirmation("password"));

      wsClient.post("api/permissions/add_user",
        "login", projectAdminUser,
        "projectKey", "sample",
        "permission", "admin");

      runSelenese(orchestrator, "/projectAdministration/ProjectAdministrationTest/project-deletion/project-deletion.html");
    } finally {
      wsClient.userClient().deactivate(projectAdminUser);
    }
  }

  // SONAR-4203
  @Test
  @Ignore("refactor with wsClient")
  public void delete_version_of_multimodule_project() {
    GregorianCalendar today = new GregorianCalendar();
    SonarScanner build = SonarScanner.create(projectDir("shared/xoo-multi-modules-sample"))
      .setProperty("sonar.dynamicAnalysis", "false")
      .setProperty("sonar.projectDate", (today.get(Calendar.YEAR) - 1) + "-01-01");
    orchestrator.executeBuild(build);

    // The analysis must be run once again to have an history so that it is possible
    // to set/delete version on old snapshot
    build.setProperty("sonar.projectDate", today.get(Calendar.YEAR) + "-01-01");
    orchestrator.executeBuild(build);

    // There are 7 modules
    assertThat(count("events where category='Version'")).as("Different number of events").isEqualTo(1);

    runSelenese(orchestrator, "/projectAdministration/ProjectAdministrationTest/project-administration/multimodule-project-modify-version.html");

    assertThat(count("events where category='Version'")).as("Different number of events").isEqualTo(2);

    runSelenese(orchestrator, "/projectAdministration/ProjectAdministrationTest/project-administration/multimodule-project-delete-version.html");

    assertThat(count("events where category='Version'")).as("Different number of events").isEqualTo(1);
  }

  @Test
  public void display_project_settings() throws UnsupportedEncodingException {
    scanSample(null, null);

    SettingsPage page = nav.logIn().submitCredentials(adminUser).openSettings("sample")
      .assertMenuContains("Analysis Scope")
      .assertMenuContains("Category 1")
      .assertMenuContains("DEV")
      .assertMenuContains("project-only")
      .assertMenuContains("Xoo")
      .assertSettingDisplayed("sonar.dbcleaner.daysBeforeDeletingClosedIssues");

    page.openCategory("project-only")
      .assertSettingDisplayed("prop_only_on_project");

    page.openCategory("General")
      .assertStringSettingValue("sonar.dbcleaner.daysBeforeDeletingClosedIssues", "30")
      .assertStringSettingValue("sonar.leak.period", "previous_version")
      .assertBooleanSettingValue("sonar.dbcleaner.cleanDirectory", true)
      .setStringValue("sonar.dbcleaner.daysBeforeDeletingClosedIssues", "1")
      .assertStringSettingValue("sonar.dbcleaner.daysBeforeDeletingClosedIssues", "1");
  }

  @Test
  public void display_module_settings() throws UnsupportedEncodingException {
    orchestrator.executeBuild(SonarScanner.create(projectDir("shared/xoo-multi-modules-sample")));

    nav.logIn().submitCredentials(adminUser)
      .openSettings("com.sonarsource.it.samples:multi-modules-sample:module_a")
      .assertMenuContains("Analysis Scope")
      .assertSettingDisplayed("sonar.coverage.exclusions");
  }

  private void scanSampleWithDate(String date) {
    scanSample(date, null);
  }

  private void scanSample(@Nullable String date, @Nullable String profile) {
    SonarScanner scan = SonarScanner.create(projectDir("shared/xoo-sample"))
      .setProperty("sonar.cpd.exclusions", "**/*");
    if (date != null) {
      scan.setProperty("sonar.projectDate", date);
    }
    if (profile != null) {
      scan.setProfile(profile);
    }
    orchestrator.executeBuild(scan);
  }

  private int count(String condition) {
    return orchestrator.getDatabase().countSql("select count(1) from " + condition);
  }

}
