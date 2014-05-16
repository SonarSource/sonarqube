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

import static org.fest.assertions.Assertions.assertThat;

public class MeasureDaoTest extends AbstractDaoTestCase {

  private MeasureDao dao;

  @Before
  public void createDao() {
    dao = new MeasureDao(getMyBatis());
  }

  @Test
  public void find_value_by_component_key_and_metric_key() throws Exception {
    setupData("shared");

    MeasureDto result = dao.findByComponentKeyAndMetricKey("org.struts:struts-core:src/org/struts/RequestContext.java", "ncloc");
    assertThat(result.getId()).isEqualTo(22);
    assertThat(result.getSnapshotId()).isEqualTo(5);
    assertThat(result.getValue()).isEqualTo(10d);
  }

  @Test
  public void find_data_by_component_key_and_metric_key() throws Exception {
    setupData("shared");

    MeasureDto result = dao.findByComponentKeyAndMetricKey("org.struts:struts-core:src/org/struts/RequestContext.java", "authors_by_line");
    assertThat(result.getId()).isEqualTo(20);
    assertThat(result.getSnapshotId()).isEqualTo(5);
    assertThat(result.getData()).isNotNull();

    assertThat(result.getData()).isEqualTo("0123456789012345678901234567890123456789");
  }

  @Test
  public void find_text_value_by_component_key_and_metric_key() throws Exception {
    setupData("shared");

    MeasureDto result = dao.findByComponentKeyAndMetricKey("org.struts:struts-core:src/org/struts/RequestContext.java", "coverage_line_hits_data");
    assertThat(result.getId()).isEqualTo(21);
    assertThat(result.getSnapshotId()).isEqualTo(5);
    assertThat(result.getData()).isEqualTo("36=1;37=1;38=1;39=1;43=1;48=1;53=1");
  }
}
