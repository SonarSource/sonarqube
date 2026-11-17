/*
 * SonarQube
 * Copyright (C) SonarSource Sàrl
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
package org.sonar.server.tester;

import org.sonar.db.DbClient;
import org.sonar.db.user.UserDto;
import org.sonar.db.user.UserTokenDto;
import org.sonar.server.user.ThreadLocalUserSession;
import org.sonar.server.user.TokenUserSession;
import org.sonar.server.user.UserSession;

/**
 * {@link UserSessionRule} only provides mock sessions. Use this helper to get a real {@link TokenUserSession} for the
 * given token, wrapped in a {@link ThreadLocalUserSession} exactly as the platform injects it.
 */
public final class TokenUserSessionTesting {

  private TokenUserSessionTesting() {
    // static helpers only
  }

  public static UserSession newTokenSession(DbClient dbClient, UserDto user, UserTokenDto token) {
    ThreadLocalUserSession threadLocalUserSession = new ThreadLocalUserSession();
    threadLocalUserSession.set(new TokenUserSession(dbClient, user, token));
    return threadLocalUserSession;
  }
}
