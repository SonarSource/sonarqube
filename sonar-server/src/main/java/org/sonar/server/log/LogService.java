/*
 * SonarQube, open source software quality management tool.
 * Copyright (C) 2008-2014 SonarSource
 * mailto:contact AT sonarsource DOT com
 *
 * SonarQube is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * SonarQube is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */
package org.sonar.server.log;

import org.sonar.core.log.Log;
import org.sonar.core.log.Loggable;
import org.sonar.core.log.db.LogDto;
import org.sonar.core.persistence.DbSession;
import org.sonar.server.db.DbClient;
import org.sonar.server.log.index.LogIndex;
import org.sonar.server.log.index.LogQuery;
import org.sonar.server.search.IndexClient;
import org.sonar.server.search.QueryOptions;
import org.sonar.server.search.Result;
import org.sonar.server.user.UserSession;

import java.util.List;

/**
 * Log service is used to log Activity classes which represents an event to DB and Index.
 *
 * @see org.sonar.core.log.Loggable
 * @since 4.4
 */
public class LogService {

  private final DbClient dbClient;
  private final IndexClient indexClient;

  public LogService(DbClient dbClient, IndexClient indexClient) {
    this.dbClient = dbClient;
    this.indexClient = indexClient;
  }

  private String getAuthor() {
    return (UserSession.get().login() != null) ? UserSession.get().login() : "UNKNOWN";
  }

  private void save(DbSession session, LogDto log) {
    dbClient.logDao().insert(session,
      log.setAuthor(getAuthor()));
  }

  public void write(DbSession session, Log.Type type, String message) {
    this.write(session, type, message, null);
  }

  public void write(DbSession session, Log.Type type, String message, Integer time) {
    this.save(session, LogDto.createFor(message)
      .setType(type)
      .setExecutionTime(time));
  }

  public <L extends Loggable> void write(DbSession session, Log.Type type, List<L> logs) {
    for (Loggable log : logs) {
      this.write(session, type, log);
    }
  }

  public <L extends Loggable> void write(DbSession session, Log.Type type, L log) {
    this.save(session, LogDto.createFor(log)
      .setType(type));
  }

  public LogQuery newLogQuery() {
    return new LogQuery();
  }

  public Result<Log> search(LogQuery query, QueryOptions options) {
    return indexClient.get(LogIndex.class).search(query, options);
  }
}
