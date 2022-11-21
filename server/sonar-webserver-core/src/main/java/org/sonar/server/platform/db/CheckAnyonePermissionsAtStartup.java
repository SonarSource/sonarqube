/*
 * SonarQube
 * Copyright (C) 2009-2022 SonarSource SA
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
package org.sonar.server.platform.db;

import java.util.List;
import java.util.Optional;
import org.sonar.api.Startable;
import org.sonar.api.config.Configuration;
import org.sonar.api.utils.log.Logger;
import org.sonar.api.utils.log.Loggers;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;

/**
 * Checks if there are any projects which have 'Anyone' group permissions at startup, after executing db migrations. If
 * any are found, it is logged as a warning, and some example projects (up to 3) are listed. This requires to be defined
 * in platform level 4 ({@link org.sonar.server.platform.platformlevel.PlatformLevel4}).
 */
public class CheckAnyonePermissionsAtStartup implements Startable {

  private static final Logger LOG = Loggers.get(CheckAnyonePermissionsAtStartup.class);
  private static final String FORCE_AUTHENTICATION_PROPERTY_NAME = "sonar.forceAuthentication";
  private final DbClient dbClient;
  private final Configuration config;

  public CheckAnyonePermissionsAtStartup(DbClient dbClient, Configuration config) {
    this.dbClient = dbClient;
    this.config = config;
  }

  @Override
  public void start() {
    Optional<Boolean> property = config.getBoolean(FORCE_AUTHENTICATION_PROPERTY_NAME);
    if (property.isEmpty() || Boolean.TRUE.equals(property.get())) {
      return;
    }

    logWarningIfProjectsWithAnyonePermissionsExist();
  }

  private void logWarningIfProjectsWithAnyonePermissionsExist() {
    try (DbSession dbSession = dbClient.openSession(false)) {
      int total = dbClient.groupPermissionDao().countProjectsWithAnyonePermissions(dbSession);
      if (total > 0) {
        List<String> list = dbClient.groupPermissionDao().selectProjectKeysWithAnyonePermissions(dbSession, 3);
        LOG.warn("A total of {} public project(s) are found to have enabled 'Anyone' group permissions, including: {}. " +
            "Make sure your project permissions are set as intended.",
          total, String.join(", ", list));
      }
    }
  }

  @Override
  public void stop() {
    // do nothing
  }

}
