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
package org.sonar.server.qualitygate.ws;

import org.sonar.api.server.ws.Request;
import org.sonar.api.server.ws.WebService;
import org.sonar.api.utils.text.JsonWriter;
import org.sonar.db.qualitygate.QualityGateConditionDto;
import org.sonar.db.qualitygate.QualityGateDto;
import org.sonar.server.exceptions.BadRequestException;

public class QGatesWs implements WebService {

  static final String PARAM_PAGE_SIZE = "pageSize";
  static final String PARAM_PAGE = "page";
  static final String PARAM_QUERY = "query";
  static final String PARAM_NAME = "name";
  static final String PARAM_ERROR = "error";
  static final String PARAM_WARNING = "warning";
  static final String PARAM_PERIOD = "period";
  static final String PARAM_OPERATOR = "op";
  static final String PARAM_METRIC = "metric";
  static final String PARAM_GATE_ID = "gateId";
  static final String PARAM_PROJECT_ID = "projectId";
  static final String PARAM_ID = "id";

  private final QGateWsAction[] actions;

  public QGatesWs(QGateWsAction... actions) {
    this.actions = actions;
  }

  @Override
  public void define(Context context) {
    NewController controller = context.createController("api/qualitygates")
      .setSince("4.3")
      .setDescription("This service manages quality gates, including conditions and project association");

    for (QGateWsAction action : actions) {
      action.define(controller);
    }

    controller.done();
  }

  static void addConditionParams(NewAction action) {
    action
      .createParam(PARAM_METRIC)
      .setDescription("Condition metric")
      .setRequired(true)
      .setExampleValue("blocker_violations");

    action.createParam(PARAM_OPERATOR)
      .setDescription("Condition operator:<br/>" +
        "<ul>" +
        "<li>EQ = equals</li>" +
        "<li>NE = is not</li>" +
        "<li>LT = is lower than</li>" +
        "<li>GT = is greater than</li>" +
        "</ui>")
      .setExampleValue(QualityGateConditionDto.OPERATOR_EQUALS)
      .setPossibleValues(QualityGateConditionDto.ALL_OPERATORS);

    action.createParam(PARAM_PERIOD)
      .setDescription("Condition period. If not set, the absolute value is considered.")
      .setPossibleValues("1");

    action.createParam(PARAM_WARNING)
      .setDescription("Condition warning threshold")
      .setExampleValue("5");

    action.createParam(PARAM_ERROR)
      .setDescription("Condition error threshold")
      .setExampleValue("10");
  }

  static Long parseId(Request request, String paramName) {
    try {
      return Long.valueOf(request.mandatoryParam(paramName));
    } catch (NumberFormatException badFormat) {
      throw new BadRequestException(paramName + " must be a valid long value");
    }
  }

  static JsonWriter writeQualityGate(QualityGateDto qualityGate, JsonWriter writer) {
    return writer.beginObject()
      .prop(PARAM_ID, qualityGate.getId())
      .prop(PARAM_NAME, qualityGate.getName())
      .endObject();
  }

  static JsonWriter writeQualityGateCondition(QualityGateConditionDto condition, JsonWriter writer) {
    writer.beginObject()
      .prop(PARAM_ID, condition.getId())
      .prop(PARAM_METRIC, condition.getMetricKey())
      .prop(PARAM_OPERATOR, condition.getOperator());
    if (condition.getWarningThreshold() != null) {
      writer.prop(PARAM_WARNING, condition.getWarningThreshold());
    }
    if (condition.getErrorThreshold() != null) {
      writer.prop(PARAM_ERROR, condition.getErrorThreshold());
    }
    if (condition.getPeriod() != null) {
      writer.prop(PARAM_PERIOD, condition.getPeriod());
    }
    writer.endObject();
    return writer;
  }

}
