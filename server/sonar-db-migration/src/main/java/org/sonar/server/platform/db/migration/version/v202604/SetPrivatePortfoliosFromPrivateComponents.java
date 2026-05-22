/*
 * SonarQube
 * Copyright (C) SonarSource Sàrl
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
package org.sonar.server.platform.db.migration.version.v202604;

import java.sql.SQLException;
import org.sonar.db.Database;
import org.sonar.server.platform.db.migration.step.DataChange;
import org.sonar.server.platform.db.migration.step.Upsert;

public class SetPrivatePortfoliosFromPrivateComponents extends DataChange {

  public SetPrivatePortfoliosFromPrivateComponents(Database db) {
    super(db);
  }

  @Override
  protected void execute(Context context) throws SQLException {
    final String portfoliosQuery = """
      UPDATE portfolios
      SET private = ?
      WHERE private = ?
        AND EXISTS (
          SELECT 1
          FROM components
          WHERE components.uuid = portfolios.uuid
            AND components.private = ?
            AND components.qualifier IN (?, ?)
            AND components.scope = ?
        )
      """;

    final String componentsQuery = """
      UPDATE components
      SET private = ?
      WHERE private = ?
        AND qualifier IN (?, ?)
        AND scope = ?
        AND EXISTS (
          SELECT 1
          FROM portfolios
          WHERE portfolios.uuid = components.uuid
            AND portfolios.private = ?
        )
    """;

    try (
      final Upsert updatePortfolios = context.prepareUpsert(portfoliosQuery);
      final Upsert updateComponents = context.prepareUpsert(componentsQuery)
    ) {
      final String portfolioQualifier = "VW";
      final String subportfolioQualifier = "SVW";
      final String scope = "PRJ";

      updatePortfolios
        .setBoolean(1, true)
        .setBoolean(2, false)
        .setBoolean(3, true)
        .setString(4, portfolioQualifier)
        .setString(5, subportfolioQualifier)
        .setString(6, scope)
        .execute();

      updateComponents
        .setBoolean(1, true)
        .setBoolean(2, false)
        .setString(3, portfolioQualifier)
        .setString(4, subportfolioQualifier)
        .setString(5, scope)
        .setBoolean(6, true)
        .execute()
        .commit();
    }
  }
}
