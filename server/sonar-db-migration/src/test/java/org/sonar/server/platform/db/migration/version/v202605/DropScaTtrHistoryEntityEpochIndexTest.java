package org.sonar.server.platform.db.migration.version.v202605;

import java.sql.SQLException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.sonar.db.MigrationDbTester;
import org.sonar.db.dialect.Oracle;

import static org.sonar.db.MigrationDbTester.createForMigrationStep;
import static org.sonar.db.OracleIndexTestUtils.assertIndexExistsForOracle;

class DropScaTtrHistoryEntityEpochIndexTest {

  private static final String TABLE_NAME = "sca_ttr_history";
  private static final String INDEX_NAME = "sca_ttr_history_ent_type_epoch";

  @RegisterExtension
  public final MigrationDbTester db = createForMigrationStep(DropScaTtrHistoryEntityEpochIndex.class);
  private final DropScaTtrHistoryEntityEpochIndex underTest = new DropScaTtrHistoryEntityEpochIndex(db.database());

  @Test
  void execute_shouldDropIndex() throws SQLException {
    assertIndexExists();

    underTest.execute();

    db.assertIndexDoesNotExist(TABLE_NAME, INDEX_NAME);
  }

  @Test
  void execute_shouldBeReentrant() throws SQLException {
    assertIndexExists();

    underTest.execute();
    underTest.execute();

    db.assertIndexDoesNotExist(TABLE_NAME, INDEX_NAME);
  }

  private void assertIndexExists() {
    if (Oracle.ID.equals(db.database().getDialect().getId())) {
      assertIndexExistsForOracle(db, false, INDEX_NAME, TABLE_NAME);
    } else {
      db.assertIndex(TABLE_NAME, INDEX_NAME, "entity_id", "entity_type", "recorded_at_epoch");
    }
  }
}
