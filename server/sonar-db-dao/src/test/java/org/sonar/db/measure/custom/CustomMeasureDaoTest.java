/*
 * SonarQube
 * Copyright (C) 2009-2018 SonarSource SA
 * mailto:info AT sonarsource DOT com
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
package org.sonar.db.measure.custom;

import java.util.Arrays;
import java.util.List;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.sonar.api.utils.System2;
import org.sonar.db.DbSession;
import org.sonar.db.DbTester;
import org.sonar.db.RowNotFoundException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.offset;
import static org.sonar.db.measure.custom.CustomMeasureTesting.newCustomMeasureDto;


public class CustomMeasureDaoTest {
  @Rule
  public DbTester db = DbTester.create(System2.INSTANCE);
  @Rule
  public ExpectedException expectedException = ExpectedException.none();

  CustomMeasureDao underTest;
  DbSession session;

  @Before
  public void setUp() {
    session = db.getSession();
    underTest = new CustomMeasureDao();
  }

  @Test
  public void insert() {
    CustomMeasureDto measure = newCustomMeasureDto();

    underTest.insert(session, measure);

    CustomMeasureDto result = underTest.selectOrFail(session, measure.getId());
    assertThat(result.getId()).isEqualTo(measure.getId());
    assertThat(result.getMetricId()).isEqualTo(measure.getMetricId());
    assertThat(result.getComponentUuid()).isEqualTo(measure.getComponentUuid());
    assertThat(result.getDescription()).isEqualTo(measure.getDescription());
    assertThat(result.getUserLogin()).isEqualTo(measure.getUserLogin());
    assertThat(result.getTextValue()).isEqualTo(measure.getTextValue());
    assertThat(result.getValue()).isCloseTo(measure.getValue(), offset(0.001d));
    assertThat(result.getCreatedAt()).isEqualTo(measure.getCreatedAt());
    assertThat(result.getUpdatedAt()).isEqualTo(measure.getUpdatedAt());
  }

  @Test
  public void delete_by_metric_id() {
    CustomMeasureDto measure = newCustomMeasureDto();
    underTest.insert(session, measure);
    assertThat(underTest.selectById(session, measure.getId())).isNotNull();

    underTest.deleteByMetricIds(session, Arrays.asList(measure.getMetricId()));

    assertThat(underTest.selectById(session, measure.getId())).isNull();
  }

  @Test
  public void update() {
    CustomMeasureDto measure = newCustomMeasureDto().setDescription("old-description");
    underTest.insert(session, measure);
    measure.setDescription("new-description");

    underTest.update(session, measure);

    assertThat(underTest.selectById(session, measure.getId()).getDescription()).isEqualTo("new-description");
  }

  @Test
  public void delete() {
    CustomMeasureDto measure = newCustomMeasureDto();
    underTest.insert(session, measure);

    underTest.delete(session, measure.getId());
    assertThat(underTest.selectById(session, measure.getId())).isNull();
  }

  @Test
  public void select_by_component_uuid() {
    underTest.insert(session, newCustomMeasureDto().setComponentUuid("u1"));
    underTest.insert(session, newCustomMeasureDto().setComponentUuid("u1"));
    underTest.insert(session, newCustomMeasureDto().setComponentUuid("u2"));
    session.commit();

    List<CustomMeasureDto> result = underTest.selectByComponentUuid(session, "u1");

    assertThat(result).hasSize(2);
    assertThat(result).extracting("componentUuid").containsOnly("u1");
    assertThat(underTest.countByComponentUuid(session, "u1")).isEqualTo(2);
  }

  @Test
  public void select_by_component_uuid_with_options() {
    underTest.insert(session, newCustomMeasureDto().setComponentUuid("u1"));
    underTest.insert(session, newCustomMeasureDto().setComponentUuid("u1"));
    underTest.insert(session, newCustomMeasureDto().setComponentUuid("u2"));
    session.commit();

    List<CustomMeasureDto> result = underTest.selectByComponentUuid(session, "u1", 0, 100);

    assertThat(result).hasSize(2);
    assertThat(result).extracting("componentUuid").containsOnly("u1");
  }

  @Test
  public void select_by_metric_id() {
    underTest.insert(session, newCustomMeasureDto().setMetricId(123));
    underTest.insert(session, newCustomMeasureDto().setMetricId(123));

    List<CustomMeasureDto> result = underTest.selectByMetricId(session, 123);

    assertThat(result).hasSize(2);
  }

  @Test
  public void count_by_component_uuid_and_metric_id() {
    underTest.insert(session, newCustomMeasureDto().setMetricId(123).setComponentUuid("123"));
    underTest.insert(session, newCustomMeasureDto().setMetricId(123).setComponentUuid("123"));

    int count = underTest.countByComponentIdAndMetricId(session, "123", 123);

    assertThat(count).isEqualTo(2);
  }

  @Test
  public void select_by_id_fail_if_no_measure_found() {
    expectedException.expect(RowNotFoundException.class);

    underTest.selectOrFail(session, 42L);
  }

  @Test
  public void select_by_metric_key_and_text_value() {
    db.prepareDbUnit(getClass(), "select_by_metric_key_and_text_value.xml");

    List<CustomMeasureDto> result = underTest.selectByMetricKeyAndTextValue(session, "customKey", "value1");

    assertThat(result).extracting("id").containsOnly(20L, 21L);

    assertThat(underTest.selectByMetricKeyAndTextValue(session, "customKey", "unknown")).isEmpty();
    assertThat(underTest.selectByMetricKeyAndTextValue(session, "unknown", "value1")).isEmpty();
  }
}
