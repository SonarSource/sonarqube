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
package org.sonar.server.qualitygate.ws;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.util.Optional;
import java.util.function.Predicate;
import java.util.stream.StreamSupport;
import javax.annotation.Nullable;
import org.sonar.db.component.SnapshotDto;
import org.sonarqube.ws.Qualitygates.ProjectStatusResponse;

import static com.google.common.base.Strings.isNullOrEmpty;
import static org.sonar.api.utils.DateUtils.formatDateTime;

public class QualityGateDetailsFormatter {
  private final Optional<String> optionalMeasureData;
  private final Optional<SnapshotDto> optionalSnapshot;
  private final ProjectStatusResponse.ProjectStatus.Builder projectStatusBuilder;

  public QualityGateDetailsFormatter(Optional<String> measureData, Optional<SnapshotDto> snapshot) {
    this.optionalMeasureData = measureData;
    this.optionalSnapshot = snapshot;
    this.projectStatusBuilder = ProjectStatusResponse.ProjectStatus.newBuilder();
  }

  public ProjectStatusResponse.ProjectStatus format() {
    if (!optionalMeasureData.isPresent()) {
      return newResponseWithoutQualityGateDetails();
    }

    JsonParser parser = new JsonParser();
    JsonObject json = parser.parse(optionalMeasureData.get()).getAsJsonObject();

    ProjectStatusResponse.Status qualityGateStatus = measureLevelToQualityGateStatus(json.get("level").getAsString());
    projectStatusBuilder.setStatus(qualityGateStatus);

    formatIgnoredConditions(json);
    formatConditions(json.getAsJsonArray("conditions"));
    formatPeriods();

    return projectStatusBuilder.build();
  }

  private void formatIgnoredConditions(JsonObject json) {
    JsonElement ignoredConditions = json.get("ignoredConditions");
    if (ignoredConditions != null) {
      projectStatusBuilder.setIgnoredConditions(ignoredConditions.getAsBoolean());
    } else {
      projectStatusBuilder.setIgnoredConditions(false);
    }
  }

  private void formatPeriods() {
    if (!optionalSnapshot.isPresent()) {
      return;
    }

    ProjectStatusResponse.Period.Builder periodBuilder = ProjectStatusResponse.Period.newBuilder();
    periodBuilder.clear();

    SnapshotDto snapshot = this.optionalSnapshot.get();
    String periodMode = snapshot.getPeriodMode();
    if (isNullOrEmpty(periodMode)) {
      return;
    }
    periodBuilder.setIndex(1);
    periodBuilder.setMode(snapshot.getPeriodMode());
    Long periodDate = snapshot.getPeriodDate();
    if (periodDate != null) {
      periodBuilder.setDate(formatDateTime(periodDate));
    }
    String periodModeParameter = snapshot.getPeriodModeParameter();
    if (!isNullOrEmpty(periodModeParameter)) {
      periodBuilder.setParameter(periodModeParameter);
    }
    projectStatusBuilder.addPeriods(periodBuilder);
  }

  private void formatConditions(@Nullable JsonArray jsonConditions) {
    if (jsonConditions == null) {
      return;
    }

    StreamSupport.stream(jsonConditions.spliterator(), false)
      .map(JsonElement::getAsJsonObject)
      .filter(isConditionOnValidPeriod())
      .forEach(this::formatCondition);
  }

  private void formatCondition(JsonObject jsonCondition) {
    ProjectStatusResponse.Condition.Builder conditionBuilder = ProjectStatusResponse.Condition.newBuilder();

    formatConditionLevel(conditionBuilder, jsonCondition);
    formatConditionMetric(conditionBuilder, jsonCondition);
    formatConditionOperation(conditionBuilder, jsonCondition);
    formatConditionPeriod(conditionBuilder, jsonCondition);
    formatConditionWarningThreshold(conditionBuilder, jsonCondition);
    formatConditionErrorThreshold(conditionBuilder, jsonCondition);
    formatConditionActual(conditionBuilder, jsonCondition);

    projectStatusBuilder.addConditions(conditionBuilder);
  }

  private static void formatConditionActual(ProjectStatusResponse.Condition.Builder conditionBuilder, JsonObject jsonCondition) {
    JsonElement actual = jsonCondition.get("actual");
    if (actual != null && !isNullOrEmpty(actual.getAsString())) {
      conditionBuilder.setActualValue(actual.getAsString());
    }
  }

  private static void formatConditionErrorThreshold(ProjectStatusResponse.Condition.Builder conditionBuilder, JsonObject jsonCondition) {
    JsonElement error = jsonCondition.get("error");
    if (error != null && !isNullOrEmpty(error.getAsString())) {
      conditionBuilder.setErrorThreshold(error.getAsString());
    }
  }

  private static void formatConditionWarningThreshold(ProjectStatusResponse.Condition.Builder conditionBuilder, JsonObject jsonCondition) {
    JsonElement warning = jsonCondition.get("warning");
    if (warning != null && !isNullOrEmpty(warning.getAsString())) {
      conditionBuilder.setWarningThreshold(warning.getAsString());
    }
  }

  private static void formatConditionPeriod(ProjectStatusResponse.Condition.Builder conditionBuilder, JsonObject jsonCondition) {
    JsonElement periodIndex = jsonCondition.get("period");
    if (periodIndex == null) {
      return;
    }
    conditionBuilder.setPeriodIndex(periodIndex.getAsInt());
  }

  private static void formatConditionOperation(ProjectStatusResponse.Condition.Builder conditionBuilder, JsonObject jsonCondition) {
    JsonElement op = jsonCondition.get("op");
    if (op != null && !isNullOrEmpty(op.getAsString())) {
      String stringOp = op.getAsString();
      ProjectStatusResponse.Comparator comparator = measureOpToQualityGateComparator(stringOp);
      conditionBuilder.setComparator(comparator);
    }
  }

  private static void formatConditionMetric(ProjectStatusResponse.Condition.Builder conditionBuilder, JsonObject jsonCondition) {
    JsonElement metric = jsonCondition.get("metric");
    if (metric != null && !isNullOrEmpty(metric.getAsString())) {
      conditionBuilder.setMetricKey(metric.getAsString());
    }
  }

  private static void formatConditionLevel(ProjectStatusResponse.Condition.Builder conditionBuilder, JsonObject jsonCondition) {
    JsonElement measureLevel = jsonCondition.get("level");
    if (measureLevel != null && !isNullOrEmpty(measureLevel.getAsString())) {
      conditionBuilder.setStatus(measureLevelToQualityGateStatus(measureLevel.getAsString()));
    }
  }

  private static ProjectStatusResponse.Status measureLevelToQualityGateStatus(String measureLevel) {
    for (ProjectStatusResponse.Status status : ProjectStatusResponse.Status.values()) {
      if (status.name().equals(measureLevel)) {
        return status;
      }
    }

    throw new IllegalStateException(String.format("Unknown quality gate status '%s'", measureLevel));
  }

  private static ProjectStatusResponse.Comparator measureOpToQualityGateComparator(String measureOp) {
    for (ProjectStatusResponse.Comparator comparator : ProjectStatusResponse.Comparator.values()) {
      if (comparator.name().equals(measureOp)) {
        return comparator;
      }
    }

    throw new IllegalStateException(String.format("Unknown quality gate comparator '%s'", measureOp));
  }

  private static ProjectStatusResponse.ProjectStatus newResponseWithoutQualityGateDetails() {
    return ProjectStatusResponse.ProjectStatus.newBuilder().setStatus(ProjectStatusResponse.Status.NONE).build();
  }

  private static Predicate<JsonObject> isConditionOnValidPeriod() {
    return jsonCondition -> {
      JsonElement periodIndex = jsonCondition.get("period");
      if (periodIndex == null) {
        return true;
      }
      int period = periodIndex.getAsInt();
      // Ignore period that was set on non leak period (for retro compatibility with project that hasn't been analyzed since a while)
      return period == 1;
    };
  }
}
