package org.sonar.server.platform.db.migration.version.v87;

import java.sql.SQLException;
import org.sonar.db.Database;
import org.sonar.server.platform.db.migration.def.BooleanColumnDef;
import org.sonar.server.platform.db.migration.sql.AddColumnsBuilder;
import org.sonar.server.platform.db.migration.step.DdlChange;

import static org.sonar.server.platform.db.migration.def.BooleanColumnDef.newBooleanColumnDefBuilder;

public class AddMonorepoColumnToProjectAlmSettingsTable extends DdlChange {

  private static final String TABLE = "project_alm_settings";

  private static final BooleanColumnDef MONOREPO = newBooleanColumnDefBuilder()
    .setColumnName("monorepo")
    .setIsNullable(true)
    .build();

  public AddMonorepoColumnToProjectAlmSettingsTable(Database db) {
    super(db);
  }

  @Override
  public void execute(Context context) throws SQLException {
    context.execute(new AddColumnsBuilder(getDialect(), TABLE)
      .addColumn(MONOREPO)
      .build());
  }
}
