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
package org.sonar.api.utils;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Predefined SLF4j loggers
 *
 * @since 1.12
 */
public final class Logs {

  private Logs() {
  }

  /**
   * This logger is always activated with level INFO
   * @deprecated default level is INFO since version 2.12. Please use your own logger.
   */
  @Deprecated
  public static final Logger INFO = LoggerFactory.getLogger("org.sonar.INFO");
}
