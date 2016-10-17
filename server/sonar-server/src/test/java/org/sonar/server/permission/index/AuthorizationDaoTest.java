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
package org.sonar.server.permission.index;

import com.google.common.base.Predicate;
import com.google.common.collect.Iterables;
import java.util.Collection;
import org.junit.Rule;
import org.junit.Test;
import org.sonar.api.utils.System2;
import org.sonar.db.DbTester;

import static org.assertj.core.api.Assertions.assertThat;

public class AuthorizationDaoTest {

  @Rule
  public DbTester dbTester = DbTester.create(System2.INSTANCE);

  private AuthorizationDao dao = new AuthorizationDao();

  @Test
  public void select_all() {
    dbTester.prepareDbUnit(getClass(), "shared.xml");

    Collection<AuthorizationDao.Dto> dtos = dao.selectAfterDate(dbTester.getDbClient(), dbTester.getSession(), 0L);
    assertThat(dtos).hasSize(2);

    AuthorizationDao.Dto abc = Iterables.find(dtos, new ProjectPredicate("ABC"));
    assertThat(abc.getGroups()).containsOnly("Anyone", "devs");
    assertThat(abc.getUsers()).containsOnly("user1");
    assertThat(abc.getUpdatedAt()).isNotNull();

    AuthorizationDao.Dto def = Iterables.find(dtos, new ProjectPredicate("DEF"));
    assertThat(def.getGroups()).containsOnly("Anyone");
    assertThat(def.getUsers()).containsOnly("user1", "user2");
    assertThat(def.getUpdatedAt()).isNotNull();
  }

  @Test
  public void select_after_date() {
    dbTester.prepareDbUnit(getClass(), "shared.xml");

    Collection<AuthorizationDao.Dto> dtos = dao.selectAfterDate(dbTester.getDbClient(), dbTester.getSession(), 1500000000L);

    // only project DEF was updated in this period
    assertThat(dtos).hasSize(1);
    AuthorizationDao.Dto def = Iterables.find(dtos, new ProjectPredicate("DEF"));
    assertThat(def).isNotNull();
    assertThat(def.getGroups()).containsOnly("Anyone");
    assertThat(def.getUsers()).containsOnly("user1", "user2");
  }

  @Test
  public void no_authorization() {
    dbTester.prepareDbUnit(getClass(), "no_authorization.xml");

    Collection<AuthorizationDao.Dto> dtos = dao.selectAfterDate(dbTester.getDbClient(), dbTester.getSession(), 0L);

    assertThat(dtos).hasSize(1);
    AuthorizationDao.Dto abc = Iterables.find(dtos, new ProjectPredicate("ABC"));
    assertThat(abc.getGroups()).isEmpty();
    assertThat(abc.getUsers()).isEmpty();
    assertThat(abc.getUpdatedAt()).isNotNull();
  }

  private static class ProjectPredicate implements Predicate<AuthorizationDao.Dto> {

    private final String projectUuid;

    ProjectPredicate(String projectUuid) {
      this.projectUuid = projectUuid;
    }

    @Override
    public boolean apply(AuthorizationDao.Dto input) {
      return input.getProjectUuid().equals(projectUuid);
    }

    @Override
    public boolean equals(Object object) {
      return true;
    }
  }
}
