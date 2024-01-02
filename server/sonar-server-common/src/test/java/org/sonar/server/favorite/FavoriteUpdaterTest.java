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
package org.sonar.server.favorite;

import java.util.stream.IntStream;
import org.junit.Rule;
import org.junit.Test;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.db.DbTester;
import org.sonar.db.component.ComponentDto;
import org.sonar.db.property.PropertyQuery;
import org.sonar.db.user.UserDto;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

public class FavoriteUpdaterTest {

  @Rule
  public DbTester db = DbTester.create();

  private DbClient dbClient = db.getDbClient();
  private DbSession dbSession = db.getSession();

  private FavoriteUpdater underTest = new FavoriteUpdater(dbClient);

  @Test
  public void put_favorite() {
    ComponentDto project = db.components().insertPrivateProject();
    UserDto user = db.users().insertUser();
    assertNoFavorite(project, user);

    underTest.add(dbSession, project, user.getUuid(), user.getLogin(), true);

    assertFavorite(project, user);
  }

  @Test
  public void do_nothing_when_no_user() {
    ComponentDto project = db.components().insertPrivateProject();

    underTest.add(dbSession, project, null, null,true);

    assertThat(dbClient.propertiesDao().selectByQuery(PropertyQuery.builder()
      .setComponentUuid(project.uuid())
      .build(), dbSession)).isEmpty();
  }

  @Test
  public void do_not_add_favorite_when_already_100_favorite_projects() {
    UserDto user = db.users().insertUser();
    IntStream.rangeClosed(1, 100).forEach(i -> db.favorites().add(db.components().insertPrivateProject(), user.getUuid(), user.getName()));
    assertThat(dbClient.propertiesDao().selectByQuery(PropertyQuery.builder()
      .setUserUuid(user.getUuid())
      .build(), dbSession)).hasSize(100);
    ComponentDto project = db.components().insertPrivateProject();

    underTest.add(dbSession, project, user.getUuid(), user.getLogin(), false);

    assertThat(dbClient.propertiesDao().selectByQuery(PropertyQuery.builder()
      .setUserUuid(user.getUuid())
      .build(), dbSession)).hasSize(100);
  }

  @Test
  public void do_not_add_favorite_when_already_100_favorite_portfolios() {
    UserDto user = db.users().insertUser();
    IntStream.rangeClosed(1, 100).forEach(i -> db.favorites().add(db.components().insertPrivateProject(),
      user.getUuid(), user.getLogin()));
    assertThat(dbClient.propertiesDao().selectByQuery(PropertyQuery.builder()
      .setUserUuid(user.getUuid())
      .build(), dbSession)).hasSize(100);
    ComponentDto project = db.components().insertPrivateProject();

    underTest.add(dbSession, project, user.getUuid(), user.getLogin(), false);

    assertThat(dbClient.propertiesDao().selectByQuery(PropertyQuery.builder()
      .setUserUuid(user.getUuid())
      .build(), dbSession)).hasSize(100);
  }

  @Test
  public void fail_when_more_than_100_projects_favorites() {
    UserDto user = db.users().insertUser();
    IntStream.rangeClosed(1, 100).forEach(i -> db.favorites().add(db.components().insertPrivateProject(),
      user.getUuid(), user.getLogin()));
    ComponentDto project = db.components().insertPrivateProject();

    assertThatThrownBy(() -> underTest.add(dbSession, project, user.getUuid(), user.getLogin(), true))
      .isInstanceOf(IllegalArgumentException.class)
      .hasMessage("You cannot have more than 100 favorites on components with qualifier 'TRK'");
  }

  @Test
  public void fail_when_adding_existing_favorite() {
    ComponentDto project = db.components().insertPrivateProject();
    UserDto user = db.users().insertUser();
    underTest.add(dbSession, project, user.getUuid(), user.getLogin(), true);
    assertFavorite(project, user);

    assertThatThrownBy(() -> underTest.add(dbSession, project, user.getUuid(), user.getLogin(), true))
      .isInstanceOf(IllegalArgumentException.class)
      .hasMessage(String.format("Component '%s' (uuid: %s) is already a favorite", project.getKey(), project.uuid()));
  }

  private void assertFavorite(ComponentDto project, UserDto user) {
    assertThat(dbClient.propertiesDao().selectByQuery(PropertyQuery.builder()
      .setUserUuid(user.getUuid())
      .setComponentUuid(project.uuid())
      .build(), dbSession)).hasSize(1);
  }

  private void assertNoFavorite(ComponentDto project, UserDto user) {
    assertThat(dbClient.propertiesDao().selectByQuery(PropertyQuery.builder()
      .setUserUuid(user.getUuid())
      .setComponentUuid(project.uuid())
      .build(), dbSession)).isEmpty();
  }
}
