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
package org.sonar.server.computation.task.projectanalysis.qualitygate;

import org.junit.Test;
import org.sonar.server.computation.task.projectanalysis.measure.Measure;

import static org.assertj.core.api.Assertions.assertThat;

public class EvaluationResultTest {
  @Test(expected = NullPointerException.class)
  public void constructor_throws_NPE_if_Level_arg_is_null() {
    new EvaluationResult(null, 11);
  }

  @Test
  public void verify_getters() {
    String value = "toto";
    Measure.Level level = Measure.Level.OK;

    EvaluationResult evaluationResult = new EvaluationResult(level, value);
    assertThat(evaluationResult.getLevel()).isEqualTo(level);
    assertThat(evaluationResult.getValue()).isEqualTo(value);
  }

  @Test
  public void toString_is_defined() {
    assertThat(new EvaluationResult(Measure.Level.OK, "toto").toString())
        .isEqualTo("EvaluationResult{level=OK, value=toto}");
  }
}
