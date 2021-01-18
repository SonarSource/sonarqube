package org.sonar.server.platform.db.migration.version.v87;

import java.sql.SQLException;
import java.sql.Types;
import org.junit.Rule;
import org.junit.Test;
import org.sonar.db.CoreDbTester;
import org.sonar.server.platform.db.migration.step.DdlChange;

import static org.assertj.core.api.Assertions.assertThat;

public class MakeMonorepoColumnInProjectAlmSettingsTableNotNullableTest {
  private static final String TABLE_NAME = "project_alm_settings";

  @Rule
  public CoreDbTester db = CoreDbTester.createForSchema(MakeMonorepoColumnInProjectAlmSettingsTableNotNullableTest.class, "schema.sql");

  private final DdlChange underTest = new MakeMonorepoColumnInProjectAlmSettingsTableNotNullable(db.database());

  @Test
  public void verify_monorepo_column_not_nullable() throws SQLException {
    insertProjectAlmSettings(1);
    insertProjectAlmSettings(2);
    insertProjectAlmSettings(3);

    underTest.execute();

    db.assertColumnDefinition(TABLE_NAME, "monorepo", Types.BOOLEAN, null, false);

    assertThat(db.countRowsOfTable(TABLE_NAME)).isEqualTo(3);
  }

  private void insertProjectAlmSettings(int id) {
    db.executeInsert("project_alm_settings",
      "UUID", "uuid-" + id,
      "ALM_SETTING_UUID", "ALM_SETTING_UUID",
      "PROJECT_UUID", "PROJECT_UUID-" + id,
      "ALM_REPO", "ALM_REPO",
      "ALM_SLUG", "ALM_SLUG",
      "MONOREPO", false,
      "UPDATED_AT", 12342342,
      "CREATED_AT",1232342
    );
  }
}
