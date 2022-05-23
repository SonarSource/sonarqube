/*
 * SonarQube
 * Copyright (C) 2009-2022 SonarSource SA
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
package org.sonar.db.report;

import javax.annotation.CheckForNull;

public class QualityGateFindingDto {
  public static final String NEW_CODE_METRIC_PREFIX = "new_";

  private String description = null;
  private String operator = null;
  private String kee = null;
  private Boolean isEnabled = null;
  private String valueType = null;
  private Double bestValue = null;
  private Double worstValue = null;
  private Boolean optimizedBestValue = null;
  private String errorThreshold = null;
  private Integer decimalScale = null;

  private String getOperator() {
    return operator;
  }

  public String getDescription() {
    return description;
  }

  public String getOperatorDescription() {
    return OperatorDescription.valueOf(getOperator()).getDescription();
  }

  public Boolean isNewCodeMetric() {
    return kee.startsWith(NEW_CODE_METRIC_PREFIX);
  }

  public boolean isEnabled() {
    return isEnabled;
  }

  public String getValueType() {
    return valueType;
  }

  @CheckForNull
  public Double getBestValue() {
    return bestValue;
  }

  @CheckForNull
  public Double getWorstValue() {
    return worstValue;
  }

  public String getErrorThreshold() {
    return errorThreshold;
  }

  public boolean isOptimizedBestValue() {
    return optimizedBestValue;
  }

  @CheckForNull
  public Integer getDecimalScale() {
    return decimalScale;
  }

  public enum OperatorDescription {
    LT("Is Less Than"),
    GT("Is Greater Than"),
    EQ("Is Equal To"),
    NE("Is Not Equal To");

    private final String desc;

    OperatorDescription(String desc) {
      this.desc = desc;
    }

    public String getDescription() {
      return desc;
    }
  }
}
