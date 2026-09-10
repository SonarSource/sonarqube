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
package org.sonar.server.startup;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sonar.api.Startable;
import org.sonar.api.server.ServerSide;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;

/**
 * Encrypts the user-scoped personal access tokens that are still stored as clear text. Tokens are encrypted when they
 * are written, so this only concerns tokens written before a secret key was configured, or before encryption existed.
 * <p>
 * This runs on every startup rather than as a database migration on purpose. An administrator who configures a secret
 * key after upgrading is the common case, and a migration would already have run by then and would never run again.
 */
@ServerSide
public class EncryptAlmPats implements Startable {

  private static final Logger LOG = LoggerFactory.getLogger(EncryptAlmPats.class);

  private final DbClient dbClient;

  public EncryptAlmPats(DbClient dbClient) {
    this.dbClient = dbClient;
  }

  @Override
  public void start() {
    try {
      encryptClearTextTokens();
    } catch (RuntimeException e) {
      LOG.warn("Failed to encrypt the DevOps platform personal access tokens that are stored as clear text. They are "
        + "left unchanged, and encrypting them is attempted again at the next restart.", e);
    }
  }

  private void encryptClearTextTokens() {
    try (DbSession dbSession = dbClient.openSession(false)) {
      int encryptedCount = dbClient.almPatDao().encryptNotEncryptedPersonalAccessTokens(dbSession);
      if (encryptedCount > 0) {
        dbSession.commit();
        LOG.info("Encrypted {} DevOps platform personal access token(s) that were stored as clear text", encryptedCount);
      }
    }
  }

  @Override
  public void stop() {
    // nothing to do
  }
}
