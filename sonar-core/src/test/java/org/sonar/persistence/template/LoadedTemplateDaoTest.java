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
package org.sonar.persistence.template;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThat;

import org.junit.Before;
import org.junit.Test;
import org.sonar.persistence.DaoTestCase;
import org.sonar.persistence.template.LoadedTemplateDto;
import org.sonar.persistence.template.LoadedTemplateDao;

public class LoadedTemplateDaoTest extends DaoTestCase {

  private LoadedTemplateDao dao;

  @Before
  public void createDao() throws Exception {
    dao = new LoadedTemplateDao(getMyBatis());
  }

  @Test
  public void shouldSelectByKeyAndType() throws Exception {
    setupData("shared");

    LoadedTemplateDto template = dao.selectByKeyAndType("SONAR-HOTSPOT", "DASHBOARD");
    assertThat(template.getId(), is(1L));
    assertThat(template.getKey(), is("SONAR-HOTSPOT"));
    assertThat(template.getType(), is("DASHBOARD"));
  }

  @Test
  public void shouldReturnNullIfIdNoneFound() throws Exception {
    setupData("shared");

    assertNull(dao.selectByKeyAndType("BAR", "DASHBOARD"));
  }

  @Test
  public void shouldInsert() throws Exception {
    setupData("shouldInsert");

    LoadedTemplateDto template = new LoadedTemplateDto("SQALE", "DASHBOARD");
    dao.insert(template);

    checkTables("shouldInsert", "loaded_templates");
  }
}
