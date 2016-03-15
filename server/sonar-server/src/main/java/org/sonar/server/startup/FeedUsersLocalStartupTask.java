/*
 * SonarQube
 * Copyright (C) 2009-2016 SonarSource SA
 * mailto:contact AT sonarsource DOT com
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

import java.util.HashSet;
import java.util.Set;
import org.picocontainer.Startable;
import org.sonar.api.config.Settings;
import org.sonar.api.user.UserQuery;
import org.sonar.api.utils.System2;
import org.sonar.api.utils.log.Logger;
import org.sonar.api.utils.log.Loggers;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.db.loadedtemplate.LoadedTemplateDto;
import org.sonar.db.user.UserDto;
import org.sonar.server.user.UserUpdater;

import static java.util.Arrays.asList;
import static org.sonar.api.CoreProperties.CORE_AUTHENTICATOR_REALM;
import static org.sonar.db.loadedtemplate.LoadedTemplateDto.ONE_SHOT_TASK_TYPE;

/**
 * Feed users local property.
 * If a realm is defined, then users are set as local only if their login are found in the property "sonar.security.localUsers",
 * otherwise user are all set as local.
 *
 * See <a href="https://jira.sonarsource.com/browse/SONAR-7254">SONAR-7254</a>.
 *
 * Should be removed after LTS 5.X
 *
 * @since 5.5
 */
public class FeedUsersLocalStartupTask implements Startable {

  private static final Logger LOG = Loggers.get(FeedUsersLocalStartupTask.class);

  private static final String TEMPLATE_KEY = "UpdateUsersLocal";
  private static final String LOCAL_USERS_PROPERTY = "sonar.security.localUsers";

  private final System2 system2;

  private final DbClient dbClient;
  private final Settings settings;

  public FeedUsersLocalStartupTask(System2 system2, DbClient dbClient, Settings settings) {
    this.system2 = system2;
    this.dbClient = dbClient;
    this.settings = settings;
  }

  @Override
  public void start() {
    DbSession dbSession = dbClient.openSession(false);
    try {
      if (hasAlreadyBeenExecuted(dbSession)) {
        return;
      }
      updateUsersLocal(dbSession);
      markAsExecuted(dbSession);
      dbSession.commit();

      if (settings.hasKey(LOCAL_USERS_PROPERTY)) {
        LOG.info("NOTE : The property '{}' is now no more needed, you can safely remove it.", LOCAL_USERS_PROPERTY);
      }
    } finally {
      dbClient.closeSession(dbSession);
    }
  }

  private void updateUsersLocal(DbSession dbSession) {
    long now = system2.now();
    Set<String> localUsers = new HashSet<>(asList(settings.getStringArray(LOCAL_USERS_PROPERTY)));
    boolean isRealmExist = settings.getString(CORE_AUTHENTICATOR_REALM) != null;
    for (UserDto user : dbClient.userDao().selectUsers(dbSession, UserQuery.ALL_ACTIVES)) {
      if (user.getExternalIdentityProvider().equals(UserUpdater.SQ_AUTHORITY)) {
        user.setLocal(!isRealmExist || localUsers.contains(user.getLogin()));
      } else {
        user.setLocal(false);
      }
      user.setUpdatedAt(now);
      dbClient.userDao().update(dbSession, user);
    }
  }

  private boolean hasAlreadyBeenExecuted(DbSession dbSession) {
    return dbClient.loadedTemplateDao().countByTypeAndKey(ONE_SHOT_TASK_TYPE, TEMPLATE_KEY, dbSession) > 0;
  }

  private void markAsExecuted(DbSession dbSession) {
    dbClient.loadedTemplateDao().insert(new LoadedTemplateDto(TEMPLATE_KEY, ONE_SHOT_TASK_TYPE), dbSession);
  }

  @Override
  public void stop() {
    // Nothing to do
  }
}
