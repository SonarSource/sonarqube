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

public class MeasureFilterFavouriteDaoTest {

  @Rule
  public DbTester db = DbTester.create(System2.INSTANCE);

  final DbSession session = db.getSession();

  MeasureFilterFavouriteDao underTest = db.getDbClient().measureFilterFavouriteDao();

  @Test
  public void insert_and_select_by_id() throws Exception {
    MeasureFilterFavouriteDto dto = new MeasureFilterFavouriteDto()
      .setMeasureFilterId(5L)
      .setUserId(10L)
      .setCreatedAt(new Date(1000L));
    underTest.insert(session, dto);
    session.commit();

    MeasureFilterFavouriteDto dtoReloaded = underTest.selectById(session, dto.getId());
    assertThat(dtoReloaded).isNotNull();
    assertThat(dtoReloaded.getMeasureFilterId()).isEqualTo(5L);
    assertThat(dtoReloaded.getUserId()).isEqualTo(10L);
    assertThat(dtoReloaded.getCreatedAt()).isEqualTo(new Date(1000L));

    assertThat(underTest.selectById(session, 123L)).isNull();
  }

}
