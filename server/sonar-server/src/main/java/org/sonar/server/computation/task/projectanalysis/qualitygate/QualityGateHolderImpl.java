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

import java.util.Optional;
import org.sonar.server.qualitygate.EvaluatedQualityGate;

import static com.google.common.base.Preconditions.checkState;
import static java.util.Objects.requireNonNull;

public class QualityGateHolderImpl implements MutableQualityGateHolder {
  private QualityGate qualityGate;
  private EvaluatedQualityGate evaluation;

  @Override
  public void setQualityGate(QualityGate g) {
    // fail fast
    requireNonNull(g);
    checkState(qualityGate == null, "QualityGateHolder can be initialized only once");

    this.qualityGate = g;
  }

  @Override
  public Optional<QualityGate> getQualityGate() {
    checkState(qualityGate != null, "QualityGate has not been set yet");
    return Optional.of(qualityGate);
  }

  @Override
  public void setEvaluation(EvaluatedQualityGate g) {
    // fail fast
    requireNonNull(g);
    checkState(evaluation == null, "QualityGateHolder evaluation can be initialized only once");

    this.evaluation = g;
  }

  @Override
  public Optional<EvaluatedQualityGate> getEvaluation() {
    checkState(evaluation != null, "Evaluation of QualityGate has not been set yet");
    return Optional.of(evaluation);
  }
}
