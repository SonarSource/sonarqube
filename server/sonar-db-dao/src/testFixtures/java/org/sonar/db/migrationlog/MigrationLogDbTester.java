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
package org.sonar.db.migrationlog;

import java.util.Arrays;
import java.util.List;
import java.util.function.Consumer;
import org.sonar.db.DbClient;
import org.sonar.db.DbTester;

public class MigrationLogDbTester {
  private final DbClient dbClient;
  private final DbTester db;

  public MigrationLogDbTester(DbTester db) {
    this.dbClient = db.getDbClient();
    this.db = db;
  }

  @SafeVarargs
  public final MigrationLogDto insert(Consumer<MigrationLogDto>... consumers) {
    MigrationLogDto migrationLogDto = new MigrationLogDto();
    Arrays.stream(consumers).forEach(c -> c.accept(migrationLogDto));
    dbClient.migrationLogDao().insert(db.getSession(), migrationLogDto);
    db.commit();
    return migrationLogDto;
  }

  public final List<MigrationLogDto> selectAll() {
    return dbClient.migrationLogDao().selectAll(db.getSession());
  }
}
