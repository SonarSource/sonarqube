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
package org.sonar.core.measure.db;

import org.junit.Before;
import org.junit.Test;
import org.sonar.core.persistence.AbstractDaoTestCase;

import static org.assertj.core.api.Assertions.assertThat;

public class MeasureFilterDaoTest extends AbstractDaoTestCase {
  private MeasureFilterDao dao;

  @Before
  public void createDao() {
    dao = new MeasureFilterDao(getMyBatis());
  }

  @Test
  public void should_find_filter() {
    setupData("shared");

    MeasureFilterDto filter = dao.findSystemFilterByName("Projects");

    assertThat(filter.getId()).isEqualTo(1L);
    assertThat(filter.getName()).isEqualTo("Projects");
  }

  @Test
  public void should_not_find_filter() {
    setupData("shared");

    assertThat(dao.findSystemFilterByName("Unknown")).isNull();
  }

  @Test
  public void should_insert() {
    setupData("shared");

    MeasureFilterDto filterDto = new MeasureFilterDto();
    filterDto.setName("Project Treemap");
    filterDto.setUserId(123L);
    filterDto.setShared(true);
    filterDto.setDescription("Treemap of projects");
    filterDto.setData("qualifiers=TRK|display=treemap");

    dao.insert(filterDto);

    checkTables("shouldInsert", new String[]{"created_at", "updated_at"}, "measure_filters");
  }
}
