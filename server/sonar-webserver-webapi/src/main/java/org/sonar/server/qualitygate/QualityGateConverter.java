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
package org.sonar.server.qualitygate;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

public class QualityGateConverter {

  private static final String FIELD_LEVEL = "level";
  private static final String FIELD_IGNORED_CONDITIONS = "ignoredConditions";

  private QualityGateConverter() {
    // prevent instantiation
  }

  public static String toJson(EvaluatedQualityGate gate) {
    JsonObject details = new JsonObject();
    details.addProperty(FIELD_LEVEL, gate.getStatus().name());
    JsonArray conditionResults = new JsonArray();
    for (EvaluatedCondition condition : gate.getEvaluatedConditions()) {
      conditionResults.add(toJson(condition));
    }
    details.add("conditions", conditionResults);
    details.addProperty(FIELD_IGNORED_CONDITIONS, gate.hasIgnoredConditionsOnSmallChangeset());
    return details.toString();
  }

  private static JsonObject toJson(EvaluatedCondition evaluatedCondition) {
    Condition condition = evaluatedCondition.getCondition();

    JsonObject result = new JsonObject();
    result.addProperty("metric", condition.getMetricKey());
    result.addProperty("op", condition.getOperator().getDbValue());
    if (condition.isOnLeakPeriod()) {
      result.addProperty("period", 1);
    }
    result.addProperty("error", condition.getErrorThreshold());
    evaluatedCondition.getValue().ifPresent(v -> result.addProperty("actual", v));
    result.addProperty(FIELD_LEVEL, evaluatedCondition.getStatus().name());
    return result;
  }
}
