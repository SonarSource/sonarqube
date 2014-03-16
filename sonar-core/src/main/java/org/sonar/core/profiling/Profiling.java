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
package org.sonar.core.profiling;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sonar.api.config.Settings;

/**
 * @since 4.1
 */
public class Profiling {

  public static final String CONFIG_PROFILING_LEVEL = "sonar.log.profilingLevel";

  private static final Logger LOGGER = LoggerFactory.getLogger(Profiling.class);

  private Settings settings;
  private ProfilingLogFactory logFactory;

  public enum Level {
    NONE, BASIC, FULL;

    public static Level fromConfigString(String settingsValue) {
      Level settingsLevel = NONE;
      if (settingsValue != null) {
        try {
          settingsLevel = Level.valueOf(settingsValue);
        } catch (IllegalArgumentException invalidSettings) {
          LOGGER.debug("Bad profiling settings, profiling is disabled", invalidSettings);
        }
      }
      return settingsLevel;
    }
  }

  public Profiling(Settings settings) {
    this(settings, new ProfilingLogFactory());
  }

  Profiling(Settings settings, ProfilingLogFactory logFactory) {
    this.settings = settings;
    this.logFactory = logFactory;
  }


  public StopWatch start(String domain, Level level) {
    StopWatch watch;
    if (isProfilingEnabled(level)) {
      watch = new LoggingWatch(logFactory.getLogger(domain));
    } else {
      watch = new NoopWatch();
    }
    return watch;
  }

  private boolean isProfilingEnabled(Level level) {
    String settingsValue = settings.getString(CONFIG_PROFILING_LEVEL);
    Level settingsLevel = Level.fromConfigString(settingsValue);
    return settingsLevel != Level.NONE && level.ordinal() <= settingsLevel.ordinal();
  }
}
