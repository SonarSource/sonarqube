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
package org.sonar.server.component.ws;

import java.util.HashMap;
import java.util.Map;
import javax.annotation.Nullable;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.sonar.api.server.ws.WebService;
import org.sonar.api.utils.System2;
import org.sonar.api.web.UserRole;
import org.sonar.db.DbTester;
import org.sonar.db.component.ComponentDto;
import org.sonar.db.component.ComponentTesting;
import org.sonar.db.component.SnapshotDto;
import org.sonar.db.component.SnapshotTesting;
import org.sonar.db.measure.MeasureDto;
import org.sonar.db.measure.MeasureTesting;
import org.sonar.db.metric.MetricDto;
import org.sonar.server.component.ComponentFinder;
import org.sonar.server.startup.RegisterMetrics;
import org.sonar.server.tester.UserSessionRule;
import org.sonar.server.ws.TestRequest;
import org.sonar.server.ws.WsActionTester;
import org.sonar.test.JsonAssert;

import static org.assertj.core.api.Assertions.assertThat;
import static org.sonar.api.measures.CoreMetrics.COVERAGE_KEY;
import static org.sonar.api.measures.CoreMetrics.DUPLICATED_LINES_DENSITY_KEY;
import static org.sonar.api.measures.CoreMetrics.LINES_KEY;
import static org.sonar.api.measures.CoreMetrics.SQALE_DEBT_RATIO_KEY;
import static org.sonar.api.measures.CoreMetrics.SQALE_RATING_KEY;
import static org.sonar.api.measures.CoreMetrics.TECHNICAL_DEBT_KEY;
import static org.sonar.api.measures.CoreMetrics.getMetric;

public class AppActionTest {

  private static final String PROJECT_KEY = "org.sonarsource.sonarqube:sonarqube";
  private static final String MODULE_KEY = "org.sonarsource.sonarqube:sonar-plugin-api";
  private static final String FILE_KEY = "org.sonarsource.sonarqube:sonar-plugin-api:src/main/java/org/sonar/api/Plugin.java";
  private static final String PROJECT_UUID = "THE_PROJECT_UUID";
  private static final String MODULE_UUID = "THE_MODULE_UUID";
  private static final String FILE_UUID = "THE_FILE_UUID";
  private static final String ANALYSIS_UUID = "THE_ANALYSIS_UUID";

  private Map<String, MetricDto> metricsByKey;

  @Rule
  public UserSessionRule userSessionRule = UserSessionRule.standalone();

  @Rule
  public DbTester dbTester = DbTester.create(System2.INSTANCE);

  private AppAction underTest = new AppAction(dbTester.getDbClient(), userSessionRule, new ComponentFinder(dbTester.getDbClient()));
  private WsActionTester wsTester = new WsActionTester(underTest);

  @Before
  public void setUp() {
    insertMetrics();
  }

  @Test
  public void file_without_measures() throws Exception {
    ComponentDto[] components = insertComponentsAndAnalysis();
    dbTester.commit();

    userSessionRule.logIn("john").addProjectPermission(UserRole.USER, components);
    TestRequest request = wsTester.newRequest().setParam("uuid", FILE_UUID);
    jsonAssert(request, "app.json");
  }

  @Test
  public void file_with_measures() throws Exception {
    ComponentDto[] components = insertComponentsAndAnalysis();
    insertFileMeasure(metricsByKey.get(LINES_KEY).getId(), 200d, null);
    insertFileMeasure(metricsByKey.get(DUPLICATED_LINES_DENSITY_KEY).getId(), 7.4, null);
    insertFileMeasure(metricsByKey.get(SQALE_RATING_KEY).getId(), null, "C");
    insertFileMeasure(metricsByKey.get(TECHNICAL_DEBT_KEY).getId(), 182d, null);
    insertFileMeasure(metricsByKey.get(SQALE_DEBT_RATIO_KEY).getId(), 35d, null);
    insertFileMeasure(metricsByKey.get(COVERAGE_KEY).getId(), 95.4d, null);
    dbTester.commit();

    userSessionRule
      .logIn("john")
      .addProjectPermission(UserRole.USER, components);
    TestRequest request = wsTester.newRequest().setParam("uuid", FILE_UUID);
    jsonAssert(request, "app_with_measures.json");
  }

  @Test
  public void file_with_coverage() throws Exception {
    ComponentDto[] components = insertComponentsAndAnalysis();
    insertFileMeasure(metricsByKey.get(COVERAGE_KEY).getId(), 95.4, null);
    dbTester.commit();

    userSessionRule.logIn("john")
      .addProjectPermission(UserRole.USER, components);
    TestRequest request = wsTester.newRequest().setParam("uuid", FILE_UUID);
    jsonAssert(request, "app_with_ut_measure.json");
  }

  @Test
  public void define_app_action() {
    WebService.Action action = wsTester.getDef();
    assertThat(action).isNotNull();
    assertThat(action.isInternal()).isTrue();
    assertThat(action.isPost()).isFalse();
    assertThat(action.handler()).isNotNull();
    assertThat(action.params()).hasSize(3);
  }

  private void insertMetrics() {
    metricsByKey = new HashMap<>();
    for (String metricKey : AppAction.METRIC_KEYS) {
      MetricDto dto = RegisterMetrics.MetricToDto.INSTANCE.apply(getMetric(metricKey));
      dbTester.getDbClient().metricDao().insert(dbTester.getSession(), dto);
      metricsByKey.put(metricKey, dto);
    }
    dbTester.commit();
  }

  private ComponentDto[] insertComponentsAndAnalysis() {
    ComponentDto project = ComponentTesting.newPrivateProjectDto(dbTester.getDefaultOrganization(), PROJECT_UUID)
      .setLongName("SonarQube")
      .setKey(PROJECT_KEY);
    ComponentDto module = ComponentTesting.newModuleDto(MODULE_UUID, project)
      .setLongName("SonarQube :: Plugin API")
      .setKey(MODULE_KEY);
    ComponentDto file = ComponentTesting.newFileDto(module, null, FILE_UUID)
      .setKey(FILE_KEY)
      .setName("Plugin.java")
      .setLongName("src/main/java/org/sonar/api/Plugin.java")
      .setPath("src/main/java/org/sonar/api/Plugin.java");
    dbTester.getDbClient().componentDao().insert(dbTester.getSession(), project, module, file);
    SnapshotDto analysis = SnapshotTesting.newAnalysis(project).setUuid(ANALYSIS_UUID);
    dbTester.getDbClient().snapshotDao().insert(dbTester.getSession(), analysis);
    return new ComponentDto[] {project, module, file};
  }

  private void insertFileMeasure(int metricId, @Nullable Double value, @Nullable String data) {
    MeasureDto measure = MeasureTesting.newMeasure()
      .setComponentUuid(FILE_UUID)
      .setAnalysisUuid(ANALYSIS_UUID)
      .setMetricId(metricId)
      .setValue(value)
      .setData(data);
    dbTester.getDbClient().measureDao().insert(dbTester.getSession(), measure);
  }

  private void jsonAssert(TestRequest request, String filename) {
    JsonAssert.assertJson(request.execute().getInput()).isSimilarTo(getClass().getResource(getClass().getSimpleName()+"/"+ filename));
  }
}
