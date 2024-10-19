/*
 * SonarQube
 * Copyright (C) 2009-2024 SonarSource SA
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
package org.sonar.db.property;

import java.util.List;
import org.apache.ibatis.annotations.Param;

public interface InternalPropertiesMapper {
  List<InternalPropertyDto> selectAsText(@Param("keys") List<String> key);

  List<InternalPropertyDto> selectAsClob(@Param("keys") List<String> key);

  void insertAsEmpty(@Param("key") String key, @Param("createdAt") long createdAt);

  void insertAsText(@Param("key") String key, @Param("value") String value, @Param("createdAt") long createdAt);

  void insertAsClob(@Param("key") String key, @Param("value") String value, @Param("createdAt") long createdAt);

  int deleteByKey(@Param("key") String key);

  /**
   * Replace the value of the specified key, only if the existing value matches the expected old value.
   * Returns 1 if the replacement succeeded, or 0 if failed (old value different, or record does not exist).
   */
  int replaceValue(@Param("key") String key, @Param("oldValue") String oldValue, @Param("newValue") String newValue);
}
