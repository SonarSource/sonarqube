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
package org.sonar.api.ce.measure;

import javax.annotation.CheckForNull;

/**
 * Settings of the current component used in {@link MeasureComputer}
 *
 * @since 5.2
 */
public interface Settings {

  /**
   * Returns the property as a string
   * Matching on key is case sensitive
   */
  @CheckForNull
  String getString(String key);

  /**
   * Returns the property as a an array
   * Returns an empty array if no property is found for this key
   * Matching on key is case sensitive
   */
  String[] getStringArray(String key);

}
