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
package org.sonar.application.command;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import org.sonar.process.Props;

import static java.util.Objects.requireNonNull;

public class JvmOptions<T extends JvmOptions> {
  private static final String JVM_OPTION_NOT_NULL_ERROR_MESSAGE = "a JVM option can't be null";

  private final LinkedHashSet<String> options = new LinkedHashSet<>();

  public JvmOptions(String... mandatoryJvmOptions) {
    Arrays.stream(requireNonNull(mandatoryJvmOptions, JVM_OPTION_NOT_NULL_ERROR_MESSAGE))
      .forEach(this::add);
  }

  public T addFromMandatoryProperty(Props props, String propertyName) {
    String value = props.nonNullValue(propertyName);
    if (!value.isEmpty()) {
      Arrays.stream(value.split(" (?=-)")).forEach(this::add);
    }

    return castThis();
  }

  /**
   * Add an option.
   * Argument is trimmed before being added.
   *
   * @throws IllegalArgumentException if argument is empty or does not start with {@code -}.
   */
  public T add(String str) {
    requireNonNull(str, JVM_OPTION_NOT_NULL_ERROR_MESSAGE);
    String value = str.trim();
    if (value.isEmpty() || !value.startsWith("-")) {
      throw new IllegalArgumentException("a JVM option can't be empty and must start with '-'");
    }
    options.add(value);

    return castThis();
  }

  @SuppressWarnings("unchecked")
  private T castThis() {
    return (T) this;
  }

  public List<String> getAll() {
    return new ArrayList<>(options);
  }

  @Override
  public String toString() {
    return options.toString();
  }
}
