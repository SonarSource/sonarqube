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
import org.sonar.core.measure.db.MeasureDao;
import org.sonar.core.measure.db.MeasureDto;
import org.sonar.core.properties.PropertiesDao;
import org.sonar.core.properties.PropertyDto;
import org.sonar.core.properties.PropertyQuery;
import org.sonar.core.resource.ResourceDao;
import org.sonar.core.resource.SnapshotDto;
import org.sonar.core.rule.RuleDto;
import org.sonar.core.timemachine.Periods;
import org.sonar.server.issue.IssueService;
import org.sonar.server.issue.RulesAggregation;
import org.sonar.server.user.MockUserSession;
import org.sonar.server.ws.WsTester;

import java.util.Date;
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
  ResourceDao resourceDao;

  @Mock
  MeasureDao measureDao;

  @Mock
  PropertiesDao propertiesDao;

  @Mock
  IssueService issueService;

  @Mock
  Periods periods;

  @Mock
  Durations durations;

  @Mock
  I18n i18n;

  WsTester tester;

  @Before
  public void setUp() throws Exception {
    when(issueService.findSeveritiesByComponent(COMPONENT_KEY)).thenReturn(mock(Multiset.class));
    when(issueService.findRulesByComponent(COMPONENT_KEY)).thenReturn(mock(RulesAggregation.class));

    tester = new WsTester(new ComponentsWs(new ComponentAppAction(resourceDao, measureDao, propertiesDao, issueService, periods, durations, i18n)));
  }

  @Test
  public void app() throws Exception {
    MockUserSession.set().addProjectPermissions(UserRole.CODEVIEWER, PROJECT_KEY).addComponent(COMPONENT_KEY, PROJECT_KEY);

    ComponentDto file = new ComponentDto().setId(10L).setQualifier("FIL").setKey(COMPONENT_KEY).setName("Plugin.java")
      .setPath("src/main/java/org/sonar/api/Plugin.java").setSubProjectId(5L).setProjectId(1L);
    when(resourceDao.selectComponentByKey(COMPONENT_KEY)).thenReturn(file);
    when(resourceDao.findById(5L)).thenReturn(new ComponentDto().setId(5L).setLongName("SonarQube :: Plugin API"));
    when(resourceDao.findById(1L)).thenReturn(new ComponentDto().setId(1L).setLongName("SonarQube"));
    when(propertiesDao.selectByQuery(any(PropertyQuery.class))).thenReturn(newArrayList(new PropertyDto()));

    WsTester.TestRequest request = tester.newGetRequest("api/components", "app").setParam("key", COMPONENT_KEY);
    request.execute().assertJson(getClass(), "app.json");
  }

  @Test
  public void app_with_measures() throws Exception {
    MockUserSession.set().addProjectPermissions(UserRole.CODEVIEWER, PROJECT_KEY).addComponent(COMPONENT_KEY, PROJECT_KEY);

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

    when(measureDao.findByComponentKeyAndMetricKey(COMPONENT_KEY, CoreMetrics.TECHNICAL_DEBT_KEY)).thenReturn(new MeasureDto().setValue(182.0));
    when(durations.format(any(Locale.class), any(Duration.class), eq(Durations.DurationFormat.SHORT))).thenReturn("3h 2min");

    WsTester.TestRequest request = tester.newGetRequest("api/components", "app").setParam("key", COMPONENT_KEY);
    request.execute().assertJson(getClass(), "app_with_measures.json");
  }

  @Test
  public void app_with_periods() throws Exception {
    MockUserSession.set().addProjectPermissions(UserRole.CODEVIEWER, PROJECT_KEY).addComponent(COMPONENT_KEY, PROJECT_KEY);

    addProjectSample();

    when(resourceDao.getLastSnapshotByResourceId(eq(1L))).thenReturn(
      new SnapshotDto().setPeriod1Mode("previous_analysis").setPeriod1Date(DateUtils.parseDate("2014-05-08"))
    );
    when(periods.label(anyString(), anyString(), any(Date.class))).thenReturn("since previous analysis (May 08 2014)");

    WsTester.TestRequest request = tester.newGetRequest("api/components", "app").setParam("key", COMPONENT_KEY);
    request.execute().assertJson(getClass(), "app_with_periods.json");
  }

  @Test
  public void app_with_severities() throws Exception {
    MockUserSession.set().addProjectPermissions(UserRole.CODEVIEWER, PROJECT_KEY).addComponent(COMPONENT_KEY, PROJECT_KEY);

    addProjectSample();

    Multiset<String> severities = HashMultiset.create();
    severities.add("MAJOR", 5);
    when(issueService.findSeveritiesByComponent(COMPONENT_KEY)).thenReturn(severities);
    when(i18n.message(any(Locale.class), eq("severity.MAJOR"), isNull(String.class))).thenReturn("Major");

    WsTester.TestRequest request = tester.newGetRequest("api/components", "app").setParam("key", COMPONENT_KEY);
    request.execute().assertJson(getClass(), "app_with_severities.json");
  }

  @Test
  public void app_with_rules() throws Exception {
    MockUserSession.set().addProjectPermissions(UserRole.CODEVIEWER, PROJECT_KEY).addComponent(COMPONENT_KEY, PROJECT_KEY);

    addProjectSample();
    when(issueService.findRulesByComponent(COMPONENT_KEY)).thenReturn(
      new RulesAggregation().add(new RuleDto().setRuleKey("AvoidCycle").setRepositoryKey("squid").setName("Avoid Cycle"))
    );

    WsTester.TestRequest request = tester.newGetRequest("api/components", "app").setParam("key", COMPONENT_KEY);
    request.execute().assertJson(getClass(), "app_with_rules.json");
  }

  private void addProjectSample() {
    ComponentDto file = new ComponentDto().setId(10L).setQualifier("FIL").setKey(COMPONENT_KEY).setName("Plugin.java")
      .setPath("src/main/java/org/sonar/api/Plugin.java").setSubProjectId(5L).setProjectId(1L);
    when(resourceDao.selectComponentByKey(COMPONENT_KEY)).thenReturn(file);
    when(resourceDao.findById(5L)).thenReturn(new ComponentDto().setId(5L).setLongName("SonarQube :: Plugin API"));
    when(resourceDao.findById(1L)).thenReturn(new ComponentDto().setId(1L).setLongName("SonarQube"));

  }

  private void addMeasure(String metricKey, Integer value) {
    when(measureDao.findByComponentKeyAndMetricKey(COMPONENT_KEY, metricKey)).thenReturn(new MeasureDto().setValue(value.doubleValue()));
    when(i18n.formatInteger(any(Locale.class), eq(value.intValue()))).thenReturn(Integer.toString(value));
  }

  private void addMeasure(String metricKey, Double value) {
    when(measureDao.findByComponentKeyAndMetricKey(COMPONENT_KEY, metricKey)).thenReturn(new MeasureDto().setValue(value));
    when(i18n.formatDouble(any(Locale.class), eq(value))).thenReturn(Double.toString(value));
  }

}
