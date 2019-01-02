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
package org.sonar.db.user;

import java.util.List;
import java.util.Map;
import org.junit.Rule;
import org.junit.Test;
import org.sonar.api.utils.internal.TestSystem2;
import org.sonar.db.DbTester;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.entry;

public class UserPropertiesDaoTest {

  private static final long NOW = 1_500_000_000_000L;

  private TestSystem2 system2 = new TestSystem2().setNow(NOW);

  @Rule
  public DbTester db = DbTester.create(system2);

  private UserPropertiesDao underTest = db.getDbClient().userPropertiesDao();

  @Test
  public void select_by_user() {
    UserDto user = db.users().insertUser();
    UserPropertyDto userSetting1 = db.users().insertUserSetting(user);
    UserPropertyDto userSetting2 = db.users().insertUserSetting(user);
    UserDto anotherUser = db.users().insertUser();
    UserPropertyDto userSetting3 = db.users().insertUserSetting(anotherUser);

    List<UserPropertyDto> results = underTest.selectByUser(db.getSession(), user);

    assertThat(results)
      .extracting(UserPropertyDto::getUuid)
      .containsExactlyInAnyOrder(userSetting1.getUuid(), userSetting2.getUuid())
      .doesNotContain(userSetting3.getUuid());
  }

  @Test
  public void insert() {
    UserDto user = db.users().insertUser();

    UserPropertyDto userSetting = underTest.insertOrUpdate(db.getSession(), new UserPropertyDto()
      .setUserUuid(user.getUuid())
      .setKey("a_key")
      .setValue("a_value"));

    Map<String, Object> map = db.selectFirst(db.getSession(), "select uuid as \"uuid\",\n" +
      " user_uuid as \"userUuid\",\n" +
      " kee as \"key\",\n" +
      " text_value as \"value\"," +
      " created_at as \"createdAt\",\n" +
      " updated_at as \"updatedAt\"" +
      " from user_properties");
    assertThat(map).contains(
      entry("uuid", userSetting.getUuid()),
      entry("userUuid", user.getUuid()),
      entry("key", "a_key"),
      entry("value", "a_value"),
      entry("createdAt", NOW),
      entry("updatedAt", NOW));
  }

  @Test
  public void update() {
    UserDto user = db.users().insertUser();
    UserPropertyDto userProperty = underTest.insertOrUpdate(db.getSession(), new UserPropertyDto()
      .setUserUuid(user.getUuid())
      .setKey("a_key")
      .setValue("old_value"));

    system2.setNow(2_000_000_000_000L);
    underTest.insertOrUpdate(db.getSession(), new UserPropertyDto()
      .setUserUuid(user.getUuid())
      .setKey("a_key")
      .setValue("new_value"));

    Map<String, Object> map = db.selectFirst(db.getSession(), "select uuid as \"uuid\",\n" +
      " user_uuid as \"userUuid\",\n" +
      " kee as \"key\",\n" +
      " text_value as \"value\"," +
      " created_at as \"createdAt\",\n" +
      " updated_at as \"updatedAt\"" +
      " from user_properties");
    assertThat(map).contains(
      entry("uuid", userProperty.getUuid()),
      entry("userUuid", user.getUuid()),
      entry("key", "a_key"),
      entry("value", "new_value"),
      entry("createdAt", NOW),
      entry("updatedAt", 2_000_000_000_000L));
  }

  @Test
  public void delete_by_user() {
    UserDto user = db.users().insertUser();
    db.users().insertUserSetting(user);
    db.users().insertUserSetting(user);
    UserDto anotherUser = db.users().insertUser();
    db.users().insertUserSetting(anotherUser);

    underTest.deleteByUser(db.getSession(), user);

    assertThat(underTest.selectByUser(db.getSession(), user)).isEmpty();
    assertThat(underTest.selectByUser(db.getSession(), anotherUser)).hasSize(1);
  }
}
