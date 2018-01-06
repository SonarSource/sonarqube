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
package org.sonar.api.utils;

import org.slf4j.LoggerFactory;

/**
 * A very simple profiler to log the time elapsed performing some tasks.
 * This implementation is not thread-safe.
 *
 * @deprecated since 5.1. Replaced by {@link org.sonar.api.utils.log.Profiler}
 * @since 2.0
 */
@Deprecated
public class TimeProfiler {

  private org.slf4j.Logger logger;
  private long start = 0;
  private String name;
  private boolean debug = false;

  public TimeProfiler(org.slf4j.Logger logger) {
    this.logger = logger;
  }

  public TimeProfiler(Class clazz) {
    this.logger = LoggerFactory.getLogger(clazz);
  }

  /**
   * Use the default Sonar logger
   */
  public TimeProfiler() {
    this.logger = LoggerFactory.getLogger(getClass());
  }

  public TimeProfiler start(String name) {
    this.name = name;
    this.start = System.currentTimeMillis();
    if (debug) {
      logger.debug("{} ...", name);
    } else {
      logger.info("{}...", name);
    }
    return this;
  }

  public TimeProfiler setLogger(org.slf4j.Logger logger) {
    this.logger = logger;
    return this;
  }

  public org.slf4j.Logger getLogger() {
    return logger;
  }

  /**
   * @since 2.4
   */
  public TimeProfiler setLevelToDebug() {
    debug = true;
    return this;
  }

  public TimeProfiler stop() {
    if (start > 0) {
      String format = "{} done: {} ms";
      if (debug) {
        logger.debug(format, name, System.currentTimeMillis() - start);
      } else {
        logger.info(format, name, System.currentTimeMillis() - start);
      }
    }
    start = 0;
    return this;
  }
}
