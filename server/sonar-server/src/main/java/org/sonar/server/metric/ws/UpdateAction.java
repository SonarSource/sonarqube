/*
 * SonarQube
 * Copyright (C) 2009-2019 SonarSource SA
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
package org.sonar.server.metric.ws;

import java.util.List;
import javax.annotation.Nullable;
import org.sonar.api.measures.Metric;
import org.sonar.api.server.ws.Request;
import org.sonar.api.server.ws.Response;
import org.sonar.api.server.ws.WebService;
import org.sonar.api.utils.text.JsonWriter;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.db.measure.custom.CustomMeasureDto;
import org.sonar.db.metric.MetricDto;
import org.sonar.server.user.UserSession;
import org.sonar.server.util.MetricKeyValidator;

import static org.sonar.db.metric.MetricValidator.MAX_DESCRIPTION_LENGTH;
import static org.sonar.db.metric.MetricValidator.MAX_DOMAIN_LENGTH;
import static org.sonar.db.metric.MetricValidator.MAX_KEY_LENGTH;
import static org.sonar.db.metric.MetricValidator.MAX_NAME_LENGTH;
import static org.sonar.server.ws.WsUtils.checkRequest;

public class UpdateAction implements MetricsWsAction {
  private static final String ACTION = "update";

  public static final String PARAM_ID = "id";
  public static final String PARAM_NAME = "name";
  public static final String PARAM_KEY = "key";
  public static final String PARAM_TYPE = "type";
  public static final String PARAM_DESCRIPTION = "description";
  public static final String PARAM_DOMAIN = "domain";

  private static final String FIELD_ID = PARAM_ID;
  private static final String FIELD_NAME = PARAM_NAME;
  private static final String FIELD_KEY = PARAM_KEY;
  private static final String FIELD_TYPE = PARAM_TYPE;
  private static final String FIELD_DESCRIPTION = PARAM_DESCRIPTION;
  private static final String FIELD_DOMAIN = PARAM_DOMAIN;

  private final DbClient dbClient;
  private final UserSession userSession;

  public UpdateAction(DbClient dbClient, UserSession userSession) {
    this.dbClient = dbClient;
    this.userSession = userSession;
  }

  @Override
  public void define(WebService.NewController context) {
    WebService.NewAction action = context.createAction(ACTION)
      .setPost(true)
      .setDescription("Update a custom metric.<br/>" +
        "Requires 'Administer System' permission.")
      .setSince("5.2")
      .setDeprecatedSince("7.7")
      .setHandler(this);

    action.createParam(PARAM_ID)
      .setRequired(true)
      .setDescription("Id of the custom metric to update")
      .setExampleValue("42");

    action.createParam(PARAM_KEY)
      .setDescription("Key")
      .setMaximumLength(MAX_KEY_LENGTH)
      .setDescription("Key")
      .setExampleValue("team_size");

    action.createParam(PARAM_NAME)
      .setMaximumLength(MAX_NAME_LENGTH)
      .setDescription("Name");

    action.createParam(PARAM_TYPE)
      .setDescription("Metric type key")
      .setPossibleValues(Metric.ValueType.names())
      .setExampleValue(Metric.ValueType.INT.name());

    action.createParam(PARAM_DESCRIPTION)
      .setDescription("Description")
      .setMaximumLength(MAX_DESCRIPTION_LENGTH)
      .setExampleValue("Size of the team");

    action.createParam(PARAM_DOMAIN)
      .setMaximumLength(MAX_DOMAIN_LENGTH)
      .setDescription("Domain")
      .setExampleValue("Tests");
  }

  @Override
  public void handle(Request request, Response response) throws Exception {
    userSession.checkLoggedIn().checkIsSystemAdministrator();
    int id = request.mandatoryParamAsInt(PARAM_ID);

    try (DbSession dbSession = dbClient.openSession(false)) {
      MetricDto metricTemplate = newMetricTemplate(request);
      MetricDto metricInDb = dbClient.metricDao().selectById(dbSession, id);
      checkMetricInDbAndTemplate(dbSession, metricInDb, metricTemplate);

      updateMetricInDb(dbSession, metricInDb, metricTemplate);
      JsonWriter json = response.newJsonWriter();
      writeMetric(json, metricInDb);
      json.close();
    }
  }

  private static MetricDto newMetricTemplate(Request request) {
    int id = request.mandatoryParamAsInt(PARAM_ID);
    String key = request.param(PARAM_KEY);
    if (key != null) {
      MetricKeyValidator.checkMetricKeyFormat(key);
    }
    String type = request.param(PARAM_TYPE);
    String name = request.param(PARAM_NAME);
    String domain = request.param(PARAM_DOMAIN);
    String description = request.param(PARAM_DESCRIPTION);

    MetricDto metricTemplate = new MetricDto().setId(id);
    if (key != null) {
      metricTemplate.setKey(key);
    }
    if (type != null) {
      metricTemplate.setValueType(type);
    }
    if (name != null) {
      metricTemplate.setShortName(name);
    }
    if (domain != null) {
      metricTemplate.setDomain(domain);
    }
    if (description != null) {
      metricTemplate.setDescription(description);
    }
    return metricTemplate;
  }

  private void updateMetricInDb(DbSession dbSession, MetricDto metricInDb, MetricDto metricTemplate) {
    String key = metricTemplate.getKey();
    String name = metricTemplate.getShortName();
    String type = metricTemplate.getValueType();
    String domain = metricTemplate.getDomain();
    String description = metricTemplate.getDescription();
    if (key != null) {
      metricInDb.setKey(key);
    }
    if (name != null) {
      metricInDb.setShortName(name);
    }
    if (type != null) {
      metricInDb.setValueType(type);
    }
    if (domain != null) {
      metricInDb.setDomain(domain);
    }
    if (description != null) {
      metricInDb.setDescription(description);
    }
    dbClient.metricDao().update(dbSession, metricInDb);
    dbSession.commit();
  }

  private void checkMetricInDbAndTemplate(DbSession dbSession, @Nullable MetricDto metricInDb, MetricDto template) {
    checkRequest(isMetricFoundInDb(metricInDb) && !isMetricDisabled(metricInDb) && isMetricCustom(metricInDb),
      "No active custom metric has been found for id '%d'.", template.getId());
    checkNoOtherMetricWithTargetKey(dbSession, metricInDb, template);
    if (haveMetricTypeChanged(metricInDb, template)) {
      List<CustomMeasureDto> customMeasures = dbClient.customMeasureDao().selectByMetricId(dbSession, metricInDb.getId());
      checkRequest(!haveAssociatedCustomMeasures(customMeasures), "You're trying to change the type '%s' while there are associated custom measures.", metricInDb.getValueType());
    }
  }

  private void checkNoOtherMetricWithTargetKey(DbSession dbSession, MetricDto metricInDb, MetricDto template) {
    String targetKey = template.getKey();
    MetricDto metricWithTargetKey = dbClient.metricDao().selectByKey(dbSession, targetKey);
    checkRequest(!isMetricFoundInDb(metricWithTargetKey) || metricInDb.getId().equals(metricWithTargetKey.getId()),
      "The key '%s' is already used by an existing metric.", targetKey);
  }

  private static void writeMetric(JsonWriter json, MetricDto metric) {
    json.beginObject();
    json.prop(FIELD_ID, String.valueOf(metric.getId()));
    json.prop(FIELD_KEY, metric.getKey());
    json.prop(FIELD_TYPE, metric.getValueType());
    json.prop(FIELD_NAME, metric.getShortName());
    json.prop(FIELD_DOMAIN, metric.getDomain());
    json.prop(FIELD_DESCRIPTION, metric.getDescription());
    json.endObject();
  }

  private static boolean isMetricCustom(MetricDto metricInDb) {
    return metricInDb.isUserManaged();
  }

  private static boolean isMetricDisabled(MetricDto metricInDb) {
    return !metricInDb.isEnabled();
  }

  private static boolean isMetricFoundInDb(@Nullable MetricDto metricInDb) {
    return metricInDb != null;
  }

  private static boolean haveAssociatedCustomMeasures(List<CustomMeasureDto> customMeasures) {
    return !customMeasures.isEmpty();
  }

  private static boolean haveMetricTypeChanged(MetricDto metricInDb, MetricDto template) {
    return !metricInDb.getValueType().equals(template.getValueType()) && template.getValueType() != null;
  }
}
