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
package org.sonar.server.db;

import org.junit.Test;
import org.sonar.core.persistence.Dto;
import org.sonar.core.persistence.DbSession;
import org.sonar.server.exceptions.NotFoundException;

import static org.fest.assertions.Assertions.assertThat;
import static org.fest.assertions.Fail.fail;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class BaseDaoTest {

  DbSession dbSession = mock(DbSession.class);

  @Test
  public void getNonNullByKey() throws Exception {
    BaseDao dao = mock(BaseDao.class);
    FakeDto dto = new FakeDto("ki");
    when(dao.doGetNullableByKey(dbSession, "ki")).thenReturn(dto);
    when(dao.getByKey(any(DbSession.class), anyString())).thenCallRealMethod();

    assertThat(dao.getByKey(dbSession, "ki")).isSameAs(dto);

    try {
      dao.getByKey(dbSession, "unknown");
      fail();
    } catch (NotFoundException e) {
      assertThat(e).hasMessage("Key 'unknown' not found");
    }
  }


  static class FakeDto extends Dto<String> {
    private final String key;

    public FakeDto(String key) {
      this.key = key;
    }

    @Override
    public String getKey() {
      return key;
    }
  }
}
