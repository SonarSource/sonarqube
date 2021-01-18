package org.sonar.server.platform.db.migration.version.v87;

import java.sql.SQLException;
import org.sonar.db.Database;
import org.sonar.server.platform.db.migration.step.DataChange;
import org.sonar.server.platform.db.migration.step.MassUpdate;

public class PopulateMonorepoColumnToProjectAlmSettingsTable extends DataChange {
  public PopulateMonorepoColumnToProjectAlmSettingsTable(Database db) {
    super(db);
  }

  @Override
  protected void execute(Context context) throws SQLException {
    MassUpdate massUpdate = context.prepareMassUpdate();
    massUpdate.select("select uuid from project_alm_settings where monorepo is null");
    massUpdate.update("update project_alm_settings set monorepo = ? where uuid = ?");
    massUpdate.execute((row, update) -> {
      update.setBoolean(1, false);
      update.setString(2, row.getString(1));
      return true;
    });
  }
}
