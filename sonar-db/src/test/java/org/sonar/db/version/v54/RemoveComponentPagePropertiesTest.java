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
package org.sonar.db.version.v54;

import javax.annotation.Nullable;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.sonar.api.utils.System2;
import org.sonar.db.DbTester;

import static java.lang.String.format;
import static org.assertj.core.api.Assertions.assertThat;

public class RemoveComponentPagePropertiesTest {

  private static final String TABLE_PROPERTIES = "properties";
  private static final String EXPECTED_PREFIX = "sonar.core.projectsdashboard.";

  @Rule
  public DbTester db = DbTester.createForSchema(System2.INSTANCE, RemoveComponentPagePropertiesTest.class, "schema.sql");

  RemoveComponentPageProperties underTest;

  @Before
  public void setUp() {
    db.executeUpdateSql("truncate table " + TABLE_PROPERTIES);

    underTest = new RemoveComponentPageProperties(db.database());
  }

  @Test
  public void migrate_empty_db() throws Exception {
    underTest.execute();

    assertThat(db.countRowsOfTable(TABLE_PROPERTIES)).isEqualTo(0);
  }

  @Test
  public void do_not_remove_property_sonar_core_projectsdashboard() throws Exception {
    String missingLastDot = EXPECTED_PREFIX.substring(0, EXPECTED_PREFIX.length() - 2);
    insertProperty(missingLastDot, null, null);

    underTest.execute();

    assertPropertiesContainsRow(missingLastDot, null, null);
    assertThat(db.countRowsOfTable(TABLE_PROPERTIES)).isEqualTo(1);
  }

  @Test
  public void remove_any_property_starting_with_sonar_core_projectsdashboard_and_a_dot() throws Exception {
    insertProperty(EXPECTED_PREFIX, null, null);
    insertProperty(EXPECTED_PREFIX + "toto", null, null);
    insertProperty(EXPECTED_PREFIX + "chum", null, null);

    underTest.execute();

    assertThat(db.countRowsOfTable(TABLE_PROPERTIES)).isEqualTo(0);
  }

  @Test
  public void remove_any_property_starting_with_sonar_core_projectsdashboard_and_a_dot_for_resource_and_or_user() throws Exception {
    String key = EXPECTED_PREFIX + "toto";
    insertProperty(key, null, null);
    insertProperty(key, 984, null);
    insertProperty(key, null, 99);
    insertProperty(key, 66, 77);

    String otherKey = "other key";
    insertProperty(otherKey, null, null);
    insertProperty(otherKey, 984, null);
    insertProperty(otherKey, null, 99);
    insertProperty(otherKey, 66, 77);

    underTest.execute();

    assertPropertiesContainsRow(otherKey, null, null);
    assertPropertiesContainsRow(otherKey, 984, null);
    assertPropertiesContainsRow(otherKey, null, 99);
    assertPropertiesContainsRow(otherKey, 66, 77);
    assertThat(db.countRowsOfTable(TABLE_PROPERTIES)).isEqualTo(4);
  }

  @Test
  public void do_not_remove_property_other_than_starting_with_sonar_core_projectsdashboard_and_a_dot() throws Exception {
    insertProperty("toto.", null, null);
    insertProperty("pouf", null, null);

    underTest.execute();

    assertPropertiesContainsRow("toto.", null, null);
    assertPropertiesContainsRow("pouf", null, null);
    assertThat(db.countRowsOfTable(TABLE_PROPERTIES)).isEqualTo(2);
  }

  private void assertPropertiesContainsRow(String key, @Nullable Integer resourceId, @Nullable Integer userId) {
    assertThat(db.countSql(propertiesRowSql(key, resourceId, userId))).isEqualTo(1);
  }

  private static String propertiesRowSql(String key, @Nullable Integer resourceId, @Nullable Integer userId) {
    return format(
      "select count(1) from properties where prop_key='%s' and resource_id %s and user_id %s and text_value = '%s'",
      key,
      whereClauseOfInteger(resourceId),
      whereClauseOfInteger(userId),
      generatedValueOfProperty(key));
  }

  private static String whereClauseOfInteger(@Nullable Integer id) {
    if (id == null) {
      return "is null";
    }
    return "=" + id;
  }

  private void insertProperty(String key, @Nullable Integer resourceId, @Nullable Integer userId) {
    db.executeUpdateSql(format(
      "insert into properties (prop_key,resource_id,text_value,user_id) values ('%s',%s,'%s',%s)",
      key, nullIntegerValue(resourceId), generatedValueOfProperty(key), nullIntegerValue(userId))
      );
  }

  private static String generatedValueOfProperty(String key) {
    return key + " value";
  }

  private static String nullIntegerValue(@Nullable Integer id) {
    if (id == null) {
      return "null";
    }
    return String.valueOf(id);
  }

}
