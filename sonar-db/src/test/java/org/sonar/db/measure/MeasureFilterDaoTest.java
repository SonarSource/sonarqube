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
package org.sonar.db.measure;

import org.junit.Rule;
import org.junit.Test;
import org.sonar.api.utils.System2;
import org.sonar.db.DbTester;

import static org.assertj.core.api.Assertions.assertThat;

public class MeasureFilterDaoTest {

  @Rule
  public DbTester db = DbTester.create(System2.INSTANCE);

  MeasureFilterDao dao = db.getDbClient().measureFilterDao();

  @Test
  public void should_find_filter() {
    db.prepareDbUnit(getClass(), "shared.xml");

    MeasureFilterDto filter = dao.selectSystemFilterByName("Projects");

    assertThat(filter.getId()).isEqualTo(1L);
    assertThat(filter.getName()).isEqualTo("Projects");
  }

  @Test
  public void should_not_find_filter() {
    db.prepareDbUnit(getClass(), "shared.xml");

    assertThat(dao.selectSystemFilterByName("Unknown")).isNull();
  }

  @Test
  public void should_insert() {
    db.prepareDbUnit(getClass(), "shared.xml");

    MeasureFilterDto filterDto = new MeasureFilterDto();
    filterDto.setName("Project Treemap");
    filterDto.setUserId(123L);
    filterDto.setShared(true);
    filterDto.setDescription("Treemap of projects");
    filterDto.setData("qualifiers=TRK|display=treemap");

    dao.insert(filterDto);

    db.assertDbUnit(getClass(), "shouldInsert-result.xml", new String[]{"created_at", "updated_at"}, "measure_filters");
  }
}
