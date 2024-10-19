/*
 * SonarQube
 * Copyright (C) 2009-2024 SonarSource SA
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
package org.sonar.server.v2.common.model;

import java.util.function.Consumer;
import java.util.function.Function;
import javax.annotation.CheckForNull;
import javax.annotation.Nullable;
import javax.validation.valueextraction.UnwrapByDefault;
import org.sonar.server.common.NonNullUpdatedValue;
import org.sonar.server.common.UpdatedValue;

@UnwrapByDefault
public class UpdateField<T> {

  private final T value;
  private final boolean isDefined;

  private UpdateField(@Nullable T value, boolean isDefined) {
    this.value = value;
    this.isDefined = isDefined;
  }

  public static <T> UpdateField<T> withValue(@Nullable T value) {
    return new UpdateField<>(value, true);
  }

  public static <T> UpdateField<T> undefined() {
    return new UpdateField<>(null, false);
  }

  @CheckForNull
  public T getValue() {
    return value;
  }

  public boolean isDefined() {
    return isDefined;
  }

  public void applyIfDefined(Consumer<T> consumer) {
    if (isDefined) {
      consumer.accept(value);
    }
  }

  public <U> UpdateField<U> map(Function<T, U> mappingFunction) {
    if (isDefined) {
      return withValue(mappingFunction.apply(value));
    }
    return undefined();
  }

  public UpdatedValue<T> toUpdatedValue() {
    return isDefined ? UpdatedValue.withValue(value) : UpdatedValue.undefined();
  }
  public NonNullUpdatedValue<T> toNonNullUpdatedValue() {
    return isDefined ? NonNullUpdatedValue.withValueOrThrow(value) : NonNullUpdatedValue.undefined();
  }

  @CheckForNull
  public T orElse(@Nullable T other) {
    return isDefined ? value : other;
  }


  @Override
  public String toString() {
    return value.toString();
  }
}
