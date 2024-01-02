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
package org.sonar.server.authentication.purge;

import java.util.concurrent.TimeUnit;
import org.sonar.api.Startable;
import org.sonar.api.utils.log.Logger;
import org.sonar.api.utils.log.Loggers;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.server.util.GlobalLockManager;

public class ExpiredSessionsCleaner implements Startable {

  private static final Logger LOG = Loggers.get(ExpiredSessionsCleaner.class);

  private static final long PERIOD_IN_SECONDS = 24 * 60 * 60L;
  private static final String LOCK_NAME = "SessionCleaner";

  private final ExpiredSessionsCleanerExecutorService executorService;
  private final DbClient dbClient;
  private final GlobalLockManager lockManager;

  public ExpiredSessionsCleaner(ExpiredSessionsCleanerExecutorService executorService, DbClient dbClient, GlobalLockManager lockManager) {
    this.executorService = executorService;
    this.dbClient = dbClient;
    this.lockManager = lockManager;
  }

  @Override
  public void start() {
    this.executorService.scheduleAtFixedRate(this::executePurge, 0, PERIOD_IN_SECONDS, TimeUnit.SECONDS);
  }

  private void executePurge() {
    if (!lockManager.tryLock(LOCK_NAME)) {
      return;
    }
    try (DbSession dbSession = dbClient.openSession(false)) {
      cleanExpiredSessionTokens(dbSession);
      cleanExpiredSamlMessageIds(dbSession);
    }
  }

  private void cleanExpiredSessionTokens(DbSession dbSession) {
    LOG.debug("Start of cleaning expired session tokens");
    int deletedSessionTokens = dbClient.sessionTokensDao().deleteExpired(dbSession);
    dbSession.commit();
    LOG.info("Purge of expired session tokens has removed {} elements", deletedSessionTokens);
  }

  private void cleanExpiredSamlMessageIds(DbSession dbSession) {
    LOG.debug("Start of cleaning expired SAML message IDs");
    int deleted = dbClient.samlMessageIdDao().deleteExpired(dbSession);
    dbSession.commit();
    LOG.info("Purge of expired SAML message ids has removed {} elements", deleted);
  }

  @Override
  public void stop() {
    // nothing to do
  }

}
