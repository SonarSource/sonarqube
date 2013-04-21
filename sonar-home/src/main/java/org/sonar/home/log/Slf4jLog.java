/*
 * SonarQube, open source software quality management tool.
 * Copyright (C) 2008-2012 SonarSource
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
 * You should have received a copy of the GNU Lesser General Public
 * License along with Sonar; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02
 */
package org.sonar.home.log;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Slf4jLog implements Log {

  private final Logger logger;

  public Slf4jLog(Logger logger) {
    this.logger = logger;
  }

  public Slf4jLog(Class loggerClass) {
    this.logger = LoggerFactory.getLogger(loggerClass);
  }

  public boolean isDebugEnabled() {
    return logger.isDebugEnabled();
  }

  public void debug(String s) {
    logger.debug(s);
  }

  public void info(String s) {
    logger.info(s);
  }

  public void warn(String s) {
    logger.warn(s);
  }

  public void error(String s, Throwable throwable) {
    logger.error(s, throwable);
  }


}
