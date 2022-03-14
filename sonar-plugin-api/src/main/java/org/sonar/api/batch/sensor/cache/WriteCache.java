/*
 * SonarQube
 * Copyright (C) 2009-2022 SonarSource SA
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
package org.sonar.api.batch.sensor.cache;

import java.io.InputStream;
import org.sonar.api.Beta;

@Beta
public interface WriteCache {
  /**
   * Save a new entry in the cache. The stream will be consumed immediately.
   * @throws IllegalArgumentException if the cache already contains the key
   */
  void write(String key, InputStream data);

  /**
   * Save a new entry in the cache.
   * @throws IllegalArgumentException if the cache already contains the key
   */
  void write(String key, byte[] data);

  /**
   * Copy a cached entry from the previous cache to the new one.
   * @throws IllegalArgumentException if the previous cache doesn't contain given key or if this cache already contains the key
   */
  void copyFromPrevious(String key);
}
