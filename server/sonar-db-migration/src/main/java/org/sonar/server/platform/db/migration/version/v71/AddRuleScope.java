package org.sonar.server.platform.db.migration.version.v71;

import java.sql.SQLException;
import org.sonar.db.Database;
import org.sonar.server.platform.db.migration.def.VarcharColumnDef;
import org.sonar.server.platform.db.migration.sql.AddColumnsBuilder;
import org.sonar.server.platform.db.migration.step.DdlChange;

public class AddRuleScope extends DdlChange {

  public AddRuleScope(Database db) {
    super(db);
  }

  @Override
  public void execute(Context context) throws SQLException {
    context.execute(new AddColumnsBuilder(getDialect(), "rules")
      .addColumn(VarcharColumnDef.newVarcharColumnDefBuilder()
        .setColumnName("scope")
        .setIsNullable(true)
        .setLimit(20)
        .build())
      .build());    
  }

}
