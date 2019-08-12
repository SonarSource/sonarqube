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

package org.sonar.server.authentication;

import javax.annotation.Nullable;
import org.sonar.api.utils.System2;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.db.user.UserDto;
import org.sonar.db.user.UserTokenDto;

public class UserLastConnectionDatesUpdaterImpl implements UserLastConnectionDatesUpdater {

  private static final long ONE_HOUR_IN_MILLISECONDS = 60 * 60 * 1000L;

  private final DbClient dbClient;
  private final System2 system2;

  public UserLastConnectionDatesUpdaterImpl(DbClient dbClient, System2 system2) {
    this.dbClient = dbClient;
    this.system2 = system2;
  }

  @Override
  public void updateLastConnectionDateIfNeeded(UserDto user) {
    Long lastConnectionDate = user.getLastConnectionDate();
    long now = system2.now();
    if (doesNotRequireUpdate(lastConnectionDate, now)) {
      return;
    }
    try (DbSession dbSession = dbClient.openSession(false)) {
      dbClient.userDao().update(dbSession, user.setLastConnectionDate(now));
      dbSession.commit();
    }
  }

  @Override
  public void updateLastConnectionDateIfNeeded(UserTokenDto userToken) {
    Long lastConnectionDate = userToken.getLastConnectionDate();
    long now = system2.now();
    if (doesNotRequireUpdate(lastConnectionDate, now)) {
      return;
    }
    try (DbSession dbSession = dbClient.openSession(false)) {
      dbClient.userTokenDao().update(dbSession, userToken.setLastConnectionDate(now));
      userToken.setLastConnectionDate(now);
      dbSession.commit();
    }
  }

  private static boolean doesNotRequireUpdate(@Nullable Long lastConnectionDate, long now) {
    // Update date only once per hour in order to decrease pressure on DB
    return lastConnectionDate != null && (now - lastConnectionDate) < ONE_HOUR_IN_MILLISECONDS;
  }
}
