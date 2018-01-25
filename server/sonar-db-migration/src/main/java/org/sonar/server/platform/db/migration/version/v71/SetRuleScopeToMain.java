package org.sonar.server.platform.db.migration.version.v71;

import java.sql.SQLException;
import org.sonar.api.rule.RuleScope;
import org.sonar.api.utils.System2;
import org.sonar.db.Database;
import org.sonar.server.platform.db.migration.step.DataChange;
import org.sonar.server.platform.db.migration.step.MassUpdate;

public class SetRuleScopeToMain extends DataChange {
  private final System2 system2;

  public SetRuleScopeToMain(Database db, System2 system2) {
    super(db);
    this.system2 = system2;
  }

  @Override
  protected void execute(Context context) throws SQLException {
    long now = system2.now();
    MassUpdate massUpdate = context.prepareMassUpdate();
    massUpdate.select("select id from rules where scope is NULL");
    massUpdate.rowPluralName("rules");
    massUpdate.update("update rules set scope=?, updated_at=? where scope is NULL");
    massUpdate.execute((row, update) -> {
      update.setString(1, RuleScope.MAIN.name());
      update.setLong(2, now);
      return true;
    });
  }
}
