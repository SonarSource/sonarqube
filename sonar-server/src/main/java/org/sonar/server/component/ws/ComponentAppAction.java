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
import org.sonar.core.measure.db.MeasureDto;
import org.sonar.core.persistence.DbSession;
import org.sonar.core.persistence.MyBatis;
import org.sonar.core.properties.PropertyDto;
import org.sonar.core.properties.PropertyQuery;
import org.sonar.core.resource.SnapshotDto;
import org.sonar.core.timemachine.Periods;
import org.sonar.server.db.DbClient;
import org.sonar.server.exceptions.NotFoundException;
import org.sonar.server.issue.IssueService;
import org.sonar.server.issue.RulesAggregation;
import org.sonar.server.source.SourceService;
import org.sonar.server.ui.ViewProxy;
import org.sonar.server.ui.Views;
import org.sonar.server.user.UserSession;

import javax.annotation.CheckForNull;
import javax.annotation.Nullable;

import java.util.Date;
import java.util.List;

import static com.google.common.collect.Lists.newArrayList;

public class ComponentAppAction implements RequestHandler {

  private static final String KEY = "key";

  private final DbClient dbClient;

  private final IssueService issueService;
  private final SourceService sourceService;
  private final Views views;
  private final Periods periods;
  private final Durations durations;
  private final I18n i18n;

  public ComponentAppAction(DbClient dbClient, IssueService issueService, SourceService sourceService, Views views, Periods periods, Durations durations, I18n i18n) {
    this.dbClient = dbClient;
    this.issueService = issueService;
    this.sourceService = sourceService;
    this.views = views;
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
      .setResponseExample(Resources.getResource(this.getClass(), "components-app-example-show.json"));

    action
      .createParam(KEY)
      .setRequired(true)
      .setDescription("File key")
      .setExampleValue("org.codehaus.sonar:sonar-plugin-api:src/main/java/org/sonar/api/Plugin.java");
  }

  @Override
  public void handle(Request request, Response response) {
    String fileKey = request.mandatoryParam(KEY);
    UserSession userSession = UserSession.get();

    JsonWriter json = response.newJsonWriter();
    json.beginObject();

    DbSession session = dbClient.openSession(false);
    try {
      ComponentDto component = dbClient.componentDao().getNullableByKey(session, fileKey);
      if (component == null) {
        throw new NotFoundException(String.format("Component '%s' does not exists.", fileKey));
      }
      userSession.checkComponentPermission(UserRole.CODEVIEWER, fileKey);

      appendComponent(json, component, userSession, session);
      appendPermissions(json, component, userSession);
      appendPeriods(json, component.projectId(), session);
      appendIssuesAggregation(json, component.key(), session);
      appendMeasures(json, component, session);
      appendExtensions(json, component, userSession);
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
    json.prop("q", component.qualifier());

    ComponentDto subProject = (ComponentDto) componentById(component.subProjectId(), session);
    ComponentDto project = (ComponentDto) componentById(component.projectId(), session);

    // Do not display sub project long name if sub project and project are the same
    boolean displaySubProjectLongName = subProject != null && !subProject.getId().equals(project.getId());

    json.prop("subProjectName", displaySubProjectLongName ? subProject.longName() : null);
    json.prop("projectName", project.longName());

    json.prop("fav", isFavourite);
    json.prop("scmAvailable", sourceService.hasScmData(component.key(), session));
  }

  private void appendPermissions(JsonWriter json, ComponentDto component, UserSession userSession) {
    json.prop("canMarkAsFavourite", userSession.isLoggedIn() && userSession.hasComponentPermission(UserRole.CODEVIEWER, component.key()));
    json.prop("canBulkChange", userSession.isLoggedIn());
  }

  private void appendMeasures(JsonWriter json, ComponentDto component, DbSession session) {
    json.name("measures").beginObject();

    String fileKey = component.getKey();
    List<MeasureDto> measures = dbClient.measureDao().findByComponentKeyAndMetricKeys(fileKey,
      newArrayList(CoreMetrics.NCLOC_KEY, CoreMetrics.COVERAGE_KEY, CoreMetrics.DUPLICATED_LINES_DENSITY_KEY, CoreMetrics.TECHNICAL_DEBT_KEY, CoreMetrics.VIOLATIONS_KEY,
        CoreMetrics.BLOCKER_VIOLATIONS_KEY, CoreMetrics.CRITICAL_VIOLATIONS_KEY, CoreMetrics.MAJOR_VIOLATIONS_KEY, CoreMetrics.MINOR_VIOLATIONS_KEY,
        CoreMetrics.INFO_VIOLATIONS_KEY, CoreMetrics.TESTS_KEY),
      session
    );

    json.prop("fNcloc", formattedMeasure(CoreMetrics.NCLOC_KEY, measures));
    json.prop("fCoverage", formattedMeasure(CoreMetrics.COVERAGE_KEY, measures));
    json.prop("fDuplicationDensity", formattedMeasure(CoreMetrics.DUPLICATED_LINES_DENSITY_KEY, measures));
    json.prop("fDebt", formattedMeasure(CoreMetrics.TECHNICAL_DEBT_KEY, measures));
    json.prop("fIssues", formattedMeasure(CoreMetrics.VIOLATIONS_KEY, measures));
    json.prop("fBlockerIssues", formattedMeasure(CoreMetrics.BLOCKER_VIOLATIONS_KEY, measures));
    json.prop("fCriticalIssues", formattedMeasure(CoreMetrics.CRITICAL_VIOLATIONS_KEY, measures));
    json.prop("fMajorIssues", formattedMeasure(CoreMetrics.MAJOR_VIOLATIONS_KEY, measures));
    json.prop("fMinorIssues", formattedMeasure(CoreMetrics.MINOR_VIOLATIONS_KEY, measures));
    json.prop("fInfoIssues", formattedMeasure(CoreMetrics.INFO_VIOLATIONS_KEY, measures));
    json.prop("fTests", formattedMeasure(CoreMetrics.TESTS_KEY, measures));
    json.endObject();
  }

  private void appendPeriods(JsonWriter json, Long projectId, DbSession session) {
    json.name("periods").beginArray();
    SnapshotDto snapshotDto = dbClient.resourceDao().getLastSnapshotByResourceId(projectId, session);
    if (snapshotDto != null) {
      for (int i = 1; i <= 5; i++) {
        String mode = snapshotDto.getPeriodMode(i);
        if (mode != null) {
          Date periodDate = snapshotDto.getPeriodDate(i);
          String label = periods.label(mode, snapshotDto.getPeriodModeParameter(i), periodDate);
          if (label != null) {
            json.beginArray()
              .value(i)
              .value(label)
              .value(periodDate != null ? DateUtils.formatDateTime(periodDate) : null)
              .endArray();
          }
        }
      }
    }
    json.endArray();
  }

  private void appendIssuesAggregation(JsonWriter json, String componentKey, DbSession session) {
    json.name("severities").beginArray();
    Multiset<String> severities = issueService.findSeveritiesByComponent(componentKey, session);
    for (String severity : severities.elementSet()) {
      json.beginArray()
        .value(severity)
        .value(i18n.message(UserSession.get().locale(), "severity." + severity, null))
        .value(severities.count(severity))
        .endArray();
    }
    json.endArray();

    json.name("rules").beginArray();
    RulesAggregation rulesAggregation = issueService.findRulesByComponent(componentKey, session);
    for (RulesAggregation.Rule rule : rulesAggregation.rules()) {
      json.beginArray()
        .value(rule.ruleKey().toString())
        .value(rule.name())
        .value(rulesAggregation.countRule(rule))
        .endArray();
    }
    json.endArray();
  }

  private void appendExtensions(JsonWriter json, ComponentDto component, UserSession userSession) {
    List<ViewProxy<Page>> extensionPages = views.getPages(NavigationSection.RESOURCE_TAB, component.scope(), component.qualifier(), component.language(), null);
    List<String> extensions = extensions(extensionPages, component, userSession);
    if (!extensions.isEmpty()) {
      json.name("extensions").beginArray();
      json.values(extensions);
      json.endArray();
    }
  }

  private List<String> extensions(List<ViewProxy<Page>> extensions, ComponentDto component, UserSession userSession){
    List<String> result = newArrayList();
    List<String> providedExtensions = newArrayList("tests_viewer", "coverage", "duplications", "issues", "source");
    for (ViewProxy<Page> page : extensions) {
      if (!providedExtensions.contains(page.getId())) {
        if (page.getUserRoles().length == 0) {
          result.add(page.getId());
        } else {
          for (String userRole : page.getUserRoles()) {
            if (userSession.hasComponentPermission(userRole, component.key())) {
              result.add(page.getId());
            }
          }
        }
      }
    }
    return result;
  }

  @CheckForNull
  private Component componentById(@Nullable Long componentId, DbSession session) {
    if (componentId != null) {
      return dbClient.componentDao().getById(componentId, session);
    }
    return null;
  }

  @CheckForNull
  private String formattedMeasure(final String metricKey, List<MeasureDto> measures) {
    MeasureDto measure = measureByMetricKey(metricKey, measures);
    if (measure != null) {
      Metric metric = CoreMetrics.getMetric(measure.getKey().metricKey());
      Double value = measure.getValue();
      if (value != null) {
        if (metric.getType().equals(Metric.ValueType.FLOAT)) {
          return i18n.formatDouble(UserSession.get().locale(), value);
        } else if (metric.getType().equals(Metric.ValueType.INT)) {
          return i18n.formatInteger(UserSession.get().locale(), value.intValue());
        } else if (metric.getType().equals(Metric.ValueType.PERCENT)) {
          return i18n.formatDouble(UserSession.get().locale(), value) + "%";
        } else if (metric.getType().equals(Metric.ValueType.WORK_DUR)) {
          return durations.format(UserSession.get().locale(), durations.create(value.longValue()), Durations.DurationFormat.SHORT);
        }
      }
    }
    return null;
  }

  @CheckForNull
  private static MeasureDto measureByMetricKey(final String metricKey, List<MeasureDto> measures) {
    return Iterables.find(measures, new Predicate<MeasureDto>() {
      @Override
      public boolean apply(@Nullable MeasureDto input) {
        return input != null && metricKey.equals(input.getKey().metricKey());
      }
    }, null);
  }

}
