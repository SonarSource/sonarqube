/*
 * SonarQube, open source software quality management tool.
 * Copyright (C) 2008-2013 SonarSource
 * mailto:contact AT sonarsource DOT com
 *
 * SonarQube is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * SonarQube is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */
package org.sonar.server.db.migrations.violation;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sonar.api.config.Settings;
import org.sonar.api.utils.MessageException;
import org.sonar.core.persistence.Database;
import org.sonar.server.db.migrations.DatabaseMigration;
import org.sonar.server.db.migrations.util.SqlUtil;

import java.sql.SQLException;

/**
 * Used in the Active Record Migration 401
 */
public class ViolationMigration implements DatabaseMigration {

  private static final Logger LOGGER = LoggerFactory.getLogger(ViolationMigration.class);

  private static final String FAILURE_MESSAGE = "Fail to convert violations to issues";

  private final Settings settings;
  private final Database db;

  public ViolationMigration(Database database, Settings settings) {
    this.db = database;
    this.settings = settings;
  }

  @Override
  public void execute() {
    try {
      LOGGER.info("Initialize input");
      Referentials referentials = new Referentials(db);
      if (referentials.totalViolations() > 0) {
        LOGGER.info("Migrate {} violations", referentials.totalViolations());
        ViolationConverters converters = new ViolationConverters(settings);
        converters.execute(referentials, db);
      }
    } catch (SQLException e) {
      LOGGER.error(FAILURE_MESSAGE, e);
      SqlUtil.log(LOGGER, e);
      throw MessageException.of(FAILURE_MESSAGE);

    } catch (Exception e) {
      LOGGER.error(FAILURE_MESSAGE, e);
      throw MessageException.of(FAILURE_MESSAGE);
    }
  }

}
