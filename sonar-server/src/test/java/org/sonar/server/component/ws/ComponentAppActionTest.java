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
import com.google.common.collect.LinkedHashMultiset;
import com.google.common.collect.Multiset;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.sonar.api.i18n.I18n;
import org.sonar.api.measures.CoreMetrics;
import org.sonar.api.rule.RuleKey;
import org.sonar.api.utils.DateUtils;
import org.sonar.api.utils.Duration;
import org.sonar.api.utils.Durations;
import org.sonar.api.web.GwtPage;
import org.sonar.api.web.NavigationSection;
import org.sonar.api.web.Page;
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
import org.sonar.server.rule.Rule;
import org.sonar.server.rule.RuleService;
import org.sonar.server.rule.index.RuleQuery;
import org.sonar.server.search.QueryOptions;
import org.sonar.server.search.Result;
import org.sonar.server.ui.ViewProxy;
import org.sonar.server.ui.Views;
import org.sonar.server.user.MockUserSession;
import org.sonar.server.ws.WsTester;

import java.util.Date;
import java.util.List;
import java.util.Locale;

import static com.google.common.collect.Lists.newArrayList;
import static org.fest.assertions.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyListOf;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Matchers.isNull;
import static org.mockito.Mockito.*;

@RunWith(MockitoJUnitRunner.class)
public class ComponentAppActionTest {

  static final String SUB_PROJECT_KEY = "org.codehaus.sonar:sonar-plugin-api";
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
  Views views;

  @Mock
  RuleService ruleService;

  @Mock
  Periods periods;

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
    when(dbClient.resourceDao()).thenReturn(resourceDao);
    when(dbClient.propertiesDao()).thenReturn(propertiesDao);
    when(dbClient.measureDao()).thenReturn(measureDao);

    when(issueService.findSeveritiesByComponent(anyString(), any(Date.class), eq(session))).thenReturn(mock(Multiset.class));
    when(issueService.findRulesByComponent(anyString(), any(Date.class), eq(session))).thenReturn(mock(RulesAggregation.class));
    when(measureDao.findByComponentKeyAndMetricKeys(anyString(), anyListOf(String.class), eq(session))).thenReturn(measures);

    tester = new WsTester(new ComponentsWs(new ComponentAppAction(dbClient, issueService, views, ruleService, periods, durations, i18n)));
  }

  @Test
  public void app() throws Exception {
    MockUserSession.set().setLogin("john").addComponentPermission(UserRole.USER, SUB_PROJECT_KEY, COMPONENT_KEY);

    ComponentDto file = new ComponentDto().setId(10L).setQualifier("FIL").setKey(COMPONENT_KEY).setName("Plugin.java")
      .setLongName("src/main/java/org/sonar/api/Plugin.java")
      .setPath("src/main/java/org/sonar/api/Plugin.java").setSubProjectId(5L).setProjectId(1L);
    when(componentDao.getNullableByKey(session, COMPONENT_KEY)).thenReturn(file);
    when(componentDao.getById(5L, session)).thenReturn(new ComponentDto().setId(5L).setLongName("SonarQube :: Plugin API").setKey(SUB_PROJECT_KEY));
    when(componentDao.getById(1L, session)).thenReturn(new ComponentDto().setId(1L).setLongName("SonarQube").setKey("org.codehaus.sonar:sonar"));
    when(propertiesDao.selectByQuery(any(PropertyQuery.class), eq(session))).thenReturn(newArrayList(new PropertyDto()));

    WsTester.TestRequest request = tester.newGetRequest("api/components", "app").setParam("key", COMPONENT_KEY);
    request.execute().assertJson(getClass(), "app.json");
  }

  @Test
  public void app_without_sub_project() throws Exception {
    String componentKey = "org.codehaus.sonar:sonar";
    MockUserSession.set().setLogin("john").addComponentPermission(UserRole.USER, componentKey, componentKey);

    ComponentDto file = new ComponentDto().setId(1L).setQualifier("TRK").setKey(componentKey).setName("SonarQube").setProjectId(1L);
    when(componentDao.getNullableByKey(session, componentKey)).thenReturn(file);
    when(componentDao.getById(1L, session)).thenReturn(new ComponentDto().setId(1L).setLongName("SonarQube").setKey("org.codehaus.sonar:sonar"));
    when(propertiesDao.selectByQuery(any(PropertyQuery.class), eq(session))).thenReturn(newArrayList(new PropertyDto()));

    WsTester.TestRequest request = tester.newGetRequest("api/components", "app").setParam("key", componentKey);
    request.execute().assertJson(getClass(), "app_without_sub_project.json");
  }

  @Test
  public void app_with_sub_project_equals_to_project() throws Exception {
    MockUserSession.set().setLogin("john").addComponentPermission(UserRole.USER, SUB_PROJECT_KEY, COMPONENT_KEY);

    ComponentDto file = new ComponentDto().setId(10L).setQualifier("FIL").setKey(COMPONENT_KEY).setName("Plugin.java")
      .setPath("src/main/java/org/sonar/api/Plugin.java").setSubProjectId(1L).setProjectId(1L);
    when(componentDao.getNullableByKey(session, COMPONENT_KEY)).thenReturn(file);
    when(componentDao.getById(1L, session)).thenReturn(new ComponentDto().setId(1L).setLongName("SonarQube").setKey("org.codehaus.sonar:sonar"));
    when(propertiesDao.selectByQuery(any(PropertyQuery.class), eq(session))).thenReturn(newArrayList(new PropertyDto()));

    WsTester.TestRequest request = tester.newGetRequest("api/components", "app").setParam("key", COMPONENT_KEY);
    request.execute().assertJson(getClass(), "app_with_sub_project_equals_to_project.json");
  }

  @Test
  public void app_with_tabs() throws Exception {
    MockUserSession.set().addComponentPermission(UserRole.USER, SUB_PROJECT_KEY, COMPONENT_KEY);
    addComponent();

    addMeasure(CoreMetrics.COVERAGE_KEY, 1.0);
    addMeasure(CoreMetrics.DUPLICATED_LINES_KEY, 2);
    addMeasure(CoreMetrics.SCM_AUTHORS_BY_LINE_KEY, 3);

    WsTester.TestRequest request = tester.newGetRequest("api/components", "app").setParam("key", COMPONENT_KEY);
    request.execute().assertJson(getClass(), "app_with_tabs.json");
  }

  @Test
  public void app_with_measures() throws Exception {
    MockUserSession.set().addComponentPermission(UserRole.USER, SUB_PROJECT_KEY, COMPONENT_KEY);
    addComponent();

    addMeasure(CoreMetrics.NCLOC_KEY, 200);
    addMeasure(CoreMetrics.COVERAGE_KEY, 95.4);
    addMeasure(CoreMetrics.DUPLICATED_LINES_DENSITY_KEY, 7.4);

    measures.add(MeasureDto.createFor(MeasureKey.of(COMPONENT_KEY, CoreMetrics.TECHNICAL_DEBT_KEY)).setValue(182.0));
    when(durations.format(any(Locale.class), any(Duration.class), eq(Durations.DurationFormat.SHORT))).thenReturn("3h 2min");

    WsTester.TestRequest request = tester.newGetRequest("api/components", "app").setParam("key", COMPONENT_KEY);
    request.execute().assertJson(getClass(), "app_with_measures.json");

    verify(measureDao).findByComponentKeyAndMetricKeys(eq(COMPONENT_KEY), measureKeysCaptor.capture(), eq(session));
    assertThat(measureKeysCaptor.getValue()).contains(CoreMetrics.NCLOC_KEY, CoreMetrics.COVERAGE_KEY, CoreMetrics.DUPLICATED_LINES_DENSITY_KEY,
      CoreMetrics.TECHNICAL_DEBT_KEY);
  }

  @Test
  public void app_with_measures_when_period_is_set() throws Exception {
    MockUserSession.set().addComponentPermission(UserRole.USER, SUB_PROJECT_KEY, COMPONENT_KEY);
    addComponent();
    addPeriod();

    addVariationMeasure(CoreMetrics.NCLOC_KEY, 2, 1);
    addVariationMeasure(CoreMetrics.COVERAGE_KEY, 5d, 1);
    addVariationMeasure(CoreMetrics.DUPLICATED_LINES_DENSITY_KEY, 1.2, 1);

    measures.add(MeasureDto.createFor(MeasureKey.of(COMPONENT_KEY, CoreMetrics.TECHNICAL_DEBT_KEY)).setVariation(1, 10.0));
    when(durations.format(any(Locale.class), any(Duration.class), eq(Durations.DurationFormat.SHORT))).thenReturn("10min");

    WsTester.TestRequest request = tester.newGetRequest("api/components", "app").setParam("key", COMPONENT_KEY).setParam("period", "1");
    request.execute().assertJson(getClass(), "app_with_measures_when_period_is_set.json");

    verify(measureDao).findByComponentKeyAndMetricKeys(eq(COMPONENT_KEY), measureKeysCaptor.capture(), eq(session));
    assertThat(measureKeysCaptor.getValue()).contains(CoreMetrics.NCLOC_KEY, CoreMetrics.COVERAGE_KEY, CoreMetrics.DUPLICATED_LINES_DENSITY_KEY,
      CoreMetrics.TECHNICAL_DEBT_KEY);
  }

  @Test
  public void app_with_issues_measures() throws Exception {
    MockUserSession.set().addComponentPermission(UserRole.USER, SUB_PROJECT_KEY, COMPONENT_KEY);
    addComponent();

    Multiset<String> severities = LinkedHashMultiset.create();
    severities.add("BLOCKER", 1);
    severities.add("CRITICAL", 2);
    severities.add("MAJOR", 5);
    severities.add("MINOR", 4);
    severities.add("INFO", 2);
    when(issueService.findSeveritiesByComponent(COMPONENT_KEY, null, session)).thenReturn(severities);
    when(i18n.message(any(Locale.class), eq("severity.BLOCKER"), isNull(String.class))).thenReturn("Blocker");
    when(i18n.message(any(Locale.class), eq("severity.CRITICAL"), isNull(String.class))).thenReturn("Critical");
    when(i18n.message(any(Locale.class), eq("severity.MAJOR"), isNull(String.class))).thenReturn("Major");
    when(i18n.message(any(Locale.class), eq("severity.MINOR"), isNull(String.class))).thenReturn("Minor");
    when(i18n.message(any(Locale.class), eq("severity.INFO"), isNull(String.class))).thenReturn("Info");

    when(i18n.formatInteger(any(Locale.class), eq(14))).thenReturn("14");
    when(i18n.formatInteger(any(Locale.class), eq(1))).thenReturn("1");
    when(i18n.formatInteger(any(Locale.class), eq(2))).thenReturn("2");
    when(i18n.formatInteger(any(Locale.class), eq(5))).thenReturn("5");
    when(i18n.formatInteger(any(Locale.class), eq(4))).thenReturn("4");

    WsTester.TestRequest request = tester.newGetRequest("api/components", "app").setParam("key", COMPONENT_KEY);
    request.execute().assertJson(getClass(), "app_with_issues_measures.json");
  }

  @Test
  public void app_with_issues_measures_when_period_is_set() throws Exception {
    MockUserSession.set().addComponentPermission(UserRole.USER, SUB_PROJECT_KEY, COMPONENT_KEY);
    addComponent();
    addPeriod();

    Multiset<String> severities = LinkedHashMultiset.create();
    severities.add("BLOCKER", 1);
    severities.add("CRITICAL", 2);
    severities.add("MAJOR", 5);
    severities.add("MINOR", 4);
    severities.add("INFO", 2);
    when(issueService.findSeveritiesByComponent(eq(COMPONENT_KEY), any(Date.class), eq(session))).thenReturn(severities);

    when(i18n.message(any(Locale.class), eq("severity.BLOCKER"), isNull(String.class))).thenReturn("Blocker");
    when(i18n.message(any(Locale.class), eq("severity.CRITICAL"), isNull(String.class))).thenReturn("Critical");
    when(i18n.message(any(Locale.class), eq("severity.MAJOR"), isNull(String.class))).thenReturn("Major");
    when(i18n.message(any(Locale.class), eq("severity.MINOR"), isNull(String.class))).thenReturn("Minor");
    when(i18n.message(any(Locale.class), eq("severity.INFO"), isNull(String.class))).thenReturn("Info");

    when(i18n.formatInteger(any(Locale.class), eq(14))).thenReturn("14");
    when(i18n.formatInteger(any(Locale.class), eq(1))).thenReturn("1");
    when(i18n.formatInteger(any(Locale.class), eq(2))).thenReturn("2");
    when(i18n.formatInteger(any(Locale.class), eq(5))).thenReturn("5");
    when(i18n.formatInteger(any(Locale.class), eq(4))).thenReturn("4");

    WsTester.TestRequest request = tester.newGetRequest("api/components", "app").setParam("key", COMPONENT_KEY).setParam("period", "1");
    request.execute().assertJson(getClass(), "app_with_issues_measures_when_period_is_set.json");
  }

  @Test
  public void app_with_tests_measure() throws Exception {
    String componentKey = "org.codehaus.sonar:sonar-server:src/test/java/org/sonar/server/issue/PlanActionTest.java";
    MockUserSession.set().addComponentPermission(UserRole.USER, SUB_PROJECT_KEY, componentKey);

    ComponentDto file = new ComponentDto().setId(10L).setQualifier("UTS").setKey(componentKey).setName("PlanActionTest.java")
      .setPath("src/test/java/org/sonar/server/issue/PlanActionTest.java").setSubProjectId(5L).setProjectId(1L);
    when(componentDao.getNullableByKey(session, componentKey)).thenReturn(file);
    when(componentDao.getById(5L, session)).thenReturn(new ComponentDto().setId(5L).setLongName("SonarQube :: Plugin API").setKey(SUB_PROJECT_KEY));
    when(componentDao.getById(1L, session)).thenReturn(new ComponentDto().setId(1L).setLongName("SonarQube").setKey("org.codehaus.sonar:sonar"));

    addMeasure(CoreMetrics.TESTS_KEY, 10);

    WsTester.TestRequest request = tester.newGetRequest("api/components", "app").setParam("key", componentKey);
    request.execute().assertJson(getClass(), "app_with_tests_measure.json");

    verify(measureDao).findByComponentKeyAndMetricKeys(eq(componentKey), measureKeysCaptor.capture(), eq(session));
    assertThat(measureKeysCaptor.getValue()).contains(CoreMetrics.TESTS_KEY);
  }

  @Test
  public void app_with_periods() throws Exception {
    MockUserSession.set().addComponentPermission(UserRole.USER, SUB_PROJECT_KEY, COMPONENT_KEY);

    addComponent();

    when(resourceDao.getLastSnapshotByResourceId(eq(1L), eq(session))).thenReturn(
      new SnapshotDto().setPeriod1Mode("previous_analysis").setPeriod1Date(DateUtils.parseDate("2014-05-08"))
    );
    when(periods.label(anyString(), anyString(), any(Date.class))).thenReturn("since previous analysis (May 08 2014)");

    WsTester.TestRequest request = tester.newGetRequest("api/components", "app").setParam("key", COMPONENT_KEY);
    request.execute().assertJson(getClass(), "app_with_periods.json");
  }

  @Test
  public void app_with_severities() throws Exception {
    MockUserSession.set().addComponentPermission(UserRole.USER, SUB_PROJECT_KEY, COMPONENT_KEY);

    addComponent();

    Multiset<String> severities = HashMultiset.create();
    severities.add("MAJOR", 5);
    when(issueService.findSeveritiesByComponent(COMPONENT_KEY, null, session)).thenReturn(severities);
    when(i18n.message(any(Locale.class), eq("severity.MAJOR"), isNull(String.class))).thenReturn("Major");

    WsTester.TestRequest request = tester.newGetRequest("api/components", "app").setParam("key", COMPONENT_KEY);
    request.execute().assertJson(getClass(), "app_with_severities.json");
  }

  @Test
  public void app_with_severities_when_period_is_set() throws Exception {
    MockUserSession.set().addComponentPermission(UserRole.USER, SUB_PROJECT_KEY, COMPONENT_KEY);
    addComponent();
    addPeriod();

    Multiset<String> severities = HashMultiset.create();
    severities.add("MAJOR", 5);
    when(issueService.findSeveritiesByComponent(eq(COMPONENT_KEY), any(Date.class), eq(session))).thenReturn(severities);
    when(i18n.message(any(Locale.class), eq("severity.MAJOR"), isNull(String.class))).thenReturn("Major");

    WsTester.TestRequest request = tester.newGetRequest("api/components", "app").setParam("key", COMPONENT_KEY).setParam("period", "1");
    request.execute().assertJson(getClass(), "app_with_severities_when_period_is_set.json");
  }

  @Test
  public void app_with_rules() throws Exception {
    MockUserSession.set().addComponentPermission(UserRole.USER, SUB_PROJECT_KEY, COMPONENT_KEY);

    addComponent();
    when(issueService.findRulesByComponent(COMPONENT_KEY, null, session)).thenReturn(
      new RulesAggregation().add(new RuleDto().setRuleKey("AvoidCycle").setRepositoryKey("squid").setName("Avoid Cycle"))
    );

    WsTester.TestRequest request = tester.newGetRequest("api/components", "app").setParam("key", COMPONENT_KEY);
    request.execute().assertJson(getClass(), "app_with_rules.json");
  }

  @Test
  public void app_with_rules_when_period_is_set() throws Exception {
    MockUserSession.set().addComponentPermission(UserRole.USER, SUB_PROJECT_KEY, COMPONENT_KEY);

    addComponent();

    Date periodDate = DateUtils.parseDate("2014-05-08");
    when(resourceDao.getLastSnapshotByResourceId(eq(1L), eq(session))).thenReturn(
      new SnapshotDto().setPeriod1Mode("previous_analysis").setPeriod1Date(periodDate)
    );
    when(periods.label(anyString(), anyString(), any(Date.class))).thenReturn("since previous analysis (May 08 2014)");

    when(issueService.findRulesByComponent(COMPONENT_KEY, periodDate, session)).thenReturn(
      new RulesAggregation().add(new RuleDto().setRuleKey("AvoidCycle").setRepositoryKey("squid").setName("Avoid Cycle"))
    );

    WsTester.TestRequest request = tester.newGetRequest("api/components", "app").setParam("key", COMPONENT_KEY).setParam("period", "1");
    request.execute().assertJson(getClass(), "app_with_rules_when_period_is_set.json");
  }

  @Test
  public void app_with_extension() throws Exception {
    MockUserSession.set().addComponentPermission(UserRole.USER, SUB_PROJECT_KEY, COMPONENT_KEY);
    addComponent();

    when(views.getPages(anyString(), anyString(), anyString(), anyString(), any(String[].class))).thenReturn(
      // Issues extension and MyGwtExtension will be ignore
      newArrayList(new ViewProxy<Page>(new MyExtension()), new ViewProxy<Page>(new MyGwtExtension()), new ViewProxy<Page>(new IssuesExtension())));

    WsTester.TestRequest request = tester.newGetRequest("api/components", "app").setParam("key", COMPONENT_KEY);
    request.execute().assertJson(getClass(), "app_with_extension.json");
  }

  @Test
  public void app_with_extension_having_permission() throws Exception {
    MockUserSession.set().addComponentPermission(UserRole.USER, SUB_PROJECT_KEY, COMPONENT_KEY);
    addComponent();

    when(views.getPages(anyString(), anyString(), anyString(), anyString(), any(String[].class))).thenReturn(
      newArrayList(new ViewProxy<Page>(new MyExtensionWithRole())));

    WsTester.TestRequest request = tester.newGetRequest("api/components", "app").setParam("key", COMPONENT_KEY);
    request.execute().assertJson(getClass(), "app_with_extension_having_permission.json");
  }

  @Test
  public void app_with_manual_rules() throws Exception {
    MockUserSession.set().addComponentPermission(UserRole.USER, SUB_PROJECT_KEY, COMPONENT_KEY);
    addComponent();

    Result<Rule> result = mock(Result.class);
    Rule rule = mock(Rule.class);
    when(rule.key()).thenReturn(RuleKey.of("manual", "API"));
    when(rule.name()).thenReturn("API");
    when(result.getHits()).thenReturn(newArrayList(rule));
    when(ruleService.search(any(RuleQuery.class), any(QueryOptions.class))).thenReturn(result);

    WsTester.TestRequest request = tester.newGetRequest("api/components", "app").setParam("key", COMPONENT_KEY);
    request.execute().assertJson(getClass(), "app_with_manual_rules.json");
  }

  private void addComponent() {
    ComponentDto file = new ComponentDto().setId(10L).setQualifier("FIL").setKey(COMPONENT_KEY).setName("Plugin.java")
      .setLongName("src/main/java/org/sonar/api/Plugin.java")
      .setPath("src/main/java/org/sonar/api/Plugin.java").setSubProjectId(5L).setProjectId(1L);
    when(componentDao.getNullableByKey(session, COMPONENT_KEY)).thenReturn(file);
    when(componentDao.getById(5L, session)).thenReturn(new ComponentDto().setId(5L).setLongName("SonarQube :: Plugin API").setKey(SUB_PROJECT_KEY));
    when(componentDao.getById(1L, session)).thenReturn(new ComponentDto().setId(1L).setLongName("SonarQube").setKey("org.codehaus.sonar:sonar"));
  }

  private void addPeriod(){
    Date periodDate = DateUtils.parseDate("2014-05-08");
    when(resourceDao.getLastSnapshotByResourceId(eq(1L), eq(session))).thenReturn(
      new SnapshotDto().setPeriod1Mode("previous_analysis").setPeriod1Date(periodDate)
    );
    when(periods.label(anyString(), anyString(), any(Date.class))).thenReturn("since previous analysis (May 08 2014)");
  }

  private void addMeasure(String metricKey, Integer value) {
    measures.add(MeasureDto.createFor(MeasureKey.of(COMPONENT_KEY, metricKey)).setValue(value.doubleValue()));
    when(i18n.formatInteger(any(Locale.class), eq(value.intValue()))).thenReturn(Integer.toString(value));
  }

  private void addMeasure(String metricKey, Double value) {
    measures.add(MeasureDto.createFor(MeasureKey.of(COMPONENT_KEY, metricKey)).setValue(value));
    when(i18n.formatDouble(any(Locale.class), eq(value))).thenReturn(Double.toString(value));
  }

  private void addVariationMeasure(String metricKey, Integer value, Integer periodIndex) {
    measures.add(MeasureDto.createFor(MeasureKey.of(COMPONENT_KEY, metricKey)).setVariation(periodIndex, value.doubleValue()));
    when(i18n.formatInteger(any(Locale.class), eq(value))).thenReturn(Integer.toString(value));
  }

  private void addVariationMeasure(String metricKey, Double value, Integer periodIndex) {
    measures.add(MeasureDto.createFor(MeasureKey.of(COMPONENT_KEY, metricKey)).setVariation(periodIndex, value));
    when(i18n.formatDouble(any(Locale.class), eq(value))).thenReturn(Double.toString(value));
  }

  @NavigationSection(NavigationSection.RESOURCE_TAB)
  private static class MyExtension implements Page {
    public String getId() {
      return "my-extension";
    }

    public String getTitle() {
      return "My extension";
    }
  }

  @NavigationSection(NavigationSection.RESOURCE_TAB)
  @UserRole(UserRole.USER)
  private static class MyExtensionWithRole implements Page {
    public String getId() {
      return "my-extension-with-permission";
    }

    public String getTitle() {
      return "My extension with permission";
    }
  }

  @NavigationSection(NavigationSection.RESOURCE_TAB)
  private static class MyGwtExtension extends GwtPage {
    public String getTitle() {
      return "My GWT extension";
    }

    @Override
    public String getGwtId() {
      return "my-gwt-extension";
    }
  }

  @NavigationSection(NavigationSection.RESOURCE_TAB)
  private static class IssuesExtension implements Page {
    public String getId() {
      return "issues";
    }

    public String getTitle() {
      return "Issues";
    }
  }

}
