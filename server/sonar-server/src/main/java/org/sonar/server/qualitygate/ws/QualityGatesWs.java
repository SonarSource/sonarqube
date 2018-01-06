/*
 * SonarQube
 * Copyright (C) 2009-2018 SonarSource SA
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
package org.sonar.server.qualitygate.ws;

import org.sonar.api.server.ws.Change;
import org.sonar.api.server.ws.Request;
import org.sonar.api.server.ws.WebService;
import org.sonar.db.qualitygate.QualityGateConditionDto;
import org.sonar.server.exceptions.BadRequestException;
import org.sonar.server.ws.RemovedWebServiceHandler;

import static org.sonar.server.qualitygate.ws.QualityGatesWsParameters.CONTROLLER_QUALITY_GATES;
import static org.sonar.server.qualitygate.ws.QualityGatesWsParameters.PARAM_ERROR;
import static org.sonar.server.qualitygate.ws.QualityGatesWsParameters.PARAM_METRIC;
import static org.sonar.server.qualitygate.ws.QualityGatesWsParameters.PARAM_OPERATOR;
import static org.sonar.server.qualitygate.ws.QualityGatesWsParameters.PARAM_PERIOD;
import static org.sonar.server.qualitygate.ws.QualityGatesWsParameters.PARAM_WARNING;

public class QualityGatesWs implements WebService {

  private static final int CONDITION_MAX_LENGTH = 64;
  private final QualityGatesWsAction[] actions;

  public QualityGatesWs(QualityGatesWsAction... actions) {
    this.actions = actions;
  }

  @Override
  public void define(Context context) {
    NewController controller = context.createController(CONTROLLER_QUALITY_GATES)
      .setSince("4.3")
      .setDescription("Manage quality gates, including conditions and project association.");

    for (QualityGatesWsAction action : actions) {
      action.define(controller);
    }

    // unset_default is no more authorized
    controller.createAction("unset_default")
      .setDescription("This webservice is no more available : a default quality gate is mandatory.")
      .setSince("4.3")
      .setDeprecatedSince("7.0")
      .setPost(true)
      .setHandler(RemovedWebServiceHandler.INSTANCE)
      .setResponseExample(RemovedWebServiceHandler.INSTANCE.getResponseExample())
      .setChangelog(
        new Change("7.0", "Unset a quality gate is no more authorized")
      );

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
      .setMaximumLength(CONDITION_MAX_LENGTH)
      .setDescription("Condition warning threshold")
      .setExampleValue("5");

    action.createParam(PARAM_ERROR)
      .setMaximumLength(CONDITION_MAX_LENGTH)
      .setDescription("Condition error threshold")
      .setExampleValue("10");
  }

  static Long parseId(Request request, String paramName) {
    try {
      return Long.valueOf(request.mandatoryParam(paramName));
    } catch (NumberFormatException badFormat) {
      throw BadRequestException.create(paramName + " must be a valid long value");
    }
  }

}
