/*
 * SonarQube
 * Copyright (C) 2009-2019 SonarSource SA
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
package org.sonar.core.util;

import com.google.common.base.Function;
import javax.annotation.Nullable;

import static com.google.common.base.Preconditions.checkArgument;

/**
 * Guava Function that does not accept null input elements
 * @since 5.1
 */
public abstract class NonNullInputFunction<F, T> implements Function<F, T> {

  @Override
  public final T apply(@Nullable F input) {
    checkArgument(input != null, "Null inputs are not allowed in this function");
    return doApply(input);
  }

  /**
   * This method is the same as {@link #apply(Object)} except that the input argument
   * is not marked as nullable
   */
  protected abstract T doApply(F input);
}
