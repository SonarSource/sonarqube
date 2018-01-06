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
package org.sonar.db.property;

import java.util.Objects;
import javax.annotation.Nullable;
import org.assertj.core.api.AbstractAssert;

import static java.util.Objects.requireNonNull;

public class PropertyDtoAssert extends AbstractAssert<PropertyDtoAssert, PropertyDto> {
  protected PropertyDtoAssert(@Nullable PropertyDto actual) {
    super(actual, PropertyDtoAssert.class);
  }

  public PropertyDtoAssert hasKey(String expected) {
    isNotNull();

    if (!Objects.equals(actual.getKey(), expected)) {
      failWithMessage("Expected PropertyDto to have key to be <%s> but was <%s>", expected, actual.getKey());
    }

    return this;
  }

  public PropertyDtoAssert hasNoUserId() {
    isNotNull();

    if (actual.getUserId() != null) {
      failWithMessage("Expected PropertyDto to have userId to be null but was <%s>", actual.getUserId());
    }

    return this;
  }

  public PropertyDtoAssert hasUserId(long expected) {
    isNotNull();

    if (!Objects.equals(actual.getUserId(), expected)) {
      failWithMessage("Expected PropertyDto to have userId to be <%s> but was <%s>", true, actual.getUserId());
    }

    return this;
  }

  public PropertyDtoAssert hasNoResourceId() {
    isNotNull();

    if (actual.getResourceId() != null) {
      failWithMessage("Expected PropertyDto to have resourceId to be null but was <%s>", actual.getResourceId());
    }

    return this;
  }

  public PropertyDtoAssert hasResourceId(long expected) {
    isNotNull();

    if (!Objects.equals(actual.getResourceId(), expected)) {
      failWithMessage("Expected PropertyDto to have resourceId to be <%s> but was <%s>", true, actual.getResourceId());
    }

    return this;
  }

  public PropertyDtoAssert hasValue(String expected) {
    requireNonNull(expected);
    isNotNull();

    if (!Objects.equals(actual.getValue(), expected)) {
      failWithMessage("Expected PropertyDto to have value to be <%s> but was <%s>", true, actual.getValue());
    }

    return this;
  }
}
