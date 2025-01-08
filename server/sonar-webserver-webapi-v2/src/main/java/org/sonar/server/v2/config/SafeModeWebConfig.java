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
package org.sonar.server.v2.config;

import org.sonar.db.Database;
import org.sonar.server.common.platform.LivenessChecker;
import org.sonar.server.health.HealthChecker;
import org.sonar.server.platform.db.migration.DatabaseMigrationState;
import org.sonar.server.platform.db.migration.version.DatabaseVersion;
import org.sonar.server.user.SystemPasscode;
import org.sonar.server.v2.api.system.controller.DatabaseMigrationsController;
import org.sonar.server.v2.api.system.controller.DefaultLivenessController;
import org.sonar.server.v2.api.system.controller.HealthController;
import org.sonar.server.v2.api.system.controller.LivenessController;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;

@Configuration
@EnableWebMvc
@Import(CommonWebConfig.class)
public class SafeModeWebConfig {

  @Bean
  public LivenessController livenessController(LivenessChecker livenessChecker, SystemPasscode systemPasscode) {
    return new DefaultLivenessController(livenessChecker, systemPasscode, null);
  }

  @Bean
  public HealthController healthController(HealthChecker healthChecker, SystemPasscode systemPasscode) {
    return new HealthController(healthChecker, systemPasscode);
  }

  @Bean
  public DatabaseMigrationsController databaseMigrationsController(DatabaseVersion databaseVersion, DatabaseMigrationState databaseMigrationState,
    Database database) {
    return new DatabaseMigrationsController(databaseVersion, databaseMigrationState, database);
  }
}
