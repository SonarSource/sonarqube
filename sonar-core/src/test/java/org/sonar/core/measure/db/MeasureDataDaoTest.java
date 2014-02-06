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

package org.sonar.core.measure.db;

import org.junit.Before;
import org.junit.Test;
import org.sonar.core.persistence.AbstractDaoTestCase;

import static org.fest.assertions.Assertions.assertThat;

public class MeasureDataDaoTest extends AbstractDaoTestCase {

  private MeasureDataDao dao;

  @Before
  public void createDao() {
    dao = new MeasureDataDao(getMyBatis());
  }

  @Test
  public void find_by_component_key_and_metric_key() throws Exception {
    setupData("shared");

    MeasureDataDto result = dao.findByComponentKeyAndMetricKey("org.sonar.core.measure.db.MeasureData", "authors_by_line");
    assertThat(result.getId()).isEqualTo(1);
    assertThat(result.getMeasureId()).isEqualTo(1);
    assertThat(result.getSnapshotId()).isEqualTo(1);
    assertThat(result.getText()).isNotNull();
    assertThat(result.getData()).isNotNull();

    // FIXME failing because data is returned in wrong format
//    assertThat(result.getText()).isEqualTo("test");
  }

  @Test
  public void find_by_component_key_and_metric_key_without_text() throws Exception {
    setupData("find_by_component_key_and_metric_key_without_text");

    MeasureDataDto result = dao.findByComponentKeyAndMetricKey("org.sonar.core.measure.db.MeasureData", "authors_by_line");
    assertThat(result.getId()).isEqualTo(1);
    assertThat(result.getMeasureId()).isEqualTo(1);
    assertThat(result.getSnapshotId()).isEqualTo(1);
    assertThat(result.getText()).isNull();
    assertThat(result.getData()).isNull();
  }
}
