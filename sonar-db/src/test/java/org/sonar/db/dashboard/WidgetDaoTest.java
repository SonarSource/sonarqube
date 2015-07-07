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
package org.sonar.db.dashboard;

import java.util.Collection;
import org.junit.Rule;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.sonar.api.utils.System2;
import org.sonar.db.DbTester;
import org.sonar.test.DbTests;

import static org.assertj.core.api.Assertions.assertThat;

@Category(DbTests.class)
public class WidgetDaoTest {

  @Rule
  public DbTester dbTester = DbTester.create(System2.INSTANCE);

  WidgetDao dao = dbTester.getDbClient().widgetDao();

  @Test
  public void should_select_all() {
    dbTester.prepareDbUnit(this.getClass(), "before.xml");

    Collection<WidgetDto> widgets = dao.findAll(dbTester.getSession());
    assertThat(widgets).hasSize(5);
    for (WidgetDto widget : widgets) {
      assertThat(widget.getId()).isNotNull();
      assertThat(widget.getName()).isNotNull();
      assertThat(widget.getDescription()).isNotNull();
    }
  }
}
