/*
 * SonarQube
 * Copyright (C) SonarSource Sàrl
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
package org.sonar.db.adminalert;

import java.util.List;
import java.util.Optional;
import org.sonar.api.utils.System2;
import org.sonar.core.util.UuidFactory;
import org.sonar.db.Dao;
import org.sonar.db.DbSession;

public class AdminAlertStatusDao implements Dao {

  private final System2 system2;
  private final UuidFactory uuidFactory;

  public AdminAlertStatusDao(System2 system2, UuidFactory uuidFactory) {
    this.system2 = system2;
    this.uuidFactory = uuidFactory;
  }

  /**
   * Returns the currently active entry for the given alert key, if any.
   * At most one active entry per key is expected at any point in time.
   */
  public Optional<AdminAlertStatusDto> findCurrentActiveByAlertKey(DbSession dbSession, String alertKey) {
    return Optional.ofNullable(getMapper(dbSession).selectCurrentActiveByAlertKey(alertKey));
  }

  /**
   * Returns all entries across all alert keys, ordered by activation time ascending.
   * Includes both active and historical (deactivated) entries.
   */
  public List<AdminAlertStatusDto> findAll(DbSession dbSession) {
    return getMapper(dbSession).selectAll();
  }

  /**
   * Returns all currently active entries.
   */
  public List<AdminAlertStatusDto> findAllActive(DbSession dbSession) {
    return getMapper(dbSession).selectAllActive();
  }

  /**
   * Inserts a new active entry for the given alert key.
   * Callers must commit the session after this call.
   */
  public void insertActivation(DbSession dbSession, String alertKey) {
    long now = system2.now();
    AdminAlertStatusDto dto = new AdminAlertStatusDto()
      .setUuid(uuidFactory.create())
      .setAlertKey(alertKey)
      .setActive(true)
      .setActivatedAt(now)
      .setUpdatedAt(now);
    getMapper(dbSession).insert(dto);
  }

  /**
   * Deactivates all currently active entries for the given alert key by setting
   * {@code is_active = false} and {@code deactivated_at = now}.
   * Callers must commit the session after this call.
   */
  public void deactivateCurrent(DbSession dbSession, String alertKey) {
    long now = system2.now();
    getMapper(dbSession).deactivateCurrent(alertKey, now, now);
  }

  private static AdminAlertStatusMapper getMapper(DbSession dbSession) {
    return dbSession.getMapper(AdminAlertStatusMapper.class);
  }
}
