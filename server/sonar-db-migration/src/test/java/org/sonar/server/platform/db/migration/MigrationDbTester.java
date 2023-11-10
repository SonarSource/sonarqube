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
package org.sonar.server.platform.db.migration;

import com.sonar.orchestrator.config.Configuration;
import com.sonar.orchestrator.db.DatabaseClient;
import com.sonar.orchestrator.db.DatabaseFactory;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import javax.annotation.Nullable;
import org.jetbrains.annotations.NotNull;
import org.sonar.api.config.internal.Settings;
import org.sonar.db.AbstractDbTester;
import org.sonar.db.DatabaseTestUtils;
import org.sonar.server.platform.db.migration.step.MigrationStep;

public class MigrationDbTester extends AbstractDbTester<MigrationTestDb> {

  private DatabaseClient databaseClient;

  private MigrationDbTester(@Nullable Class<? extends MigrationStep> migrationStepClass) {
    super(new MigrationTestDb(migrationStepClass));
  }

  public static MigrationDbTester createEmpty() {
    return new MigrationDbTester(null);
  }

  public static MigrationDbTester createForMigrationStep(Class<? extends MigrationStep> migrationStepClass) {
    return new MigrationDbTester(migrationStepClass);
  }

  @Override
  protected void before() {
    db.start();

    //Some DataChange steps might fill the tables with some data, data will be removed to ensure tests run on empty tables
    truncateAllTables();
    Configuration configuration = retrieveConfiguration();
    databaseClient = DatabaseFactory.create(configuration, configuration.locators());
  }

  @NotNull
  private Configuration retrieveConfiguration() {
    Settings settings = db.getDatabase().getSettings();
    Configuration.Builder builder = Configuration.builder();
    settings.getProperties().forEach(builder::setProperty);
    return builder.build();
  }

  @Override
  protected void after() {
    dropDatabase();
    db.stop();
  }

  private void dropDatabase() {
    try (Connection connection = db.getDatabase().getDataSource().getConnection()) {
      for (String s : databaseClient.getDropDdl()) {
        PreparedStatement preparedStatement = connection.prepareStatement(s);
        preparedStatement.execute();
      }
    } catch (SQLException e) {
      throw new RuntimeException(e);
    }
  }

  public void truncateAllTables() {
    try {
      DatabaseTestUtils.truncateAllTables(db.getDatabase().getDataSource());
    } catch (SQLException e) {
      throw new IllegalStateException("Fail to truncate db tables", e);
    }
  }
}
