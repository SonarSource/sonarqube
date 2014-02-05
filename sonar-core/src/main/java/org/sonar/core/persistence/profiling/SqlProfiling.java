/*
 * SonarQube, open source software quality management tool.
 * Copyright (C) 2008-2013 SonarSource
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
package org.sonar.core.persistence.profiling;

import org.sonar.api.config.Settings;
import org.sonar.core.profiling.Profiling;
import org.sonar.core.profiling.Profiling.Level;
import org.sonar.core.profiling.StopWatch;

class SqlProfiling {

  private final Profiling profiling;

  SqlProfiling() {
    Settings settings = new Settings();
    settings.setProperty(Profiling.CONFIG_PROFILING_LEVEL, Profiling.Level.FULL.toString());
    profiling = new Profiling(settings);
  }

  StopWatch start() {
    return profiling.start("sql", Level.FULL);
  }

  void stop(StopWatch watch, String sql) {
    watch.stop(String.format("Executed SQL: %s", sql.replaceAll("\\s+", " ")));
  }
}
