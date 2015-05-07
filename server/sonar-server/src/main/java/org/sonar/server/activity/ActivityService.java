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

import org.sonar.api.ServerSide;
import org.sonar.api.utils.KeyValueFormat;
import org.sonar.api.utils.internal.Uuids;
import org.sonar.core.activity.db.ActivityDto;
import org.sonar.server.activity.index.ActivityIndexer;
import org.sonar.server.db.DbClient;
import org.sonar.server.user.UserSession;

@ServerSide
public class ActivityService {

  private final DbClient dbClient;
  private final ActivityIndexer indexer;

  public ActivityService(DbClient dbClient, ActivityIndexer indexer) {
    this.dbClient = dbClient;
    this.indexer = indexer;
  }

  public void save(Activity activity) {
    ActivityDto dto = new ActivityDto()
      .setKey(Uuids.create())
      .setAuthor(UserSession.get().login())
      .setAction(activity.getAction())
      .setMessage(activity.getMessage())
      .setData(KeyValueFormat.format(activity.getData()))
      .setType(activity.getType().name());
    dbClient.activityDao().insert(dto);
    indexer.index();
  }
}
