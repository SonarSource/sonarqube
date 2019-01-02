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
package org.sonar.server.platform.db.migration.version.v72;

import com.google.common.collect.ImmutableMap;
import java.sql.SQLException;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.sonar.db.CoreDbTester;

import static org.assertj.core.api.Assertions.assertThat;

public class PurgeDuplicateRulesParametersTest {

  @Rule
  public final CoreDbTester db = CoreDbTester.createForSchema(PurgeDuplicateRulesParametersTest.class, "rules_parameters_etc.sql");

  @Rule
  public ExpectedException expectedException = ExpectedException.none();

  private PurgeDuplicateRulesParameters underTest = new PurgeDuplicateRulesParameters(db.database());

  @Test
  public void migration_deletes_duplicates() throws SQLException {
    ImmutableMap<String, Object> dup = ImmutableMap.<String, Object>builder()
      .put("rule_id", 10)
      .put("name", "foo")
      .put("param_type", "INTEGER")
      .build();
    db.executeInsert("rules_parameters", dup);
    Object id1 = lastId();

    db.executeInsert("rules_parameters", dup);
    Object id2 = lastId();

    db.executeInsert("rules_parameters", ImmutableMap.<String, Object>builder()
      .put("rule_id", 20)
      .put("name", "bar")
      .put("param_type", "INTEGER")
      .build());
    Object id3 = lastId();

    db.executeInsert("active_rule_parameters", ImmutableMap.<String, Object>builder()
      .put("active_rule_id", 1)
      .put("rules_parameter_id", id1)
      .put("rules_parameter_key", "foo")
      .build());
    db.executeInsert("active_rule_parameters", ImmutableMap.<String, Object>builder()
      .put("active_rule_id", 2)
      .put("rules_parameter_id", id2)
      .put("rules_parameter_key", "foo")
      .build());
    db.executeInsert("active_rule_parameters", ImmutableMap.<String, Object>builder()
      .put("active_rule_id", 3)
      .put("rules_parameter_id", id3)
      .put("rules_parameter_key", "bar")
      .build());

    assertThat(db.countRowsOfTable("rules_parameters")).isEqualTo(3);
    assertThat(db.countRowsOfTable("active_rule_parameters")).isEqualTo(3);

    underTest.execute();

    assertThat(db.countRowsOfTable("rules_parameters")).isEqualTo(2);
    assertThat(db.countRowsOfTable("active_rule_parameters")).isEqualTo(2);
    assertThat(db.countSql("select count(*) from rules_parameters where id = " + id2)).isEqualTo(0);
    assertThat(db.countSql("select count(*) from active_rule_parameters where rules_parameter_id = " + id2)).isEqualTo(0);
  }

  private long lastId() {
    return (Long) db.selectFirst("select max(id) id from rules_parameters").get("ID");
  }

}
