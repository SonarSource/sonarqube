/*
 * SonarQube
 * Copyright (C) 2009-2019 SonarSource SA
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

import java.util.List;
import org.junit.Rule;
import org.junit.Test;
import org.sonar.api.utils.System2;
import org.sonar.db.DbSession;
import org.sonar.db.DbTester;
import org.sonar.db.component.ComponentDto;
import org.sonar.db.metric.MetricDto;
import org.sonar.db.user.UserDto;

import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.offset;
import static org.assertj.core.api.Assertions.tuple;
import static org.sonar.db.measure.custom.CustomMeasureTesting.newCustomMeasureDto;

public class CustomMeasureDaoTest {
  @Rule
  public DbTester db = DbTester.create(System2.INSTANCE);

  private DbSession session = db.getSession();

  private CustomMeasureDao underTest = new CustomMeasureDao();

  @Test
  public void insert() {
    UserDto user = db.users().insertUser();
    ComponentDto project = db.components().insertPrivateProject();
    MetricDto metric = db.measures().insertMetric(m -> m.setUserManaged(true));
    CustomMeasureDto measure = newCustomMeasureDto()
      .setComponentUuid(project.uuid())
      .setMetricId(metric.getId())
      .setUserUuid(user.getUuid());

    underTest.insert(session, measure);

    CustomMeasureDto result = underTest.selectById(session, measure.getId());
    assertThat(result.getId()).isEqualTo(measure.getId());
    assertThat(result.getMetricId()).isEqualTo(metric.getId());
    assertThat(result.getComponentUuid()).isEqualTo(project.uuid());
    assertThat(result.getUserUuid()).isEqualTo(user.getUuid());
    assertThat(result.getDescription()).isEqualTo(measure.getDescription());
    assertThat(result.getTextValue()).isEqualTo(measure.getTextValue());
    assertThat(result.getValue()).isCloseTo(measure.getValue(), offset(0.001d));
    assertThat(result.getCreatedAt()).isEqualTo(measure.getCreatedAt());
    assertThat(result.getUpdatedAt()).isEqualTo(measure.getUpdatedAt());
  }

  @Test
  public void delete_by_metric_id() {
    UserDto user = db.users().insertUser();
    ComponentDto project = db.components().insertPrivateProject();
    MetricDto metric = db.measures().insertMetric(m -> m.setUserManaged(true));
    CustomMeasureDto measure = db.measures().insertCustomMeasure(user, project, metric);

    underTest.deleteByMetricIds(session, singletonList(measure.getMetricId()));

    assertThat(underTest.selectById(session, measure.getId())).isNull();
  }

  @Test
  public void update() {
    UserDto user = db.users().insertUser();
    ComponentDto project = db.components().insertPrivateProject();
    MetricDto metric = db.measures().insertMetric(m -> m.setUserManaged(true));
    CustomMeasureDto measure = db.measures().insertCustomMeasure(user, project, metric, m -> m.setDescription("old-description"));

    underTest.update(session, measure.setDescription("new-description"));

    assertThat(underTest.selectById(session, measure.getId()).getDescription()).isEqualTo("new-description");
  }

  @Test
  public void delete() {
    UserDto user = db.users().insertUser();
    ComponentDto project = db.components().insertPrivateProject();
    MetricDto metric = db.measures().insertMetric(m -> m.setUserManaged(true));
    CustomMeasureDto measure = db.measures().insertCustomMeasure(user, project, metric);

    underTest.delete(session, measure.getId());

    assertThat(underTest.selectById(session, measure.getId())).isNull();
  }

  @Test
  public void select_by_component_uuid() {
    UserDto user = db.users().insertUser();
    MetricDto metric = db.measures().insertMetric(m -> m.setUserManaged(true));
    ComponentDto project1 = db.components().insertPrivateProject();
    CustomMeasureDto measure1 = db.measures().insertCustomMeasure(user, project1, metric);
    CustomMeasureDto measure2 = db.measures().insertCustomMeasure(user, project1, metric);
    ComponentDto project2 = db.components().insertPrivateProject();
    CustomMeasureDto measure3 = db.measures().insertCustomMeasure(user, project2, metric);

    assertThat(underTest.selectByComponentUuid(session, project1.uuid()))
      .extracting(CustomMeasureDto::getId, CustomMeasureDto::getComponentUuid)
      .containsOnly(
        tuple(measure1.getId(), project1.uuid()),
        tuple(measure2.getId(), project1.uuid()))
      .doesNotContain(tuple(measure3.getId(), project2.uuid()));

    assertThat(underTest.countByComponentUuid(session, project1.uuid())).isEqualTo(2);
  }

  @Test
  public void select_by_component_uuid_with_options() {
    UserDto user = db.users().insertUser();
    MetricDto metric = db.measures().insertMetric(m -> m.setUserManaged(true));
    ComponentDto project1 = db.components().insertPrivateProject();
    CustomMeasureDto measure1 = db.measures().insertCustomMeasure(user, project1, metric);
    CustomMeasureDto measure2 = db.measures().insertCustomMeasure(user, project1, metric);
    ComponentDto project2 = db.components().insertPrivateProject();
    CustomMeasureDto measure3 = db.measures().insertCustomMeasure(user, project2, metric);

    assertThat(underTest.selectByComponentUuid(session, project1.uuid(), 0, 100))
      .extracting(CustomMeasureDto::getId, CustomMeasureDto::getComponentUuid)
      .containsOnly(
        tuple(measure1.getId(), project1.uuid()),
        tuple(measure2.getId(), project1.uuid()))
      .doesNotContain(tuple(measure3.getId(), project2.uuid()));
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
  public void select_by_metric_key_and_text_value() {
    UserDto user = db.users().insertUser();
    ComponentDto project = db.components().insertPrivateProject();
    MetricDto metric = db.measures().insertMetric(m -> m.setUserManaged(true));
    CustomMeasureDto customMeasure1 = db.measures().insertCustomMeasure(user, project, metric, m -> m.setTextValue("value"));
    CustomMeasureDto customMeasure2 = db.measures().insertCustomMeasure(user, project, metric, m -> m.setTextValue("value"));
    CustomMeasureDto customMeasure3 = db.measures().insertCustomMeasure(user, project, metric, m -> m.setTextValue("other value"));

    assertThat(underTest.selectByMetricKeyAndTextValue(session, metric.getKey(), "value"))
      .extracting(CustomMeasureDto::getId)
      .containsExactlyInAnyOrder(customMeasure1.getId(), customMeasure2.getId())
      .doesNotContain(customMeasure3.getId());

    assertThat(underTest.selectByMetricKeyAndTextValue(session, metric.getKey(), "unknown")).isEmpty();
    assertThat(underTest.selectByMetricKeyAndTextValue(session, "unknown", "value")).isEmpty();
  }
}
