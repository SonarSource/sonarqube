/*
 * SonarQube
 * Copyright (C) 2009-2024 SonarSource SA
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

import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.sonar.api.server.ws.WebService;
import org.sonar.server.qualitygate.Condition;

import static org.sonar.server.qualitygate.QualityGateConditionsUpdater.INVALID_METRIC_KEYS;
import static org.sonar.server.qualitygate.ws.QualityGatesWsParameters.CONTROLLER_QUALITY_GATES;
import static org.sonar.server.qualitygate.ws.QualityGatesWsParameters.PARAM_ERROR;
import static org.sonar.server.qualitygate.ws.QualityGatesWsParameters.PARAM_METRIC;
import static org.sonar.server.qualitygate.ws.QualityGatesWsParameters.PARAM_OPERATOR;

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

    controller.done();
  }

  static void addConditionParams(NewAction action) {
    action
      .createParam(PARAM_METRIC)
      .setDescription("Condition metric.<br/>" +
        " Only metric of the following types are allowed:" +
        "<ul>" +
        "<li>INT</li>" +
        "<li>MILLISEC</li>" +
        "<li>RATING</li>" +
        "<li>WORK_DUR</li>" +
        "<li>FLOAT</li>" +
        "<li>PERCENT</li>" +
        "<li>LEVEL</li></ul>" +
        "Following metrics are forbidden:" +
        "<ul>" + getInvalidMetrics() + "</ul>")
      .setRequired(true)
      .setExampleValue("blocker_violations, vulnerabilities, new_code_smells");

    action.createParam(PARAM_OPERATOR)
      .setDescription("Condition operator:<br/>" +
        "<ul>" +
        "<li>LT = is lower than</li>" +
        "<li>GT = is greater than</li></ul>")
      .setExampleValue(Condition.Operator.GREATER_THAN.getDbValue())
      .setPossibleValues(getPossibleOperators());

    action.createParam(PARAM_ERROR)
      .setMaximumLength(CONDITION_MAX_LENGTH)
      .setDescription("Condition error threshold")
      .setRequired(true)
      .setExampleValue("10");
  }

  private static String getInvalidMetrics() {
    return INVALID_METRIC_KEYS.stream().map(s -> "<li>" + s + "</li>")
      .collect(Collectors.joining());
  }

  private static Set<String> getPossibleOperators() {
    return Stream.of(Condition.Operator.values())
      .map(Condition.Operator::getDbValue)
      .collect(Collectors.toSet());
  }
}
