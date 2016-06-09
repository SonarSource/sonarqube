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
package org.sonar.db.version.v56;

import java.sql.SQLException;
import org.sonar.api.platform.ServerUpgradeStatus;
import org.sonar.api.utils.log.Loggers;
import org.sonar.db.Database;
import org.sonar.db.dialect.MySql;
import org.sonar.db.version.DdlChange;

public class FixTypeOfRuleTypeOnMysql extends DdlChange {

  private static final int SQ_5_5 = 1100;
  private static final int SQ_5_6 = 1150;

  private final ServerUpgradeStatus dbVersion;

  public FixTypeOfRuleTypeOnMysql(Database db, ServerUpgradeStatus dbVersion) {
    super(db);
    this.dbVersion = dbVersion;
  }

  @Override
  public void execute(Context context) throws SQLException {
    // In SQ 5.6, migration 1100 create columns with expected type TINYINT(2)
    // In SQ 5.5, migration 1100 create columns with type TINYINT(1) instead of TINYINT(2)
    // In SQ 5.4 and lower, the type TINYINT(1) was used only for boolean columns, so no problem
    // As an optimization fix must be applied only for instances upgrading from 5.5.x
    if (getDialect().getId().equals(MySql.ID) &&
      dbVersion.getInitialDbVersion() >= SQ_5_5 && dbVersion.getInitialDbVersion() < SQ_5_6) {
      Loggers.get(getClass()).info("Changing TINYINT(1) to TINYINT(2)");
      context.execute("ALTER TABLE rules MODIFY COLUMN rule_type TINYINT (2)");
      context.execute("ALTER TABLE issues MODIFY COLUMN issue_type TINYINT (2)");
    }
  }
}
