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

public class AuthorDaoTest extends AbstractDaoTestCase {

  private AuthorDao dao;

  @Before
  public void setUp() {
    dao = new AuthorDao(getMyBatis());
  }

  @Test
  public void shouldSelectByLogin() {
    setupData("shouldSelectByLogin");

    AuthorDto authorDto = dao.selectByLogin("godin");
    assertThat(authorDto.getId()).isEqualTo(1L);
    assertThat(authorDto.getPersonId()).isEqualTo(13L);
    assertThat(authorDto.getLogin()).isEqualTo("godin");

    assertThat(dao.selectByLogin("simon")).isNull();
  }

  @Test
  public void shouldInsert() {
    setupData("shouldInsert");

    AuthorDto authorDto = new AuthorDto()
      .setLogin("godin")
      .setPersonId(13L);

    dao.insert(authorDto);

    checkTables("shouldInsert", new String[]{"created_at", "updated_at"}, "authors");
  }

  @Test
  public void countDeveloperLogins() {
    setupData("countDeveloperLogins");

    assertThat(dao.countDeveloperLogins(1L)).isEqualTo(2);
    assertThat(dao.countDeveloperLogins(98765L)).isEqualTo(0);
  }

  @Test
  public void shouldPreventConcurrentInserts() {
    setupData("shouldPreventConcurrentInserts");

    // already exists in database with id 1
    AuthorDto authorDto = new AuthorDto()
      .setLogin("godin")
      .setPersonId(13L);
    dao.insert(authorDto);

    checkTables("shouldPreventConcurrentInserts", new String[]{"created_at", "updated_at"}, "authors");
    assertThat(authorDto.getId()).isEqualTo(1L);
    assertThat(authorDto.getPersonId()).isEqualTo(100L);
  }
}
