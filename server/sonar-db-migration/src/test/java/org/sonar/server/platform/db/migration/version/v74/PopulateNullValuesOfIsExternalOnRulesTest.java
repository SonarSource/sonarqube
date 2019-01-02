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
package org.sonar.server.platform.db.migration.version.v74;

import java.sql.SQLException;
import java.util.stream.Collectors;
import javax.annotation.Nullable;
import org.assertj.core.groups.Tuple;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.sonar.api.utils.System2;
import org.sonar.api.utils.internal.TestSystem2;
import org.sonar.db.CoreDbTester;

import static org.apache.commons.lang.RandomStringUtils.randomAlphanumeric;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;

public class PopulateNullValuesOfIsExternalOnRulesTest {

  private final static long PAST = 10_000_000_000L;
  private final static long NOW = 50_000_000_000L;

  @Rule
  public ExpectedException expectedException = ExpectedException.none();
  @Rule
  public CoreDbTester db = CoreDbTester.createForSchema(PopulateNullValuesOfIsExternalOnRulesTest.class, "rules.sql");

  private System2 system2 = new TestSystem2().setNow(NOW);

  private PopulateNullValuesOfIsExternalOnRules underTest = new PopulateNullValuesOfIsExternalOnRules(db.database(), system2);

  @Test
  public void set_is_external_to_false() throws SQLException {
    insertRule(1, null);
    insertRule(2, null);

    underTest.execute();

    assertRules(
      tuple(1L, false, NOW),
      tuple(2L, false, NOW));
  }

  @Test
  public void does_nothing_when_is_external_is_already_set() throws SQLException {
    insertRule(1, true);
    insertRule(2, false);

    underTest.execute();

    assertRules(
      tuple(1L, true, PAST),
      tuple(2L, false, PAST));
  }

  @Test
  public void migration_is_re_entrant() throws SQLException {
    insertRule(1, null);

    underTest.execute();
    underTest.execute();

    assertRules(
      tuple(1L, false, NOW));
  }

  private void assertRules(Tuple... expectedTuples) {
    assertThat(db.select("SELECT ID, IS_EXTERNAL, UPDATED_AT FROM RULES")
      .stream()
      .map(row -> new Tuple(row.get("ID"), row.get("IS_EXTERNAL"), row.get("UPDATED_AT")))
      .collect(Collectors.toList()))
        .containsExactlyInAnyOrder(expectedTuples);
  }

  private void insertRule(int id, @Nullable Boolean isEternal) {
    db.executeInsert("RULES",
      "ID", id,
      "IS_EXTERNAL", isEternal,
      "PLUGIN_RULE_KEY", randomAlphanumeric(3),
      "PLUGIN_NAME", randomAlphanumeric(3),
      "SCOPE", "MAIN",
      "CREATED_AT", PAST,
      "UPDATED_AT", PAST);
  }

}
