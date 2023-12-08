/*
 * SonarQube
 * Copyright (C) 2009-2023 SonarSource SA
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
package org.sonar.server.common;

import java.util.Objects;
import java.util.function.Consumer;
import java.util.function.Function;
import javax.annotation.Nullable;

public class UpdatedValue<T> {
  final T value;
  final boolean isDefined;

  UpdatedValue(@Nullable T value, boolean isDefined) {
    this.value = value;
    this.isDefined = isDefined;
  }

  public static <T> UpdatedValue<T> withValue(@Nullable T value) {
    return new UpdatedValue<>(value, true);
  }

  public static <T> UpdatedValue<T> undefined() {
    return new UpdatedValue<>(null, false);
  }

  public <U> UpdatedValue<U> map(Function<T, U> mappingFunction) {
    if (isDefined) {
      return withValue(mappingFunction.apply(value));
    }
    return undefined();
  }

  public void applyIfDefined(Consumer<T> consumer) {
    if (isDefined) {
      consumer.accept(value);
    }
  }

  public boolean contains(T testValue) {
    if (isDefined && value != null) {
      return value.equals(testValue);
    }
    return false;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    UpdatedValue<?> that = (UpdatedValue<?>) o;
    return isDefined == that.isDefined && Objects.equals(value, that.value);
  }

  @Override
  public int hashCode() {
    return Objects.hash(value, isDefined);
  }
}
