/*
 * SonarQube
 * Copyright (C) 2009-2017 SonarSource SA
 * mailto:info AT sonarsource DOT com
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
import org.sonar.db.DbSession;
import org.sonar.db.DbTester;

import static org.assertj.core.api.Assertions.assertThat;

public class LoadedTemplateDaoTest {

  @Rule
  public DbTester dbTester = DbTester.create(System2.INSTANCE);

  private LoadedTemplateDao underTest = dbTester.getDbClient().loadedTemplateDao();
  private DbSession dbSession = dbTester.getSession();

  @Test
  public void shouldCountByTypeAndKey() {
    dbTester.prepareDbUnit(getClass(), "shouldCountByTypeAndKey.xml");

    assertThat(underTest.countByTypeAndKey("DASHBOARD", "HOTSPOTS", dbSession)).isEqualTo(1);
    assertThat(underTest.countByTypeAndKey("DASHBOARD", "UNKNOWN", dbSession)).isEqualTo(0);
    assertThat(underTest.countByTypeAndKey("PROFILE", "HOTSPOTS", dbSession)).isEqualTo(0);
  }

  @Test
  public void shouldInsert() {
    dbTester.prepareDbUnit(getClass(), "shouldInsert.xml");

    LoadedTemplateDto template = new LoadedTemplateDto("SQALE", "DASHBOARD");
    underTest.insert(template, dbSession);
    dbSession.commit();

    dbTester.assertDbUnit(getClass(), "shouldInsert-result.xml", "loaded_templates");
  }
}
