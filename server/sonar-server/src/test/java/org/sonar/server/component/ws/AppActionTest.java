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
package org.sonar.server.component.ws;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import javax.annotation.Nullable;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.sonar.api.measures.CoreMetrics;
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
import org.sonar.server.ws.WsTester;

import static org.mockito.Mockito.mock;

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

  private WsTester wsTester;

  @Before
  public void setUp() {
    insertMetrics();
    wsTester = new WsTester(new ComponentsWs(
      new AppAction(dbTester.getDbClient(), userSessionRule, new ComponentFinder(dbTester.getDbClient())), mock(SearchViewComponentsAction.class)));
  }

  @Test
  public void file_without_measures() throws Exception {
    insertComponentsAndAnalysis();
    dbTester.commit();

    userSessionRule.login("john").addComponentPermission(UserRole.USER, MODULE_KEY, FILE_KEY);
    WsTester.TestRequest request = wsTester.newGetRequest("api/components", "app").setParam("uuid", FILE_UUID);
    request.execute().assertJson(getClass(), "app.json");
  }

  @Test
  public void file_with_measures() throws Exception {
    insertComponentsAndAnalysis();
    insertFileMeasure(metricsByKey.get(CoreMetrics.LINES_KEY).getId(), 200d, null);
    insertFileMeasure(metricsByKey.get(CoreMetrics.DUPLICATED_LINES_DENSITY_KEY).getId(), 7.4, null);
    insertFileMeasure(metricsByKey.get(CoreMetrics.SQALE_RATING_KEY).getId(), null, "C");
    insertFileMeasure(metricsByKey.get(CoreMetrics.TECHNICAL_DEBT_KEY).getId(), 182d, null);
    insertFileMeasure(metricsByKey.get(CoreMetrics.SQALE_DEBT_RATIO_KEY).getId(), 35d, null);
    insertFileMeasure(metricsByKey.get(CoreMetrics.COVERAGE_KEY).getId(), 95.4d, null);
    dbTester.commit();

    userSessionRule
      .login("john")
      .setLocale(Locale.ENGLISH)
      .addComponentPermission(UserRole.USER, PROJECT_KEY, FILE_KEY);
    WsTester.TestRequest request = wsTester.newGetRequest("api/components", "app").setParam("uuid", FILE_UUID);
    request.execute().assertJson(getClass(), "app_with_measures.json");
  }

  @Test
  public void file_with_overall_coverage() throws Exception {
    insertComponentsAndAnalysis();
    insertFileMeasure(metricsByKey.get(CoreMetrics.OVERALL_COVERAGE_KEY).getId(), 90.1, null);
    insertFileMeasure(metricsByKey.get(CoreMetrics.COVERAGE_KEY).getId(), 95.4, null);
    insertFileMeasure(metricsByKey.get(CoreMetrics.IT_COVERAGE_KEY).getId(), 85.2, null);
    dbTester.commit();

    userSessionRule.login("john").addComponentPermission(UserRole.USER, PROJECT_KEY, FILE_KEY);
    WsTester.TestRequest request = wsTester.newGetRequest("api/components", "app").setParam("uuid", FILE_UUID);
    request.execute().assertJson(getClass(), "app_with_overall_measure.json");
  }

  @Test
  public void file_with_ut_coverage() throws Exception {
    insertComponentsAndAnalysis();
    insertFileMeasure(metricsByKey.get(CoreMetrics.COVERAGE_KEY).getId(), 95.4, null);
    insertFileMeasure(metricsByKey.get(CoreMetrics.IT_COVERAGE_KEY).getId(), 85.2, null);
    dbTester.commit();

    userSessionRule.login("john").addComponentPermission(UserRole.USER, PROJECT_KEY, FILE_KEY);
    WsTester.TestRequest request = wsTester.newGetRequest("api/components", "app").setParam("uuid", FILE_UUID);
    request.execute().assertJson(getClass(), "app_with_ut_measure.json");
  }

  @Test
  public void file_with_it_coverage_only() throws Exception {
    insertComponentsAndAnalysis();
    insertFileMeasure(metricsByKey.get(CoreMetrics.IT_COVERAGE_KEY).getId(), 85.2, null);
    dbTester.commit();

    userSessionRule.login("john").addComponentPermission(UserRole.USER, PROJECT_KEY, FILE_KEY);
    WsTester.TestRequest request = wsTester.newGetRequest("api/components", "app").setParam("uuid", FILE_UUID);
    request.execute().assertJson(getClass(), "app_with_it_measure.json");
  }


  private void insertMetrics() {
    metricsByKey = new HashMap<>();
    for (String metricKey : AppAction.METRIC_KEYS) {
      MetricDto dto = RegisterMetrics.MetricToDto.INSTANCE.apply(CoreMetrics.getMetric(metricKey));
      dbTester.getDbClient().metricDao().insert(dbTester.getSession(), dto);
      metricsByKey.put(metricKey, dto);
    }
    dbTester.commit();
  }

  private void insertComponentsAndAnalysis() {
    ComponentDto project = ComponentTesting.newProjectDto(PROJECT_UUID)
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
    SnapshotDto analysis = SnapshotTesting.newAnalysis(project)
      .setUuid(ANALYSIS_UUID);
    dbTester.getDbClient().snapshotDao().insert(dbTester.getSession(), analysis);
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

  // @Test
  // public void app_with_overall_measure() throws Exception {
  // userSessionRule.addComponentPermission(UserRole.USER, SUB_PROJECT_KEY, FILE_KEY);
  // ComponentDto project = newProject();
  // newComponent(project);
  //
  // addMeasure(CoreMetrics.OVERALL_COVERAGE_KEY, 90.1);
  // addMeasure(CoreMetrics.COVERAGE_KEY, 95.4);
  // addMeasure(CoreMetrics.IT_COVERAGE_KEY, 85.2);
  //
  // WsTester.TestRequest request = wsTester.newGetRequest("api/components", "app").setParam("uuid", COMPONENT_UUID);
  // request.execute().assertJson(getClass(), "app_with_overall_measure.json");
  // }
  //
  // @Test
  // public void app_with_ut_measure() throws Exception {
  // userSessionRule.addComponentPermission(UserRole.USER, SUB_PROJECT_KEY, FILE_KEY);
  // ComponentDto project = newProject();
  // newComponent(project);
  //
  // addMeasure(CoreMetrics.COVERAGE_KEY, 95.4);
  // addMeasure(CoreMetrics.IT_COVERAGE_KEY, 85.2);
  //
  // WsTester.TestRequest request = wsTester.newGetRequest("api/components", "app").setParam("uuid", COMPONENT_UUID);
  // request.execute().assertJson(getClass(), "app_with_ut_measure.json");
  // }
  //
  // @Test
  // public void app_with_it_measure() throws Exception {
  // userSessionRule.addComponentPermission(UserRole.USER, SUB_PROJECT_KEY, FILE_KEY);
  // ComponentDto project = newProject();
  // newComponent(project);
  //
  // addMeasure(CoreMetrics.IT_COVERAGE_KEY, 85.2);
  //
  // WsTester.TestRequest request = wsTester.newGetRequest("api/components", "app").setParam("uuid", COMPONENT_UUID);
  // request.execute().assertJson(getClass(), "app_with_it_measure.json");
  // }
  //
  // @Test
  // public void fail_on_unknown_component() {
  // userSessionRule.login("john").addComponentPermission(UserRole.USER, SUB_PROJECT_KEY, FILE_KEY);
  // when(componentDao.selectByUuid(session, COMPONENT_UUID)).thenReturn(Optional.<ComponentDto>absent());
  //
  // try {
  // wsTester.newGetRequest("api/components", "app").setParam("uuid", COMPONENT_UUID).execute();
  // fail();
  // } catch (Exception e) {
  // assertThat(e).isInstanceOf(NotFoundException.class).hasMessage("Component id 'ABCDE' not found");
  // }
  // }
  //
  // private ComponentDto newProject() {
  // return ComponentTesting.newProjectDto()
  // .setId(1L)
  // .setName("SonarQube")
  // .setUuid(PROJECT_UUID)
  // .setLongName("SonarQube")
  // .setKey("org.codehaus.sonar:sonar");
  // }
  //
  // private ComponentDto newComponent(ComponentDto project) {
  // ComponentDto file = ComponentTesting.newFileDto(project)
  // .setId(10L)
  // .setQualifier("FIL")
  // .setKey(FILE_KEY)
  // .setUuid(COMPONENT_UUID)
  // .setProjectUuid(PROJECT_UUID)
  // .setName("Plugin.java")
  // .setLongName("src/main/java/org/sonar/api/Plugin.java")
  // .setPath("src/main/java/org/sonar/api/Plugin.java")
  // .setRootUuid("uuid_5");
  // when(componentDao.selectByUuid(session, COMPONENT_UUID)).thenReturn(Optional.of(file));
  // when(componentDao.selectOrFailByUuid(session, "uuid_5")).thenReturn(new ComponentDto().setUuid("uuid_5").setLongName("SonarQube ::
  // Plugin API").setKey(SUB_PROJECT_KEY));
  // when(componentDao.selectOrFailByUuid(session, project.uuid())).thenReturn(project);
  // return file;
  // }
  //
  // private void addMeasure(String metricKey, Integer value) {
  // measures.add(new MeasureDto().setMetricKey(metricKey).setValue(value.doubleValue()));
  // when(i18n.formatInteger(any(Locale.class), eq(value.intValue()))).thenReturn(Integer.toString(value));
  // }
  //
  // private void addMeasure(String metricKey, Double value) {
  // measures.add(new MeasureDto().setMetricKey(metricKey).setValue(value));
  // when(i18n.formatDouble(any(Locale.class), eq(value))).thenReturn(Double.toString(value));
  // }
  //
  // private void addMeasure(String metricKey, String value) {
  // measures.add(new MeasureDto().setMetricKey(metricKey).setData(value));
  // }

}
