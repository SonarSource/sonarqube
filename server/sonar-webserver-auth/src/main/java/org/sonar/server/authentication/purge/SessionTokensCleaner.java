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

package org.sonar.server.authentication.purge;

import java.util.concurrent.TimeUnit;
import org.sonar.api.Startable;
import org.sonar.api.config.Configuration;
import org.sonar.api.utils.log.Logger;
import org.sonar.api.utils.log.Loggers;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.server.util.GlobalLockManager;

public class SessionTokensCleaner implements Startable {

  private static final Logger LOG = Loggers.get(SessionTokensCleaner.class);

  private static final String PURGE_DELAY_CONFIGURATION = "sonar.authentication.session.tokens.purge.delay";
  private static final long DEFAULT_PURGE_DELAY_IN_SECONDS = 24 * 60 * 60L;
  private static final String LOCK_NAME = "SessionCleaner";

  private final SessionTokensCleanerExecutorService executorService;
  private final DbClient dbClient;
  private final Configuration configuration;
  private final GlobalLockManager lockManager;

  public SessionTokensCleaner(SessionTokensCleanerExecutorService executorService, DbClient dbClient, Configuration configuration, GlobalLockManager lockManager) {
    this.executorService = executorService;
    this.dbClient = dbClient;
    this.configuration = configuration;
    this.lockManager = lockManager;
  }

  @Override
  public void start() {
    this.executorService.scheduleAtFixedRate(this::executePurge, 0, configuration.getLong(PURGE_DELAY_CONFIGURATION).orElse(DEFAULT_PURGE_DELAY_IN_SECONDS), TimeUnit.SECONDS);
  }

  private void executePurge() {
    if (!lockManager.tryLock(LOCK_NAME)) {
      return;
    }
    LOG.debug("Start of cleaning expired session tokens");
    try (DbSession dbSession = dbClient.openSession(false)) {
      int deletedSessionTokens = dbClient.sessionTokensDao().deleteExpired(dbSession);
      dbSession.commit();
      LOG.info("Purge of expired session tokens has removed {} elements", deletedSessionTokens);
    }
  }

  @Override
  public void stop() {
    // nothing to do
  }

}
