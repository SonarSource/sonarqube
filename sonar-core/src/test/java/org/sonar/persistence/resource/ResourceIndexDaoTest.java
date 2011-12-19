/*
 * Sonar, open source software quality management tool.
 * Copyright (C) 2008-2011 SonarSource
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
package org.sonar.persistence.resource;

import org.hamcrest.core.Is;
import org.junit.Before;
import org.junit.Test;
import org.sonar.persistence.DaoTestCase;

import java.util.List;

import static org.junit.Assert.assertThat;

public class ResourceIndexDaoTest extends DaoTestCase {

  private static ResourceIndexDao dao;

  @Before
  public void createDao() {
    dao = new ResourceIndexDao(getMyBatis());
  }

  @Test
  public void testSearch() {
    setupData("testSearch");

    List<ResourceIndexDto> rows = dao.search("zip");
    assertThat(rows.size(), Is.is(1));
    assertThat(rows.get(0).getPosition(), Is.is(0));
    assertThat(rows.get(0).getKey(), Is.is("ziputils"));

    rows = dao.search("util");
    assertThat(rows.size(), Is.is(2));
    assertThat(rows.get(0).getPosition(), Is.is(3));
    assertThat(rows.get(0).getKey(), Is.is("utils"));
    assertThat(rows.get(0).getResourceId(), Is.is(10));

    assertThat(rows.get(1).getPosition(), Is.is(4));
    assertThat(rows.get(1).getKey(), Is.is("utils"));
    assertThat(rows.get(1).getResourceId(), Is.is(130));
  }

  @Test
  public void shouldNotBeCaseSensitiveSearch() {
    setupData("testSearch");

    List<ResourceIndexDto> rows = dao.search("ZipU");
    assertThat(rows.size(), Is.is(1));
    assertThat(rows.get(0).getKey(), Is.is("ziputils"));
  }

  @Test
  public void testMinimumSizeOfSearchInput() {
    setupData("testSearch");

    List<ResourceIndexDto> rows = dao.search("zi");
    assertThat(rows.size(), Is.is(0));
  }

  @Test
  public void testIndex() {
    setupData("testIndex");

    dao.index("ZipUtils", 10, 8);

    checkTables("testIndex", "resource_index");
  }

  @Test
  public void testIndexAll() {
    setupData("testIndexAll");

    dao.index(new ResourceIndexerFilter());

    checkTables("testIndexAll", "resource_index");
  }

}
