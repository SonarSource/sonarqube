/*
 * Sonar, open source software quality management tool.
 * Copyright (C) 2008-2012 SonarSource
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

package org.sonar.core.source.jdbc;

import org.junit.Before;
import org.junit.Test;
import org.sonar.core.persistence.AbstractDaoTestCase;

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

    SnapshotDataDto dto = dao.selectBySnapshot(10L);

    assertThat(dto.getId()).isEqualTo(101L);
    assertThat(dto.getResourceId()).isEqualTo(1L);
    assertThat(dto.getSnapshotId()).isEqualTo(10L);
    assertThat(dto.getData()).isEqualTo("0,10,k");
    assertThat(dto.getDataType()).isEqualTo("highlight_syntax");
  }

  @Test
  public void should_serialize_snapshot_data() throws Exception {

    String data = "0,10,k;";
    String dataType = "highlight_syntax";

    SnapshotDataDto dto = new SnapshotDataDto(11L, 1L, data, dataType);

    dao.insert(dto);

    SnapshotDataDto serializedData = dao.selectBySnapshot(11L);

    assertThat(serializedData.getResourceId()).isEqualTo(1L);
    assertThat(serializedData.getSnapshotId()).isEqualTo(11L);
    assertThat(serializedData.getData()).isEqualTo(data);
    assertThat(serializedData.getDataType()).isEqualTo(dataType);
  }
}
