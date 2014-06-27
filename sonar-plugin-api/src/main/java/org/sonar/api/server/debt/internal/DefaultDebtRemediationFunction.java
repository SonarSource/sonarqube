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

package org.sonar.api.server.debt.internal;

import com.google.common.base.Objects;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.builder.EqualsBuilder;
import org.sonar.api.server.debt.DebtRemediationFunction;
import org.sonar.api.utils.Duration;

import javax.annotation.CheckForNull;
import javax.annotation.Nullable;

public class DefaultDebtRemediationFunction implements DebtRemediationFunction {

  private static final int HOURS_IN_DAY = 24;

  private final Type type;
  private final String coefficient;
  private final String offset;

  public DefaultDebtRemediationFunction(@Nullable Type type, @Nullable String coefficient, @Nullable String offset) {
    this.type = type;
    this.coefficient = sanitizeValue("coefficient", coefficient);
    this.offset = sanitizeValue("offset", offset);
    validate();
  }

  @CheckForNull
  private String sanitizeValue(String label, @Nullable String s) {
    if (StringUtils.isNotBlank(s)) {
      try {
        Duration duration = Duration.decode(s, HOURS_IN_DAY);
        return duration.encode(HOURS_IN_DAY);
      } catch (Exception e) {
        throw new IllegalArgumentException(String.format("Invalid %s: %s (%s)", label, s, e.getMessage()), e);
      }
    }
    return null;
  }

  @Override
  public Type type() {
    return type;
  }

  @Override
  @CheckForNull
  public String coefficient() {
    return coefficient;
  }

  @Override
  @CheckForNull
  public String offset() {
    return offset;
  }

  private void validate() {
    if (type == null) {
      throw new IllegalArgumentException("Remediation function type cannot be null");
    }
    switch (type) {
      case LINEAR:
        if (this.coefficient == null || this.offset != null) {
          throw new IllegalArgumentException("Linear functions must only have a non empty coefficient");
        }
        break;
      case LINEAR_OFFSET:
        if (this.coefficient == null || this.offset == null) {
          throw new IllegalArgumentException("Linear with offset functions must have both non null coefficient and offset");
        }
        break;
      case CONSTANT_ISSUE:
        if (this.coefficient != null || this.offset == null) {
          throw new IllegalArgumentException("Constant/issue functions must only have a non empty offset");
        }
        break;
      default:
        throw new IllegalArgumentException(String.format("Unknown type on %s", this));
    }
  }

  @Override
  public boolean equals(Object o) {
    if (!(o instanceof DefaultDebtRemediationFunction)) {
      return false;
    }
    if (this == o) {
      return true;
    }
    DefaultDebtRemediationFunction other = (DefaultDebtRemediationFunction) o;
    return new EqualsBuilder()
      .append(coefficient, other.coefficient())
      .append(offset, other.offset())
      .append(type, other.type())
      .isEquals();
  }

  @Override
  public int hashCode() {
    int result = type.hashCode();
    result = 31 * result + (coefficient != null ? coefficient.hashCode() : 0);
    result = 31 * result + (offset != null ? offset.hashCode() : 0);
    return result;
  }

  @Override
  public String toString() {
    return Objects.toStringHelper(DebtRemediationFunction.class)
      .add("type", type)
      .add("coefficient", coefficient)
      .add("offset", offset)
      .toString();
  }
}
