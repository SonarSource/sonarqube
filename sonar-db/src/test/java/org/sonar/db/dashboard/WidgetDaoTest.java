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
package org.sonar.db.dashboard;

import java.util.Collection;
import org.junit.Rule;
import org.junit.Test;
import org.sonar.api.utils.System2;
import org.sonar.db.DbSession;
import org.sonar.db.DbTester;

import static org.assertj.core.api.Assertions.assertThat;

public class WidgetDaoTest {

  @Rule
  public DbTester db = DbTester.create(System2.INSTANCE);
  DbSession dbSession = db.getSession();

  WidgetDao underTest = db.getDbClient().widgetDao();

  @Test
  public void should_select_all() {
    db.prepareDbUnit(this.getClass(), "before.xml");

    Collection<WidgetDto> widgets = underTest.findAll(db.getSession());
    assertThat(widgets).hasSize(5);
    for (WidgetDto widget : widgets) {
      assertThat(widget.getId()).isNotNull();
      assertThat(widget.getName()).isNotNull();
      assertThat(widget.getDescription()).isNotNull();
    }
  }

  @Test
  public void select_by_dashboard_key() {
    db.prepareDbUnit(this.getClass(), "before.xml");

    Collection<WidgetDto> result = underTest.findByDashboard(dbSession, 1L);

    assertThat(result).hasSize(5);
  }

  @Test
  public void select_by_key() {
    db.prepareDbUnit(this.getClass(), "before.xml");

    WidgetDto result = underTest.selectByKey(dbSession, 1L);

    assertThat(result).isNotNull();
  }
}
