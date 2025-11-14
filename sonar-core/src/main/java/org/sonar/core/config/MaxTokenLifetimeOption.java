/*
 * SonarQube
 * Copyright (C) 2009-2025 SonarSource Sàrl
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
package org.sonar.core.config;

import java.util.Map;
import java.util.Optional;
import java.util.stream.Stream;

import static java.util.function.UnaryOperator.identity;
import static java.util.stream.Collectors.toMap;

public enum MaxTokenLifetimeOption {
  THIRTY_DAYS("30 days", 30),
  NINETY_DAYS("90 days", 90),
  ONE_YEAR("1 year", 365),
  NO_EXPIRATION("No expiration");

  private static final Map<String, MaxTokenLifetimeOption> INTERVALS_MAP;

  static {
    INTERVALS_MAP = Stream.of(MaxTokenLifetimeOption.values())
      .collect(toMap(MaxTokenLifetimeOption::getName, identity()));
  }

  private final String name;
  private final Integer days;

  MaxTokenLifetimeOption(String name) {
    this.name = name;
    this.days = null;
  }

  MaxTokenLifetimeOption(String name, Integer days) {
    this.name = name;
    this.days = days;
  }

  public String getName() {
    return name;
  }

  public Optional<Integer> getDays() {
    return Optional.ofNullable(days);
  }

  public static MaxTokenLifetimeOption get(String name) {
    return Optional.ofNullable(INTERVALS_MAP.get(name))
      .orElseThrow(() -> new IllegalArgumentException("No token expiration interval with name \"" + name + "\" found."));
  }

}
