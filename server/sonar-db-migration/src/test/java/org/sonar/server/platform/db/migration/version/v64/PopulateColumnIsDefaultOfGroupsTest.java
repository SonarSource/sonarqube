/*
 * SonarQube
 * Copyright (C) 2009-2017 SonarSource SA
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
package org.sonar.server.platform.db.migration.version.v64;

import java.sql.SQLException;
import java.util.Date;
import java.util.List;
import javax.annotation.Nullable;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.sonar.api.utils.System2;
import org.sonar.core.util.stream.Collectors;
import org.sonar.db.CoreDbTester;

import static java.lang.String.format;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class PopulateColumnIsDefaultOfGroupsTest {

  private static final long NOW = 5000_000_000_000L;
  private static final long PAST = 1000_000_000_000L;

  @Rule
  public ExpectedException expectedException = ExpectedException.none();

  @Rule
  public CoreDbTester dbTester = CoreDbTester.createForSchema(PopulateColumnIsDefaultOfGroupsTest.class, "initial.sql");

  private System2 system2 = mock(System2.class);

  private PopulateColumnIsDefaultOfGroups underTest = new PopulateColumnIsDefaultOfGroups(dbTester.database(), system2);

  @Before
  public void setUp() throws Exception {
    when(system2.now()).thenReturn(NOW);
  }

  @Test
  public void set_default_group_when_defined_in_setting() throws SQLException {
    long defaultGroupId = insertGroup("default-group", null, PAST);
    insertDefaultGroupProperty(defaultGroupId);
    insertGroup("another-group", null, PAST);

    underTest.execute();

    assertThat(selectGroups()).extracting(Group::getName, Group::isDefault, Group::getUpdatedAt)
      .containsOnly(tuple("default-group", true, NOW), tuple("another-group", false, NOW));
  }

  @Test
  public void set_default_group_when_default_sonar_users_is_used() throws SQLException {
    insertGroup("sonar-users", null, PAST);

    underTest.execute();

    assertThat(selectGroups()).extracting(Group::getName, Group::isDefault, Group::getUpdatedAt)
      .containsOnly(tuple("sonar-users", true, NOW));
  }

  @Test
  public void does_not_update_already_migrated_data() throws Exception {
    insertGroup("sonar-users", true, PAST);
    insertGroup("another-group", false, PAST);

    underTest.execute();

    assertThat(selectGroups()).extracting(Group::getName, Group::isDefault, Group::getUpdatedAt)
      .containsOnly(tuple("sonar-users", true, PAST), tuple("another-group", false, PAST));
  }

  @Test
  public void migration_is_reentrant() throws SQLException {
    long defaultGroupId = insertGroup("default-group", null, PAST);
    insertDefaultGroupProperty(defaultGroupId);
    insertGroup("another-group", null, PAST);

    underTest.execute();
    assertThat(selectGroups()).extracting(Group::getName, Group::isDefault, Group::getUpdatedAt)
      .containsOnly(tuple("default-group", true, NOW), tuple("another-group", false, NOW));

    underTest.execute();
    assertThat(selectGroups()).extracting(Group::getName, Group::isDefault, Group::getUpdatedAt)
      .containsOnly(tuple("default-group", true, NOW), tuple("another-group", false, NOW));
  }

  @Test
  public void fail_when_no_default_group_in_setting_and_sonar_users_does_not_exist() throws Exception {
    expectedException.expect(IllegalStateException.class);
    expectedException.expectMessage("The default group 'sonar-users' doesn't exist. Please create it and restart the migration");

    underTest.execute();
  }

  @Test
  public void fail_when_default_group_defined_is_setting_does_not_exist() throws Exception {
    insertDefaultGroupProperty(123L);

    expectedException.expect(IllegalStateException.class);
    expectedException.expectMessage("The default group defined in setting 'sonar.defaultGroup' doesn't exist. Please set this setting to a valid group name and restart the migration");

    underTest.execute();
  }

  @Test
  public void fail_when_default_group_setting_is_empty() throws Exception {
    insertDefaultGroupProperty(null);

    expectedException.expect(IllegalStateException.class);
    expectedException.expectMessage("The default group 'sonar-users' doesn't exist. Please create it and restart the migration");

    underTest.execute();
  }

  private void insertDefaultGroupProperty(@Nullable Long value) {
    dbTester.executeInsert(
      "PROPERTIES",
      "PROP_KEY", "sonar.defaultGroup",
      "TEXT_VALUE", value,
      "IS_EMPTY", Boolean.toString(value == null),
      "CREATED_AT", "1000");
  }

  private long insertGroup(String name, @Nullable Boolean isDefault, long updatedAt) {
    dbTester.executeInsert(
      "GROUPS",
      "NAME", name,
      "ORGANIZATION_UUID", "ORGANIZATION_UUID",
      "IS_DEFAULT", isDefault == null ? null : Boolean.toString(isDefault),
      "UPDATED_AT", new Date(updatedAt));
    return (Long) dbTester.selectFirst(format("select id from groups where name='%s'", name)).get("ID");
  }

  private List<Group> selectGroups() {
    return dbTester.select("select name,is_default,updated_at from groups").stream()
      .map(map -> new Group((String) map.get("NAME"), (Boolean) map.get("IS_DEFAULT"), (Long) map.get("UPDATED_AT")))
      .collect(Collectors.toList());
  }

  private static class Group {
    private final String name;
    private final boolean isDefault;
    private final long updatedAt;

    Group(String name, boolean isDefault, long updatedAt) {
      this.name = name;
      this.isDefault = isDefault;
      this.updatedAt = updatedAt;
    }

    String getName() {
      return name;
    }

    boolean isDefault() {
      return isDefault;
    }

    long getUpdatedAt() {
      return updatedAt;
    }
  }

}
