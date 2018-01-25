package org.sonar.server.platform.db.migration.version.v71;

import java.sql.SQLException;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.sonar.db.CoreDbTester;

import static java.sql.Types.VARCHAR;

public class AddRuleScopeTest {
  @Rule
  public final CoreDbTester dbTester = CoreDbTester.createForSchema(AddRuleScopeTest.class, "rules.sql");

  @Rule
  public ExpectedException expectedException = ExpectedException.none();

  private AddRuleScope underTest = new AddRuleScope(dbTester.database());

  @Test
  public void column_is_added_to_table() throws SQLException {
    underTest.execute();

    dbTester.assertColumnDefinition("rules", "scope", VARCHAR, null, true);
  }

  @Test
  public void migration_is_not_reentrant() throws SQLException {
    underTest.execute();

    expectedException.expect(IllegalStateException.class);

    underTest.execute();
  }
}
