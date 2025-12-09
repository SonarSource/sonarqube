/*
 * SonarQube
 * Copyright (C) 2009-2025 SonarSource Sàrl
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
package org.sonar.db.audit;

import java.util.List;
import org.sonar.api.utils.System2;
import org.sonar.core.util.UuidFactory;
import org.sonar.db.Dao;
import org.sonar.db.DatabaseUtils;
import org.sonar.db.DbSession;
import org.sonar.db.Pagination;

import static org.sonar.server.platform.db.migration.def.VarcharColumnDef.MAX_SIZE;

public class AuditDao implements Dao {

  public static final int DEFAULT_PAGE_SIZE = 10000;
  public static final String EXCEEDED_LENGTH = "{ \"valueLengthExceeded\": true }";

  private final UuidFactory uuidFactory;
  private final System2 system2;

  public AuditDao(System2 system2, UuidFactory uuidFactory) {
    this.system2 = system2;
    this.uuidFactory = uuidFactory;
  }

  private static AuditMapper getMapper(DbSession dbSession) {
    return dbSession.getMapper(AuditMapper.class);
  }

  public List<AuditDto> selectByPeriodPaginated(DbSession dbSession, long start, long end, int page) {
    return getMapper(dbSession).selectByPeriodPaginated(start, end, Pagination.forPage(page).andSize(DEFAULT_PAGE_SIZE));
  }

  public void insert(DbSession dbSession, AuditDto auditDto) {
    if (auditDto.getUuid() == null) {
      auditDto.setUuid(uuidFactory.create());
    }
    if (auditDto.getCreatedAt() == 0) {
      long now = system2.now();
      auditDto.setCreatedAt(now);
    }
    if (auditDto.getNewValue().length() > MAX_SIZE) {
      auditDto.setNewValue(EXCEEDED_LENGTH);
    }
    getMapper(dbSession).insert(auditDto);
  }

  public List<AuditDto> selectOlderThan(DbSession dbSession, long beforeTimestamp) {
    return getMapper(dbSession).selectOlderThan(beforeTimestamp);
  }

  public long deleteBefore(DbSession dbSession, long threshold) {
    List<String> uuids = getMapper(dbSession).selectUuidsOlderThan(threshold);
    DatabaseUtils.executeLargeInputsWithoutOutput(uuids, list -> {
      getMapper(dbSession).purgeUuids(list);
      dbSession.commit();
    });
    return uuids.size();
  }

}
