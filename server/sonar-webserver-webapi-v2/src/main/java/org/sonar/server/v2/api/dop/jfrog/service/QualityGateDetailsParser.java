/*
 * SonarQube
 * Copyright (C) 2009-2025 SonarSource Sàrl
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
package org.sonar.server.v2.api.dop.jfrog.service;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import jakarta.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.sonar.server.v2.api.dop.jfrog.response.GateCondition;
import org.sonar.server.v2.api.dop.jfrog.response.GateStatus;
import org.sonar.server.v2.api.dop.jfrog.response.QualityGateEvidence;
import org.sonar.server.v2.api.dop.jfrog.response.SonarQubePredicate;

public final class QualityGateDetailsParser {

  private QualityGateDetailsParser() {
  }

  public static SonarQubePredicate parse(@Nullable String measureData) {
    return Optional.ofNullable(measureData)
      .map(QualityGateDetailsParser::parseFromJson)
      .orElseGet(QualityGateDetailsParser::createEmptyPredicate);
  }

  private static SonarQubePredicate createEmptyPredicate() {
    QualityGateEvidence qualityGate = QualityGateEvidence.builder()
      .status(GateStatus.NONE)
      .ignoredConditions(false)
      .conditions(List.of())
      .build();
    return new SonarQubePredicate(List.of(qualityGate));
  }

  private static SonarQubePredicate parseFromJson(String measureData) {
    JsonObject json = JsonParser.parseString(measureData).getAsJsonObject();
    GateStatus status = parseStatus(json.get("level").getAsString());
    boolean ignoredConditions = parseIgnoredConditions(json);
    List<GateCondition> conditions = parseConditions(json.getAsJsonArray("conditions"));

    QualityGateEvidence qualityGate = QualityGateEvidence.builder()
      .status(status)
      .ignoredConditions(ignoredConditions)
      .conditions(conditions)
      .build();

    return new SonarQubePredicate(List.of(qualityGate));
  }

  static GateStatus parseStatus(String level) {
    return switch (level) {
      case "OK" -> GateStatus.OK;
      case "ERROR" -> GateStatus.ERROR;
      case "WARN" -> GateStatus.WARN;
      default -> GateStatus.NONE;
    };
  }

  private static boolean parseIgnoredConditions(JsonObject json) {
    JsonElement ignoredConditions = json.get("ignoredConditions");
    return ignoredConditions != null && ignoredConditions.getAsBoolean();
  }

  private static List<GateCondition> parseConditions(@Nullable JsonArray jsonConditions) {
    if (jsonConditions == null) {
      return List.of();
    }

    List<GateCondition> conditions = new ArrayList<>();
    for (JsonElement element : jsonConditions) {
      JsonObject jsonCondition = element.getAsJsonObject();
      parseCondition(jsonCondition).ifPresent(conditions::add);
    }
    return conditions;
  }

  private static Optional<GateCondition> parseCondition(JsonObject jsonCondition) {
    // Skip conditions on non-leak period (for retro compatibility)
    JsonElement periodIndex = jsonCondition.get("period");
    if (periodIndex != null && periodIndex.getAsInt() != 1) {
      return Optional.empty();
    }

    GateCondition.Builder builder = GateCondition.builder();

    JsonElement level = jsonCondition.get("level");
    if (level != null) {
      builder.status(parseStatus(level.getAsString()));
    }

    JsonElement metric = jsonCondition.get("metric");
    if (metric != null) {
      builder.metricKey(metric.getAsString());
    }

    JsonElement op = jsonCondition.get("op");
    if (op != null) {
      builder.comparator(parseComparator(op.getAsString()));
    }

    JsonElement error = jsonCondition.get("error");
    if (error != null) {
      builder.errorThreshold(error.getAsString());
    }

    JsonElement actual = jsonCondition.get("actual");
    if (actual != null) {
      builder.actualValue(actual.getAsString());
    }

    return Optional.of(builder.build());
  }

  private static GateCondition.Comparator parseComparator(String op) {
    return switch (op) {
      case "LT" -> GateCondition.Comparator.LT;
      case "GT" -> GateCondition.Comparator.GT;
      case "EQ" -> GateCondition.Comparator.EQ;
      case "NE" -> GateCondition.Comparator.NE;
      default -> throw new IllegalStateException("Unknown comparator: " + op);
    };
  }

}
