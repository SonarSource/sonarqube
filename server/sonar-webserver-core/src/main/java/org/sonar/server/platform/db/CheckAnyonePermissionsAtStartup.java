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
package org.sonar.server.platform.db;

import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sonar.api.Startable;
import org.sonar.api.config.Configuration;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;

/**
 * Checks if there are any projects which have 'Anyone' group permissions at startup, after executing db migrations. If
 * any are found, it is logged as a warning, and some example projects (up to 3) are listed. This requires to be defined
 * in platform level 4 ({@link org.sonar.server.platform.platformlevel.PlatformLevel4}).
 */
public class CheckAnyonePermissionsAtStartup implements Startable {

  private static final Logger LOG = LoggerFactory.getLogger(CheckAnyonePermissionsAtStartup.class);
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

    logWarningsIfAnyonePermissionsExist();
  }

  private void logWarningsIfAnyonePermissionsExist() {
    try (DbSession dbSession = dbClient.openSession(false)) {
      if (!dbClient.groupPermissionDao().selectGlobalPermissionsOfGroup(dbSession, null).isEmpty()) {
        LOG.warn("Authentication is not enforced, and permissions assigned to the 'Anyone' group globally expose the " +
          "instance to security risks. Unauthenticated visitors may unintentionally have permissions on projects.");
      }

      int total = dbClient.groupPermissionDao().countEntitiesWithAnyonePermissions(dbSession);
      if (total > 0) {
        LOG.atWarn()
          .addArgument(total)
          .addArgument(String.join(", ", dbClient.groupPermissionDao().selectProjectKeysWithAnyonePermissions(dbSession, 3)))
          .log("Authentication is not enforced, and project permissions assigned to the 'Anyone' group expose {} "
            + "public project(s) to security risks, including: {}. Unauthenticated visitors have permissions on these project(s).");
      }
    }
  }

  @Override
  public void stop() {
    // do nothing
  }

}
