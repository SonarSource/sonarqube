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
package org.sonar.api.server.debt.internal;

import com.google.common.base.MoreObjects;
import javax.annotation.CheckForNull;
import javax.annotation.Nullable;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.builder.EqualsBuilder;
import org.sonar.api.server.debt.DebtRemediationFunction;
import org.sonar.api.utils.Duration;

import static com.google.common.base.Preconditions.checkArgument;

public class DefaultDebtRemediationFunction implements DebtRemediationFunction {

  private static final int HOURS_IN_DAY = 24;

  private final Type type;
  private final String gapMultiplier;
  private final String baseEffort;

  public DefaultDebtRemediationFunction(@Nullable Type type, @Nullable String gapMultiplier, @Nullable String baseEffort) {
    this.type = type;
    this.gapMultiplier = sanitizeValue("gap multiplier", gapMultiplier);
    this.baseEffort = sanitizeValue("base effort", baseEffort);
    validate();
  }

  @CheckForNull
  private static String sanitizeValue(String label, @Nullable String s) {
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

  /**
   * @deprecated since 5.5, replaced by {@link #gapMultiplier}
   */
  @Override
  @CheckForNull
  @Deprecated
  public String coefficient() {
    return gapMultiplier();
  }


  @Override
  @CheckForNull
  public String gapMultiplier() {
    return gapMultiplier;
  }

  /**
   * @deprecated since 5.5, replaced by {@link #baseEffort}
   */
  @Override
  @CheckForNull
  @Deprecated
  public String offset() {
    return baseEffort();
  }

  @Override
  public String baseEffort() {
    return baseEffort;
  }


  private void validate() {
    checkArgument(type != null, "Remediation function type cannot be null");
    switch (type) {
      case LINEAR:
        checkArgument(this.gapMultiplier != null && this.baseEffort == null, "Linear functions must only have a non empty gap multiplier");
        break;
      case LINEAR_OFFSET:
        checkArgument(this.gapMultiplier != null && this.baseEffort != null, "Linear with offset functions must have both non null gap multiplier and base effort");
        break;
      case CONSTANT_ISSUE:
        checkArgument(this.gapMultiplier == null && this.baseEffort != null, "Constant/issue functions must only have a non empty base effort");
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
      .append(gapMultiplier, other.gapMultiplier())
      .append(baseEffort, other.baseEffort())
      .append(type, other.type())
      .isEquals();
  }

  @Override
  public int hashCode() {
    int result = type.hashCode();
    result = 31 * result + (gapMultiplier != null ? gapMultiplier.hashCode() : 0);
    result = 31 * result + (baseEffort != null ? baseEffort.hashCode() : 0);
    return result;
  }

  
  @Override
  public String toString() {
    return MoreObjects.toStringHelper(DebtRemediationFunction.class)
      .add("type", type)
      .add("gap multiplier", gapMultiplier)
      .add("base effort", baseEffort)
      .toString();
  }
}
