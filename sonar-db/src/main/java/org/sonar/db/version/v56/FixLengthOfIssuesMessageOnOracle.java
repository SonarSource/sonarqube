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
import org.sonar.db.Database;
import org.sonar.db.dialect.Oracle;
import org.sonar.db.version.DdlChange;

public class FixLengthOfIssuesMessageOnOracle extends DdlChange {

  public FixLengthOfIssuesMessageOnOracle(Database db) {
    super(db);
  }

  @Override
  public void execute(Context context) throws SQLException {
    if (getDialect().getId().equals(Oracle.ID)) {
      // in order to not depend on value of variable NLS_LENGTH_SEMANTICS, unit of length
      // is enforced to CHAR so that we're sure that type can't be 4000 BYTE.
      context.execute("ALTER TABLE issues MODIFY (message VARCHAR (4000 CHAR))");
    }
  }

}
