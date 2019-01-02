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

public class PopulateIsAdHocOnRulesTest {

  private final static long PAST = 10_000_000_000L;
  private final static long NOW = 50_000_000_000L;

  @Rule
  public ExpectedException expectedException = ExpectedException.none();
  @Rule
  public CoreDbTester db = CoreDbTester.createForSchema(PopulateIsAdHocOnRulesTest.class, "rules.sql");

  private System2 system2 = new TestSystem2().setNow(NOW);

  private PopulateIsAdHocOnRules underTest = new PopulateIsAdHocOnRules(db.database(), system2);

  @Test
  public void set_is_ad_hoc_to_true_on_external_rules() throws SQLException {
    insertRule(1, true, null);
    insertRule(2, true, null);

    underTest.execute();

    assertRules(
      tuple(1L, true, true, NOW),
      tuple(2L, true, true, NOW));
  }

  @Test
  public void set_is_ad_hoc_to_false_on_none_external_rules() throws SQLException {
    insertRule(1, false, null);
    insertRule(2, false, null);

    underTest.execute();

    assertRules(
      tuple(1L, false, false, NOW),
      tuple(2L, false, false, NOW));
  }

  @Test
  public void does_nothing_when_is_ad_hoc_is_already_set() throws SQLException {
    insertRule(1, true, true);
    insertRule(2, false, false);

    underTest.execute();

    assertRules(
      tuple(1L, true, true, PAST),
      tuple(2L, false, false, PAST));
  }

  @Test
  public void migration_is_re_entrant() throws SQLException {
    insertRule(1, true, null);
    insertRule(2, false, null);

    underTest.execute();
    underTest.execute();

    assertRules(
      tuple(1L, true, true, NOW),
      tuple(2L, false, false, NOW));
  }

  private void assertRules(Tuple... expectedTuples) {
    assertThat(db.select("SELECT ID, IS_EXTERNAL, IS_AD_HOC, UPDATED_AT FROM RULES")
      .stream()
      .map(row -> new Tuple(row.get("ID"), row.get("IS_EXTERNAL"), row.get("IS_AD_HOC"), row.get("UPDATED_AT")))
      .collect(Collectors.toList()))
        .containsExactlyInAnyOrder(expectedTuples);
  }

  private void insertRule(int id, boolean isEternal, @Nullable Boolean isAdHoc) {
    db.executeInsert("RULES",
      "ID", id,
      "IS_EXTERNAL", isEternal,
      "IS_AD_HOC", isAdHoc,
      "PLUGIN_RULE_KEY", randomAlphanumeric(3),
      "PLUGIN_NAME", randomAlphanumeric(3),
      "SCOPE", "MAIN",
      "CREATED_AT", PAST,
      "UPDATED_AT", PAST);
  }

}
