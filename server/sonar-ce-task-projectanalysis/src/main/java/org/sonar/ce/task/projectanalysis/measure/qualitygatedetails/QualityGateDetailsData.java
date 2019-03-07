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
package org.sonar.ce.task.projectanalysis.measure.qualitygatedetails;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;
import javax.annotation.concurrent.Immutable;
import org.sonar.ce.task.projectanalysis.measure.Measure;
import org.sonar.ce.task.projectanalysis.qualitygate.Condition;
import org.sonar.server.qualitygate.ConditionComparator;

import static java.util.Objects.requireNonNull;

@Immutable
public class QualityGateDetailsData {
  private static final String FIELD_LEVEL = "level";
  private static final String FIELD_IGNORED_CONDITIONS = "ignoredConditions";

  private final Measure.Level level;
  private final List<EvaluatedCondition> conditions;
  private final boolean ignoredConditions;

  public QualityGateDetailsData(Measure.Level level, Iterable<EvaluatedCondition> conditions, boolean ignoredConditions) {
    this.level = requireNonNull(level);
    this.conditions = StreamSupport.stream(conditions.spliterator(), false)
      .sorted(new ConditionComparator<>(c -> c.getCondition().getMetric().getKey()))
      .collect(Collectors.toList());
    this.ignoredConditions = ignoredConditions;
  }

  public String toJson() {
    JsonObject details = new JsonObject();
    details.addProperty(FIELD_LEVEL, level.toString());
    JsonArray conditionResults = new JsonArray();
    for (EvaluatedCondition condition : this.conditions) {
      conditionResults.add(toJson(condition));
    }
    details.add("conditions", conditionResults);
    details.addProperty(FIELD_IGNORED_CONDITIONS, ignoredConditions);
    return details.toString();
  }

  private static JsonObject toJson(EvaluatedCondition evaluatedCondition) {
    Condition condition = evaluatedCondition.getCondition();

    JsonObject result = new JsonObject();
    result.addProperty("metric", condition.getMetric().getKey());
    result.addProperty("op", condition.getOperator().getDbValue());
    if (condition.useVariation()) {
      // without this for new_ metrics, the UI will show "-" instead of
      // the actual value in the QG failure reason
      result.addProperty("period", 1);
    }
    result.addProperty("error", condition.getErrorThreshold());
    result.addProperty("actual", evaluatedCondition.getActualValue());
    result.addProperty(FIELD_LEVEL, evaluatedCondition.getLevel().name());
    return result;
  }

}
