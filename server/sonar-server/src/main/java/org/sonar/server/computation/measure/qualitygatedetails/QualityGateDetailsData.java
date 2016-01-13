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
package org.sonar.server.computation.measure.qualitygatedetails;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import java.util.List;
import javax.annotation.concurrent.Immutable;
import org.sonar.server.computation.measure.Measure;
import org.sonar.server.computation.qualitygate.Condition;

import static com.google.common.collect.FluentIterable.from;
import static java.util.Objects.requireNonNull;

@Immutable
public class QualityGateDetailsData {
  private static final String FIELD_LEVEL = "level";

  private final Measure.Level level;
  private final List<EvaluatedCondition> conditions;

  public QualityGateDetailsData(Measure.Level level, Iterable<EvaluatedCondition> conditions) {
    this.level = requireNonNull(level);
    this.conditions = from(conditions).toList();
  }

  public String toJson() {
    JsonObject details = new JsonObject();
    details.addProperty(FIELD_LEVEL, level.toString());
    JsonArray conditionResults = new JsonArray();
    for (EvaluatedCondition condition : this.conditions) {
      conditionResults.add(toJson(condition));
    }
    details.add("conditions", conditionResults);
    return details.toString();
  }

  private static JsonObject toJson(EvaluatedCondition evaluatedCondition) {
    Condition condition = evaluatedCondition.getCondition();

    JsonObject result = new JsonObject();
    result.addProperty("metric", condition.getMetric().getKey());
    result.addProperty("op", condition.getOperator().getDbValue());
    if (condition.getPeriod() != null) {
      result.addProperty("period", condition.getPeriod());
    }
    if (condition.getWarningThreshold() != null) {
      result.addProperty("warning", condition.getWarningThreshold());
    }
    if (condition.getErrorThreshold() != null) {
      result.addProperty("error", condition.getErrorThreshold());
    }
    result.addProperty("actual", evaluatedCondition.getActualValue());
    result.addProperty(FIELD_LEVEL, evaluatedCondition.getLevel().name());
    return result;
  }

}
