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
package org.sonar.ce.task.projectanalysis.measure;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.sonar.ce.task.projectanalysis.component.Developer;
import org.sonar.ce.task.projectanalysis.component.DumbDeveloper;

import static org.assertj.core.api.Assertions.assertThat;

public class MeasureKeyTest {

  @Rule
  public ExpectedException thrown = ExpectedException.none();

  static final Developer DEVELOPER = new DumbDeveloper("DEV1");

  @Test
  public void fail_with_NPE_when_metric_key_is_null() {
    thrown.expect(NullPointerException.class);

    new MeasureKey(null, DEVELOPER);
  }

  @Test
  public void test_equals_and_hashcode() {
    MeasureKey measureKey = new MeasureKey("metricKey", null);
    MeasureKey measureKey2 = new MeasureKey("metricKey", null);
    MeasureKey anotherMeasureKey = new MeasureKey("anotherMetricKey", null);

    MeasureKey developerMeasureKey = new MeasureKey("metricKey", DEVELOPER);
    MeasureKey developerMeasureKey2 = new MeasureKey("metricKey", DEVELOPER);

    assertThat(measureKey).isEqualTo(measureKey);
    assertThat(measureKey).isEqualTo(measureKey2);
    assertThat(measureKey).isNotEqualTo(null);
    assertThat(measureKey).isNotEqualTo(anotherMeasureKey);

    assertThat(developerMeasureKey).isEqualTo(developerMeasureKey2);

    assertThat(measureKey.hashCode()).isEqualTo(measureKey.hashCode());
    assertThat(measureKey.hashCode()).isEqualTo(measureKey2.hashCode());
    assertThat(measureKey.hashCode()).isNotEqualTo(anotherMeasureKey.hashCode());

    assertThat(developerMeasureKey.hashCode()).isEqualTo(developerMeasureKey2.hashCode());
  }

  @Test
  public void to_string() {
    assertThat(new MeasureKey("metricKey", DEVELOPER).toString()).isEqualTo(
      "MeasureKey{metricKey='metricKey', developer=Developer{key='DEV1'}}");
    assertThat(new MeasureKey("metricKey", null).toString()).isEqualTo("MeasureKey{metricKey='metricKey', developer=null}");
  }
}
