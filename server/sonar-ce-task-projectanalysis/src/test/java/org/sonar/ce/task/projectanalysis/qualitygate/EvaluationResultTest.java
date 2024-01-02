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
package org.sonar.ce.task.projectanalysis.qualitygate;

import org.junit.Test;
import org.sonar.ce.task.projectanalysis.measure.Measure;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

public class EvaluationResultTest {
  @Test
  public void constructor_throws_NPE_if_Level_arg_is_null() {
    assertThatThrownBy(() -> new EvaluationResult(null, 11))
      .isInstanceOf(NullPointerException.class);
  }

  @Test
  public void verify_getters() {
    String value = "toto";
    Measure.Level level = Measure.Level.OK;

    EvaluationResult evaluationResult = new EvaluationResult(level, value);
    assertThat(evaluationResult.level()).isEqualTo(level);
    assertThat(evaluationResult.value()).isEqualTo(value);
  }

  @Test
  public void toString_is_defined() {
    assertThat(new EvaluationResult(Measure.Level.OK, "toto"))
      .hasToString("EvaluationResult{level=OK, value=toto}");
  }
}
