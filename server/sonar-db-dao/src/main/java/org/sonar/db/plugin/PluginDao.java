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
package org.sonar.db.plugin;

import java.util.List;
import java.util.Optional;
import org.sonar.db.Dao;
import org.sonar.db.DbSession;
import org.sonar.db.audit.AuditPersister;
import org.sonar.db.audit.model.PluginNewValue;

public class PluginDao implements Dao {
  private final AuditPersister auditPersister;

  public PluginDao(AuditPersister auditPersister) {
    this.auditPersister = auditPersister;
  }

  /**
   * It may return plugins that are no longer installed.
   */
  public List<PluginDto> selectAll(DbSession dbSession) {
    return mapper(dbSession).selectAll();
  }

  /**
   * It may return a plugin that is no longer installed.
   */
  public Optional<PluginDto> selectByKey(DbSession dbSession, String key) {
    return Optional.ofNullable(mapper(dbSession).selectByKey(key));
  }

  public void insert(DbSession dbSession, PluginDto dto) {
    mapper(dbSession).insert(dto);
    auditPersister.addPlugin(dbSession, new PluginNewValue(dto));
  }

  public void update(DbSession dbSession, PluginDto dto) {
    mapper(dbSession).update(dto);
    auditPersister.updatePlugin(dbSession, new PluginNewValue(dto));
  }

  private static PluginMapper mapper(DbSession dbSession) {
    return dbSession.getMapper(PluginMapper.class);
  }
}
