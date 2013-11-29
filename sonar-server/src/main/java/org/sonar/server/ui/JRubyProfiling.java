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

package org.sonar.server.ui;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sonar.core.profiling.Profiling;
import org.sonar.core.profiling.Profiling.Level;
import org.sonar.core.profiling.StopWatch;

/**
 * @since 4.1
 */
public class JRubyProfiling {

  private static final Logger LOG = LoggerFactory.getLogger(JRubyProfiling.class);

  private Profiling profiling;

  public JRubyProfiling(Profiling profiling) {
    this.profiling = profiling;
  }

  public StopWatch start(String domain, String level) {
    Level profilingLevel = Level.NONE;
    try {
      profilingLevel = Level.valueOf(level);
    } catch (IllegalArgumentException iae) {
      LOG.warn("Unknown profiling level, defaulting to NONE: " + level, iae);
    }
    return profiling.start(domain, profilingLevel);
  }
}
