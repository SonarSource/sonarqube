/*
 * SonarQube
 * Copyright (C) 2009-2025 SonarSource SA
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
package org.sonar.ce.task.projectanalysis.qualitygate;

import com.tngtech.java.junit.dataprovider.DataProvider;
import com.tngtech.java.junit.dataprovider.DataProviderRunner;
import com.tngtech.java.junit.dataprovider.UseDataProvider;
import org.junit.Test;
import org.junit.runner.RunWith;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.sonar.ce.task.projectanalysis.qualitygate.ConditionStatus.EvaluationStatus.NO_VALUE;
import static org.sonar.ce.task.projectanalysis.qualitygate.ConditionStatus.EvaluationStatus.OK;
import static org.sonar.ce.task.projectanalysis.qualitygate.ConditionStatus.EvaluationStatus.values;

@RunWith(DataProviderRunner.class)
public class ConditionStatusTest {
  private static final String SOME_VALUE = "value";


  @Test
  public void create_throws_NPE_if_status_argument_is_null() {
    assertThatThrownBy(() -> ConditionStatus.create(null, SOME_VALUE))
      .isInstanceOf(NullPointerException.class)
      .hasMessage("status can not be null");
  }

  @Test
  public void create_throws_IAE_if_status_argument_is_NO_VALUE() {
    assertThatThrownBy(() -> ConditionStatus.create(NO_VALUE, SOME_VALUE))
      .isInstanceOf(IllegalArgumentException.class)
      .hasMessage("EvaluationStatus 'NO_VALUE' can not be used with this method, use constant ConditionStatus.NO_VALUE_STATUS instead.");
  }

  @Test
  @UseDataProvider("allStatusesButNO_VALUE")
  public void create_throws_NPE_if_value_is_null_and_status_argument_is_not_NO_VALUE(ConditionStatus.EvaluationStatus status) {
    assertThatThrownBy(() -> ConditionStatus.create(status, null))
      .isInstanceOf(NullPointerException.class)
      .hasMessage("value can not be null");
  }

  @Test
  public void verify_getters() {
    ConditionStatus underTest = ConditionStatus.create(OK, SOME_VALUE);

    assertThat(underTest.getStatus()).isEqualTo(OK);
    assertThat(underTest.getValue()).isEqualTo(SOME_VALUE);
  }

  @Test
  public void verify_toString() {
    assertThat(ConditionStatus.create(OK, SOME_VALUE)).hasToString("ConditionStatus{status=OK, value='value'}");
    assertThat(ConditionStatus.NO_VALUE_STATUS).hasToString("ConditionStatus{status=NO_VALUE, value='null'}");
  }

  @Test
  public void constant_NO_VALUE_STATUS_has_status_NO_VALUE_and_null_value() {
    assertThat(ConditionStatus.NO_VALUE_STATUS.getStatus()).isEqualTo(NO_VALUE);
    assertThat(ConditionStatus.NO_VALUE_STATUS.getValue()).isNull();
  }

  @DataProvider
  public static Object[][] allStatusesButNO_VALUE() {
    Object[][] res = new Object[values().length - 1][1];
    int i = 0;
    for (ConditionStatus.EvaluationStatus status : values()) {
      if (status != NO_VALUE) {
        res[i][0] = status;
        i++;
      }
    }
    return res;
  }
}
