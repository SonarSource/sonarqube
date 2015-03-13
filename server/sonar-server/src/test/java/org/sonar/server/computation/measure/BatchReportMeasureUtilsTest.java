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

import org.junit.Test;
import org.sonar.batch.protocol.Constants;
import org.sonar.batch.protocol.output.BatchReport;

import static org.assertj.core.api.Assertions.assertThat;
import static org.sonar.batch.protocol.Constants.MeasureValueType.*;
import static org.sonar.server.computation.measure.BatchReportMeasureUtils.checkMeasure;
import static org.sonar.server.computation.measure.BatchReportMeasureUtils.valueAsDouble;

public class BatchReportMeasureUtilsTest {

  @Test(expected = IllegalStateException.class)
  public void fail_when_no_metric_key() throws Exception {
    BatchReport.Measure measure = BatchReport.Measure.newBuilder()
      .setValueType(STRING)
      .setStringValue("string-value")
      .build();

    checkMeasure(measure);
  }

  @Test(expected = IllegalStateException.class)
  public void fail_when_no_value() throws Exception {
    BatchReport.Measure measure = BatchReport.Measure.newBuilder()
      .setMetricKey("repo:metric-key")
      .build();

    checkMeasure(measure);
  }

  @Test(expected = IllegalStateException.class)
  public void fail_when_no_consistency_between_string_value_and_numerical_value_type() throws Exception {
    BatchReport.Measure measure = BatchReport.Measure.newBuilder()
      .setValueType(DOUBLE)
      .setStringValue("string-value")
      .build();

    checkMeasure(measure);
  }

  @Test(expected = IllegalStateException.class)
  public void fail_when_no_consistence_between_numerical_value_types() throws Exception {
    BatchReport.Measure measure = BatchReport.Measure.newBuilder()
      .setValueType(DOUBLE)
      .setIntValue(3)
      .build();

    checkMeasure(measure);
  }

  @Test
  public void validate_all_value_type_without_exception_thrown() throws Exception {
    checkMeasure(newBuilder().setValueType(STRING).setStringValue("string-value").build());
    checkMeasure(newBuilder().setValueType(DOUBLE).setDoubleValue(1.0d).build());
    checkMeasure(newBuilder().setValueType(Constants.MeasureValueType.INT).setIntValue(1).build());
    checkMeasure(newBuilder().setValueType(Constants.MeasureValueType.LONG).setLongValue(2L).build());
    checkMeasure(newBuilder().setValueType(BOOLEAN).setBooleanValue(true).build());
  }

  @Test
  public void value_type_as_double_correct_for_all_numerical_types() throws Exception {
    assertThat(valueAsDouble(newBuilder().setBooleanValue(true).setValueType(BOOLEAN).build())).isEqualTo(1.0d);
    assertThat(valueAsDouble(newBuilder().setIntValue(2).setValueType(INT).build())).isEqualTo(2.0d);
    assertThat(valueAsDouble(newBuilder().setLongValue(3L).setValueType(LONG).build())).isEqualTo(3.0d);
    assertThat(valueAsDouble(newBuilder().setDoubleValue(4.4d).setValueType(DOUBLE).build())).isEqualTo(4.4d);
  }

  private BatchReport.Measure.Builder newBuilder() {
    return BatchReport.Measure.newBuilder().setMetricKey("repo:metric-key");
  }
}