/*
 * SonarQube
 * Copyright (C) 2009-2019 SonarSource SA
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
package org.sonar.core.util;

public interface UuidFactory {

  /**
   * Create a universally unique identifier. Underlying algorithm, so format and max length,
   * can vary over SonarQube versions.
   * <p/>
   * UUID is a base64 ASCII encoded string and is URL-safe. It does not contain - and + characters
   * but only letters, digits, dash (-) and underscore (_). Length can vary but does
   * not exceed {@link Uuids#MAX_LENGTH} characters (arbitrary value).
   */
  String create();

}
