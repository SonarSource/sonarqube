package org.sonar.server.platform.db.migration.version.v102;

import java.sql.SQLException;
import java.sql.Types;
import org.junit.Rule;
import org.junit.Test;
import org.sonar.db.CoreDbTester;
import org.sonar.server.platform.db.migration.step.DdlChange;

import static org.sonar.server.platform.db.migration.def.VarcharColumnDef.UUID_SIZE;
import static org.sonar.server.platform.db.migration.version.v102.CreateGithubOrganizationsGroupsTable.GROUP_UUID_COLUMN_NAME;
import static org.sonar.server.platform.db.migration.version.v102.CreateGithubOrganizationsGroupsTable.ORGANIZATION_COLUMN_NAME;
import static org.sonar.server.platform.db.migration.version.v102.CreateGithubOrganizationsGroupsTable.TABLE_NAME;

public class CreateGithubOrganizationsGroupsTableTest {
  @Rule
  public final CoreDbTester db = CoreDbTester.createEmpty();

  private final DdlChange createGithubOrganizationsGroupsTable = new CreateGithubOrganizationsGroupsTable(db.database());

  @Test
  public void migration_should_create_a_table() throws SQLException {
    db.assertTableDoesNotExist(TABLE_NAME);

    createGithubOrganizationsGroupsTable.execute();

    db.assertTableExists(TABLE_NAME);
    db.assertColumnDefinition(TABLE_NAME, GROUP_UUID_COLUMN_NAME, Types.VARCHAR, UUID_SIZE, false);
    db.assertColumnDefinition(TABLE_NAME, ORGANIZATION_COLUMN_NAME, Types.VARCHAR, 100, false);
    db.assertPrimaryKey(TABLE_NAME, "pk_github_orgs_groups", GROUP_UUID_COLUMN_NAME);
  }

  @Test
  public void migration_should_be_reentrant() throws SQLException {
    db.assertTableDoesNotExist(TABLE_NAME);

    createGithubOrganizationsGroupsTable.execute();
    // re-entrant
    createGithubOrganizationsGroupsTable.execute();

    db.assertTableExists(TABLE_NAME);
  }
}
