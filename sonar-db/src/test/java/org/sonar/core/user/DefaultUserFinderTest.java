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
package org.sonar.core.user;

import java.util.Arrays;
import java.util.Collection;
import org.junit.Test;
import org.sonar.api.user.User;
import org.sonar.api.user.UserQuery;
import org.sonar.db.user.UserDao;
import org.sonar.db.user.UserDto;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class DefaultUserFinderTest {
  UserDao dao = mock(UserDao.class);
  DefaultUserFinder finder = new DefaultUserFinder(dao);

  @Test
  public void findByLogin() {
    UserDto dto = new UserDto().setLogin("david").setName("David").setEmail("dav@id.com");
    when(dao.selectActiveUserByLogin("david")).thenReturn(dto);

    assertThat(finder.findByLogin("david").name()).isEqualTo("David");
  }

  @Test
  public void findByLogins() {
    UserDto david = new UserDto().setLogin("david").setName("David").setEmail("dav@id.com");
    UserDto john = new UserDto().setLogin("john").setName("John").setEmail("jo@hn.com");
    when(dao.selectByLogins(Arrays.asList("david", "john"))).thenReturn(Arrays.asList(david, john));

    Collection<User> users = finder.findByLogins(Arrays.asList("david", "john"));
    assertThat(users).hasSize(2);
    for (User user : users) {
      assertThat(user.login()).isIn("david", "john");
    }
  }

  @Test
  public void findByQuery() {
    UserQuery query = UserQuery.builder().logins("simon").build();
    finder.find(query);
    verify(dao).selectUsers(query);
  }
}
