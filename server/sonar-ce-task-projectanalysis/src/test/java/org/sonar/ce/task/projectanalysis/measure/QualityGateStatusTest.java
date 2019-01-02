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

import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class QualityGateStatusTest {

  private static final String SOME_TEXT = "some text";

  @Test(expected = NullPointerException.class)
  public void one_arg_constructor_throws_NPE_if_Level_arg_is_null() {
    new QualityGateStatus(null);
  }

  @Test(expected = NullPointerException.class)
  public void two_args_constructor_throws_NPE_if_Level_arg_is_null() {
    new QualityGateStatus(null, SOME_TEXT);
  }

  @Test
  public void one_arg_constructor_sets_a_null_text() {
    QualityGateStatus qualityGateStatus = new QualityGateStatus(Measure.Level.OK);

    assertThat(qualityGateStatus.getStatus()).isEqualTo(Measure.Level.OK);
    assertThat(qualityGateStatus.getText()).isNull();
  }

  @Test
  public void two_args_constructor_sets_text() {
    QualityGateStatus qualityGateStatus = new QualityGateStatus(Measure.Level.OK, SOME_TEXT);

    assertThat(qualityGateStatus.getStatus()).isEqualTo(Measure.Level.OK);
    assertThat(qualityGateStatus.getText()).isEqualTo(SOME_TEXT);

    assertThat(new QualityGateStatus(Measure.Level.OK, null).getText()).isNull();
  }

  @Test
  public void two_args_constructor_supports_null_text_arg() {
    assertThat(new QualityGateStatus(Measure.Level.OK, null).getText()).isNull();
  }

  @Test
  public void verify_equals() {
    for (Measure.Level level : Measure.Level.values()) {
      QualityGateStatus status = new QualityGateStatus(level, null);
      assertThat(status).isEqualTo(status);
      assertThat(status).isEqualTo(new QualityGateStatus(level, null));
      assertThat(status).isNotEqualTo(new QualityGateStatus(level, "bar"));
      assertThat(status).isNotEqualTo(new QualityGateStatus(level, ""));
      assertThat(status).isNotEqualTo(null);
    }
  }

  @Test
  public void verify_toString() {
    assertThat(new QualityGateStatus(Measure.Level.OK, "foo").toString()).isEqualTo("QualityGateStatus{status=OK, text=foo}");
  }
}
