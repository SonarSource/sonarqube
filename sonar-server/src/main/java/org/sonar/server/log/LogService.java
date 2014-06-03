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

import org.sonar.core.log.Activity;
import org.sonar.core.log.db.LogDto;
import org.sonar.core.persistence.DbSession;
import org.sonar.server.db.DbClient;
import org.sonar.server.user.UserSession;

import java.util.List;

/**
 * Log service is used to log Activity classes which represents an event to DB and Index.
 *
 * @see org.sonar.core.log.Activity
 * @since 4.4
 */
public class LogService {

  private final DbClient dbClient;

  public LogService(DbClient dbClient) {
    this.dbClient = dbClient;
  }

  public void write(Activity activity) {
    DbSession session = dbClient.openSession(false);
    try {
      this.write(session, activity);
    } finally {
      session.close();
    }
  }

  public <K extends Activity> void write(DbSession session, K activity) {

    dbClient.logDao().insert(session, new LogDto(UserSession.get().login(), activity));
  }

  public <K extends Activity> void write(DbSession session, List<K> activities) {
    for(Activity activity:activities){
      write(session, activity);
    }
  }
}
