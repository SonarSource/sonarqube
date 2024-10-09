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
package org.sonar.scanner.repository;

import java.util.HashMap;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.sonar.api.utils.Preconditions.checkArgument;

public class TelemetryCache {

  private static final Logger LOG = LoggerFactory.getLogger(TelemetryCache.class);

  private static final int MAX_ENTRIES = 1000;

  private final Map<String, String> telemetryEntries = new HashMap<>();

  /**
   * Value is overridden if the key was already stored.
   * Only the first {@link #MAX_ENTRIES} entries are stored.
   * @throws IllegalArgumentException if key is null
   * @throws IllegalArgumentException if value is null
   * @since 10.8
   */
  public TelemetryCache put(String key, String value) {
    checkArgument(key != null, "Key of the telemetry entry must not be null");
    checkArgument(value != null, "Value of the telemetry entry must not be null");

    if (telemetryEntries.size() < MAX_ENTRIES || telemetryEntries.containsKey(key)) {
      telemetryEntries.put(key, value);
    } else {
      LOG.warn("Telemetry cache is full, dropping telemetry metric '{}'", key);
    }
    return this;
  }

  public Map<String, String> getAll() {
    return telemetryEntries;
  }
}
