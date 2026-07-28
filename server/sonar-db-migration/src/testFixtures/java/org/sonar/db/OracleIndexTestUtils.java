package org.sonar.db;

import java.sql.ResultSet;
import java.sql.SQLException;

import static org.assertj.core.api.Assertions.assertThat;

public class OracleIndexTestUtils {
  private OracleIndexTestUtils() {
    // utils only
  }

  /**
   * For Oracle, we only verify the index exists and is unique, without checking column names.
   * Oracle creates function-based indexes with auto-generated virtual column names (like sys_nc00014$)
   * when using CASE expressions, so we can't verify against the original column name.
   */
  public static void assertIndexExistsForOracle(MigrationDbTester db, boolean unique, String indexName, String tableName) {
    try (var connection = db.openConnection()) {
      try (ResultSet rs = connection.getMetaData().getIndexInfo(null, null, tableName.toUpperCase(), false, false)) {
        boolean indexFound = false;
        while (rs.next()) {
          if (indexName.equalsIgnoreCase(rs.getString("INDEX_NAME"))) {
            indexFound = true;
            if (unique) {
              assertThat(rs.getBoolean("NON_UNIQUE")).as("Index should be unique").isFalse();
            } else {
              assertThat(rs.getBoolean("NON_UNIQUE")).as("Index should not be unique").isTrue();
            }
            break;
          }
        }
        assertThat(indexFound).as("Index %s should exist", indexName).isTrue();
      }
    } catch (SQLException e) {
      throw new IllegalStateException("Failed to check index", e);
    }
  }
}
