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
package org.sonar.db.measure;

import java.util.Date;
import org.junit.Rule;
import org.junit.Test;
import org.sonar.api.utils.System2;
import org.sonar.db.DbSession;
import org.sonar.db.DbTester;

import static org.assertj.core.api.Assertions.assertThat;

public class MeasureFilterDaoTest {

  @Rule
  public DbTester db = DbTester.create(System2.INSTANCE);

  final DbSession session = db.getSession();

  MeasureFilterDao underTest = db.getDbClient().measureFilterDao();

  @Test
  public void should_find_filter() {
    db.prepareDbUnit(getClass(), "shared.xml");

    MeasureFilterDto filter = underTest.selectSystemFilterByName("Projects");

    assertThat(filter.getId()).isEqualTo(1L);
    assertThat(filter.getName()).isEqualTo("Projects");
  }

  @Test
  public void should_not_find_filter() {
    db.prepareDbUnit(getClass(), "shared.xml");

    assertThat(underTest.selectSystemFilterByName("Unknown")).isNull();
  }

  @Test
  public void select_by_id() throws Exception {
    MeasureFilterDto dto = new MeasureFilterDto()
      .setUserId(10L)
      .setName("name")
      .setDescription("description")
      .setData("data")
      .setShared(true)
      .setCreatedAt(new Date(1000L))
      .setUpdatedAt(new Date(2000L));
    underTest.insert(session, dto);
    session.commit();

    MeasureFilterDto dtoReloded = underTest.selectById(session, dto.getId());
    assertThat(dtoReloded).isNotNull();
    assertThat(dtoReloded.getUserId()).isEqualTo(10L);
    assertThat(dtoReloded.getName()).isEqualTo("name");
    assertThat(dtoReloded.getDescription()).isEqualTo("description");
    assertThat(dtoReloded.getData()).isEqualTo("data");
    assertThat(dtoReloded.isShared()).isTrue();
    assertThat(dtoReloded.getCreatedAt()).isEqualTo(new Date(1000L));
    assertThat(dtoReloded.getUpdatedAt()).isEqualTo(new Date(2000L));

    assertThat(underTest.selectById(session, 123L)).isNull();
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

    underTest.insert(filterDto);

    db.assertDbUnit(getClass(), "shouldInsert-result.xml", new String[]{"created_at", "updated_at"}, "measure_filters");
  }
}
