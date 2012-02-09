/*
 * Sonar, open source software quality management tool.
 * Copyright (C) 2008-2012 SonarSource
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
package org.sonar.api.measures;

import com.google.common.annotations.Beta;

/**
 * Provides access to measures for the lines of file.
 * Examples:
 * <ul>
 * <li>line 1 is a line of code</li>
 * <li>line 2 contains comment</li>
 * <li>line 3 contains 2 branches</li>
 * <li>author of line 4 is Simon</li>
 * </ul>
 * Numbering of lines starts from 1.
 *
 * <p>This interface is not intended to be implemented by clients.</p>
 *
 * @since 2.14
 */
@Beta
public interface FileLinesContext {

  void setIntValue(String metricKey, int line, int value);

  /**
   * @return value, or null if no such metric for given line
   */
  Integer getIntValue(String metricKey, int line);

  void setStringValue(String metricKey, int line, String value);

  /**
   * @return value, or null if no such metric for given line
   */
  String getStringValue(String metricKey, int line);

  void save();

}
