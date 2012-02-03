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
package org.sonar.core.properties;

import static org.hamcrest.Matchers.hasItems;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;

import java.util.List;

import org.junit.Before;
import org.junit.Test;
import org.sonar.core.persistence.DaoTestCase;

public class PropertiesDaoTest extends DaoTestCase {

  private PropertiesDao dao;

  @Before
  public void createDao() throws Exception {
    dao = new PropertiesDao(getMyBatis());
  }

  @Test
  public void shouldFindUserIdsForFavouriteResource() throws Exception {
    setupData("shouldFindUserIdsForFavouriteResource");
    List<String> userIds = dao.findUserIdsForFavouriteResource(2);
    assertThat(userIds.size(), is(2));
    assertThat(userIds, hasItems("user3", "user4"));
  }
}
