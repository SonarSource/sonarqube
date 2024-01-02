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
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;
import org.junit.Rule;
import org.junit.Test;
import org.sonar.core.util.UuidFactory;
import org.sonar.core.util.UuidFactoryFast;
import org.sonar.db.CoreDbTester;
import org.sonar.server.platform.db.migration.step.DataChange;

import static org.assertj.core.api.Assertions.assertThat;

public class MigrateBitbucketOrgPluginSettingsToBuiltInSettingsTest {

  private final UuidFactory uuidFactory = UuidFactoryFast.getInstance();

  @Rule
  public CoreDbTester db = CoreDbTester.createForSchema(MigrateBitbucketOrgPluginSettingsToBuiltInSettingsTest.class, "schema.sql");

  private final DataChange underTest = new MigrateBitbucketOrgPluginSettingsToBuiltInSettings(db.database());

  @Test
  public void migration_populate_new_property_based_on_plugin_property() throws SQLException {
    insertProperty("sonar.auth.bitbucket.teams", "restriction-value");
    insertProperty("another.property.not.impacted.by.migration", "some value");

    underTest.execute();

    assertPropertyMigratedCorrectly();
  }

  @Test
  public void migration_dont_fail_if_plugin_property_is_missing() throws SQLException {
    underTest.execute();

    String selectSql = "select count(*) as COUNT from properties where PROP_KEY='sonar.auth.bitbucket.workspaces'";
    assertThat(db.select(selectSql).stream().map(row -> row.get("COUNT")).collect(Collectors.toList()))
      .containsExactlyInAnyOrder(0L);
  }

  @Test
  public void migration_should_be_reentrant() throws SQLException {
    insertProperty("sonar.auth.bitbucket.teams", "restriction-value");
    insertProperty("another.property.not.impacted.by.migration", "some value");

    underTest.execute();
    // re-entrant
    underTest.execute();

    assertPropertyMigratedCorrectly();
  }

  private void assertPropertyMigratedCorrectly() {
    String selectSql = "select TEXT_VALUE from properties where PROP_KEY='sonar.auth.bitbucket.workspaces'";
    assertThat(db.select(selectSql).stream().map(row -> row.get("TEXT_VALUE")).collect(Collectors.toList()))
      .containsExactlyInAnyOrder("restriction-value");
  }

  private void insertProperty(String key, String value) {
    Map<String, Object> map = new HashMap<>();
    map.put("UUID", uuidFactory.create());
    map.put("PROP_KEY", key);
    map.put("IS_EMPTY", false);
    map.put("TEXT_VALUE", value);
    map.put("CREATED_AT", System.currentTimeMillis());
    db.executeInsert("properties", map);
  }
}
