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
import org.assertj.core.groups.Tuple;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.sonar.api.utils.System2;
import org.sonar.api.utils.internal.TestSystem2;
import org.sonar.api.utils.log.LogTester;
import org.sonar.db.CoreDbTester;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;

public class RenameOldSonarQubeWayQualityGateTest {

  private final static long PAST = 10_000_000_000L;
  private final static long NOW = 50_000_000_000L;

  private final static String SONARQUBE_WAY_QUALITY_GATE = "SonarQube way";
  private final static String SONAR_WAY_OUTDATED_QUALITY_GATE = "Sonar way (outdated copy)";
  private final static String SONAR_WAY_QUALITY_GATE = "Sonar way";

  @Rule
  public ExpectedException expectedException = ExpectedException.none();
  @Rule
  public CoreDbTester db = CoreDbTester.createForSchema(PopulateQualityGatesIsBuiltInTest.class, "quality_gates.sql");
  @Rule
  public LogTester logTester = new LogTester();

  private System2 system2 = new TestSystem2().setNow(NOW);

  private RenameOldSonarQubeWayQualityGate underTest = new RenameOldSonarQubeWayQualityGate(db.database(), system2);

  @Test
  public void has_no_effect_if_table_is_empty() throws SQLException {
    underTest.execute();

    assertThat(db.countRowsOfTable("quality_gates")).isEqualTo(0);
  }

  @Test
  public void should_rename_SonarQubeWay_quality_gate() throws SQLException {
    insertQualityGate(SONARQUBE_WAY_QUALITY_GATE, false);

    underTest.execute();

    assertQualityGates(
      tuple(SONAR_WAY_OUTDATED_QUALITY_GATE, false, new Date(PAST), new Date(NOW))
    );
  }

  @Test
  public void should_set_builtin_to_false_when_renaming() throws SQLException {
    insertQualityGate(SONARQUBE_WAY_QUALITY_GATE, true);

    underTest.execute();

    assertQualityGates(
      tuple(SONAR_WAY_OUTDATED_QUALITY_GATE, false, new Date(PAST), new Date(NOW))
    );
  }

  @Test
  public void should_log_a_meaningful_info_if_outdated_copy_exists() {
    insertQualityGate(SONARQUBE_WAY_QUALITY_GATE, false);
    insertQualityGate(SONAR_WAY_OUTDATED_QUALITY_GATE, false);

    try {
      underTest.execute();
    } catch (Exception ex) {
      logTester.logs().contains("There is already a quality profile with name [Sonar way (outdated copy)]");
    }
  }


  @Test
  public void should_update_only_SonarQubeWay() throws SQLException {
    insertQualityGate("Whatever", true);
    insertQualityGate("Whatever2", false);
    insertQualityGate(SONAR_WAY_QUALITY_GATE, true);
    insertQualityGate(SONARQUBE_WAY_QUALITY_GATE, false);

    underTest.execute();

    assertQualityGates(
      tuple("Whatever", true, new Date(PAST), new Date(PAST)),
      tuple("Whatever2", false, new Date(PAST), new Date(PAST)),
      tuple(SONAR_WAY_QUALITY_GATE, true, new Date(PAST), new Date(PAST)),
      tuple(SONAR_WAY_OUTDATED_QUALITY_GATE, false, new Date(PAST), new Date(NOW))
    );
  }

  @Test
  public void is_reentrant() throws SQLException {
    insertQualityGate(SONARQUBE_WAY_QUALITY_GATE, false);

    underTest.execute();
    underTest.execute();

    assertQualityGates(
      tuple(SONAR_WAY_OUTDATED_QUALITY_GATE, false, new Date(PAST), new Date(NOW))
    );
  }

  private void assertQualityGates(Tuple... expectedTuples) {
    assertThat(db.select("SELECT NAME, IS_BUILT_IN, CREATED_AT, UPDATED_AT FROM QUALITY_GATES")
      .stream()
      .map(map -> new Tuple(map.get("NAME"), map.get("IS_BUILT_IN"), map.get("CREATED_AT"), map.get("UPDATED_AT")))
      .collect(Collectors.toList()))
      .containsExactlyInAnyOrder(expectedTuples);
  }


  private void insertQualityGate(String name, boolean builtIn) {
    db.executeInsert(
      "QUALITY_GATES",
      "NAME", name,
      "IS_BUILT_IN", String.valueOf(builtIn),
      "CREATED_AT", new Date(PAST),
      "UPDATED_AT", new Date(PAST));
  }
}
