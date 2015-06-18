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

package org.sonar.server.measure.custom.ws;

import com.google.common.base.Joiner;
import java.net.HttpURLConnection;
import org.sonar.api.PropertyType;
import org.sonar.api.measures.Metric;
import org.sonar.api.server.ws.Request;
import org.sonar.api.server.ws.Response;
import org.sonar.api.server.ws.WebService;
import org.sonar.api.utils.System2;
import org.sonar.api.utils.text.JsonWriter;
import org.sonar.api.web.UserRole;
import org.sonar.core.component.ComponentDto;
import org.sonar.core.measure.custom.db.CustomMeasureDto;
import org.sonar.core.metric.db.MetricDto;
import org.sonar.core.permission.GlobalPermissions;
import org.sonar.core.persistence.DbSession;
import org.sonar.core.persistence.MyBatis;
import org.sonar.server.db.DbClient;
import org.sonar.server.exceptions.NotFoundException;
import org.sonar.server.exceptions.ServerException;
import org.sonar.server.user.UserSession;
import org.sonar.server.util.TypeValidations;

import static com.google.common.base.Preconditions.checkArgument;

public class CreateAction implements CustomMeasuresWsAction {
  public static final String ACTION = "create";
  public static final String PARAM_PROJECT_ID = "projectId";
  public static final String PARAM_PROJECT_KEY = "projectKey";
  public static final String PARAM_METRIC_ID = "metricId";
  public static final String PARAM_METRIC_KEY = "metricKey";
  public static final String PARAM_VALUE = "value";
  public static final String PARAM_DESCRIPTION = "description";

  private static final String FIELD_ID = "id";
  private static final String FIELD_PROJECT_ID = PARAM_PROJECT_ID;
  private static final String FIELD_PROJECT_KEY = PARAM_PROJECT_KEY;
  private static final String FIELD_VALUE = PARAM_VALUE;
  private static final String FIELD_DESCRIPTION = PARAM_DESCRIPTION;
  private static final String FIELD_METRIC = "metric";
  private static final String FIELD_METRIC_KEY = "key";
  private static final String FIELD_METRIC_ID = "id";
  private static final String FIELD_METRIC_TYPE = "type";

  private final DbClient dbClient;
  private final UserSession userSession;
  private final System2 system;
  private final TypeValidations typeValidations;
  private final CustomMeasureJsonWriter customMeasureJsonWriter;

  public CreateAction(DbClient dbClient, UserSession userSession, System2 system, TypeValidations typeValidations, CustomMeasureJsonWriter customMeasureJsonWriter) {
    this.dbClient = dbClient;
    this.userSession = userSession;
    this.system = system;
    this.typeValidations = typeValidations;
    this.customMeasureJsonWriter = customMeasureJsonWriter;
  }

  @Override
  public void define(WebService.NewController context) {
    WebService.NewAction action = context.createAction(ACTION)
      .setDescription("Create a custom measure.<br /> " +
        "The project id or the project key must be provided. The metric id or the metric key must be provided.<br/>" +
        "Requires 'Administer System' permission or 'Administer' permission on the project.")
      .setSince("5.2")
      .setPost(true)
      .setHandler(this);

    action.createParam(PARAM_PROJECT_ID)
      .setDescription("Project id")
      .setExampleValue("ce4c03d6-430f-40a9-b777-ad877c00aa4d");

    action.createParam(PARAM_PROJECT_KEY)
      .setDescription("Project key")
      .setExampleValue("org.apache.hbas:hbase");

    action.createParam(PARAM_METRIC_ID)
      .setDescription("Metric id")
      .setExampleValue("16");

    action.createParam(PARAM_METRIC_KEY)
      .setDescription("Metric key")
      .setExampleValue("ncloc");

    action.createParam(PARAM_VALUE)
      .setRequired(true)
      .setDescription(measureValueDescription())
      .setExampleValue("47");

    action.createParam(PARAM_DESCRIPTION)
      .setDescription("Description")
      .setExampleValue("Team size growing.");
  }

  @Override
  public void handle(Request request, Response response) throws Exception {
    DbSession dbSession = dbClient.openSession(false);
    String description = request.param(PARAM_DESCRIPTION);
    long now = system.now();

    try {
      ComponentDto component = searchProject(dbSession, request);
      MetricDto metric = searchMetric(dbSession, request);
      checkPermissions(component);
      checkMeasureDoesNotExistAlready(dbSession, component, metric);
      CustomMeasureDto measure = new CustomMeasureDto()
        .setComponentUuid(component.uuid())
        .setComponentId(component.getId())
        .setMetricId(metric.getId())
        .setDescription(description)
        .setCreatedAt(now);
      setMeasureValue(measure, request, metric);
      dbClient.customMeasureDao().insert(dbSession, measure);
      dbSession.commit();

      JsonWriter json = response.newJsonWriter();
      writeMeasure(json, measure, component, metric, request.mandatoryParam(PARAM_VALUE));
      json.close();
    } finally {
      MyBatis.closeQuietly(dbSession);
    }
  }

  private void checkPermissions(ComponentDto component) {
    if (userSession.hasGlobalPermission(GlobalPermissions.SYSTEM_ADMIN)) {
      return;
    }

    userSession.checkLoggedIn().checkProjectUuidPermission(UserRole.ADMIN, component.projectUuid());
  }

  private void checkMeasureDoesNotExistAlready(DbSession dbSession, ComponentDto component, MetricDto metric) {
    int nbMeasuresOnSameMetricAndMeasure = dbClient.customMeasureDao().countByComponentIdAndMetricId(dbSession, component.uuid(), metric.getId());
    if (nbMeasuresOnSameMetricAndMeasure > 0) {
      throw new ServerException(HttpURLConnection.HTTP_CONFLICT, String.format("A measure already exists for project id '%s' and metric id '%d'",
        component.uuid(), metric.getId()));
    }
  }

  private void writeMeasure(JsonWriter json, CustomMeasureDto measure, ComponentDto component, MetricDto metric, String measureWithoutInternalFormatting) {
    customMeasureJsonWriter.write(json, measure, metric, component);
  }

  private void setMeasureValue(CustomMeasureDto measure, Request request, MetricDto metric) {
    String valueAsString = request.mandatoryParam(PARAM_VALUE);
    Metric.ValueType metricType = Metric.ValueType.valueOf(metric.getValueType());
    try {
      switch (metricType) {
        case BOOL:
          checkAndSetBooleanMeasureValue(measure, valueAsString);
          break;
        case INT:
        case MILLISEC:
          checkAndSetIntegerMeasureValue(measure, valueAsString);
          break;
        case WORK_DUR:
          checkAndSetWorkDurationMeasureValue(measure, valueAsString);
          break;
        case FLOAT:
        case PERCENT:
        case RATING:
          checkAndSetFloatMeasureValue(measure, valueAsString);
          break;
        case LEVEL:
          checkAndSetLevelMeasureValue(measure, valueAsString);
          break;
        case STRING:
        case DATA:
        case DISTRIB:
          measure.setTextValue(valueAsString);
          break;
        default:
          throw new IllegalArgumentException("Unsupported metric type:" + metricType.description());
      }
    } catch (Exception e) {
      throw new IllegalArgumentException(String.format("Ill formatted value '%s' for metric type '%s'", valueAsString, metricType.description()), e);
    }
  }

  private void checkAndSetLevelMeasureValue(CustomMeasureDto measure, String valueAsString) {
    typeValidations.validate(valueAsString, PropertyType.METRIC_LEVEL.name(), null);
    measure.setTextValue(valueAsString);
  }

  private void checkAndSetFloatMeasureValue(CustomMeasureDto measure, String valueAsString) {
    typeValidations.validate(valueAsString, PropertyType.FLOAT.name(), null);
    measure.setValue(Double.parseDouble(valueAsString));
  }

  private void checkAndSetWorkDurationMeasureValue(CustomMeasureDto measure, String valueAsString) {
    typeValidations.validate(valueAsString, PropertyType.LONG.name(), null);
    measure.setValue(Long.parseLong(valueAsString));
  }

  private void checkAndSetIntegerMeasureValue(CustomMeasureDto measure, String valueAsString) {
    typeValidations.validate(valueAsString, PropertyType.INTEGER.name(), null);
    measure.setValue(Integer.parseInt(valueAsString));
  }

  private void checkAndSetBooleanMeasureValue(CustomMeasureDto measure, String valueAsString) {
    typeValidations.validate(valueAsString, PropertyType.BOOLEAN.name(), null);
    measure.setValue(Boolean.parseBoolean(valueAsString) ? 1.0d : 0.0d);
  }

  private MetricDto searchMetric(DbSession dbSession, Request request) {
    Integer metricId = request.paramAsInt(PARAM_METRIC_ID);
    String metricKey = request.param(PARAM_METRIC_KEY);
    checkArgument(metricId != null ^ metricKey != null, "The metric id or the metric key must be provided, not both.");

    if (metricId != null) {
      return dbClient.metricDao().selectById(dbSession, metricId);
    }

    return dbClient.metricDao().selectByKey(dbSession, metricKey);
  }

  private ComponentDto searchProject(DbSession dbSession, Request request) {
    String projectUuid = request.param(PARAM_PROJECT_ID);
    String projectKey = request.param(PARAM_PROJECT_KEY);
    checkArgument(projectUuid != null ^ projectKey != null, "The project key or the project id must be provided, not both.");

    if (projectUuid != null) {
      ComponentDto project = dbClient.componentDao().selectNullableByUuid(dbSession, projectUuid);
      if (project == null) {
        throw new NotFoundException(String.format("Project id '%s' not found", projectUuid));
      }

      return project;
    }

    ComponentDto project = dbClient.componentDao().selectNullableByKey(dbSession, projectKey);
    if (project == null) {
      throw new NotFoundException(String.format("Project key '%s' not found", projectKey));
    }

    return project;
  }

  private static String measureValueDescription() {
    StringBuilder description = new StringBuilder("Measure value. Value type depends on metric type:");
    description.append("<ul>");
    for (Metric.ValueType metricType : Metric.ValueType.values()) {
      description.append("<li>");
      description.append(String.format("%s - %s", metricType.description(), metricTypeWsDescription(metricType)));
      description.append("</li>");
    }
    description.append("</ul>");

    return description.toString();
  }

  private static String metricTypeWsDescription(Metric.ValueType metricType) {
    switch (metricType) {
      case BOOL:
        return "the possible values are true or false";
      case INT:
      case MILLISEC:
        return "type: integer";
      case FLOAT:
      case PERCENT:
      case RATING:
        return "type: double";
      case LEVEL:
        return "the possible values are " + formattedMetricLevelNames();
      case STRING:
      case DATA:
      case DISTRIB:
        return "type: string";
      case WORK_DUR:
        return "long representing the number of minutes";
      default:
        return "metric type not supported";
    }
  }

  private static String formattedMetricLevelNames() {
    return Joiner.on(", ").join(Metric.Level.names());
  }
}
