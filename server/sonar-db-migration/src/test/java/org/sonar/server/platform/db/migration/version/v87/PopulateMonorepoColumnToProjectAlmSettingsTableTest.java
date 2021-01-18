package org.sonar.server.platform.db.migration.version.v87;

import java.sql.SQLException;
import java.sql.Types;
import org.junit.Rule;
import org.junit.Test;
import org.sonar.db.CoreDbTester;
import org.sonar.server.platform.db.migration.step.DataChange;

import static org.assertj.core.api.Assertions.assertThat;

public class PopulateMonorepoColumnToProjectAlmSettingsTableTest {
  @Rule
  public CoreDbTester db = CoreDbTester.createForSchema(PopulateMonorepoColumnToProjectAlmSettingsTableTest.class, "schema.sql");

  private final DataChange underTest = new PopulateMonorepoColumnToProjectAlmSettingsTable(db.database());

  @Test
  public void populate_monorepo_on_empty_table() throws SQLException {
    underTest.execute();

    assertThat(db.countSql("select count(*) from project_alm_settings")).isZero();
  }

  @Test
  public void populate_monorepo_column() throws SQLException {
    insertProjectAlmSettings(1);
    insertProjectAlmSettings(2);
    insertProjectAlmSettings(3);

    underTest.execute();

    db.assertColumnDefinition("project_alm_settings", "monorepo", Types.BOOLEAN, null, true);
    assertThat(db.countSql("select count(uuid) from project_alm_settings where monorepo = false")).isEqualTo(3);
  }

  private void insertProjectAlmSettings(int id) {
    db.executeInsert("project_alm_settings",
      "UUID", "uuid-" + id,
      "ALM_SETTING_UUID", "ALM_SETTING_UUID",
      "PROJECT_UUID", "PROJECT_UUID-" + id,
      "ALM_REPO", "ALM_REPO",
      "ALM_SLUG", "ALM_SLUG",
      "UPDATED_AT", 12342342,
      "CREATED_AT",1232342
    );
  }
}
