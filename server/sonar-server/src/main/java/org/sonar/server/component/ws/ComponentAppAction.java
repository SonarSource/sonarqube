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

import com.google.common.base.Predicate;
import com.google.common.collect.Iterables;
import com.google.common.collect.Multiset;
import com.google.common.io.Resources;
import org.apache.commons.lang.StringUtils;
import org.sonar.api.component.Component;
import org.sonar.api.i18n.I18n;
import org.sonar.api.measures.CoreMetrics;
import org.sonar.api.measures.Metric;
import org.sonar.api.server.ws.Request;
import org.sonar.api.server.ws.RequestHandler;
import org.sonar.api.server.ws.Response;
import org.sonar.api.server.ws.WebService;
import org.sonar.api.utils.DateUtils;
import org.sonar.api.utils.Durations;
import org.sonar.api.utils.text.JsonWriter;
import org.sonar.api.web.NavigationSection;
import org.sonar.api.web.Page;
import org.sonar.api.web.UserRole;
import org.sonar.core.component.ComponentDto;
import org.sonar.core.component.SnapshotDto;
import org.sonar.core.measure.db.MeasureDto;
import org.sonar.core.persistence.DbSession;
import org.sonar.core.persistence.MyBatis;
import org.sonar.core.properties.PropertyDto;
import org.sonar.core.properties.PropertyQuery;
import org.sonar.core.timemachine.Periods;
import org.sonar.server.db.DbClient;
import org.sonar.server.exceptions.NotFoundException;
import org.sonar.server.issue.IssueService;
import org.sonar.server.issue.RulesAggregation;
import org.sonar.server.rule.Rule;
import org.sonar.server.rule.RuleService;
import org.sonar.server.rule.index.RuleDoc;
import org.sonar.server.rule.index.RuleQuery;
import org.sonar.server.search.QueryContext;
import org.sonar.server.search.Result;
import org.sonar.server.ui.ViewProxy;
import org.sonar.server.ui.Views;
import org.sonar.server.user.UserSession;

import javax.annotation.CheckForNull;
import javax.annotation.Nullable;

import java.util.Date;
import java.util.List;
import java.util.Map;

import static com.google.common.collect.Lists.newArrayList;
import static com.google.common.collect.Maps.newHashMap;

public class ComponentAppAction implements RequestHandler {

  private static final String PARAM_KEY = "key";
  private static final String PARAM_PERIOD = "period";

  private final DbClient dbClient;

  private final IssueService issueService;
  private final Views views;
  private final RuleService ruleService;
  private final Periods periods;
  private final Durations durations;
  private final I18n i18n;

  public ComponentAppAction(DbClient dbClient, IssueService issueService, Views views, RuleService ruleService, Periods periods, Durations durations, I18n i18n) {
    this.dbClient = dbClient;
    this.issueService = issueService;
    this.views = views;
    this.ruleService = ruleService;
    this.periods = periods;
    this.durations = durations;
    this.i18n = i18n;
  }

  void define(WebService.NewController controller) {
    WebService.NewAction action = controller.createAction("app")
      .setDescription("Coverage data required for rendering the component viewer")
      .setSince("4.4")
      .setInternal(true)
      .setHandler(this)
      .setResponseExample(Resources.getResource(this.getClass(), "components-example-app.json"));

    action
      .createParam(PARAM_KEY)
      .setRequired(true)
      .setDescription("File key")
      .setExampleValue("org.codehaus.sonar:sonar-plugin-api:src/main/java/org/sonar/api/Plugin.java");

    action
      .createParam(PARAM_PERIOD)
      .setDescription("Period index in order to get differential measures")
      .setPossibleValues(1, 2, 3, 4, 5);
  }

  @Override
  public void handle(Request request, Response response) {
    String fileKey = request.mandatoryParam(PARAM_KEY);
    UserSession userSession = UserSession.get();

    JsonWriter json = response.newJsonWriter();
    json.beginObject();

    DbSession session = dbClient.openSession(false);
    try {
      ComponentDto component = dbClient.componentDao().getNullableByKey(session, fileKey);
      if (component == null) {
        throw new NotFoundException(String.format("Component '%s' does not exist", fileKey));
      }
      userSession.checkComponentPermission(UserRole.USER, fileKey);

      List<Period> periodList = periods(component.projectId(), session);
      Integer periodIndex = request.paramAsInt(PARAM_PERIOD);
      Date periodDate = periodDate(periodIndex, periodList);

      RulesAggregation rulesAggregation = issueService.findRulesByComponent(component.key(), periodDate, session);
      Multiset<String> severitiesAggregation = issueService.findSeveritiesByComponent(component.key(), periodDate, session);
      Map<String, MeasureDto> measuresByMetricKey = measuresByMetricKey(component, session);

      appendComponent(json, component, userSession, session);
      appendPermissions(json, component, userSession);
      appendPeriods(json, periodList);
      appendIssuesAggregation(json, rulesAggregation, severitiesAggregation);
      appendMeasures(json, measuresByMetricKey, severitiesAggregation, periodIndex);
      appendTabs(json, measuresByMetricKey);
      appendExtensions(json, component, userSession);
      appendManualRules(json);
    } finally {
      MyBatis.closeQuietly(session);
    }

    json.endObject();
    json.close();
  }

  private void appendComponent(JsonWriter json, ComponentDto component, UserSession userSession, DbSession session) {
    List<PropertyDto> propertyDtos = dbClient.propertiesDao().selectByQuery(PropertyQuery.builder()
      .setKey("favourite")
      .setComponentId(component.getId())
      .setUserId(userSession.userId())
      .build(),
      session
      );
    boolean isFavourite = propertyDtos.size() == 1;

    json.prop("key", component.key());
    json.prop("path", component.path());
    json.prop("name", component.name());
    json.prop("longName", component.longName());
    json.prop("q", component.qualifier());

    ComponentDto subProject = (ComponentDto) nullableComponentById(component.subProjectId(), session);
    ComponentDto project = (ComponentDto) componentById(component.projectId(), session);

    // Do not display sub project if sub project and project are the same
    boolean displaySubProject = subProject != null && !subProject.getId().equals(project.getId());

    json.prop("subProject", displaySubProject ? subProject.key() : null);
    json.prop("subProjectName", displaySubProject ? subProject.longName() : null);
    json.prop("project", project.key());
    json.prop("projectName", project.longName());

    json.prop("fav", isFavourite);
  }

  private void appendTabs(JsonWriter json, Map<String, MeasureDto> measuresByMetricKey) {
    List<String> tabs = newArrayList();
    if (measuresByMetricKey.get(CoreMetrics.SCM_AUTHORS_BY_LINE_KEY) != null) {
      tabs.add("scm");
    }
    if (hasCoverage(measuresByMetricKey)) {
      tabs.add("coverage");
    }
    if (measuresByMetricKey.get(CoreMetrics.DUPLICATED_LINES_KEY) != null) {
      tabs.add("duplications");
    }
    if (!tabs.isEmpty()) {
      json.name("tabs").beginArray().values(tabs).endArray();
    }
  }

  private boolean hasCoverage(Map<String, MeasureDto> measuresByMetricKey) {
    return measuresByMetricKey.get(CoreMetrics.OVERALL_COVERAGE_KEY) != null
      || measuresByMetricKey.get(CoreMetrics.IT_COVERAGE_KEY) != null
      || measuresByMetricKey.get(CoreMetrics.COVERAGE_KEY) != null;
  }

  private void appendPermissions(JsonWriter json, ComponentDto component, UserSession userSession) {
    boolean hasBrowsePermission = userSession.hasComponentPermission(UserRole.USER, component.key());
    json.prop("canMarkAsFavourite", userSession.isLoggedIn() && hasBrowsePermission);
    json.prop("canBulkChange", userSession.isLoggedIn());
    json.prop("canCreateManualIssue", userSession.isLoggedIn() && hasBrowsePermission);
  }

  private void appendMeasures(JsonWriter json, Map<String, MeasureDto> measuresByMetricKey, Multiset<String> severitiesAggregation, Integer periodIndex) {
    json.name("measures").beginObject();

    json.prop("fNcloc", formatMeasureOrVariation(measuresByMetricKey.get(CoreMetrics.NCLOC_KEY), periodIndex));
    json.prop("fCoverage", formatMeasureOrVariation(coverageMeasure(measuresByMetricKey), periodIndex));
    json.prop("fDuplicationDensity", formatMeasureOrVariation(measuresByMetricKey.get(CoreMetrics.DUPLICATED_LINES_DENSITY_KEY), periodIndex));
    json.prop("fDebt", formatMeasureOrVariation(measuresByMetricKey.get(CoreMetrics.TECHNICAL_DEBT_KEY), periodIndex));
    json.prop("fSqaleRating", formatMeasureOrVariation(measuresByMetricKey.get(CoreMetrics.SQALE_RATING_KEY), periodIndex));
    json.prop("fSqaleDebtRatio", formatMeasureOrVariation(measuresByMetricKey.get(CoreMetrics.SQALE_DEBT_RATIO_KEY), periodIndex));
    json.prop("fTests", formatMeasureOrVariation(measuresByMetricKey.get(CoreMetrics.TESTS_KEY), periodIndex));

    json.prop("fIssues", i18n.formatInteger(UserSession.get().locale(), severitiesAggregation.size()));
    for (String severity : severitiesAggregation.elementSet()) {
      json.prop("f" + StringUtils.capitalize(severity.toLowerCase()) + "Issues", i18n.formatInteger(UserSession.get().locale(), severitiesAggregation.count(severity)));
    }
    json.endObject();
  }

  private MeasureDto coverageMeasure(Map<String, MeasureDto> measuresByMetricKey) {
    MeasureDto overallCoverage = measuresByMetricKey.get(CoreMetrics.OVERALL_COVERAGE_KEY);
    MeasureDto itCoverage = measuresByMetricKey.get(CoreMetrics.IT_COVERAGE_KEY);
    MeasureDto utCoverage = measuresByMetricKey.get(CoreMetrics.COVERAGE_KEY);
    if (overallCoverage != null) {
      return overallCoverage;
    } else if (utCoverage != null) {
      return utCoverage;
    } else {
      return itCoverage;
    }
  }

  private void appendPeriods(JsonWriter json, List<Period> periodList) {
    json.name("periods").beginArray();
    for (Period period : periodList) {
      Date periodDate = period.date();
      json.beginArray()
        .value(period.index())
        .value(period.label())
        .value(periodDate != null ? DateUtils.formatDateTime(periodDate) : null)
        .endArray();
    }
    json.endArray();
  }

  private void appendIssuesAggregation(JsonWriter json, RulesAggregation rulesAggregation, Multiset<String> severitiesAggregation) {
    json.name("severities").beginArray();
    for (String severity : severitiesAggregation.elementSet()) {
      json.beginArray()
        .value(severity)
        .value(i18n.message(UserSession.get().locale(), "severity." + severity, null))
        .value(severitiesAggregation.count(severity))
        .endArray();
    }
    json.endArray();

    json.name("rules").beginArray();
    for (RulesAggregation.Rule rule : rulesAggregation.rules()) {
      json.beginArray()
        .value(rule.ruleKey().toString())
        .value(rule.name())
        .value(rulesAggregation.countRule(rule))
        .endArray();
    }
    json.endArray();
  }

  private void appendManualRules(JsonWriter json) {
    Result<Rule> result = ruleService.search(new RuleQuery().setRepositories(newArrayList(RuleDoc.MANUAL_REPOSITORY)), new QueryContext().setMaxLimit());
    if (result != null && !result.getHits().isEmpty()) {
      List<Rule> manualRules = result.getHits();
      json.name("manual_rules").beginArray();
      for (Rule manualRule : manualRules) {
        json.beginObject()
          .prop("key", manualRule.key().toString())
          .prop("name", manualRule.name())
          .endObject();
      }
      json.endArray();
    }
  }

  private void appendExtensions(JsonWriter json, ComponentDto component, UserSession userSession) {
    List<ViewProxy<Page>> extensionPages = views.getPages(NavigationSection.RESOURCE_TAB, component.scope(), component.qualifier(), component.language(), null);
    Map<String, String> extensions = extensions(extensionPages, component, userSession);
    if (!extensions.isEmpty()) {
      json.name("extensions").beginArray();
      for (Map.Entry<String, String> entry : extensions.entrySet()) {
        json.beginArray().value(entry.getKey()).value(entry.getValue()).endArray();
      }
      json.endArray();
    }
  }

  private Map<String, String> extensions(List<ViewProxy<Page>> extensions, ComponentDto component, UserSession userSession) {
    Map<String, String> result = newHashMap();
    List<String> providedExtensions = newArrayList("tests_viewer", "coverage", "duplications", "issues", "source");
    for (ViewProxy<Page> page : extensions) {
      if (!providedExtensions.contains(page.getId())) {
        addExtension(page, result, component, userSession);
      }
    }
    return result;
  }

  private void addExtension(ViewProxy<Page> page, Map<String, String> result, ComponentDto component, UserSession userSession) {
    if (page.getUserRoles().length == 0) {
      result.put(page.getId(), page.getTitle());
    } else {
      for (String userRole : page.getUserRoles()) {
        if (userSession.hasComponentPermission(userRole, component.key())) {
          result.put(page.getId(), page.getTitle());
        }
      }
    }
  }

  private List<Period> periods(Long projectId, DbSession session) {
    List<Period> periodList = newArrayList();
    SnapshotDto snapshotDto = dbClient.resourceDao().getLastSnapshotByResourceId(projectId, session);
    if (snapshotDto != null) {
      for (int i = 1; i <= 5; i++) {
        String mode = snapshotDto.getPeriodMode(i);
        if (mode != null) {
          Date periodDate = snapshotDto.getPeriodDate(i);
          String label = periods.label(mode, snapshotDto.getPeriodModeParameter(i), periodDate);
          if (label != null) {
            periodList.add(new Period(i, label, periodDate));
          }
        }
      }
    }
    return periodList;
  }

  private Map<String, MeasureDto> measuresByMetricKey(ComponentDto component, DbSession session) {
    Map<String, MeasureDto> measuresByMetricKey = newHashMap();
    String fileKey = component.getKey();
    for (MeasureDto measureDto : dbClient.measureDao().findByComponentKeyAndMetricKeys(fileKey,
      newArrayList(CoreMetrics.NCLOC_KEY, CoreMetrics.COVERAGE_KEY, CoreMetrics.IT_COVERAGE_KEY, CoreMetrics.OVERALL_COVERAGE_KEY,
        CoreMetrics.DUPLICATED_LINES_KEY, CoreMetrics.DUPLICATED_LINES_DENSITY_KEY, CoreMetrics.TECHNICAL_DEBT_KEY, CoreMetrics.TESTS_KEY,
        CoreMetrics.SCM_AUTHORS_BY_LINE_KEY, CoreMetrics.SQALE_RATING_KEY, CoreMetrics.SQALE_DEBT_RATIO_KEY),
      session)) {
      measuresByMetricKey.put(measureDto.getKey().metricKey(), measureDto);
    }
    return measuresByMetricKey;
  }

  @CheckForNull
  private Date periodDate(@Nullable final Integer periodIndex, List<Period> periodList) {
    if (periodIndex != null) {
      Period period = Iterables.find(periodList, new Predicate<Period>() {
        @Override
        public boolean apply(@Nullable Period input) {
          return input != null && periodIndex.equals(input.index());
        }
      }, null);
      return period != null ? period.date() : null;
    }
    return null;
  }

  @CheckForNull
  private Component nullableComponentById(@Nullable Long componentId, DbSession session) {
    if (componentId != null) {
      return componentById(componentId, session);
    }
    return null;
  }

  private Component componentById(Long componentId, DbSession session) {
    return dbClient.componentDao().getById(componentId, session);
  }

  @CheckForNull
  private String formatMeasureOrVariation(@Nullable MeasureDto measure, @Nullable Integer periodIndex) {
    if (periodIndex == null) {
      return formatMeasure(measure);
    } else {
      return formatVariation(measure, periodIndex);
    }
  }

  @CheckForNull
  private String formatMeasure(@Nullable MeasureDto measure) {
    if (measure != null) {
      Metric metric = CoreMetrics.getMetric(measure.getKey().metricKey());
      Metric.ValueType metricType = metric.getType();
      Double value = measure.getValue();
      String data = measure.getData();

      if (metricType.equals(Metric.ValueType.FLOAT) && value != null) {
        return i18n.formatDouble(UserSession.get().locale(), value);
      }
      if (metricType.equals(Metric.ValueType.INT) && value != null) {
        return i18n.formatInteger(UserSession.get().locale(), value.intValue());
      }
      if (metricType.equals(Metric.ValueType.PERCENT) && value != null) {
        return i18n.formatDouble(UserSession.get().locale(), value) + "%";
      }
      if (metricType.equals(Metric.ValueType.WORK_DUR) && value != null) {
        return durations.format(UserSession.get().locale(), durations.create(value.longValue()), Durations.DurationFormat.SHORT);
      }
      if ((metricType.equals(Metric.ValueType.STRING) || metricType.equals(Metric.ValueType.RATING)) && data != null) {
        return data;
      }
    }
    return null;
  }

  @CheckForNull
  private String formatVariation(@Nullable MeasureDto measure, Integer periodIndex) {
    if (measure != null) {
      Double variation = measure.getVariation(periodIndex);
      if (variation != null) {
        Metric metric = CoreMetrics.getMetric(measure.getKey().metricKey());
        Metric.ValueType metricType = metric.getType();
        if (metricType.equals(Metric.ValueType.FLOAT) || metricType.equals(Metric.ValueType.PERCENT)) {
          return i18n.formatDouble(UserSession.get().locale(), variation);
        }
        if (metricType.equals(Metric.ValueType.INT)) {
          return i18n.formatInteger(UserSession.get().locale(), variation.intValue());
        }
        if (metricType.equals(Metric.ValueType.WORK_DUR)) {
          return durations.format(UserSession.get().locale(), durations.create(variation.longValue()), Durations.DurationFormat.SHORT);
        }
      }
    }
    return null;
  }

  protected static class Period {
    Integer index;
    String label;
    Date date;

    protected Period(Integer index, String label, @Nullable Date date) {
      this.index = index;
      this.label = label;
      this.date = date;
    }

    public Integer index() {
      return index;
    }

    public String label() {
      return label;
    }

    @CheckForNull
    public Date date() {
      return date;
    }
  }

}
