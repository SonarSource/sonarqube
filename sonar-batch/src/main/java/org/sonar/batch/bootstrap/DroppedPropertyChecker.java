/*
 * SonarQube, open source software quality management tool.
 * Copyright (C) 2008-2014 SonarSource
 * mailto:contact AT sonarsource DOT com
 *
 * SonarQube is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * SonarQube is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */
package org.sonar.batch.bootstrap;

import java.util.Map;
import org.sonar.api.config.Settings;
import org.sonar.api.utils.log.Logger;
import org.sonar.api.utils.log.Loggers;

import static java.util.Objects.requireNonNull;

public class DroppedPropertyChecker {
  private final Settings settings;
  private final Logger logger;
  private final Map<String, String> properties;

  public DroppedPropertyChecker(Settings settings, Map<String, String> properties) {
    this.settings = requireNonNull(settings);
    this.logger = Loggers.get(settings.getClass());
    this.properties = requireNonNull(properties);
  }

  public void checkDroppedProperties() {
    for (Map.Entry<String, String> entry : properties.entrySet()) {
      if (settings.hasKey(entry.getKey())) {
        logger.warn("Property '{}' is not supported any more. {}", entry.getKey(), entry.getValue());
      }
    }
  }

}
