/*
 * SonarQube, open source software quality management tool.
 * Copyright (C) 2008-2014 SonarSource
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
package org.sonar.server.dashboard.db;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.sonar.core.dashboard.WidgetDto;
import org.sonar.core.persistence.DbSession;
import org.sonar.core.persistence.DbTester;
import org.sonar.core.persistence.MyBatis;
import org.sonar.test.DbTests;

import java.util.Collection;

import static org.assertj.core.api.Assertions.assertThat;

@Category(DbTests.class)
public class WidgetDaoTest {

  WidgetDao dao;

  @Rule
  public DbTester dbTester = new DbTester();

  private DbSession session;

  @Before
  public void setUp() throws Exception {
    dao = new WidgetDao(dbTester.myBatis());
    session = dbTester.myBatis().openSession(false);
  }

  @After
  public void tearDown() {
    MyBatis.closeQuietly(session);
  }

  @Test
  public void should_select_all() throws Exception {
    dbTester.prepareDbUnit(this.getClass(), "before.xml");
    session.commit();

    Collection<WidgetDto> widgets = dao.findAll(session);
    assertThat(widgets).hasSize(5);
    for (WidgetDto widget : widgets) {
      assertThat(widget.getId()).isNotNull();
      assertThat(widget.getName()).isNotNull();
      assertThat(widget.getDescription()).isNotNull();
    }
  }
}
