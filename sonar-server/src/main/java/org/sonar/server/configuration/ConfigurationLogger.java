/*
 * Sonar, open source software quality management tool.
 * Copyright (C) 2009 SonarSource SA
 * mailto:contact AT sonarsource DOT com
 *
 * Sonar is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * Sonar is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with Sonar; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02
 */
package org.sonar.server.configuration;

import org.apache.commons.configuration.Configuration;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Iterator;

public final class ConfigurationLogger {

  private ConfigurationLogger() {
    // only static methods
  }

  public static void log(Configuration configuration) {
    Logger log = LoggerFactory.getLogger(ConfigurationLogger.class);
    if (log.isDebugEnabled()) {
      Iterator<String> keys = configuration.getKeys();
      while (keys.hasNext()) {
        String key = keys.next();
        String property = getTruncatedProperty(configuration, key);
        log.debug("Property: " + key + " is: '" + property + "'");
      }
    }
  }

  static String getTruncatedProperty(Configuration configuration, String key) {
    String property = StringUtils.join(configuration.getStringArray(key), ",");
    return StringUtils.abbreviate(property, 100);
  }

}
