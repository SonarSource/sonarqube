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
import org.sonar.server.qualitygate.EvaluatedQualityGate;

import static java.util.Collections.emptyList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;

public class QualityGateHolderImplTest {

  private static final QualityGate QUALITY_GATE = new QualityGate("4612", "name", emptyList());

  @Test
  public void getQualityGate_throws_ISE_if_QualityGate_not_set() {
    assertThatThrownBy(() -> new QualityGateHolderImpl().getQualityGate())
      .isInstanceOf(IllegalStateException.class);
  }

  @Test
  public void setQualityGate_throws_NPE_if_argument_is_null() {
    assertThatThrownBy(() -> new QualityGateHolderImpl().setQualityGate(null))
      .isInstanceOf(NullPointerException.class);
  }

  @Test
  public void setQualityGate_throws_ISE_if_called_twice() {
    assertThatThrownBy(() -> {
      QualityGateHolderImpl holder = new QualityGateHolderImpl();

      holder.setQualityGate(QUALITY_GATE);
      holder.setQualityGate(QUALITY_GATE);
    })
      .isInstanceOf(IllegalStateException.class);
  }

  @Test
  public void getQualityGate_returns_QualityGate_set_by_setQualityGate() {
    QualityGateHolderImpl holder = new QualityGateHolderImpl();

    holder.setQualityGate(QUALITY_GATE);

    assertThat(holder.getQualityGate()).containsSame(QUALITY_GATE);
  }

  @Test
  public void getEvaluation_throws_ISE_if_QualityGate_not_set() {
    assertThatThrownBy(() -> new QualityGateHolderImpl().getEvaluation())
      .isInstanceOf(IllegalStateException.class);
  }

  @Test
  public void setEvaluation_throws_NPE_if_argument_is_null() {
    assertThatThrownBy(() -> new QualityGateHolderImpl().setEvaluation(null))
      .isInstanceOf(NullPointerException.class);
  }

  @Test
  public void setEvaluation_throws_ISE_if_called_twice() {
    assertThatThrownBy(() -> {
      QualityGateHolderImpl holder = new QualityGateHolderImpl();

      EvaluatedQualityGate evaluation = mock(EvaluatedQualityGate.class);
      holder.setEvaluation(evaluation);
      holder.setEvaluation(evaluation);
    })
      .isInstanceOf(IllegalStateException.class);
  }

  @Test
  public void getEvaluation_returns_QualityGate_set_by_setQualityGate() {
    QualityGateHolderImpl holder = new QualityGateHolderImpl();

    EvaluatedQualityGate evaluation = mock(EvaluatedQualityGate.class);
    holder.setEvaluation(evaluation);

    assertThat(holder.getEvaluation()).containsSame(evaluation);
  }

}
