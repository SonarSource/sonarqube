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

package org.sonar.server.favorite;

import org.junit.Rule;
import org.junit.Test;
import org.sonar.api.utils.System2;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.db.DbTester;
import org.sonar.db.property.PropertyQuery;
import org.sonar.server.tester.UserSessionRule;

import static org.assertj.core.api.Assertions.assertThat;

public class FavoriteUpdaterTest {
  private static final long COMPONENT_ID = 23L;
  private static final long USER_ID = 42L;

  @Rule
  public UserSessionRule userSession = UserSessionRule.standalone().login().setUserId((int) USER_ID);

  @Rule
  public DbTester db = DbTester.create(System2.INSTANCE);
  private DbClient dbClient = db.getDbClient();
  private DbSession dbSession = db.getSession();

  private FavoriteUpdater underTest = new FavoriteUpdater(dbClient, userSession);

  @Test
  public void put_favorite() {
    assertNoFavorite();

    underTest.put(dbSession, COMPONENT_ID);

    assertFavorite();
  }

  @Test
  public void do_nothing_when_not_logged_in() {
    userSession.anonymous();

    underTest.put(dbSession, COMPONENT_ID);

    assertNoFavorite();
  }

  @Test
  public void put_existing_favorite() {
    underTest.put(dbSession, COMPONENT_ID);
    assertFavorite();

    underTest.put(dbSession, COMPONENT_ID);

    assertFavorite();
  }

  private void assertFavorite() {
    assertThat(dbClient.propertiesDao().selectByQuery(PropertyQuery.builder()
      .setUserId((int) USER_ID)
      .setComponentId(COMPONENT_ID)
      .build(), dbSession)).hasSize(1);
  }

  private void assertNoFavorite() {
    assertThat(dbClient.propertiesDao().selectByQuery(PropertyQuery.builder()
      .setUserId((int) USER_ID)
      .setComponentId(COMPONENT_ID)
      .build(), dbSession)).isEmpty();
  }
}
