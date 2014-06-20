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
package org.sonar.server.activity;

import org.sonar.core.activity.Activity;
import org.sonar.core.activity.ActivityLog;
import org.sonar.core.activity.db.ActivityDto;
import org.sonar.core.persistence.DbSession;
import org.sonar.server.activity.index.ActivityIndex;
import org.sonar.server.activity.index.ActivityQuery;
import org.sonar.server.db.DbClient;
import org.sonar.server.search.IndexClient;
import org.sonar.server.search.QueryOptions;
import org.sonar.server.search.Result;
import org.sonar.server.user.UserSession;

import javax.annotation.Nullable;

import java.util.List;

/**
 * Log service is used to log Activity classes which represents an event to DB and Index.
 *
 * @see org.sonar.core.activity.ActivityLog
 * @since 4.4
 */
public class ActivityService {

  private final DbClient dbClient;
  private final IndexClient indexClient;

  public ActivityService(DbClient dbClient, IndexClient indexClient) {
    this.dbClient = dbClient;
    this.indexClient = indexClient;
  }

  @Nullable
  private String getAuthor() {
    return (UserSession.get().login() != null) ? UserSession.get().login() : null;
  }

  private void save(DbSession session, ActivityDto log) {
    dbClient.activityDao().insert(session,
      log.setAuthor(getAuthor()));
  }

  public void write(DbSession session, Activity.Type type, String message) {
    this.write(session, type, message, null);
  }

  public void write(DbSession session, Activity.Type type, String message, Integer time) {
    this.save(session, ActivityDto.createFor(message)
      .setType(type)
      .setExecutionTime(time));
  }

  public <L extends ActivityLog> void write(DbSession session, Activity.Type type, List<L> logs) {
    for (ActivityLog log : logs) {
      this.write(session, type, log);
    }
  }

  public <L extends ActivityLog> void write(DbSession session, Activity.Type type, L log) {
    this.save(session, ActivityDto.createFor(log)
      .setType(type));
  }

  public ActivityQuery newActivityQuery() {
    return new ActivityQuery();
  }

  public Result<Activity> search(ActivityQuery query, QueryOptions options) {
    ActivityIndex index = indexClient.get(ActivityIndex.class);
    return
      new Result<Activity>(index, index.search(query, options));
  }
}
