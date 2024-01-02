/*
 * SonarQube
 * Copyright (C) 2009-2024 SonarSource SA
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

import org.sonar.api.utils.log.Logger;
import org.sonar.api.utils.log.Loggers;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.db.user.UserDto;
import org.sonar.server.authentication.event.AuthenticationEvent;
import org.sonar.server.authentication.event.AuthenticationException;
import org.sonar.server.notification.NotificationManager;

import static org.sonar.server.log.ServerProcessLogging.STARTUP_LOGGER_NAME;
import static org.sonar.server.property.InternalProperties.DEFAULT_ADMIN_CREDENTIAL_USAGE_EMAIL;

/**
 * Detect usage of an active admin account with default credential in order to ask this account to reset its password during authentication.
 */
public class DefaultAdminCredentialsVerifierImpl implements DefaultAdminCredentialsVerifier {

  private static final Logger LOGGER = Loggers.get(STARTUP_LOGGER_NAME);

  private final DbClient dbClient;
  private final CredentialsLocalAuthentication localAuthentication;
  private final NotificationManager notificationManager;

  public DefaultAdminCredentialsVerifierImpl(DbClient dbClient, CredentialsLocalAuthentication localAuthentication, NotificationManager notificationManager) {
    this.dbClient = dbClient;
    this.localAuthentication = localAuthentication;
    this.notificationManager = notificationManager;
  }

  public void runAtStart() {
    try (DbSession session = dbClient.openSession(false)) {
      UserDto admin = getAdminUser(session);
      if (admin == null || !isDefaultCredentialUser(session, admin)) {
        return;
      }
      addWarningInSonarDotLog();
      dbClient.userDao().update(session, admin.setResetPassword(true));
      sendEmailToAdmins(session);
      session.commit();
    }
  }

  @Override
  public boolean hasDefaultCredentialUser() {
    try (DbSession session = dbClient.openSession(false)) {
      UserDto admin = getAdminUser(session);
      if (admin == null) {
        return false;
      } else {
        return isDefaultCredentialUser(session, admin);
      }
    }
  }

  private UserDto getAdminUser(DbSession session) {
    return dbClient.userDao().selectActiveUserByLogin(session, "admin");
  }

  private static void addWarningInSonarDotLog() {
    String highlighter = "####################################################################################################################";
    String msg = "Default Administrator credentials are still being used. Make sure to change the password or deactivate the account.";

    LOGGER.warn(highlighter);
    LOGGER.warn(msg);
    LOGGER.warn(highlighter);
  }

  private boolean isDefaultCredentialUser(DbSession dbSession, UserDto user) {
    try {
      localAuthentication.authenticate(dbSession, user, "admin", AuthenticationEvent.Method.BASIC);
      return true;
    } catch (AuthenticationException ex) {
      return false;
    }
  }

  private void sendEmailToAdmins(DbSession session) {
    if (dbClient.internalPropertiesDao().selectByKey(session, DEFAULT_ADMIN_CREDENTIAL_USAGE_EMAIL)
      .map(Boolean::parseBoolean)
      .orElse(false)) {
      return;
    }
    notificationManager.scheduleForSending(new DefaultAdminCredentialsVerifierNotification());
    dbClient.internalPropertiesDao().save(session, DEFAULT_ADMIN_CREDENTIAL_USAGE_EMAIL, Boolean.TRUE.toString());
  }
}
