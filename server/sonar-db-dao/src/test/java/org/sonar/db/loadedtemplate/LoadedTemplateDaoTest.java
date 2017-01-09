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
package org.sonar.db.loadedtemplate;

import org.junit.Rule;
import org.junit.Test;
import org.sonar.api.utils.System2;
import org.sonar.db.DbTester;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;

public class LoadedTemplateDaoTest {

  @Rule
  public DbTester dbTester = DbTester.create(System2.INSTANCE);

  LoadedTemplateDao dao = dbTester.getDbClient().loadedTemplateDao();

  @Test
  public void shouldCountByTypeAndKey() {
    dbTester.prepareDbUnit(getClass(), "shouldCountByTypeAndKey.xml");

    assertThat(dao.countByTypeAndKey("DASHBOARD", "HOTSPOTS"), is(1));
    assertThat(dao.countByTypeAndKey("DASHBOARD", "UNKNOWN"), is(0));
    assertThat(dao.countByTypeAndKey("PROFILE", "HOTSPOTS"), is(0));
  }

  @Test
  public void shouldInsert() {
    dbTester.prepareDbUnit(getClass(), "shouldInsert.xml");

    LoadedTemplateDto template = new LoadedTemplateDto("SQALE", "DASHBOARD");
    dao.insert(template);

    dbTester.assertDbUnit(getClass(), "shouldInsert-result.xml", "loaded_templates");
  }
}
