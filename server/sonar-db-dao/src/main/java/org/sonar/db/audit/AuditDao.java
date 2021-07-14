/*
 * SonarQube
 * Copyright (C) 2009-2021 SonarSource SA
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

import org.sonar.api.utils.System2;
import org.sonar.core.util.UuidFactory;
import org.sonar.db.Dao;
import org.sonar.db.DbSession;

import java.util.List;

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

  public List<AuditDto> selectAll(DbSession dbSession) {
    return getMapper(dbSession).selectAll();
  }

  public List<AuditDto> selectByPeriod(DbSession dbSession, long beginning, long end) {
    return getMapper(dbSession).selectByPeriod(beginning, end);
  }

  public List<AuditDto> selectIfBeforeSelectedDate(DbSession dbSession, long end) {
    return getMapper(dbSession).selectIfBeforeSelectedDate(end);
  }

  public void insert(DbSession dbSession, AuditDto auditDto) {
    long now = system2.now();
    auditDto.setUuid(uuidFactory.create());
    auditDto.setCreatedAt(now);
    if (auditDto.getNewValue().length() > MAX_SIZE) {
      auditDto.setNewValue(EXCEEDED_LENGTH);
    }
    getMapper(dbSession).insert(auditDto);
  }

  public void deleteIfBeforeSelectedDate(DbSession dbSession, long timestamp) {
    getMapper(dbSession).deleteIfBeforeSelectedDate(timestamp);
  }
}
