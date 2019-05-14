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
package org.sonar.db.property;

import java.util.Optional;
import java.util.Set;
import org.sonar.api.utils.System2;
import org.sonar.core.util.UuidFactory;
import org.sonar.db.Dao;
import org.sonar.db.DbSession;

/**
 * A simple key-value store per component.
 */
public class InternalComponentPropertiesDao implements Dao {

  private final System2 system2;
  private final UuidFactory uuidFactory;

  public InternalComponentPropertiesDao(System2 system2, UuidFactory uuidFactory) {
    this.system2 = system2;
    this.uuidFactory = uuidFactory;
  }

  private void insertOrUpdate(DbSession dbSession, InternalComponentPropertyDto dto) {
    InternalComponentPropertiesMapper mapper = getMapper(dbSession);

    dto.setUpdatedAt(system2.now());

    if (mapper.update(dto) == 1) {
      return;
    }

    dto.setUuid(uuidFactory.create());
    dto.setCreatedAt(system2.now());
    mapper.insert(dto);
  }

  /**
   * For the given component uuid, update the value of the specified key, if exists,
   * otherwise insert it.
   */
  public void insertOrUpdate(DbSession dbSession, String componentUuid, String key, String value) {
    insertOrUpdate(dbSession, new InternalComponentPropertyDto().setComponentUuid(componentUuid).setKey(key).setValue(value));
  }

  /**
   * For the given component uuid, replace the value of the specified key "atomically":
   * only replace if the old value is still the same as the current value.
   */
  public void replaceValue(DbSession dbSession, String componentUuid, String key, String oldValue, String newValue) {
    getMapper(dbSession).replaceValue(componentUuid, key, oldValue, newValue, system2.now());
  }

  public Optional<InternalComponentPropertyDto> selectByComponentUuidAndKey(DbSession dbSession, String componentUuid, String key) {
    return getMapper(dbSession).selectByComponentUuidAndKey(componentUuid, key);
  }

  public int deleteByComponentUuid(DbSession dbSession, String componentUuid) {
    return getMapper(dbSession).deleteByComponentUuidAndKey(componentUuid);
  }

  /**
   * Select the projects.kee values for internal component properties having specified key and value.
   */
  public Set<String> selectDbKeys(DbSession dbSession, String key, String value) {
    return getMapper(dbSession).selectDbKeys(key, value);
  }

  private static InternalComponentPropertiesMapper getMapper(DbSession dbSession) {
    return dbSession.getMapper(InternalComponentPropertiesMapper.class);
  }
}
