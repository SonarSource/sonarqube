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
package it.projectAdministration;

import com.sonar.orchestrator.Orchestrator;
import com.sonar.orchestrator.build.SonarScanner;
import com.sonar.orchestrator.selenium.Selenese;
import it.Category1Suite;
import java.sql.SQLException;
import java.util.Calendar;
import java.util.GregorianCalendar;
import javax.annotation.Nullable;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.rules.ExpectedException;
import org.sonar.wsclient.SonarClient;
import org.sonar.wsclient.base.HttpException;
import org.sonar.wsclient.services.PropertyQuery;
import org.sonar.wsclient.services.ResourceQuery;
import org.sonar.wsclient.user.UserParameters;
import util.QaOnly;
import util.selenium.SeleneseTest;

import static org.assertj.core.api.Assertions.assertThat;
import static util.ItUtils.projectDir;

@Category(QaOnly.class)
public class ProjectAdministrationTest {

  private static final String DELETE_WS_ENDPOINT = "api/projects/bulk_delete";
  @ClassRule
  public static Orchestrator orchestrator = Category1Suite.ORCHESTRATOR;
  @Rule
  public ExpectedException expectedException = ExpectedException.none();

  private static final String PROJECT_KEY = "sample";
  private static final String FILE_KEY = "sample:src/main/xoo/sample/Sample.xoo";

  @Before
  public void deleteAnalysisData() throws SQLException {
    orchestrator.resetData();
  }

  @Test
  public void delete_project_by_web_service() {
    scanSampleWithDate("2012-01-01");

    assertThat(orchestrator.getServer().getWsClient().find(ResourceQuery.create(PROJECT_KEY))).isNotNull();
    assertThat(orchestrator.getServer().getWsClient().find(ResourceQuery.create(FILE_KEY))).isNotNull();

    orchestrator.getServer().adminWsClient().post(DELETE_WS_ENDPOINT, "keys", PROJECT_KEY);

    assertThat(orchestrator.getServer().getWsClient().find(ResourceQuery.create(PROJECT_KEY))).isNull();
    assertThat(orchestrator.getServer().getWsClient().find(ResourceQuery.create(FILE_KEY))).isNull();
  }

  @Test
  public void fail_when_trying_to_delete_a_file() {
    expectedException.expect(HttpException.class);
    scanSampleWithDate("2012-01-01");

    assertThat(orchestrator.getServer().getWsClient().find(ResourceQuery.create(PROJECT_KEY))).isNotNull();
    assertThat(orchestrator.getServer().getWsClient().find(ResourceQuery.create(FILE_KEY))).isNotNull();

    // it's forbidden to delete only some files
    orchestrator.getServer().adminWsClient().post(DELETE_WS_ENDPOINT, "keys", FILE_KEY);
  }

  @Test
  public void fail_when_insufficient_privilege() {
    expectedException.expect(HttpException.class);
    scanSampleWithDate("2012-01-01");

    assertThat(orchestrator.getServer().getWsClient().find(ResourceQuery.create(PROJECT_KEY))).isNotNull();

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

      new SeleneseTest(
        Selenese.builder().setHtmlTestsInClasspath("project-deletion", "/projectAdministration/ProjectAdministrationTest/project-deletion/project-deletion.html").build())
        .runOn(orchestrator);
    } finally {
      wsClient.userClient().deactivate(projectAdminUser);
    }
  }

  // SONAR-4203
  @Test
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

    Selenese selenese = Selenese.builder()
      .setHtmlTestsInClasspath("modify_version_of_multimodule_project",
        "/projectAdministration/ProjectAdministrationTest/project-administration/multimodule-project-modify-version.html"
      ).build();
    new SeleneseTest(selenese).runOn(orchestrator);

    assertThat(count("events where category='Version'")).as("Different number of events").isEqualTo(2);

    selenese = Selenese.builder()
      .setHtmlTestsInClasspath("delete_version_of_multimodule_project",
        "/projectAdministration/ProjectAdministrationTest/project-administration/multimodule-project-delete-version.html"
      ).build();
    new SeleneseTest(selenese).runOn(orchestrator);

    assertThat(count("events where category='Version'")).as("Different number of events").isEqualTo(1);
  }

  /**
   * SONAR-3425
   */
  @Test
  public void project_settings() {
    scanSampleWithDate("2012-01-01");

    Selenese selenese = Selenese.builder().setHtmlTestsInClasspath("project-settings",
      // SONAR-3425
      "/projectAdministration/ProjectAdministrationTest/project-settings/override-global-settings.html",

      "/projectAdministration/ProjectAdministrationTest/project-settings/only-on-project-settings.html"
      ).build();
    new SeleneseTest(selenese).runOn(orchestrator);

    // GET /api/properties/sonar.exclusions?resource=sample
    assertThat(orchestrator.getServer().getAdminWsClient().find(PropertyQuery.createForResource("sonar.exclusions", "sample")).getValue())
      .isEqualTo("my-exclusions");

    // GET /api/properties?resource=sample
    // Note that this WS is used by SonarLint
    assertThat(orchestrator.getServer().getAdminWsClient().findAll(PropertyQuery.createForResource(null, "sample"))).isNotEmpty();
  }

  /**
   * SONAR-1608
   */
  @Test
  public void bulk_update_project_keys() {
    SonarScanner build = SonarScanner.create(projectDir("shared/xoo-multi-modules-sample"));
    orchestrator.executeBuild(build);

    Selenese selenese = Selenese.builder()
      .setHtmlTestsInClasspath("project-bulk-update-keys",
        "/projectAdministration/ProjectAdministrationTest/project-update-keys/bulk-update-impossible-because-duplicate-keys.html",
        "/projectAdministration/ProjectAdministrationTest/project-update-keys/bulk-update-impossible-because-no-match.html",
        "/projectAdministration/ProjectAdministrationTest/project-update-keys/bulk-update-success.html"
      ).build();
    new SeleneseTest(selenese).runOn(orchestrator);
  }

  /**
   * SONAR-1608
   */
  @Test
  public void fine_grain_update_project_keys() {
    SonarScanner build = SonarScanner.create(projectDir("shared/xoo-multi-modules-sample"));
    orchestrator.executeBuild(build);

    Selenese selenese = Selenese.builder()
      .setHtmlTestsInClasspath("project-fine-grained-update-keys",
        "/projectAdministration/ProjectAdministrationTest/project-update-keys/fine-grained-update-impossible.html",
        "/projectAdministration/ProjectAdministrationTest/project-update-keys/fine-grained-update-success.html"
      ).build();
    new SeleneseTest(selenese).runOn(orchestrator);
  }

  /**
   * SONAR-4060
   */
  @Test
  public void display_module_settings() {
    orchestrator.executeBuild(SonarScanner.create(projectDir("shared/xoo-multi-modules-sample")));

    Selenese selenese = Selenese.builder().setHtmlTestsInClasspath("module-settings",
      // SONAR-3425
      "/projectAdministration/ProjectAdministrationTest/module-settings/display-module-settings.html"
      ).build();
    new SeleneseTest(selenese).runOn(orchestrator);
  }

  private void scanSampleWithDate(String date) {
    scanSample(date, null);
  }

  private void scanSample(@Nullable String date, @Nullable String profile) {
    SonarScanner scan = SonarScanner.create(projectDir("shared/xoo-sample"))
      .setProperties("sonar.cpd.skip", "true");
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
