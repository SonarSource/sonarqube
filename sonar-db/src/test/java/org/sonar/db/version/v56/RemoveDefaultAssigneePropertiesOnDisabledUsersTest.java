/*
 * SonarQube
 * Copyright (C) 2009-2016 SonarSource SA
 * mailto:contact AT sonarsource DOT com
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
package org.sonar.db.version.v56;

import com.google.common.collect.ImmutableMap;
import java.util.List;
import java.util.Map;
import org.junit.Rule;
import org.junit.Test;
import org.sonar.api.utils.System2;
import org.sonar.db.DbTester;
import org.sonar.db.version.MigrationStep;

import static org.assertj.core.api.Assertions.assertThat;

public class RemoveDefaultAssigneePropertiesOnDisabledUsersTest {

  @Rule
  public DbTester db = DbTester.createForSchema(System2.INSTANCE, RemoveDefaultAssigneePropertiesOnDisabledUsersTest.class, "schema.sql");

  private MigrationStep migration = new RemoveDefaultAssigneePropertiesOnDisabledUsers(db.database());;

  @Test
  public void delete_properties() throws Exception {
    insertUser("user1", false);
    insertUser("user2", true);

    insertProperty("sonar.issues.defaultAssigneeLogin", "user1", 10L);
    insertProperty("sonar.issues.defaultAssigneeLogin", "user1", 11L);
    insertProperty("sonar.other.property", "user1", 11L);
    insertProperty("sonar.issues.defaultAssigneeLogin", "user2", 13L);

    migration.execute();

    List<Map<String, Object>> rows = db.select("select prop_key, text_value from properties");
    assertThat(rows).containsOnly(
      ImmutableMap.<String, Object>of("PROP_KEY", "sonar.issues.defaultAssigneeLogin","TEXT_VALUE", "user2"),
      ImmutableMap.<String, Object>of("PROP_KEY", "sonar.other.property","TEXT_VALUE", "user1")
    );
  }

  private void insertUser(String login, boolean active) {
    db.executeInsert("users", ImmutableMap.of(
      "LOGIN", login,
      "ACTIVE", Boolean.toString(active)
    ));
  }

  private void insertProperty(String key, String value, long componentId) {
    db.executeInsert("properties", ImmutableMap.of(
      "PROP_KEY", key,
      "RESOURCE_ID", Long.toString(componentId),
      "TEXT_VALUE", value
    ));
  }

}
