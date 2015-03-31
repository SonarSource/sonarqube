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

package org.sonar.server.component.ws;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.sonar.api.i18n.I18n;
import org.sonar.api.measures.CoreMetrics;
import org.sonar.api.utils.Duration;
import org.sonar.api.utils.Durations;
import org.sonar.api.web.UserRole;
import org.sonar.core.component.ComponentDto;
import org.sonar.core.measure.db.MeasureDto;
import org.sonar.core.persistence.DbSession;
import org.sonar.core.properties.PropertiesDao;
import org.sonar.core.properties.PropertyDto;
import org.sonar.core.properties.PropertyQuery;
import org.sonar.server.component.ComponentTesting;
import org.sonar.server.component.db.ComponentDao;
import org.sonar.server.db.DbClient;
import org.sonar.server.exceptions.NotFoundException;
import org.sonar.server.measure.persistence.MeasureDao;
import org.sonar.server.user.MockUserSession;
import org.sonar.server.ws.WsTester;

import java.util.List;
import java.util.Locale;

import static com.google.common.collect.Lists.newArrayList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.fail;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyListOf;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.*;

@RunWith(MockitoJUnitRunner.class)
public class ComponentAppActionTest {

  static final String SUB_PROJECT_KEY = "org.codehaus.sonar:sonar-plugin-api";
  static final String COMPONENT_KEY = "org.codehaus.sonar:sonar-plugin-api:src/main/java/org/sonar/api/Plugin.java";
  static final String COMPONENT_UUID = "ABCDE";
  static final String PROJECT_UUID = "THE_PROJECT";

  @Mock
  DbSession session;

  @Mock
  ComponentDao componentDao;

  @Mock
  PropertiesDao propertiesDao;

  @Mock
  MeasureDao measureDao;
  @Mock
  Durations durations;

  @Mock
  I18n i18n;

  @Captor
  ArgumentCaptor<List<String>> measureKeysCaptor;

  List<MeasureDto> measures = newArrayList();

  WsTester tester;

  @Before
  public void setUp() throws Exception {
    DbClient dbClient = mock(DbClient.class);
    when(dbClient.openSession(false)).thenReturn(session);
    when(dbClient.componentDao()).thenReturn(componentDao);
    when(dbClient.propertiesDao()).thenReturn(propertiesDao);
    when(dbClient.measureDao()).thenReturn(measureDao);

    when(measureDao.findByComponentKeyAndMetricKeys(eq(session), anyString(), anyListOf(String.class))).thenReturn(measures);

    tester = new WsTester(new ComponentsWs(new ComponentAppAction(dbClient, durations, i18n), mock(SearchAction.class)));
  }

  @Test
  public void app() throws Exception {
    MockUserSession.set().setLogin("john").addComponentPermission(UserRole.USER, SUB_PROJECT_KEY, COMPONENT_KEY);
    ComponentDto project = newProject();

    ComponentDto file = ComponentTesting.newFileDto(project)
      .setId(10L)
      .setKey(COMPONENT_KEY)
      .setUuid(COMPONENT_UUID)
      .setName("Plugin.java")
      .setProjectUuid("THE_PROJECT")
      .setLongName("src/main/java/org/sonar/api/Plugin.java")
      .setPath("src/main/java/org/sonar/api/Plugin.java")
      .setParentProjectId(5L);
    when(componentDao.getNullableByUuid(session, COMPONENT_UUID)).thenReturn(file);
    when(componentDao.getById(5L, session)).thenReturn(new ComponentDto().setId(5L).setLongName("SonarQube :: Plugin API").setKey(SUB_PROJECT_KEY));
    when(componentDao.getByUuid(session, project.uuid())).thenReturn(project);
    when(propertiesDao.selectByQuery(any(PropertyQuery.class), eq(session))).thenReturn(newArrayList(new PropertyDto()));

    WsTester.TestRequest request = tester.newGetRequest("api/components", "app").setParam("uuid", COMPONENT_UUID);
    request.execute().assertJson(getClass(), "app.json");
  }

  @Test
  public void app_with_measures() throws Exception {
    MockUserSession.set().addComponentPermission(UserRole.USER, SUB_PROJECT_KEY, COMPONENT_KEY);
    ComponentDto project = newProject();
    newComponent(project);

    addMeasure(CoreMetrics.LINES_KEY, 200);
    addMeasure(CoreMetrics.COVERAGE_KEY, 95.4);
    addMeasure(CoreMetrics.DUPLICATED_LINES_DENSITY_KEY, 7.4);
    addMeasure(CoreMetrics.SQALE_RATING_KEY, "C");
    addMeasure(CoreMetrics.SQALE_DEBT_RATIO_KEY, 35d);

    measures.add(new MeasureDto().setComponentKey(COMPONENT_KEY).setMetricKey(CoreMetrics.TECHNICAL_DEBT_KEY).setValue(182.0));
    when(durations.format(any(Locale.class), any(Duration.class), eq(Durations.DurationFormat.SHORT))).thenReturn("3h 2min");

    WsTester.TestRequest request = tester.newGetRequest("api/components", "app").setParam("uuid", COMPONENT_UUID);
    request.execute().assertJson(getClass(), "app_with_measures.json");

    verify(measureDao).findByComponentKeyAndMetricKeys(eq(session), eq(COMPONENT_KEY), measureKeysCaptor.capture());
    assertThat(measureKeysCaptor.getValue()).contains(CoreMetrics.LINES_KEY, CoreMetrics.COVERAGE_KEY, CoreMetrics.DUPLICATED_LINES_DENSITY_KEY,
      CoreMetrics.TECHNICAL_DEBT_KEY);
  }

  @Test
  public void app_with_overall_measure() throws Exception {
    MockUserSession.set().addComponentPermission(UserRole.USER, SUB_PROJECT_KEY, COMPONENT_KEY);
    ComponentDto project = newProject();
    newComponent(project);

    addMeasure(CoreMetrics.OVERALL_COVERAGE_KEY, 90.1);
    addMeasure(CoreMetrics.COVERAGE_KEY, 95.4);
    addMeasure(CoreMetrics.IT_COVERAGE_KEY, 85.2);

    WsTester.TestRequest request = tester.newGetRequest("api/components", "app").setParam("uuid", COMPONENT_UUID);
    request.execute().assertJson(getClass(), "app_with_overall_measure.json");
  }

  @Test
  public void app_with_ut_measure() throws Exception {
    MockUserSession.set().addComponentPermission(UserRole.USER, SUB_PROJECT_KEY, COMPONENT_KEY);
    ComponentDto project = newProject();
    newComponent(project);

    addMeasure(CoreMetrics.COVERAGE_KEY, 95.4);
    addMeasure(CoreMetrics.IT_COVERAGE_KEY, 85.2);

    WsTester.TestRequest request = tester.newGetRequest("api/components", "app").setParam("uuid", COMPONENT_UUID);
    request.execute().assertJson(getClass(), "app_with_ut_measure.json");
  }

  @Test
  public void app_with_it_measure() throws Exception {
    MockUserSession.set().addComponentPermission(UserRole.USER, SUB_PROJECT_KEY, COMPONENT_KEY);
    ComponentDto project = newProject();
    newComponent(project);

    addMeasure(CoreMetrics.IT_COVERAGE_KEY, 85.2);

    WsTester.TestRequest request = tester.newGetRequest("api/components", "app").setParam("uuid", COMPONENT_UUID);
    request.execute().assertJson(getClass(), "app_with_it_measure.json");
  }

  @Test
  public void fail_on_unknown_component() throws Exception {
    MockUserSession.set().setLogin("john").addComponentPermission(UserRole.USER, SUB_PROJECT_KEY, COMPONENT_KEY);
    when(componentDao.getNullableByUuid(session, COMPONENT_UUID)).thenReturn(null);

    try {
      tester.newGetRequest("api/components", "app").setParam("uuid", COMPONENT_UUID).execute();
      fail();
    } catch (Exception e) {
      assertThat(e).isInstanceOf(NotFoundException.class).hasMessage("Component 'ABCDE' does not exist");
    }
  }

  private ComponentDto newProject() {
    return ComponentTesting.newProjectDto()
      .setId(1L)
      .setName("SonarQube")
      .setUuid(PROJECT_UUID)
      .setLongName("SonarQube")
      .setKey("org.codehaus.sonar:sonar");
  }

  private ComponentDto newComponent(ComponentDto project) {
    ComponentDto file = ComponentTesting.newFileDto(project)
      .setId(10L)
      .setQualifier("FIL")
      .setKey(COMPONENT_KEY)
      .setUuid(COMPONENT_UUID)
      .setProjectUuid(PROJECT_UUID)
      .setName("Plugin.java")
      .setLongName("src/main/java/org/sonar/api/Plugin.java")
      .setPath("src/main/java/org/sonar/api/Plugin.java")
      .setParentProjectId(5L);
    when(componentDao.getNullableByUuid(session, COMPONENT_UUID)).thenReturn(file);
    when(componentDao.getById(5L, session)).thenReturn(new ComponentDto().setId(5L).setLongName("SonarQube :: Plugin API").setKey(SUB_PROJECT_KEY));
    when(componentDao.getByUuid(session, project.uuid())).thenReturn(project);
    return file;
  }

  private void addMeasure(String metricKey, Integer value) {
    measures.add(new MeasureDto().setComponentKey(COMPONENT_KEY).setMetricKey(metricKey).setValue(value.doubleValue()));
    when(i18n.formatInteger(any(Locale.class), eq(value.intValue()))).thenReturn(Integer.toString(value));
  }

  private void addMeasure(String metricKey, Double value) {
    measures.add(new MeasureDto().setComponentKey(COMPONENT_KEY).setMetricKey(metricKey).setValue(value));
    when(i18n.formatDouble(any(Locale.class), eq(value))).thenReturn(Double.toString(value));
  }

  private void addMeasure(String metricKey, String value) {
    measures.add(new MeasureDto().setComponentKey(COMPONENT_KEY).setMetricKey(metricKey).setData(value));
  }

}
