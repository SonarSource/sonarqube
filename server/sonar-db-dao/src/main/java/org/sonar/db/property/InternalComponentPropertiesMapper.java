/*
 * SonarQube
 * Copyright (C) 2009-2025 SonarSource SA
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

import java.util.Optional;
import java.util.Set;
import org.apache.ibatis.annotations.Param;

public interface InternalComponentPropertiesMapper {

  Optional<InternalComponentPropertyDto> selectByComponentUuidAndKey(@Param("componentUuid") String componentUuid, @Param("key") String key);

  void insert(@Param("dto") InternalComponentPropertyDto dto);

  int update(@Param("dto") InternalComponentPropertyDto dto);

  /**
   * Replace value (and update updated_at) only if current value matches oldValue
   */
  void replaceValue(@Param("componentUuid") String componentUuid, @Param("key") String key, @Param("oldValue") String oldValue, @Param("newValue") String newValue,
    @Param("updatedAt") Long updatedAt);

  int deleteByComponentUuidAndKey(@Param("componentUuid") String componentUuid);

  Set<String> selectDbKeys(@Param("key") String key, @Param("value") String value);
}
