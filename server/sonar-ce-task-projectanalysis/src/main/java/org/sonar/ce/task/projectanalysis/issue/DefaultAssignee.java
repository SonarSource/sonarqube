/*
 * SonarQube
 * Copyright (C) 2009-2019 SonarSource SA
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
import org.sonar.api.utils.log.Logger;
import org.sonar.api.utils.log.Loggers;
import org.sonar.ce.task.projectanalysis.component.ConfigurationRepository;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.db.user.UserDto;
import org.sonar.ce.task.projectanalysis.analysis.AnalysisMetadataHolder;
import org.sonar.ce.task.projectanalysis.component.ConfigurationRepository;

import static com.google.common.base.Strings.isNullOrEmpty;
import static org.sonar.api.CoreProperties.DEFAULT_ISSUE_ASSIGNEE;

/**
 * The user who is optionally declared as being the assignee
 * of all the issues which SCM author is not associated with any SonarQube user.
 */
public class DefaultAssignee {

  private static final Logger LOG = Loggers.get(DefaultAssignee.class);

  private final DbClient dbClient;
  private final ConfigurationRepository configRepository;
  private final AnalysisMetadataHolder analysisMetadataHolder;

  private boolean loaded = false;
  private String userUuid = null;

  public DefaultAssignee(DbClient dbClient, ConfigurationRepository configRepository, AnalysisMetadataHolder analysisMetadataHolder) {
    this.dbClient = dbClient;
    this.configRepository = configRepository;
    this.analysisMetadataHolder = analysisMetadataHolder;
  }

  @CheckForNull
  public String loadDefaultAssigneeUuid() {
    if (loaded) {
      return userUuid;
    }
    String login = configRepository.getConfiguration().get(DEFAULT_ISSUE_ASSIGNEE).orElse(null);
    if (!isNullOrEmpty(login)) {
      userUuid = findValidUserUuidFromLogin(login);
    }
    loaded = true;
    return userUuid;
  }

  private String findValidUserUuidFromLogin(String login) {
    try (DbSession dbSession = dbClient.openSession(false)) {
      UserDto user = dbClient.userDao().selectActiveUserByLogin(dbSession, login);
      if (user == null) {
        LOG.info("Property {} is set with an unknown login: {}", DEFAULT_ISSUE_ASSIGNEE, login);
        return null;
      }
      if (!isUserMemberOfOrganization(dbSession, user)) {
        LOG.info("Property {} is set with a user which is not member of the organization of the project : {}", DEFAULT_ISSUE_ASSIGNEE, login);
        return null;
      }
      return user.getUuid();
    }
  }

  private boolean isUserMemberOfOrganization(DbSession dbSession, UserDto user) {
    return dbClient.organizationMemberDao().select(dbSession, analysisMetadataHolder.getOrganization().getUuid(), user.getId()).isPresent();
  }
}
