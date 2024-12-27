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
package org.sonar.ce.task.projectanalysis.issue;

import com.google.common.base.Joiner;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sonar.ce.task.projectanalysis.util.cache.CacheLoader;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.db.user.UserIdDto;

/**
 * Loads the association between a SCM account and a SQ user
 */
public class ScmAccountToUserLoader implements CacheLoader<String, UserIdDto> {

  private static final Logger LOGGER = LoggerFactory.getLogger(ScmAccountToUserLoader.class);

  private final DbClient dbClient;

  public ScmAccountToUserLoader(DbClient dbClient) {
    this.dbClient = dbClient;
  }

  @Override
  public UserIdDto load(String scmAccount) {
    try (DbSession dbSession = dbClient.openSession(false)) {
      List<UserIdDto> users = dbClient.userDao().selectActiveUsersByScmAccountOrLoginOrEmail(dbSession, scmAccount);
      if (users.size() == 1) {
        return users.iterator().next();
      }
      if (!users.isEmpty()) {
        Collection<String> logins = users.stream()
          .map(UserIdDto::getLogin)
          .sorted()
          .toList();
        LOGGER.atWarn()
          .addArgument(scmAccount)
          .addArgument(Joiner.on(", ").join(logins))
          .log("Multiple users share the SCM account '{}': {}");
      }
      return null;
    }
  }

  @Override
  public Map<String, UserIdDto> loadAll(Collection<? extends String> scmAccounts) {
    throw new UnsupportedOperationException("Loading by multiple scm accounts is not supported yet");
  }
}
