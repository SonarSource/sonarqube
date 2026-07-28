package org.sonar.server.platform.db.migration.version.v202605;

import java.sql.SQLException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.sonar.db.MigrationDbTester;
import org.sonar.db.dialect.Oracle;

import static org.sonar.db.MigrationDbTester.createForMigrationStep;
import static org.sonar.db.OracleIndexTestUtils.assertIndexExistsForOracle;

class DropScaTtrHistoryUniqueIndexTest {

  private static final String TABLE_NAME = "sca_ttr_history";
  private static final String INDEX_NAME = "sca_ttr_history_uq_idx";

  @RegisterExtension
  public final MigrationDbTester db = createForMigrationStep(DropScaTtrHistoryUniqueIndex.class);
  private final DropScaTtrHistoryUniqueIndex underTest = new DropScaTtrHistoryUniqueIndex(db.database());

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
      assertIndexExistsForOracle(db, true, INDEX_NAME, TABLE_NAME);
    } else {
      db.assertUniqueIndex(TABLE_NAME, INDEX_NAME, "entity_id", "entity_type", "sca_dimension_id", "recorded_at_epoch");
    }
  }
}
