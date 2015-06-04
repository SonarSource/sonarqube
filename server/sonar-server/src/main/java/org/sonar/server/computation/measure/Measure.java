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
package org.sonar.server.computation.measure;

import com.google.common.base.Optional;
import javax.annotation.CheckForNull;
import javax.annotation.Nullable;

public interface Measure {

  enum ValueType {
    NO_VALUE, BOOLEAN, INT, LONG, DOUBLE, STRING
  }

  enum AlertStatus {
    OK("Green"),
    WARN("Orange"),
    ERROR("Red");

    private final String colorName;

    AlertStatus(String colorName) {
      this.colorName = colorName;
    }

    public String getColorName() {
      return colorName;
    }

    public static Optional<Measure.AlertStatus> toAlertStatus(@Nullable String alertStatus) {
      if (alertStatus == null) {
        return Optional.absent();
      }
      try {
        return Optional.of(Measure.AlertStatus.valueOf(alertStatus));
      } catch (IllegalArgumentException e) {
        return Optional.absent();
      }
    }
  }

  /**
   * The type of value stored in the measure.
   */
  ValueType getValueType();

  /**
   * The value of this measure as a boolean if the type is {@link Measure.ValueType#BOOLEAN}.
   *
   * @throws IllegalStateException if the value type of the measure is not {@link Measure.ValueType#BOOLEAN}
   */
  boolean getBooleanValue();

  /**
   * The value of this measure as a int if the type is {@link Measure.ValueType#INT}.
   *
   * @throws IllegalStateException if the value type of the measure is not {@link Measure.ValueType#INT}
   */
  int getIntValue();

  /**
   * The value of this measure as a long if the type is {@link Measure.ValueType#LONG}.
   *
   * @throws IllegalStateException if the value type of the measure is not {@link Measure.ValueType#LONG}
   */
  long getLongValue();

  /**
   * The value of this measure as a double if the type is {@link Measure.ValueType#DOUBLE}.
   *
   * @throws IllegalStateException if the value type of the measure is not {@link Measure.ValueType#DOUBLE}
   */
  double getDoubleValue();

  /**
   * The value of this measure as a String if the type is {@link Measure.ValueType#STRING}.
   *
   * @throws IllegalStateException if the value type of the measure is not {@link Measure.ValueType#STRING}
   */
  String getStringValue();

  /**
   * The data of this measure if it exists.
   * <p>
   * If the measure type is {@link Measure.ValueType#STRING}, the value returned by this function is the same as {@link #getStringValue()}.
   * </p>
   *
   * @throws IllegalStateException if the value type of the measure is not {@link Measure.ValueType#BOOLEAN}
   */
  @CheckForNull
  String getData();

  /**
   * Any Measure, which ever is its value type, can have an alert status.
   */
  boolean hasAlertStatus();

  /**
   * The alert status for this measure.
   * <strong>Don't call this method unless you've checked the result of {@link #hasAlertStatus()} first</strong>
   *
   * @throws IllegalStateException if the measure has no alert status
   */
  AlertStatus getAlertStatus();

  /**
   * Any Measure, which ever is its value type, can have an alert status.
   * <p>It does not make sense to have an alert text without an alert status but nothing enforces this consistency
   * as the measure level</p>
   */
  boolean hasAlertText();

  /**
   * The alert text for this measure.
   * <strong>Don't call this method unless you've checked the result of {@link #hasAlertText()} first</strong>
   *
   * @throws IllegalStateException if the measure has no alert text
   */
  String getAlertText();

  Integer getSeverityIndex();

}
