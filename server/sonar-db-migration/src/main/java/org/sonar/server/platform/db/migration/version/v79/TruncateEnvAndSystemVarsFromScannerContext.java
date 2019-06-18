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
package org.sonar.server.platform.db.migration.version.v79;

  import java.nio.charset.StandardCharsets;
  import java.sql.SQLException;
  import org.sonar.db.Database;
  import org.sonar.server.platform.db.migration.SupportsBlueGreen;
  import org.sonar.server.platform.db.migration.step.DataChange;
  import org.sonar.server.platform.db.migration.step.MassUpdate;
  import org.sonar.server.platform.db.migration.step.Select;
  import org.sonar.server.platform.db.migration.step.SqlStatement;

@SupportsBlueGreen
public class TruncateEnvAndSystemVarsFromScannerContext extends DataChange {

  public TruncateEnvAndSystemVarsFromScannerContext(Database db) {
    super(db);
  }

  @Override
  protected void execute(Context context) throws SQLException {
    MassUpdate massUpdate = context.prepareMassUpdate();
    massUpdate.select("select task_uuid, context_data from ce_scanner_context sc");
    massUpdate.update("update ce_scanner_context set context_data = ? where task_uuid = ?");
    massUpdate.rowPluralName("truncate scanner context content");
    massUpdate.execute(TruncateEnvAndSystemVarsFromScannerContext::truncateScannerContext);
  }

  private static boolean truncateScannerContext(Select.Row row, SqlStatement update) throws SQLException {
    String taskUuid = row.getString(1);
    byte[] bytes = row.getBytes(2);
    String reportContent = new String(bytes, StandardCharsets.UTF_8);
    int startIndex = reportContent.indexOf("SonarQube plugins:");
    if (startIndex != -1) {
      reportContent = reportContent.substring(startIndex);
    }
    update.setBytes(1, reportContent.getBytes(StandardCharsets.UTF_8));
    update.setString(2, taskUuid);
    return true;
  }
}
