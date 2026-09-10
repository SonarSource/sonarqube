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

import static org.sonar.api.CoreProperties.ENCRYPTION_SECRET_KEY_PATH;

/**
 * Encrypts the user-scoped personal access tokens that are still stored as clear text, rewrites the encrypted ones
 * with the current secret key while a key is being replaced, and warns about the tokens that are still in clear text
 * afterwards. Tokens are encrypted when they are written, so the first only concerns tokens written before a secret
 * key was configured, or before encryption existed. The warning only reports anything on an instance with no secret
 * key configured, which is the one case where encrypting them is not possible.
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
    try (DbSession dbSession = dbClient.openSession(false)) {
      // the rotation runs first so that it only sees the tokens that were already encrypted when the node started:
      // the pass after it writes with the current key, so a token it rewrote never needs rotating, and rotating it
      // anyway would cost a second update and report a rotation that did not happen
      runQuietly(dbSession, () -> reEncryptTokensWithCurrentSecretKey(dbSession),
        "Failed to re-encrypt the DevOps platform personal access tokens with the current secret key. The tokens "
          + "re-encrypted before the failure are kept, and the remaining ones are attempted again at the next restart.");
      runQuietly(dbSession, () -> encryptClearTextTokens(dbSession),
        "Failed to encrypt the DevOps platform personal access tokens that are stored as clear text. The tokens "
          + "encrypted before the failure are kept, and the remaining ones are attempted again at the next restart.");
      runQuietly(dbSession, () -> warnAboutRemainingClearTextTokens(dbSession),
        "Failed to count the DevOps platform personal access tokens that are stored as clear text, so this startup "
          + "reports nothing about them. This is attempted again at the next restart.");
    } catch (RuntimeException e) {
      LOG.warn("Failed to open a database session to rewrite the DevOps platform personal access tokens. They are "
        + "left unchanged, and this is attempted again at the next restart.", e);
    }
  }

  /**
   * Each pass is wrapped on its own, rather than all three together, so that one failing neither takes credit for
   * what another one did nor cancels the ones after it. Wrapped together, a transient failure of either rewrite
   * would silently take the clear text warning with it.
   */
  private static void runQuietly(DbSession dbSession, Runnable pass, String failureMessage) {
    try {
      pass.run();
    } catch (RuntimeException e) {
      // what the failed pass left in the session must not be committed by the next one, and a rollback failing too,
      // on a connection that is already gone, must not cancel that next pass either
      try {
        dbSession.rollback();
      } catch (RuntimeException rollbackFailure) {
        e.addSuppressed(rollbackFailure);
      }
      LOG.warn(failureMessage, e);
    }
  }

  /**
   * Anything still in clear text at this point means no secret key is configured, because the pass above would
   * otherwise have encrypted it. Without this the exposure is silent, and an administrator has no way to notice it.
   */
  private void warnAboutRemainingClearTextTokens(DbSession dbSession) {
    int clearTextCount = dbClient.almPatDao().countNotEncryptedPersonalAccessTokens(dbSession);
    if (clearTextCount > 0) {
      LOG.warn("{} DevOps platform personal access token(s) are stored as clear text, because no secret key is configured. "
        + "Set the '{}' property, or the SONAR_SECRET_KEY environment variable, to have them encrypted on the next restart.",
        clearTextCount, ENCRYPTION_SECRET_KEY_PATH);
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
