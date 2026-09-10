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
 * Encrypts the user-scoped personal access tokens that are still stored as clear text, and rewrites the encrypted
 * ones with the current secret key while a key is being replaced. Tokens are encrypted when they are written, so the
 * first only concerns tokens written before a secret key was configured, or before encryption existed.
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
      rewriteTokens();
    } catch (RuntimeException e) {
      LOG.warn("Failed to rewrite the DevOps platform personal access tokens. The tokens rewritten before the failure "
        + "are kept, and the remaining ones are attempted again at the next restart.", e);
    }
  }

  /**
   * The rotation pass runs first so that it only sees the tokens that were already encrypted when the node started.
   * Encrypting a clear text token uses the current key, so a token written by the other pass never needs rotating,
   * and rewriting it again would cost a second update and report a rotation that did not happen.
   */
  private void rewriteTokens() {
    try (DbSession dbSession = dbClient.openSession(false)) {
      reEncryptTokensWithCurrentSecretKey(dbSession);
      encryptClearTextTokens(dbSession);
    }
  }

  private void encryptClearTextTokens(DbSession dbSession) {
    int encryptedCount = dbClient.almPatDao().encryptNotEncryptedPersonalAccessTokens(dbSession);
    if (encryptedCount > 0) {
      LOG.info("Encrypted {} DevOps platform personal access token(s) that were stored as clear text", encryptedCount);
    }
  }

  /**
   * Only does anything while a secret key is being replaced. Once every token has been rewritten with the new key, the
   * previous one can be removed from the configuration, and this stops running.
   */
  private void reEncryptTokensWithCurrentSecretKey(DbSession dbSession) {
    int reEncryptedCount = dbClient.almPatDao().reEncryptPersonalAccessTokens(dbSession);
    if (reEncryptedCount > 0) {
      LOG.info("Re-encrypted {} DevOps platform personal access token(s) with the current secret key. The previous "
        + "secret key can be removed from the configuration once every other encrypted setting has been rewritten too.", reEncryptedCount);
    }
  }

  @Override
  public void stop() {
    // nothing to do
  }
}
