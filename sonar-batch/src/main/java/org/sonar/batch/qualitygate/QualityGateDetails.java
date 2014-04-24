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
package org.sonar.batch.qualitygate;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.inject.internal.util.Lists;
import org.sonar.api.measures.Metric.Level;

import java.util.List;

/**
 * Holds the details of a quality gate evaluation (status per condition)
 *
 * @since 4.4
 */
class QualityGateDetails {

  private static final String FIELD_LEVEL = "level";

  private Level level = Level.OK;

  private List<EvaluatedCondition> conditions = Lists.newArrayList();

  void setLevel(Level level) {
    this.level = level;
  }

  void addCondition(ResolvedCondition condition, Level level, Double actualValue) {
    conditions.add(new EvaluatedCondition(condition, level, actualValue));
  }

  String toJson() {
    JsonObject details = new JsonObject();
    details.addProperty(FIELD_LEVEL, level.toString());
    JsonArray conditionResults = new JsonArray();
    for (EvaluatedCondition condition: this.conditions) {
      conditionResults.add(condition.toJson());
    }
    details.add("conditions", conditionResults);
    return details.toString();
  }

  static class EvaluatedCondition {

    private ResolvedCondition condition;

    private Level level;

    private String actualValue;

    EvaluatedCondition(ResolvedCondition condition, Level level, Double actualValue) {
      this.condition = condition;
      this.level = level;
      this.actualValue = actualValue == null ? "" : actualValue.toString();
    }

    JsonObject toJson() {
      JsonObject result = new JsonObject();
      result.addProperty("metric", condition.metricKey());
      result.addProperty("op", condition.operator());
      if (condition.period() != null) {
        result.addProperty("period", condition.period());
      }
      if (condition.warningThreshold() != null) {
        result.addProperty("warning", condition.warningThreshold());
      }
      if (condition.errorThreshold() != null) {
        result.addProperty("error", condition.errorThreshold());
      }
      result.addProperty("actual", actualValue);
      result.addProperty(FIELD_LEVEL, level.toString());
      return result;
    }
  }
}
