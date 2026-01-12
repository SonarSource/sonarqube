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
package org.sonar.server.v2.api.dop.jfrog.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

/**
 * Evidence for a quality gate evaluation result.
 * Named QualityGateEvidence to avoid confusion with existing QualityGate classes.
 */
public record QualityGateEvidence(
  @JsonProperty("type") GateType type,
  @JsonProperty("status") GateStatus status,
  @JsonProperty("ignoredConditions") boolean ignoredConditions,
  @JsonProperty("conditions") List<GateCondition> conditions) {

  public enum GateType {
    QUALITY
  }

  public static Builder builder() {
    return new Builder();
  }

  public static class Builder {
    private GateType type = GateType.QUALITY;
    private GateStatus status;
    private boolean ignoredConditions;
    private List<GateCondition> conditions;

    public Builder type(GateType type) {
      this.type = type;
      return this;
    }

    public Builder status(GateStatus status) {
      this.status = status;
      return this;
    }

    public Builder ignoredConditions(boolean ignoredConditions) {
      this.ignoredConditions = ignoredConditions;
      return this;
    }

    public Builder conditions(List<GateCondition> conditions) {
      this.conditions = conditions;
      return this;
    }

    public QualityGateEvidence build() {
      return new QualityGateEvidence(type, status, ignoredConditions, conditions);
    }
  }

}
