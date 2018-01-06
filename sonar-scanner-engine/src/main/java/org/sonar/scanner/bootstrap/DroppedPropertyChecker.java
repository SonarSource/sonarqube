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
package org.sonar.scanner.bootstrap;

import java.util.Map;
import org.sonar.api.utils.log.Logger;
import org.sonar.api.utils.log.Loggers;

import static java.util.Objects.requireNonNull;

public class DroppedPropertyChecker {

  private static final Logger LOG = Loggers.get(DroppedPropertyChecker.class);

  private final Map<String, String> settings;
  private final Map<String, String> properties;

  public DroppedPropertyChecker(Map<String, String> properties, Map<String, String> droppedPropertiesAndMsg) {
    this.settings = requireNonNull(properties);
    this.properties = requireNonNull(droppedPropertiesAndMsg);
  }

  public void checkDroppedProperties() {
    for (Map.Entry<String, String> entry : properties.entrySet()) {
      if (settings.containsKey(entry.getKey())) {
        LOG.warn("Property '{}' is not supported any more. {}", entry.getKey(), entry.getValue());
      }
    }
  }

}
