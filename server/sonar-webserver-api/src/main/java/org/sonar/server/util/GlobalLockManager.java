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
package org.sonar.server.util;

import org.sonar.db.property.InternalPropertiesDao;

public interface GlobalLockManager {

  int LOCK_NAME_MAX_LENGTH = InternalPropertiesDao.LOCK_NAME_MAX_LENGTH;
  int DEFAULT_LOCK_DURATION_SECONDS = 180;

  /**
   * Try to acquire a lock on the given name for the {@link #DEFAULT_LOCK_DURATION_SECONDS default duration},
   * using the generic locking mechanism of {@see org.sonar.db.property.InternalPropertiesDao}.
   *
   * @throws IllegalArgumentException if name's length is > {@link #LOCK_NAME_MAX_LENGTH} or empty
   */
  boolean tryLock(String name);

  /**
   * Try to acquire a lock on the given name for the specified duration,
   * using the generic locking mechanism of {@see org.sonar.db.property.InternalPropertiesDao}.
   *
   * @throws IllegalArgumentException if name's length is > {@link #LOCK_NAME_MAX_LENGTH} or empty
   */
  boolean tryLock(String name, int durationSecond);
}
