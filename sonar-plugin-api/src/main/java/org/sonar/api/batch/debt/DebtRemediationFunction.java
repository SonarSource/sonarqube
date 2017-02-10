/*
 * SonarQube
 * Copyright (C) 2009-2017 SonarSource SA
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
package org.sonar.api.batch.debt;

import org.apache.commons.lang.builder.EqualsBuilder;
import org.apache.commons.lang.builder.HashCodeBuilder;
import org.apache.commons.lang.builder.ReflectionToStringBuilder;
import org.apache.commons.lang.builder.ToStringStyle;
import org.sonar.api.utils.Duration;

import javax.annotation.CheckForNull;
import javax.annotation.Nullable;

/**
 * @since 4.3
 */
public class DebtRemediationFunction {

  public enum Type {
    LINEAR, LINEAR_OFFSET, CONSTANT_ISSUE
  }

  private Type type;
  private Duration coefficient;
  private Duration offset;

  private DebtRemediationFunction(Type type, @Nullable Duration coefficient, @Nullable Duration offset) {
    this.type = type;
    this.coefficient = coefficient;
    this.offset = offset;
  }

  public static DebtRemediationFunction create(Type type, @Nullable Duration coefficient, @Nullable Duration offset) {
    return new DebtRemediationFunction(type, coefficient, offset);
  }

  public static DebtRemediationFunction createLinear(Duration coefficient) {
    return new DebtRemediationFunction(Type.LINEAR, coefficient, null);
  }

  public static DebtRemediationFunction createLinearWithOffset(Duration coefficient, Duration offset) {
    return new DebtRemediationFunction(Type.LINEAR_OFFSET, coefficient, offset);
  }

  public static DebtRemediationFunction createConstantPerIssue(Duration offset) {
    return new DebtRemediationFunction(Type.CONSTANT_ISSUE, null, offset);
  }

  public Type type() {
    return type;
  }

  @CheckForNull
  public Duration coefficient() {
    return coefficient;
  }

  @CheckForNull
  public Duration offset() {
    return offset;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    DebtRemediationFunction that = (DebtRemediationFunction) o;
    return new EqualsBuilder()
      .append(type, that.type())
      .append(coefficient, that.coefficient())
      .append(offset, that.offset())
      .isEquals();
  }

  @Override
  public int hashCode() {
    return new HashCodeBuilder(15, 31)
      .append(type)
      .append(coefficient)
      .append(offset)
      .toHashCode();
  }

  @Override
  public String toString() {
    return new ReflectionToStringBuilder(this, ToStringStyle.SHORT_PREFIX_STYLE).toString();
  }
}
