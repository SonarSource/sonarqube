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
package org.sonar.api.measures;

/**
 * Provides facility to store measures for the lines of file.
 * Examples:
 * <ul>
 * <li>line 1 is a line of code</li>
 * <li>line 2 contains comment</li>
 * <li>line 3 contains 2 branches</li>
 * <li>author of line 4 is Simon</li>
 * </ul>
 * Numbering of lines starts from 1.
 * Also note that you can't update what already was saved, however it is safe to call {@link #save()} several times.
 * <p>
 * Instances of this interface can be obtained using {@link FileLinesContextFactory}.
 * <br>
 * This interface is not intended to be implemented by clients.
 *
 * @since 2.14
 */
public interface FileLinesContext {

  /**
   * @throws UnsupportedOperationException on attempt to update already saved data
   */
  void setIntValue(String metricKey, int line, int value);

  /**
   * @return value, or null if no such metric for given line
   * @deprecated since 5.0 sensors should not read data
   */
  @Deprecated
  Integer getIntValue(String metricKey, int line);

  /**
   * @throws UnsupportedOperationException on attempt to update already saved data
   */
  void setStringValue(String metricKey, int line, String value);

  /**
   * @return value, or null if no such metric for given line
   * @deprecated since 5.0 sensors should not read data
   */
  @Deprecated
  String getStringValue(String metricKey, int line);

  /**
   * Saves unsaved values.
   */
  void save();

}
