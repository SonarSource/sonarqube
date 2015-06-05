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

package org.sonar.server.custommeasure.persistence;

import java.util.Arrays;
import java.util.List;
import org.junit.After;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.sonar.core.custommeasure.db.CustomMeasureDto;
import org.sonar.core.persistence.DbSession;
import org.sonar.core.persistence.DbTester;
import org.sonar.server.db.DbClient;
import org.sonar.test.DbTests;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.offset;
import static org.sonar.server.custommeasure.persistence.CustomMeasureTesting.newCustomMeasureDto;

@Category(DbTests.class)
public class CustomMeasureDaoTest {
  @ClassRule
  public static DbTester db = new DbTester();

  CustomMeasureDao sut;
  DbSession session;

  @Before
  public void setUp() {
    DbClient dbClient = new DbClient(db.database(), db.myBatis(), new CustomMeasureDao());
    session = dbClient.openSession(false);
    sut = dbClient.customMeasureDao();
    db.truncateTables();
  }

  @After
  public void tearDown() {
    session.close();
  }

  @Test
  public void insert() {
    CustomMeasureDto measure = newCustomMeasureDto();

    sut.insert(session, measure);

    CustomMeasureDto result = sut.selectNullableById(session, measure.getId());
    assertThat(result.getId()).isEqualTo(measure.getId());
    assertThat(result.getMetricId()).isEqualTo(measure.getMetricId());
    assertThat(result.getComponentId()).isEqualTo(measure.getComponentId());
    assertThat(result.getDescription()).isEqualTo(measure.getDescription());
    assertThat(result.getUserLogin()).isEqualTo(measure.getUserLogin());
    assertThat(result.getTextValue()).isEqualTo(measure.getTextValue());
    assertThat(result.getValue()).isCloseTo(measure.getValue(), offset(0.001d));
    assertThat(result.getCreatedAt()).isEqualTo(measure.getCreatedAt());
    assertThat(result.getUpdatedAt()).isEqualTo(measure.getUpdatedAt());
  }

  @Test
  public void delete() {
    CustomMeasureDto measure = newCustomMeasureDto();
    sut.insert(session, measure);
    assertThat(sut.selectNullableById(session, measure.getId())).isNotNull();

    sut.deleteByMetricIds(session, Arrays.asList(measure.getMetricId()));

    assertThat(sut.selectNullableById(session, measure.getId())).isNull();
  }

  @Test
  public void select_by_component_id() {
    sut.insert(session, newCustomMeasureDto().setComponentId(1));
    sut.insert(session, newCustomMeasureDto().setComponentId(1));
    sut.insert(session, newCustomMeasureDto().setComponentId(2));
    session.commit();

    List<CustomMeasureDto> result = sut.selectByComponentId(session, 1L);

    assertThat(result).hasSize(2);
    assertThat(result).extracting("componentId").containsOnly(1L);
  }
}
