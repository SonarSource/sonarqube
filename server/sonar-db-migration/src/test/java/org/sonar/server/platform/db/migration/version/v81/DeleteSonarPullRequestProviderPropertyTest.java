/*
 * SonarQube
 * Copyright (C) 2009-2019 SonarSource SA
 * mailto:info AT sonarsource DOT com
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */

package org.sonar.server.platform.db.migration.version.v81;

import java.sql.SQLException;
import javax.annotation.Nullable;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.sonar.api.utils.System2;
import org.sonar.db.CoreDbTester;
import org.sonar.server.platform.db.migration.step.DataChange;

import static java.util.stream.Collectors.toSet;
import static org.assertj.core.api.Assertions.assertThat;

public class DeleteSonarPullRequestProviderPropertyTest {

  @Rule
  public CoreDbTester db = CoreDbTester.createForSchema(DeleteSonarPullRequestProviderPropertyTest.class, "schema.sql");
  @Rule
  public ExpectedException expectedException = ExpectedException.none();

  private DataChange underTest = new DeleteSonarPullRequestProviderProperty(db.database());

  @Test
  public void delete_provider_property() throws SQLException {
    insertProperty("sonar.pullrequest.provider", null);
    insertProperty("sonar.pullrequest.provider", 1);
    insertProperty("sonar.pullrequest.provider", 2);

    underTest.execute();

    assertNoProperties();
  }

  @Test
  public void do_not_delete_other_property() throws SQLException {
    insertProperty("sonar.pullrequest.provider", null);
    insertProperty("sonar.other.property", null);

    underTest.execute();

    assertPropertyKeys("sonar.other.property");
  }

  @Test
  public void do_nothing_when_no_property() throws SQLException {
    underTest.execute();

    assertNoProperties();
  }

  @Test
  public void migration_is_reentrant() throws SQLException {
    insertProperty("sonar.pullrequest.provider", null);

    underTest.execute();
    underTest.execute();

    assertNoProperties();
  }

  private void assertPropertyKeys(String... expectedKeys) {
    assertThat(db.select("SELECT prop_key FROM properties")
      .stream()
      .map(map -> map.get("PROP_KEY"))
      .collect(toSet()))
        .containsExactlyInAnyOrder(expectedKeys);
  }

  private void assertNoProperties() {
    assertPropertyKeys();
  }

  private void insertProperty(String key, @Nullable Integer projectId) {
    db.executeInsert(
      "PROPERTIES",
      "PROP_KEY", key,
      "RESOURCE_ID", projectId,
      "USER_ID", null,
      "IS_EMPTY", false,
      "TEXT_VALUE", "AnyValue",
      "CLOB_VALUE", null,
      "CREATED_AT", System2.INSTANCE.now());
  }

}
