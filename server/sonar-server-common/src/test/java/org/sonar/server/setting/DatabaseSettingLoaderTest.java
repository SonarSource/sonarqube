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
package org.sonar.server.setting;

import java.util.Map;
import org.junit.Rule;
import org.junit.Test;
import org.sonar.api.utils.System2;
import org.sonar.db.DbTester;
import org.sonar.db.property.PropertyDto;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.data.MapEntry.entry;

public class DatabaseSettingLoaderTest {

  private static final String A_KEY = "a_key";

  @Rule
  public DbTester dbTester = DbTester.create(System2.INSTANCE);

  private DatabaseSettingLoader underTest = new DatabaseSettingLoader(dbTester.getDbClient());

  @Test
  public void test_load() {
    insertPropertyIntoDb(A_KEY, "foo");

    assertThat(underTest.load(A_KEY)).isEqualTo("foo");
    assertThat(underTest.load("missing")).isNull();
  }

  @Test
  public void null_value_in_db_is_considered_as_empty_string() {
    insertPropertyIntoDb(A_KEY, null);

    assertThat(underTest.load(A_KEY)).isEqualTo("");
  }

  @Test
  public void test_empty_value_in_db() {
    insertPropertyIntoDb(A_KEY, "");
    assertThat(underTest.load(A_KEY)).isEqualTo("");
  }

  @Test
  public void test_loadAll_with_no_properties() {
    Map<String, String> map = underTest.loadAll();
    assertThat(map).isEmpty();
  }

  @Test
  public void test_loadAll() {
    insertPropertyIntoDb("foo", "1");
    insertPropertyIntoDb("bar", "2");

    Map<String, String> map = underTest.loadAll();

    assertThat(map).containsOnly(entry("foo", "1"), entry("bar", "2"));
  }

  private void insertPropertyIntoDb(String key, String value) {
    dbTester.getDbClient().propertiesDao().saveProperty(new PropertyDto().setKey(key).setValue(value));
  }

}
