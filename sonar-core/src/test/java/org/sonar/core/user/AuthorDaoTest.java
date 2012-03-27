/*
 * Sonar, open source software quality management tool.
 * Copyright (C) 2008-2012 SonarSource
 * mailto:contact AT sonarsource DOT com
 *
 * Sonar is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * Sonar is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with Sonar; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02
 */
package org.sonar.core.user;

import org.junit.Before;
import org.junit.Test;
import org.sonar.core.persistence.DaoTestCase;

import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.Assert.assertThat;

public class AuthorDaoTest extends DaoTestCase {

  private AuthorDao dao;

  @Before
  public void setUp() {
    dao = new AuthorDao(getMyBatis());
  }

  @Test
  public void shouldSelect() {
    setupData("shouldSelect");

    AuthorDto authorDto = dao.select("godin");
    assertThat(authorDto.getId(), is(1));
    assertThat(authorDto.getPersonId(), is(13));
    assertThat(authorDto.getLogin(), is("godin"));

    assertThat(dao.select("simon"), is(nullValue()));
  }

  @Test
  public void shouldInsert() {
    setupData("shouldInsert");

    AuthorDto authorDto = new AuthorDto()
        .setLogin("godin")
        .setPersonId(13);

    dao.insert(authorDto);

    checkTables("shouldInsert", new String[] {"created_at", "updated_at"}, "authors");
  }
}
