/*
 * SonarQube
 * Copyright (C) 2009-2024 SonarSource SA
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
package org.sonar.server.common.newcodeperiod;

import org.junit.Test;
import org.sonar.db.newcodeperiod.NewCodePeriodDto;
import org.sonar.db.newcodeperiod.NewCodePeriodType;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

public class CaycUtilsTest {

  @Test
  public void reference_branch_is_compliant() {
    var newCodePeriod = new NewCodePeriodDto()
      .setType(NewCodePeriodType.REFERENCE_BRANCH)
      .setValue("master");
    assertThat(CaycUtils.isNewCodePeriodCompliant(newCodePeriod.getType(), newCodePeriod.getValue())).isTrue();
  }

  @Test
  public void previous_version_is_compliant() {
    var newCodePeriod = new NewCodePeriodDto()
      .setType(NewCodePeriodType.PREVIOUS_VERSION)
      .setValue("1.0");
    assertThat(CaycUtils.isNewCodePeriodCompliant(newCodePeriod.getType(), newCodePeriod.getValue())).isTrue();
  }

  @Test
  public void number_of_days_smaller_than_90_is_compliant() {
    var newCodePeriod = new NewCodePeriodDto()
      .setType(NewCodePeriodType.NUMBER_OF_DAYS)
      .setValue("30");
    assertThat(CaycUtils.isNewCodePeriodCompliant(newCodePeriod.getType(), newCodePeriod.getValue())).isTrue();
  }

  @Test
  public void number_of_days_smaller_than_1_is_not_compliant() {
    var newCodePeriod = new NewCodePeriodDto()
      .setType(NewCodePeriodType.NUMBER_OF_DAYS)
      .setValue("0");
    assertThat(CaycUtils.isNewCodePeriodCompliant(newCodePeriod.getType(), newCodePeriod.getValue())).isFalse();
  }

  @Test
  public void number_of_days_bigger_than_90_is_not_compliant() {
    var newCodePeriod = new NewCodePeriodDto()
      .setType(NewCodePeriodType.NUMBER_OF_DAYS)
      .setValue("91");
    assertThat(CaycUtils.isNewCodePeriodCompliant(newCodePeriod.getType(), newCodePeriod.getValue())).isFalse();
  }

  @Test
  public void specific_analysis_is_compliant() {
    var newCodePeriod = new NewCodePeriodDto()
      .setType(NewCodePeriodType.SPECIFIC_ANALYSIS)
      .setValue("sdfsafsdf");
    assertThat(CaycUtils.isNewCodePeriodCompliant(newCodePeriod.getType(), newCodePeriod.getValue())).isTrue();
  }

  @Test
  public void wrong_number_of_days_format_should_throw_exception() {
    assertThatThrownBy(() -> CaycUtils.isNewCodePeriodCompliant(NewCodePeriodType.NUMBER_OF_DAYS, "abc"))
      .isInstanceOf(IllegalArgumentException.class)
      .hasMessageContaining("Failed to parse number of days: abc");
  }
}