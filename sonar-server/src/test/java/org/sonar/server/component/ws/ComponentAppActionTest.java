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

import com.google.common.collect.HashMultiset;
import com.google.common.collect.Multiset;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.sonar.api.i18n.I18n;
import org.sonar.api.measures.CoreMetrics;
import org.sonar.api.utils.DateUtils;
import org.sonar.api.utils.Duration;
import org.sonar.api.utils.Durations;
import org.sonar.api.web.UserRole;
import org.sonar.core.component.ComponentDto;
import org.sonar.core.measure.db.MeasureDto;
import org.sonar.core.measure.db.MeasureKey;
import org.sonar.core.persistence.DbSession;
import org.sonar.core.properties.PropertiesDao;
import org.sonar.core.properties.PropertyDto;
import org.sonar.core.properties.PropertyQuery;
import org.sonar.core.resource.ResourceDao;
import org.sonar.core.resource.SnapshotDto;
import org.sonar.core.rule.RuleDto;
import org.sonar.core.timemachine.Periods;
import org.sonar.server.component.persistence.ComponentDao;
import org.sonar.server.db.DbClient;
import org.sonar.server.issue.IssueService;
import org.sonar.server.issue.RulesAggregation;
import org.sonar.server.measure.persistence.MeasureDao;
import org.sonar.server.source.SourceService;
import org.sonar.server.user.MockUserSession;
import org.sonar.server.ws.WsTester;

import java.util.Date;
import java.util.List;
import java.util.Locale;

import static com.google.common.collect.Lists.newArrayList;
import static org.mockito.Matchers.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class ComponentAppActionTest {

  static final String PROJECT_KEY = "org.codehaus.sonar:sonar-plugin-api:api";
  static final String COMPONENT_KEY = "org.codehaus.sonar:sonar-plugin-api:src/main/java/org/sonar/api/Plugin.java";

  @Mock
  DbSession session;

  @Mock
  ComponentDao componentDao;

  @Mock
  ResourceDao resourceDao;

  @Mock
  PropertiesDao propertiesDao;

  @Mock
  MeasureDao measureDao;

  @Mock
  IssueService issueService;

  @Mock
  SourceService sourceService;

  @Mock
  Periods periods;

  @Mock
  Durations durations;

  @Mock
  I18n i18n;

  List<MeasureDto> measures = newArrayList();

  WsTester tester;

  @Before
  public void setUp() throws Exception {
    DbClient dbClient = mock(DbClient.class);
    when(dbClient.openSession(false)).thenReturn(session);
    when(dbClient.getDao(ComponentDao.class)).thenReturn(componentDao);
    when(dbClient.getDao(ResourceDao.class)).thenReturn(resourceDao);
    when(dbClient.getDao(PropertiesDao.class)).thenReturn(propertiesDao);
    when(dbClient.getDao(MeasureDao.class)).thenReturn(measureDao);

    when(issueService.findSeveritiesByComponent(anyString(), eq(session))).thenReturn(mock(Multiset.class));
    when(issueService.findRulesByComponent(anyString(), eq(session))).thenReturn(mock(RulesAggregation.class));
    when(measureDao.findByComponentKeyAndMetricKeys(anyString(), anyListOf(String.class), eq(session))).thenReturn(measures);

    tester = new WsTester(new ComponentsWs(new ComponentAppAction(dbClient, issueService, sourceService, periods, durations, i18n)));
  }

  @Test
  public void app() throws Exception {
    MockUserSession.set().setLogin("john").addComponentPermission(UserRole.CODEVIEWER, PROJECT_KEY, COMPONENT_KEY);

    ComponentDto file = new ComponentDto().setId(10L).setQualifier("FIL").setKey(COMPONENT_KEY).setName("Plugin.java")
      .setPath("src/main/java/org/sonar/api/Plugin.java").setSubProjectId(5L).setProjectId(1L);
    when(componentDao.getByKey(COMPONENT_KEY, session)).thenReturn(file);
    when(componentDao.getById(5L, session)).thenReturn(new ComponentDto().setId(5L).setLongName("SonarQube :: Plugin API"));
    when(componentDao.getById(1L, session)).thenReturn(new ComponentDto().setId(1L).setLongName("SonarQube"));
    when(propertiesDao.selectByQuery(any(PropertyQuery.class), eq(session))).thenReturn(newArrayList(new PropertyDto()));
    when(sourceService.hasScmData(eq(COMPONENT_KEY), eq(session))).thenReturn(true);

    WsTester.TestRequest request = tester.newGetRequest("api/components", "app").setParam("key", COMPONENT_KEY);
    request.execute().assertJson(getClass(), "app.json");
  }

  @Test
  public void app_without_sub_project() throws Exception {
    String componentKey = "org.codehaus.sonar:sonar";
    MockUserSession.set().setLogin("john").addComponentPermission(UserRole.CODEVIEWER, componentKey, componentKey);

    ComponentDto file = new ComponentDto().setId(1L).setQualifier("TRK").setKey(componentKey).setName("SonarQube").setProjectId(1L);
    when(componentDao.getByKey(componentKey, session)).thenReturn(file);
    when(componentDao.getById(1L, session)).thenReturn(new ComponentDto().setId(1L).setLongName("SonarQube"));
    when(propertiesDao.selectByQuery(any(PropertyQuery.class), eq(session))).thenReturn(newArrayList(new PropertyDto()));

    WsTester.TestRequest request = tester.newGetRequest("api/components", "app").setParam("key", componentKey);
    request.execute().assertJson(getClass(), "app_without_sub_project.json");
  }

  @Test
  public void app_with_sub_project_equals_to_project() throws Exception {
    MockUserSession.set().setLogin("john").addComponentPermission(UserRole.CODEVIEWER, PROJECT_KEY, COMPONENT_KEY);

    ComponentDto file = new ComponentDto().setId(10L).setQualifier("FIL").setKey(COMPONENT_KEY).setName("Plugin.java")
      .setPath("src/main/java/org/sonar/api/Plugin.java").setSubProjectId(1L).setProjectId(1L);
    when(componentDao.getByKey(COMPONENT_KEY, session)).thenReturn(file);
    when(componentDao.getById(1L, session)).thenReturn(new ComponentDto().setId(1L).setLongName("SonarQube"));
    when(propertiesDao.selectByQuery(any(PropertyQuery.class), eq(session))).thenReturn(newArrayList(new PropertyDto()));
    when(sourceService.hasScmData(eq(COMPONENT_KEY), eq(session))).thenReturn(true);

    WsTester.TestRequest request = tester.newGetRequest("api/components", "app").setParam("key", COMPONENT_KEY);
    request.execute().assertJson(getClass(), "app_with_sub_project_equals_to_project.json");
  }

  @Test
  public void app_with_measures() throws Exception {
    MockUserSession.set().addComponentPermission(UserRole.CODEVIEWER, PROJECT_KEY, COMPONENT_KEY);

    addProjectSample();

    addMeasure(CoreMetrics.NCLOC_KEY, 200);
    addMeasure(CoreMetrics.COVERAGE_KEY, 95.4);
    addMeasure(CoreMetrics.DUPLICATED_LINES_DENSITY_KEY, 7.4);
    addMeasure(CoreMetrics.VIOLATIONS_KEY, 14);
    addMeasure(CoreMetrics.BLOCKER_VIOLATIONS_KEY, 1);
    addMeasure(CoreMetrics.CRITICAL_VIOLATIONS_KEY, 2);
    addMeasure(CoreMetrics.MAJOR_VIOLATIONS_KEY, 5);
    addMeasure(CoreMetrics.MINOR_VIOLATIONS_KEY, 4);
    addMeasure(CoreMetrics.INFO_VIOLATIONS_KEY, 2);

    measures.add(MeasureDto.createFor(MeasureKey.of(COMPONENT_KEY, CoreMetrics.TECHNICAL_DEBT_KEY)).setValue(182.0));
    when(durations.format(any(Locale.class), any(Duration.class), eq(Durations.DurationFormat.SHORT))).thenReturn("3h 2min");

    WsTester.TestRequest request = tester.newGetRequest("api/components", "app").setParam("key", COMPONENT_KEY);
    request.execute().assertJson(getClass(), "app_with_measures.json");
  }

  @Test
  public void app_with_periods() throws Exception {
    MockUserSession.set().addComponentPermission(UserRole.CODEVIEWER, PROJECT_KEY, COMPONENT_KEY);

    addProjectSample();

    when(resourceDao.getLastSnapshotByResourceId(eq(1L), eq(session))).thenReturn(
      new SnapshotDto().setPeriod1Mode("previous_analysis").setPeriod1Date(DateUtils.parseDate("2014-05-08"))
    );
    when(periods.label(anyString(), anyString(), any(Date.class))).thenReturn("since previous analysis (May 08 2014)");

    WsTester.TestRequest request = tester.newGetRequest("api/components", "app").setParam("key", COMPONENT_KEY);
    request.execute().assertJson(getClass(), "app_with_periods.json");
  }

  @Test
  public void app_with_severities() throws Exception {
    MockUserSession.set().addComponentPermission(UserRole.CODEVIEWER, PROJECT_KEY, COMPONENT_KEY);

    addProjectSample();

    Multiset<String> severities = HashMultiset.create();
    severities.add("MAJOR", 5);
    when(issueService.findSeveritiesByComponent(COMPONENT_KEY, session)).thenReturn(severities);
    when(i18n.message(any(Locale.class), eq("severity.MAJOR"), isNull(String.class))).thenReturn("Major");

    WsTester.TestRequest request = tester.newGetRequest("api/components", "app").setParam("key", COMPONENT_KEY);
    request.execute().assertJson(getClass(), "app_with_severities.json");
  }

  @Test
  public void app_with_rules() throws Exception {
    MockUserSession.set().addComponentPermission(UserRole.CODEVIEWER, PROJECT_KEY, COMPONENT_KEY);

    addProjectSample();
    when(issueService.findRulesByComponent(COMPONENT_KEY, session)).thenReturn(
      new RulesAggregation().add(new RuleDto().setRuleKey("AvoidCycle").setRepositoryKey("squid").setName("Avoid Cycle"))
    );

    WsTester.TestRequest request = tester.newGetRequest("api/components", "app").setParam("key", COMPONENT_KEY);
    request.execute().assertJson(getClass(), "app_with_rules.json");
  }

  private void addProjectSample() {
    ComponentDto file = new ComponentDto().setId(10L).setQualifier("FIL").setKey(COMPONENT_KEY).setName("Plugin.java")
      .setPath("src/main/java/org/sonar/api/Plugin.java").setSubProjectId(5L).setProjectId(1L);
    when(componentDao.getByKey(COMPONENT_KEY, session)).thenReturn(file);
    when(componentDao.getById(5L, session)).thenReturn(new ComponentDto().setId(5L).setLongName("SonarQube :: Plugin API"));
    when(componentDao.getById(1L, session)).thenReturn(new ComponentDto().setId(1L).setLongName("SonarQube"));
  }

  private void addMeasure(String metricKey, Integer value) {
    measures.add(MeasureDto.createFor(MeasureKey.of(COMPONENT_KEY, metricKey)).setValue(value.doubleValue()));
    when(i18n.formatInteger(any(Locale.class), eq(value.intValue()))).thenReturn(Integer.toString(value));
  }

  private void addMeasure(String metricKey, Double value) {
    measures.add(MeasureDto.createFor(MeasureKey.of(COMPONENT_KEY, metricKey)).setValue(value));
    when(i18n.formatDouble(any(Locale.class), eq(value))).thenReturn(Double.toString(value));
  }

}
