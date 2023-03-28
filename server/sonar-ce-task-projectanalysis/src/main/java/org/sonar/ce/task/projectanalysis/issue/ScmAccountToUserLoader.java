/*
 * SonarQube
 * Copyright (C) 2009-2023 SonarSource SA
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
package org.sonar.ce.task.projectanalysis.issue;

import com.google.common.base.Joiner;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import org.sonar.api.utils.log.Logger;
import org.sonar.api.utils.log.Loggers;
import org.sonar.ce.task.projectanalysis.util.cache.CacheLoader;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.db.user.UserDto;

/**
 * Loads the association between a SCM account and a SQ user
 */
public class ScmAccountToUserLoader implements CacheLoader<String, String> {

  private static final Logger LOGGER = Loggers.get(ScmAccountToUserLoader.class);

  private final DbClient dbClient;

  public ScmAccountToUserLoader(DbClient dbClient) {
    this.dbClient = dbClient;
  }

  @Override
  public String load(String scmAccount) {
    try (DbSession dbSession = dbClient.openSession(false)) {
      List<String> userUuids = dbClient.userDao().selectUserUuidByScmAccountOrLoginOrEmail(dbSession, scmAccount);
      if (userUuids.size() == 1) {
        return userUuids.iterator().next();
      }
      if (!userUuids.isEmpty()) {
        List<UserDto> userDtos = dbClient.userDao().selectByUuids(dbSession, userUuids);
        Collection<String> logins = userDtos.stream()
          .map(UserDto::getLogin)
          .sorted()
          .toList();
        LOGGER.warn(String.format("Multiple users share the SCM account '%s': %s", scmAccount, Joiner.on(", ").join(logins)));
      }
      return null;
    }
  }

  @Override
  public Map<String, String> loadAll(Collection<? extends String> scmAccounts) {
    throw new UnsupportedOperationException("Loading by multiple scm accounts is not supported yet");
  }
}
