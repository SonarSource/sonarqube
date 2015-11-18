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

package org.sonar.server.computation.measure;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.sonar.server.computation.component.Developer;
import org.sonar.server.computation.component.DumbDeveloper;

import static org.assertj.core.api.Assertions.assertThat;

public class MeasureKeyTest {

  @Rule
  public ExpectedException thrown = ExpectedException.none();

  static final Developer DEVELOPER = new DumbDeveloper("DEV1");

  @Test
  public void fail_with_NPE_when_metric_key_is_null() {
    thrown.expect(NullPointerException.class);

    new MeasureKey(null, 1, 2, DEVELOPER);
  }

  @Test
  public void fail_with_IAE_when_rule_id_is_default_value() {
    thrown.expect(IllegalArgumentException.class);

    new MeasureKey("metricKey", -6253, 2, DEVELOPER);
  }

  @Test
  public void fail_with_IAE_when_characteristic_id_is_default_value() {
    thrown.expect(IllegalArgumentException.class);

    new MeasureKey("metricKey", 1, -6253, DEVELOPER);
  }

  @Test
  public void test_equals_and_hashcode() throws Exception {
    MeasureKey measureKey = new MeasureKey("metricKey", null, null, null);
    MeasureKey measureKey2 = new MeasureKey("metricKey", null, null, null);
    MeasureKey anotherMeasureKey = new MeasureKey("anotherMetricKey", null, null, null);

    MeasureKey ruleMeasureKey = new MeasureKey("metricKey", 1, null, null);
    MeasureKey ruleMeasureKey2 = new MeasureKey("metricKey", 1, null, null);
    MeasureKey anotherRuleMeasureKey = new MeasureKey("metricKey", 2, null, null);

    MeasureKey characteristicMeasureKey = new MeasureKey("metricKey", null, 1, null);
    MeasureKey characteristicMeasureKey2 = new MeasureKey("metricKey", null, 1, null);
    MeasureKey anotherCharacteristicMeasureKey = new MeasureKey("metricKey", null, 2, null);

    MeasureKey developerMeasureKey = new MeasureKey("metricKey", null, null, DEVELOPER);
    MeasureKey developerMeasureKey2 = new MeasureKey("metricKey", null, null, DEVELOPER);
    MeasureKey developerCharacteristicMeasureKey = new MeasureKey("metricKey", null, 2, DEVELOPER);

    assertThat(measureKey).isEqualTo(measureKey);
    assertThat(measureKey).isEqualTo(measureKey2);
    assertThat(measureKey).isNotEqualTo(null);
    assertThat(measureKey).isNotEqualTo(anotherMeasureKey);

    assertThat(ruleMeasureKey).isEqualTo(ruleMeasureKey2);
    assertThat(ruleMeasureKey).isNotEqualTo(anotherRuleMeasureKey);

    assertThat(characteristicMeasureKey).isEqualTo(characteristicMeasureKey2);
    assertThat(characteristicMeasureKey).isNotEqualTo(anotherCharacteristicMeasureKey);

    assertThat(developerMeasureKey).isEqualTo(developerMeasureKey2);
    assertThat(developerMeasureKey).isNotEqualTo(developerCharacteristicMeasureKey);

    assertThat(measureKey.hashCode()).isEqualTo(measureKey.hashCode());
    assertThat(measureKey.hashCode()).isEqualTo(measureKey2.hashCode());
    assertThat(measureKey.hashCode()).isNotEqualTo(anotherMeasureKey.hashCode());

    assertThat(ruleMeasureKey.hashCode()).isEqualTo(ruleMeasureKey2.hashCode());
    assertThat(characteristicMeasureKey.hashCode()).isEqualTo(characteristicMeasureKey2.hashCode());
    assertThat(developerMeasureKey.hashCode()).isEqualTo(developerMeasureKey2.hashCode());
  }

  @Test
  public void to_string() {
    assertThat(new MeasureKey("metricKey", 1, 2, DEVELOPER).toString()).isEqualTo(
      "MeasureKey{metricKey='metricKey', ruleId=1, characteristicId=2, developer=Developer{key='DEV1'}}");
    assertThat(new MeasureKey("metricKey", 1, null, null).toString()).isEqualTo("MeasureKey{metricKey='metricKey', ruleId=1, characteristicId=-6253, developer=null}");
    assertThat(new MeasureKey("metricKey", null, 2, null).toString()).isEqualTo("MeasureKey{metricKey='metricKey', ruleId=-6253, characteristicId=2, developer=null}");
  }
}
