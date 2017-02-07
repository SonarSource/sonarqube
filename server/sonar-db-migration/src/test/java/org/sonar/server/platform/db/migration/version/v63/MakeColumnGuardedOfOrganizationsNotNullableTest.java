package org.sonar.server.platform.db.migration.version.v63;

import java.sql.SQLException;
import java.sql.Types;
import javax.annotation.Nullable;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.sonar.api.utils.System2;
import org.sonar.db.DbTester;

public class MakeColumnGuardedOfOrganizationsNotNullableTest {
  private static final String TABLE_ORGANIZATIONS = "organizations";

  @Rule
  public DbTester dbTester = DbTester.createForSchema(System2.INSTANCE, MakeColumnGuardedOfOrganizationsNotNullableTest.class, "organizations_with_nullable_guarded.sql");
  @Rule
  public ExpectedException expectedException = ExpectedException.none();

  private MakeColumnGuardedOfOrganizationsNotNullable underTest = new MakeColumnGuardedOfOrganizationsNotNullable(dbTester.database());

  @Test
  public void migration_sets_guarded_column_not_nullable_on_empty_table() throws SQLException {
    underTest.execute();

    verifyColumnDefinition();
  }

  @Test
  public void migration_sets_guarded_column_not_nullable_on_populated_table() throws SQLException {
    insertOrganization("org_A", true);
    insertOrganization("org_B", false);

    underTest.execute();

    verifyColumnDefinition();
  }

  @Test
  public void migration_fails_if_some_row_has_a_null_guarded() throws SQLException {
    insertOrganization("org_c", null);

    expectedException.expect(IllegalStateException.class);
    expectedException.expectMessage("Fail to execute");

    underTest.execute();
  }

  private void verifyColumnDefinition() {
    dbTester.assertColumnDefinition(TABLE_ORGANIZATIONS, "guarded", Types.BOOLEAN, null, false);
  }

  private void insertOrganization(String uuid, @Nullable Boolean guarded) {
    dbTester.executeInsert(
      "ORGANIZATIONS",
      "UUID", uuid,
      "KEE", uuid,
      "NAME", uuid,
      "GUARDED", guarded == null ? null : String.valueOf(guarded),
      "CREATED_AT", "1000",
      "UPDATED_AT", "1000");
  }
}
