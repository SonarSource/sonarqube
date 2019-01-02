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
package org.sonar.application.command;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import javax.annotation.CheckForNull;
import javax.annotation.Nullable;
import org.sonar.process.MessageException;
import org.sonar.process.Props;

import static java.lang.String.format;
import static java.util.Objects.requireNonNull;
import static java.util.stream.Collectors.joining;

public class JvmOptions<T extends JvmOptions> {
  private static final String JVM_OPTION_NOT_NULL_ERROR_MESSAGE = "a JVM option can't be null";

  private final HashMap<String, String> mandatoryOptions = new HashMap<>();
  private final LinkedHashSet<String> options = new LinkedHashSet<>();

  public JvmOptions() {
    this(Collections.emptyMap());
  }

  public JvmOptions(Map<String, String> mandatoryJvmOptions) {
    requireNonNull(mandatoryJvmOptions, JVM_OPTION_NOT_NULL_ERROR_MESSAGE)
      .entrySet()
      .stream()
      .filter(e -> {
        requireNonNull(e.getKey(), "JVM option prefix can't be null");
        if (e.getKey().trim().isEmpty()) {
          throw new IllegalArgumentException("JVM option prefix can't be empty");
        }
        requireNonNull(e.getValue(), "JVM option value can't be null");
        return true;
      }).forEach(e -> {
        String key = e.getKey().trim();
        String value = e.getValue().trim();
        mandatoryOptions.put(key, value);
        add(key + value);
      });
  }

  public T addFromMandatoryProperty(Props props, String propertyName) {
    String value = props.nonNullValue(propertyName);
    if (!value.isEmpty()) {
      List<String> jvmOptions = Arrays.stream(value.split(" (?=-)")).map(String::trim).collect(Collectors.toList());
      checkOptionFormat(propertyName, jvmOptions);
      checkMandatoryOptionOverwrite(propertyName, jvmOptions);
      options.addAll(jvmOptions);
    }

    return castThis();
  }

  private static void checkOptionFormat(String propertyName, List<String> jvmOptionsFromProperty) {
    List<String> invalidOptions = jvmOptionsFromProperty.stream()
      .filter(JvmOptions::isInvalidOption)
      .collect(Collectors.toList());
    if (!invalidOptions.isEmpty()) {
      throw new MessageException(format(
        "a JVM option can't be empty and must start with '-'. The following JVM options defined by property '%s' are invalid: %s",
        propertyName,
        invalidOptions.stream()
          .collect(joining(", "))));
    }
  }

  private void checkMandatoryOptionOverwrite(String propertyName, List<String> jvmOptionsFromProperty) {
    List<Match> matches = jvmOptionsFromProperty.stream()
      .map(jvmOption -> new Match(jvmOption, mandatoryOptionFor(jvmOption)))
      .filter(match -> match.getMandatoryOption() != null)
      .collect(Collectors.toList());
    if (!matches.isEmpty()) {
      throw new MessageException(format(
        "a JVM option can't overwrite mandatory JVM options. The following JVM options defined by property '%s' are invalid: %s",
        propertyName,
        matches.stream()
          .map(m -> m.getOption() + " overwrites " + m.mandatoryOption.getKey() + m.mandatoryOption.getValue())
          .collect(joining(", "))));
    }
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
    if (isInvalidOption(value)) {
      throw new IllegalArgumentException("a JVM option can't be empty and must start with '-'");
    }
    checkMandatoryOptionOverwrite(value);
    options.add(value);

    return castThis();
  }

  private void checkMandatoryOptionOverwrite(String value) {
    Map.Entry<String, String> overriddenMandatoryOption = mandatoryOptionFor(value);
    if (overriddenMandatoryOption != null) {
      throw new MessageException(String.format(
        "a JVM option can't overwrite mandatory JVM options. %s overwrites %s",
        value,
        overriddenMandatoryOption.getKey() + overriddenMandatoryOption.getValue()));
    }
  }

  @CheckForNull
  private Map.Entry<String, String> mandatoryOptionFor(String jvmOption) {
    return mandatoryOptions.entrySet().stream()
      .filter(s -> jvmOption.startsWith(s.getKey()) && !jvmOption.equals(s.getKey() + s.getValue()))
      .findFirst()
      .orElse(null);
  }

  private static boolean isInvalidOption(String value) {
    return value.isEmpty() || !value.startsWith("-");
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

  private static final class Match {
    private final String option;

    private final Map.Entry<String, String> mandatoryOption;

    private Match(String option, @Nullable Map.Entry<String, String> mandatoryOption) {
      this.option = option;
      this.mandatoryOption = mandatoryOption;
    }

    String getOption() {
      return option;
    }

    @CheckForNull
    Map.Entry<String, String> getMandatoryOption() {
      return mandatoryOption;
    }

  }
}
