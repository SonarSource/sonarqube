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
package org.sonar.db.qualitygate;

import javax.annotation.CheckForNull;

public class QualityGateFindingDto {
  public static final String RATING_VALUE_TYPE = "RATING";
  public static final String PERCENT_VALUE_TYPE = "PERCENT";
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

  public String getDescription() {
    return description;
  }

  public String getOperatorDescription() {
    if (isRating(getValueType())) {
      return RatingType.valueOf(getOperator()).getDescription();
    }

    return PercentageType.valueOf(getOperator()).getDescription();
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
    if (isRating(getValueType())) {
      return RatingValue.valueOf(Integer.parseInt(errorThreshold));
    }

    return errorThreshold;
  }

  public boolean isOptimizedBestValue() {
    return optimizedBestValue;
  }

  @CheckForNull
  public Integer getDecimalScale() {
    return decimalScale;
  }

  private String getOperator() {
    return operator;
  }

  private static boolean isRating(String metricType) {
    return RATING_VALUE_TYPE.equals(metricType);
  }

  public enum RatingType {
    LT("Is Better Than"),
    GT("Is Worse Than"),
    EQ("Is"),
    NE("Is Not");

    private final String desc;

    RatingType(String desc) {
      this.desc = desc;
    }

    public String getDescription() {
      return desc;
    }
  }

  public enum PercentageType {
    LT("Is Less Than"),
    GT("Is Greater Than"),
    EQ("Is Equal To"),
    NE("Is Not Equal To");

    private final String desc;

    PercentageType(String desc) {
      this.desc = desc;
    }

    public String getDescription() {
      return desc;
    }
  }

  public enum RatingValue {
    A, B, C, D, E;

    public static String valueOf(int index) {
      return values()[index - 1].name();
    }
  }
}
