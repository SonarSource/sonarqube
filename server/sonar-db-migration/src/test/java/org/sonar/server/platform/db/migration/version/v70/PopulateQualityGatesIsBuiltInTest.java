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
package org.sonar.server.platform.db.migration.version.v70;

import java.sql.SQLException;
import java.util.Date;
import java.util.stream.Collectors;
import javax.annotation.Nullable;
import org.assertj.core.groups.Tuple;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.sonar.api.utils.System2;
import org.sonar.api.utils.internal.TestSystem2;
import org.sonar.db.CoreDbTester;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;

public class PopulateQualityGatesIsBuiltInTest {

  private final static long PAST = 10_000_000_000L;
  private final static long NOW = 50_000_000_000L;

  @Rule
  public ExpectedException expectedException = ExpectedException.none();

  @Rule
  public CoreDbTester db = CoreDbTester.createForSchema(PopulateQualityGatesIsBuiltInTest.class, "quality_gates.sql");

  private System2 system2 = new TestSystem2().setNow(NOW);

  private PopulateQualityGatesIsBuiltIn underTest = new PopulateQualityGatesIsBuiltIn(db.database(), system2);

  @Test
  public void has_no_effect_if_table_is_empty() throws SQLException {
    underTest.execute();

    assertThat(db.countRowsOfTable("quality_gates")).isEqualTo(0);
  }

  @Test
  public void updates_sonarqube_way_is_build_in_column_to_true() throws SQLException {
    insertQualityGate("SonarQube way", null);

    underTest.execute();

    assertQualityGates(tuple("SonarQube way", true, new Date(PAST), new Date(NOW)));
  }

  @Test
  public void updates_none_sonarqube_way_is_build_in_column_to_false() throws SQLException {
    insertQualityGate("Other 1", null);
    insertQualityGate("Other 2", null);

    underTest.execute();

    assertQualityGates(
      tuple("Other 1", false, new Date(PAST), new Date(NOW)),
      tuple("Other 2", false, new Date(PAST), new Date(NOW)));
  }

  @Test
  public void does_nothing_when_built_in_column_is_set() throws SQLException {
    insertQualityGate("SonarQube way", true);
    insertQualityGate("Other way", false);

    underTest.execute();

    assertQualityGates(
      tuple("SonarQube way", true, new Date(PAST), new Date(PAST)),
      tuple("Other way", false, new Date(PAST), new Date(PAST)));
  }

  @Test
  public void execute_is_reentreant() throws SQLException {
    insertQualityGate("SonarQube way", null);
    insertQualityGate("Other way", null);

    underTest.execute();

    underTest.execute();

    assertQualityGates(
      tuple("SonarQube way", true, new Date(PAST), new Date(NOW)),
      tuple("Other way", false, new Date(PAST), new Date(NOW)));
  }

  private void assertQualityGates(Tuple... expectedTuples) {
    assertThat(db.select("SELECT NAME, IS_BUILT_IN, CREATED_AT, UPDATED_AT FROM QUALITY_GATES")
      .stream()
      .map(map -> new Tuple(map.get("NAME"), map.get("IS_BUILT_IN"), map.get("CREATED_AT"), map.get("UPDATED_AT")))
      .collect(Collectors.toList()))
        .containsExactlyInAnyOrder(expectedTuples);
  }

  private void insertQualityGate(String name, @Nullable Boolean builtIn) {
    db.executeInsert(
      "QUALITY_GATES",
      "NAME", name,
      "IS_BUILT_IN", builtIn == null ? null : builtIn.toString(),
      "CREATED_AT", new Date(PAST),
      "UPDATED_AT", new Date(PAST));
  }

}
