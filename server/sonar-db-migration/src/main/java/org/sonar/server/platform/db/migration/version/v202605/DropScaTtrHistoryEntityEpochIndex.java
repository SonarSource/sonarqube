package org.sonar.server.platform.db.migration.version.v202605;

import org.sonar.db.Database;
import org.sonar.server.platform.db.migration.step.DropIndexChange;

/**
 * In order to resize the entity_id and entity_type columns on sca_ttr_history, we need to drop this index first.
 * The index will be recreated after resizing.
 */
public class DropScaTtrHistoryEntityEpochIndex extends DropIndexChange {
  static final String TABLE_NAME = "sca_ttr_history";
  static final String INDEX_NAME = "sca_ttr_history_ent_type_epoch";

  public DropScaTtrHistoryEntityEpochIndex(Database db) {
    super(db, INDEX_NAME, TABLE_NAME);
  }
}
