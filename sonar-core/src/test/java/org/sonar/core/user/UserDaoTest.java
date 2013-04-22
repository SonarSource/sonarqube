/*
 * SonarQube, open source software quality management tool.
 * Copyright (C) 2008-2013 SonarSource
 * mailto:contact AT sonarsource DOT com
 *
 * SonarQube is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * SonarQube is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */
package org.sonar.core.user;

import org.junit.Before;
import org.junit.Test;
import org.sonar.core.persistence.AbstractDaoTestCase;

import static org.fest.assertions.Assertions.assertThat;


public class UserDaoTest extends AbstractDaoTestCase {

  private UserDao dao;

  @Before
  public void setUp() {
    dao = new UserDao(getMyBatis());
  }

  @Test
  public void selectUserByLogin_ignore_same_disabled_login() {
    setupData("selectUserByLogin");

    UserDto user = dao.selectUserByLogin("marius");
    assertThat(user).isNotNull();
    assertThat(user.getId()).isEqualTo(101L);
    assertThat(user.getLogin()).isEqualTo("marius");
    assertThat(user.getName()).isEqualTo("Marius");
    assertThat(user.getEmail()).isEqualTo("marius@lesbronzes.fr");
    assertThat(user.getCreatedAt()).isNotNull();
    assertThat(user.getUpdatedAt()).isNotNull();
    assertThat(user.isEnabled()).isTrue();
  }

  @Test
  public void selectUserByLogin_ignore_disabled() {
    setupData("selectUserByLogin");

    UserDto user = dao.selectUserByLogin("disabled");
    assertThat(user).isNull();
  }

  @Test
  public void selectUserByLogin_not_found() {
    setupData("selectUserByLogin");

    UserDto user = dao.selectUserByLogin("not_found");
    assertThat(user).isNull();
  }

  @Test
  public void selectGroupByName() {
    setupData("selectGroupByName");

    GroupDto group = dao.selectGroupByName("sonar-users");
    assertThat(group).isNotNull();
    assertThat(group.getId()).isEqualTo(1L);
    assertThat(group.getName()).isEqualTo("sonar-users");
    assertThat(group.getDescription()).isEqualTo("Sonar Users");
    assertThat(group.getCreatedAt()).isNotNull();
    assertThat(group.getUpdatedAt()).isNotNull();
  }

  @Test
  public void selectGroupByName_not_found() {
    setupData("selectGroupByName");

    GroupDto group = dao.selectGroupByName("not-found");
    assertThat(group).isNull();
  }
}
