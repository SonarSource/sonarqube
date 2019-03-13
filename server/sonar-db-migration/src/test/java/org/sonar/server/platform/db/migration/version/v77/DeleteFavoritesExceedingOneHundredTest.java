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
package org.sonar.server.platform.db.migration.version.v77;

import java.sql.SQLException;
import org.junit.Rule;
import org.junit.Test;
import org.sonar.db.CoreDbTester;

import static java.util.stream.IntStream.rangeClosed;
import static org.apache.commons.lang.math.RandomUtils.nextInt;
import static org.assertj.core.api.Assertions.assertThat;

public class DeleteFavoritesExceedingOneHundredTest {

  private static final String TABLE = "properties";
  private static final String FAVOURITE_PROPERTY = "favourite";

  @Rule
  public CoreDbTester db = CoreDbTester.createForSchema(DeleteFavoritesExceedingOneHundredTest.class, "schema.sql");

  private DeleteFavoritesExceedingOneHundred underTest = new DeleteFavoritesExceedingOneHundred(db.database());

  @Test
  public void delete_favorites_when_user_has_more_than_100() throws SQLException {
    int user1Id = nextInt();
    insertProperties(user1Id, "TRK", 110);
    int user2Id = nextInt();
    insertProperties(user2Id, "TRK", 130);
    int user3Id = nextInt();
    insertProperties(user3Id, "TRK", 90);

    underTest.execute();

    assertFavorites(user1Id, 100);
    assertFavorites(user2Id, 100);
    assertFavorites(user3Id, 90);
  }

  @Test
  public void keep_in_priority_projects_over_files() throws SQLException {
    int userId = nextInt();
    insertProperties(userId, "TRK", 90);
    insertProperties(userId, "FIL", 20);
    underTest.execute();

    assertFavorites(userId, "TRK", 90);
    assertFavorites(userId, "FIL", 10);
  }

  @Test
  public void keep_in_priority_views_over_sub_views() throws SQLException {
    int userId = nextInt();
    insertProperties(userId, "VW", 90);
    insertProperties(userId, "SVW", 20);
    underTest.execute();

    assertFavorites(userId, "VW", 90);
    assertFavorites(userId, "SVW", 10);
  }

  @Test
  public void keep_in_priority_apps_over_sub_views() throws SQLException {
    int userId = nextInt();
    insertProperties(userId, "APP", 90);
    insertProperties(userId, "SVW", 20);
    underTest.execute();

    assertFavorites(userId, "APP", 90);
    assertFavorites(userId, "SVW", 10);
  }

  @Test
  public void keep_in_priority_enabled_projects() throws SQLException {
    int userId = nextInt();
    insertProperties(userId, "TRK", 90);
    rangeClosed(1, 20).forEach(i -> {
      int componentId = insertDisabledComponent("TRK");
      insertProperty(FAVOURITE_PROPERTY, userId, componentId);
    });

    underTest.execute();

    assertFavorites(userId, "TRK", true, 90);
    assertFavorites(userId, "TRK", false, 10);
  }

  @Test
  public void ignore_non_favorite_properties() throws SQLException {
    int userId = nextInt();
    rangeClosed(1, 130).forEach(i -> {
      int componentId = insertComponent("TRK");
      insertProperty("other", userId, componentId);
    });

    underTest.execute();

    assertProperties("other", userId, 130);
  }

  private void assertFavorites(int userId, int expectedSize) {
    assertProperties(FAVOURITE_PROPERTY, userId, expectedSize);
  }

  private void assertFavorites(int userId, String qualifier, int expectedSize) {
    assertThat(db.countSql("SELECT count(prop.id) FROM properties prop " +
      "INNER JOIN projects p ON p.id=prop.resource_id AND p.qualifier='" + qualifier + "'" +
      "WHERE prop.user_id=" + userId + " AND prop.prop_key='" + FAVOURITE_PROPERTY + "'"))
        .isEqualTo(expectedSize);
  }

  private void assertFavorites(int userId, String qualifier, boolean enabled, int expectedSize) {
    assertThat(db.countSql("SELECT count(prop.id) FROM properties prop " +
      "INNER JOIN projects p ON p.id=prop.resource_id AND p.qualifier='" + qualifier + "' AND p.enabled='" + enabled + "'" +
      "WHERE prop.user_id=" + userId + " AND prop.prop_key='" + FAVOURITE_PROPERTY + "'"))
        .isEqualTo(expectedSize);
  }

  private void assertProperties(String propertyKey, int userId, int expectedSize) {
    assertThat(db.countSql("SELECT count(ID) FROM properties WHERE user_id=" + userId + " AND prop_key='" + propertyKey + "'")).isEqualTo(expectedSize);
  }

  private void insertProperties(int userId, String qualifier, int nbProperties) {
    rangeClosed(1, nbProperties).forEach(i -> {
      int componentId = insertComponent(qualifier);
      insertProperty(FAVOURITE_PROPERTY, userId, componentId);
    });
  }

  private void insertProperty(String key, int userId, int componentId) {
    db.executeInsert(
      TABLE,
      "PROP_KEY", key,
      "USER_ID", userId,
      "RESOURCE_ID", componentId,
      "IS_EMPTY", true,
      "CREATED_AT", 123456);
  }

  private int insertComponent(String qualifier) {
    return insertCommonComponent(qualifier, true);
  }

  private int insertDisabledComponent(String qualifier) {
    return insertCommonComponent(qualifier, false);
  }

  private int insertCommonComponent(String qualifier, boolean enabled) {
    int id = nextInt();
    String uuid = "uuid_" + id;
    db.executeInsert(
      "projects",
      "ID", id,
      "UUID", uuid,
      "ORGANIZATION_UUID", "org_" + id,
      "UUID_PATH", "path_" + id,
      "ROOT_UUID", "root_" + id,
      "PROJECT_UUID", "project_" + id,
      "QUALIFIER", qualifier,
      "ENABLED", enabled,
      "PRIVATE", false);
    return id;
  }

}
