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
package org.sonar.server.qualitygate;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import java.util.List;
import javax.annotation.CheckForNull;
import org.sonar.api.measures.CoreMetrics;

import static org.sonar.db.qualitygate.QualityGateConditionDto.OPERATOR_GREATER_THAN;

/**
 * Offers constants describing the Hardcoded Quality Gate for short living branches.
 */
public final class ShortLivingBranchQualityGate {
  public static final long ID = -1_963_456_987L;
  public static final String NAME = "Hardcoded short living branch quality gate";
  public static final List<Condition> CONDITIONS = ImmutableList.of(
    new Condition(CoreMetrics.BUGS_KEY, OPERATOR_GREATER_THAN, "0", false),
    new Condition(CoreMetrics.VULNERABILITIES_KEY, OPERATOR_GREATER_THAN, "0", false),
    new Condition(CoreMetrics.CODE_SMELLS_KEY, OPERATOR_GREATER_THAN, "0", false));

  public static final QualityGate GATE = new QualityGate(String.valueOf(ID), NAME, ImmutableSet.of(
    new org.sonar.server.qualitygate.Condition(CoreMetrics.BUGS_KEY, org.sonar.server.qualitygate.Condition.Operator.GREATER_THAN, "0", null, false),
    new org.sonar.server.qualitygate.Condition(CoreMetrics.VULNERABILITIES_KEY, org.sonar.server.qualitygate.Condition.Operator.GREATER_THAN, "0", null, false),
    new org.sonar.server.qualitygate.Condition(CoreMetrics.CODE_SMELLS_KEY, org.sonar.server.qualitygate.Condition.Operator.GREATER_THAN, "0", null, false)));

  private ShortLivingBranchQualityGate() {
    // prevents instantiation
  }

  public static final class Condition {
    private final String metricKey;
    private final String operator;
    private final String errorThreshold;
    private final boolean onLeak;

    public Condition(String metricKey, String operator, String errorThreshold, boolean onLeak) {
      this.metricKey = metricKey;
      this.operator = operator;
      this.errorThreshold = errorThreshold;
      this.onLeak = onLeak;
    }

    public String getMetricKey() {
      return metricKey;
    }

    public String getOperator() {
      return operator;
    }

    public String getErrorThreshold() {
      return errorThreshold;
    }

    @CheckForNull
    public String getWarnThreshold() {
      return null;
    }

    public boolean isOnLeak() {
      return onLeak;
    }
  }
}
