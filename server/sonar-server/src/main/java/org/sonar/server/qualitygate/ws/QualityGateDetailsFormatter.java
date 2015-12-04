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

package org.sonar.server.qualitygate.ws;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import javax.annotation.CheckForNull;
import javax.annotation.Nullable;
import org.sonar.db.component.SnapshotDto;
import org.sonarqube.ws.WsQualityGates.ProjectStatusWsResponse;

import static com.google.common.base.Strings.isNullOrEmpty;
import static org.sonar.api.utils.DateUtils.formatDateTime;

public class QualityGateDetailsFormatter {
  @CheckForNull
  private final String measureData;
  private final SnapshotDto snapshot;
  private final ProjectStatusWsResponse.ProjectStatus.Builder projectStatusBuilder;

  public QualityGateDetailsFormatter(@Nullable String measureData, SnapshotDto snapshot) {
    this.measureData = measureData;
    this.snapshot = snapshot;
    this.projectStatusBuilder = ProjectStatusWsResponse.ProjectStatus.newBuilder();
  }

  public ProjectStatusWsResponse.ProjectStatus format() {
    if (isNullOrEmpty(measureData)) {
      return newResponseWithoutQualityGateDetails();
    }

    JsonParser parser = new JsonParser();
    JsonObject json = parser.parse(measureData).getAsJsonObject();

    ProjectStatusWsResponse.Status qualityGateStatus = measureLevelToQualityGateStatus(json.get("level").getAsString());
    projectStatusBuilder.setStatus(qualityGateStatus);

    formatConditions(json.getAsJsonArray("conditions"));
    formatPeriods();

    return projectStatusBuilder.build();
  }

  private void formatPeriods() {
    ProjectStatusWsResponse.Period.Builder periodBuilder = ProjectStatusWsResponse.Period.newBuilder();
    for (int i = 1; i <= 5; i++) {
      periodBuilder.clear();
      boolean doesPeriodExist = false;

      if (!isNullOrEmpty(snapshot.getPeriodMode(i))) {
        doesPeriodExist = true;
        periodBuilder.setIndex(i);
        periodBuilder.setMode(snapshot.getPeriodMode(i));
        if (snapshot.getPeriodDate(i) != null) {
          periodBuilder.setDate(formatDateTime(snapshot.getPeriodDate(i)));
        }
        if (!isNullOrEmpty(snapshot.getPeriodModeParameter(i))) {
          periodBuilder.setParameter(snapshot.getPeriodModeParameter(i));
        }
      }

      if (doesPeriodExist) {
        projectStatusBuilder.addPeriods(periodBuilder);
      }
    }
  }

  private void formatConditions(@Nullable JsonArray jsonConditions) {
    if (jsonConditions == null) {
      return;
    }

    for (JsonElement jsonCondition : jsonConditions) {
      formatCondition(jsonCondition.getAsJsonObject());
    }
  }

  private void formatCondition(JsonObject jsonCondition) {
    ProjectStatusWsResponse.Condition.Builder conditionBuilder = ProjectStatusWsResponse.Condition.newBuilder();

    JsonElement measureLevel = jsonCondition.get("level");
    if (measureLevel != null && !isNullOrEmpty(measureLevel.getAsString())) {
      conditionBuilder.setStatus(measureLevelToQualityGateStatus(measureLevel.getAsString()));
    }

    JsonElement metric = jsonCondition.get("metric");
    if (metric != null && !isNullOrEmpty(metric.getAsString())) {
      conditionBuilder.setMetricKey(metric.getAsString());
    }

    JsonElement op = jsonCondition.get("op");
    if (op != null && !isNullOrEmpty(op.getAsString())) {
      String stringOp = op.getAsString();
      ProjectStatusWsResponse.Comparator comparator = measureOpToQualityGateComparator(stringOp);
      conditionBuilder.setComparator(comparator);
    }

    JsonElement periodIndex = jsonCondition.get("period");
    if (periodIndex != null && !isNullOrEmpty(periodIndex.getAsString())) {
      conditionBuilder.setPeriodIndex(periodIndex.getAsInt());
    }

    JsonElement warning = jsonCondition.get("warning");
    if (warning != null && !isNullOrEmpty(warning.getAsString())) {
      conditionBuilder.setWarningThreshold(warning.getAsString());
    }

    JsonElement error = jsonCondition.get("error");
    if (error != null && !isNullOrEmpty(error.getAsString())) {
      conditionBuilder.setErrorThreshold(error.getAsString());
    }

    JsonElement actual = jsonCondition.get("actual");
    if (actual != null && !isNullOrEmpty(actual.getAsString())) {
      conditionBuilder.setActualValue(actual.getAsString());
    }

    projectStatusBuilder.addConditions(conditionBuilder);
  }

  private static ProjectStatusWsResponse.Status measureLevelToQualityGateStatus(String measureLevel) {
    for (ProjectStatusWsResponse.Status status : ProjectStatusWsResponse.Status.values()) {
      if (status.name().equals(measureLevel)) {
        return status;
      }
    }

    throw new IllegalStateException(String.format("Unknown quality gate status '%s'", measureLevel));
  }

  private static ProjectStatusWsResponse.Comparator measureOpToQualityGateComparator(String measureOp) {
    for (ProjectStatusWsResponse.Comparator comparator : ProjectStatusWsResponse.Comparator.values()) {
      if (comparator.name().equals(measureOp)) {
        return comparator;
      }
    }

    throw new IllegalStateException(String.format("Unknown quality gate comparator '%s'", measureOp));
  }

  private static ProjectStatusWsResponse.ProjectStatus newResponseWithoutQualityGateDetails() {
    return ProjectStatusWsResponse.ProjectStatus.newBuilder().setStatus(ProjectStatusWsResponse.Status.NONE).build();
  }
}
