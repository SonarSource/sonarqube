/*
 * SonarQube
 * Copyright (C) 2009-2025 SonarSource SA
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

public class QualityGateFindingDto {
  public static final String RATING_VALUE_TYPE = "RATING";
  public static final String PERCENT_VALUE_TYPE = "PERCENT";

  private String description = null;
  private String operator = null;
  private String valueType = null;
  private String errorThreshold = null;
  private String qualityGateName = null;

  public String getDescription() {
    return description;
  }

  public String getOperatorDescription() {
    if (isRating(getValueType())) {
      return RatingType.valueOf(getOperator()).getDescription();
    }

    return PercentageType.valueOf(getOperator()).getDescription();
  }

  public String getErrorThreshold() {
    if (isRating(getValueType())) {
      return RatingValue.valueOf(Integer.parseInt(errorThreshold));
    }

    if (isPercentage(getValueType())) {
      return errorThreshold + "%";
    }

    return errorThreshold;
  }

  public String getQualityGateName() {
    return qualityGateName;
  }

  private String getOperator() {
    return operator;
  }

  private String getValueType() {
    return valueType;
  }

  private static boolean isRating(String metricType) {
    return RATING_VALUE_TYPE.equals(metricType);
  }

  private static boolean isPercentage(String metricType) {
    return PERCENT_VALUE_TYPE.equals(metricType);
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
