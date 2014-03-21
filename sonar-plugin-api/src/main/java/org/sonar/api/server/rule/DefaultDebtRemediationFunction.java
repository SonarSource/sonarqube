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

package org.sonar.api.server.rule;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.builder.EqualsBuilder;
import org.apache.commons.lang.builder.HashCodeBuilder;
import org.apache.commons.lang.builder.ReflectionToStringBuilder;
import org.apache.commons.lang.builder.ToStringStyle;

import javax.annotation.CheckForNull;
import javax.annotation.Nullable;

class DefaultDebtRemediationFunction implements DebtRemediationFunction {

  private Type type;
  private String factor;
  private String offset;

  private DefaultDebtRemediationFunction(Type type, @Nullable String factor, @Nullable String offset) {
    this.type = type;
    // TODO validate factor and offset format
    this.factor = StringUtils.deleteWhitespace(factor);
    this.offset = StringUtils.deleteWhitespace(offset);
    validate();
  }

  static DebtRemediationFunction create(Type type, @Nullable String factor, @Nullable String offset) {
    return new DefaultDebtRemediationFunction(type, factor, offset);
  }

  static DebtRemediationFunction createLinear(String factor) {
    return new DefaultDebtRemediationFunction(Type.LINEAR, factor, null);
  }

  static DebtRemediationFunction createLinearWithOffset(String factor, String offset) {
    return new DefaultDebtRemediationFunction(Type.LINEAR_OFFSET, factor, offset);
  }

  static DebtRemediationFunction createConstantPerIssue(String offset) {
    return new DefaultDebtRemediationFunction(Type.CONSTANT_ISSUE, null, offset);
  }

  public Type type() {
    return type;
  }

  @CheckForNull
  public String factor() {
    return factor;
  }

  @CheckForNull
  public String offset() {
    return offset;
  }

  private void validate() {
    switch (type) {
      case LINEAR:
        if (this.factor == null || this.offset != null) {
          throw new ValidationException(String.format("%s is invalid, Linear remediation function should only define a factor", this));
        }
        break;
      case LINEAR_OFFSET:
        if (this.factor == null || this.offset == null) {
          throw new ValidationException(String.format("%s is invalid,  Linear with offset remediation function should define both factor and offset", this));
        }
        break;
      case CONSTANT_ISSUE:
        if (this.factor != null || this.offset == null) {
          throw new ValidationException(String.format("%s is invalid, Constant/issue remediation function should only define an offset", this));
        }
        break;
      default:
        throw new IllegalStateException(String.format("Remediation function of %s is unknown", this));
    }
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
      .append(factor, that.factor())
      .append(offset, that.offset())
      .isEquals();
  }

  @Override
  public int hashCode() {
    return new HashCodeBuilder(15, 33)
      .append(type)
      .append(factor)
      .append(offset)
      .toHashCode();
  }

  @Override
  public String toString() {
    return new ReflectionToStringBuilder(this, ToStringStyle.SHORT_PREFIX_STYLE).toString();
  }
}
