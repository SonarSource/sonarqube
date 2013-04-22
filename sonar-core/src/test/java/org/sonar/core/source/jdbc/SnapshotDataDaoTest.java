/*
 * SonarQube, open source software quality management tool.
 * Copyright (C) 2008-2013 SonarSource
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

package org.sonar.core.source.jdbc;

import org.junit.Before;
import org.junit.Test;
import org.sonar.core.persistence.AbstractDaoTestCase;

import java.util.Collection;

import static org.fest.assertions.Assertions.assertThat;

public class SnapshotDataDaoTest extends AbstractDaoTestCase {

  private SnapshotDataDao dao;

  @Before
  public void createDao() {
    dao = new SnapshotDataDao(getMyBatis());
    setupData("shared");
  }

  @Test
  public void should_retrieve_snapshot_data_by_snapshot_id() throws Exception {

    Collection<SnapshotDataDto> data = dao.selectSnapshotData(10L);

    assertThat(data).onProperty("snapshotId").containsOnly(10L, 10L);
    assertThat(data).onProperty("dataType").containsOnly("highlight_syntax", "symbol");
    assertThat(data).onProperty("data").containsOnly("0,10,k;", "20,25,20,35,45;");
  }

  @Test
  public void should_serialize_snapshot_data() throws Exception {

    String data = "0,10,k;";
    String dataType = "highlight_syntax";

    SnapshotDataDto dto = new SnapshotDataDto();
    dto.setResourceId(1L);
    dto.setSnapshotId(11L);
    dto.setData(data);
    dto.setDataType(dataType);

    dao.insert(dto);

    Collection<SnapshotDataDto> serializedData = dao.selectSnapshotData(11L);

    assertThat(serializedData).onProperty("snapshotId").containsOnly(11L);
    assertThat(serializedData).onProperty("dataType").containsOnly(dataType);
    assertThat(serializedData).onProperty("data").containsOnly(data);
  }
}
