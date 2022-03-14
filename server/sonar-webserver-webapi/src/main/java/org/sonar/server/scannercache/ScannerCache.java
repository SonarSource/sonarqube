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
package org.sonar.server.scannercache;

import javax.annotation.CheckForNull;
import org.sonar.api.server.ServerSide;
import org.sonar.db.DbClient;
import org.sonar.db.DbInputStream;
import org.sonar.db.DbSession;
import org.sonar.db.scannercache.ScannerCacheDao;

@ServerSide
public class ScannerCache {
  private final DbClient dbClient;
  private final ScannerCacheDao dao;

  public ScannerCache(DbClient dbClient, ScannerCacheDao dao) {
    this.dbClient = dbClient;
    this.dao = dao;
  }

  @CheckForNull
  public DbInputStream get(String branchUuid) {
    try (DbSession session = dbClient.openSession(false)) {
      return dao.selectData(session, branchUuid);
    }
  }

  public void clear() {
    try (DbSession session = dbClient.openSession(false)) {
      dao.removeAll(session);
      session.commit();
    }
  }
}
