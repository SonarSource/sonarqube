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
package org.sonar.db.schemamigration;

import java.util.List;
import org.sonar.db.Dao;
import org.sonar.db.DbSession;

import static com.google.common.base.Preconditions.checkArgument;
import static java.util.Objects.requireNonNull;

public class SchemaMigrationDao implements Dao {
  public List<Integer> selectVersions(DbSession dbSession) {
    return getMapper(dbSession).selectVersions();
  }

  public void insert(DbSession dbSession, String version) {
    requireNonNull(version, "version can't be null");
    checkArgument(!version.isEmpty(), "version can't be empty");
    getMapper(dbSession).insert(version);
  }

  private static SchemaMigrationMapper getMapper(DbSession dbSession) {
    return dbSession.getMapper(SchemaMigrationMapper.class);
  }
}
