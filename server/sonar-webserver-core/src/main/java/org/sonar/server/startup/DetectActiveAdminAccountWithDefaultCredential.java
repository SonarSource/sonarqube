/*
 * SonarQube
 * Copyright (C) 2009-2020 SonarSource SA
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

package org.sonar.server.startup;

import org.picocontainer.Startable;
import org.sonar.api.utils.log.Logger;
import org.sonar.api.utils.log.Loggers;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.db.user.UserDto;
import org.sonar.server.authentication.CredentialsLocalAuthentication;
import org.sonar.server.authentication.event.AuthenticationEvent;
import org.sonar.server.authentication.event.AuthenticationException;

/**
 * Detect usage of an active admin account with default credential in order to ask this account to reset its password during authentication.
 */
public class DetectActiveAdminAccountWithDefaultCredential implements Startable {

  private static final Logger LOGGER = Loggers.get(DetectActiveAdminAccountWithDefaultCredential.class);

  private final DbClient dbClient;
  private final CredentialsLocalAuthentication localAuthentication;

  public DetectActiveAdminAccountWithDefaultCredential(DbClient dbClient, CredentialsLocalAuthentication localAuthentication) {
    this.dbClient = dbClient;
    this.localAuthentication = localAuthentication;
  }

  @Override
  public void start() {
    try (DbSession dbSession = dbClient.openSession(false)) {
      UserDto admin = dbClient.userDao().selectActiveUserByLogin(dbSession, "admin");
      if (admin == null || !isDefaultCredentialUser(dbSession, admin)) {
        return;
      }
      LOGGER.warn("*******************************************************************************************************************");
      LOGGER.warn("Default Administrator credentials are still being used. Make sure to change the password or deactivate the account.");
      LOGGER.warn("*******************************************************************************************************************");
      dbClient.userDao().update(dbSession, admin.setResetPassword(true));
      dbSession.commit();
    }
  }

  private boolean isDefaultCredentialUser(DbSession dbSession, UserDto user) {
    try {
      localAuthentication.authenticate(dbSession, user, "admin", AuthenticationEvent.Method.BASIC);
      return true;
    } catch (AuthenticationException ex) {
      return false;
    }
  }

  @Override
  public void stop() {
    // Nothing to do
  }
}
