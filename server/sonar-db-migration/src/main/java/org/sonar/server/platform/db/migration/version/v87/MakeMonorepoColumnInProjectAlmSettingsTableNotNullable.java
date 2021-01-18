package org.sonar.server.platform.db.migration.version.v87;

import java.sql.SQLException;
import org.sonar.db.Database;
import org.sonar.server.platform.db.migration.def.BooleanColumnDef;
import org.sonar.server.platform.db.migration.sql.AlterColumnsBuilder;
import org.sonar.server.platform.db.migration.step.DdlChange;

import static org.sonar.server.platform.db.migration.def.BooleanColumnDef.newBooleanColumnDefBuilder;

public class MakeMonorepoColumnInProjectAlmSettingsTableNotNullable extends DdlChange {
  private static final String TABLE = "project_alm_settings";

  private static final BooleanColumnDef MONOREPO = newBooleanColumnDefBuilder()
    .setColumnName("monorepo")
    .setIsNullable(false)
    .build();

  public MakeMonorepoColumnInProjectAlmSettingsTableNotNullable(Database db) {
    super(db);
  }

  @Override
  public void execute(Context context) throws SQLException {
    context.execute(new AlterColumnsBuilder(getDialect(), TABLE)
      .updateColumn(MONOREPO)
      .build());
  }
}
