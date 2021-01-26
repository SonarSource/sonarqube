/*
 * SonarQube
 * Copyright (C) 2009-2021 SonarSource SA
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
package org.sonar.alm.client;

import java.util.OptionalLong;
import org.sonar.api.config.Configuration;
import org.sonar.api.utils.log.Loggers;

/**
 * Implementation of {@link TimeoutConfiguration} reading values from configuration properties.
 */
public class TimeoutConfigurationImpl implements TimeoutConfiguration {
  private static final String CONNECT_TIMEOUT_PROPERTY = "sonar.alm.timeout.connect";
  private static final String READ_TIMEOUT_PROPERTY = "sonar.alm.timeout.read";

  private static final long DEFAULT_TIMEOUT = 30_000;
  private final Configuration configuration;

  public TimeoutConfigurationImpl(Configuration configuration) {
    this.configuration = configuration;
  }

  @Override
  public long getConnectTimeout() {
    return safelyParseLongValue(CONNECT_TIMEOUT_PROPERTY).orElse(DEFAULT_TIMEOUT);
  }

  private OptionalLong safelyParseLongValue(String property) {
    return configuration.get(property)
      .map(value -> {
        try {
          return OptionalLong.of(Long.parseLong(value));
        } catch (NumberFormatException e) {
          Loggers.get(TimeoutConfigurationImpl.class)
            .warn("Value of property {} can not be parsed to a long: {}", property, value);
          return OptionalLong.empty();
        }
      })
      .orElse(OptionalLong.empty());

  }

  @Override
  public long getReadTimeout() {
    return safelyParseLongValue(READ_TIMEOUT_PROPERTY).orElse(DEFAULT_TIMEOUT);
  }
}
