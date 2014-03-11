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
package org.sonar.core.template;

import org.junit.Before;
import org.junit.Test;
import org.sonar.core.persistence.AbstractDaoTestCase;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;

public class LoadedTemplateDaoTest extends AbstractDaoTestCase {

  private LoadedTemplateDao dao;

  @Before
  public void createDao() throws Exception {
    dao = new LoadedTemplateDao(getMyBatis());
  }

  @Test
  public void shouldCountByTypeAndKey() throws Exception {
    setupData("shouldCountByTypeAndKey");
    assertThat(dao.countByTypeAndKey("DASHBOARD", "HOTSPOTS"), is(1));
    assertThat(dao.countByTypeAndKey("DASHBOARD", "UNKNOWN"), is(0));
    assertThat(dao.countByTypeAndKey("PROFILE", "HOTSPOTS"), is(0));
  }

  @Test
  public void shouldInsert() throws Exception {
    setupData("shouldInsert");

    LoadedTemplateDto template = new LoadedTemplateDto("SQALE", "DASHBOARD");
    dao.insert(template);

    checkTables("shouldInsert", "loaded_templates");
  }
}
