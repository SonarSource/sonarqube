/*
 * SonarQube
 * Copyright (C) 2009-2024 SonarSource SA
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
package org.sonar.server.platform.db.migration.version.v92;

import java.sql.SQLException;
import org.junit.Rule;
import org.junit.Test;
import org.sonar.core.util.UuidFactoryFast;
import org.sonar.db.CoreDbTester;
import org.sonar.server.platform.db.migration.step.DataChange;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;

public class DeprecateSWRecommendedQProfileTest {
  private final UuidFactoryFast uuids = UuidFactoryFast.getInstance();

  @Rule
  public CoreDbTester db = CoreDbTester.createForSchema(DeprecateSWRecommendedQProfileTest.class, "schema.sql");

  private final DataChange underTest = new DeprecateSWRecommendedQProfile(db.database());

  @Test
  public void deprecate_qps() throws SQLException {
    insertQps("Sonar way Recommended", "js", true);
    insertQps("Sonar way recommended", "ts", true);
    underTest.execute();

    assertThat(db.select("select name as \"NAME\", language as \"LANG\", is_built_in as \"BUILTIN\" from rules_profiles"))
      .extracting(m -> m.get("NAME"), m -> m.get("LANG"), m -> m.get("BUILTIN"))
      .containsOnly(
        tuple("Sonar way Recommended (deprecated)", "js", false),
        tuple("Sonar way Recommended (deprecated)", "ts", false));
  }

  @Test
  public void deprecate_qps_no_op_if_no_match() throws SQLException {
    insertQps("Sonar way Recommended", "js", false);
    insertQps("Sonar way Recommended", "java", true);
    underTest.execute();

    assertThat(db.select("select name as \"NAME\", language as \"LANG\", is_built_in as \"BUILTIN\" from rules_profiles"))
      .extracting(m -> m.get("NAME"), m -> m.get("LANG"), m -> m.get("BUILTIN"))
      .containsOnly(
        tuple("Sonar way Recommended", "js", false),
        tuple("Sonar way Recommended", "java", true));
  }

  @Test
  public void deprecate_qps_is_reentrant() throws SQLException {
    insertQps("Sonar way Recommended", "js", true);
    insertQps("Sonar way recommended", "ts", true);
    underTest.execute();

    assertThat(db.select("select name as \"NAME\", language as \"LANG\", is_built_in as \"BUILTIN\" from rules_profiles"))
      .extracting(m -> m.get("NAME"), m -> m.get("LANG"), m -> m.get("BUILTIN"))
      .containsOnly(
        tuple("Sonar way Recommended (deprecated)", "js", false),
        tuple("Sonar way Recommended (deprecated)", "ts", false));
  }

  @Test
  public void deprecate_qps_does_not_create_duplicate_names() throws SQLException {
    insertQps("Sonar way Recommended", "js", true);
    insertQps("Sonar way Recommended (deprecated)", "js", false);

    underTest.execute();

    assertThat(db.select("select name as \"NAME\", language as \"LANG\", is_built_in as \"BUILTIN\" from rules_profiles"))
      .extracting(m -> m.get("NAME"), m -> m.get("LANG"), m -> m.get("BUILTIN"))
      .containsOnly(
        tuple("Sonar way Recommended (deprecated)", "js", false),
        tuple("Sonar way Recommended", "js", false));
  }

  private void insertQps(String name, String lang, boolean builtIn) {
    db.executeInsert("rules_profiles",
      "NAME", name,
      "LANGUAGE", lang,
      "IS_BUILT_IN", builtIn,
      "RULES_UPDATED_AT", "2021-10-01",
      "CREATED_AT", "2021-10-01",
      "UPDATED_AT", "2021-10-01",
      "UUID", uuids.create());
  }

}
