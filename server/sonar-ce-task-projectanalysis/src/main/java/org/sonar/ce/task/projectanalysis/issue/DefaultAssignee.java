/*
 * SonarQube
 * Copyright (C) 2009-2025 SonarSource SA
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

import javax.annotation.CheckForNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sonar.ce.task.projectanalysis.component.ConfigurationRepository;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.db.user.UserDto;
import org.sonar.db.user.UserIdDto;

import static com.google.common.base.Strings.isNullOrEmpty;
import static org.sonar.api.CoreProperties.DEFAULT_ISSUE_ASSIGNEE;

/**
 * The user who is optionally declared as being the assignee
 * of all the issues which SCM author is not associated with any SonarQube user.
 */
public class DefaultAssignee {

  private static final Logger LOG = LoggerFactory.getLogger(DefaultAssignee.class);

  private final DbClient dbClient;
  private final ConfigurationRepository configRepository;

  private boolean loaded = false;
  private UserIdDto userId = null;

  public DefaultAssignee(DbClient dbClient, ConfigurationRepository configRepository) {
    this.dbClient = dbClient;
    this.configRepository = configRepository;
  }

  @CheckForNull
  public UserIdDto loadDefaultAssigneeUserId() {
    if (loaded) {
      return userId;
    }
    String login = configRepository.getConfiguration().get(DEFAULT_ISSUE_ASSIGNEE).orElse(null);
    if (!isNullOrEmpty(login)) {
      userId = findValidUserUuidFromLogin(login);
    }
    loaded = true;
    return userId;
  }

  private UserIdDto findValidUserUuidFromLogin(String login) {
    try (DbSession dbSession = dbClient.openSession(false)) {
      UserDto user = dbClient.userDao().selectActiveUserByLogin(dbSession, login);
      if (user == null) {
        LOG.info("Property {} is set with an unknown login: {}", DEFAULT_ISSUE_ASSIGNEE, login);
        return null;
      }
      return new UserIdDto(user.getUuid(), user.getLogin());
    }
  }

}
