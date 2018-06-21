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
import javax.annotation.Nullable;
import org.junit.rules.ExternalResource;
import org.sonar.server.qualitygate.EvaluatedQualityGate;

import static com.google.common.base.Preconditions.checkState;

public class QualityGateHolderRule extends ExternalResource implements QualityGateHolder {
  @Nullable
  private Optional<QualityGate> qualityGate;
  @Nullable
  private Optional<EvaluatedQualityGate> evaluation;

  public void setQualityGate(@Nullable QualityGate qualityGate) {
    this.qualityGate = Optional.ofNullable(qualityGate);
  }

  @Override
  public Optional<QualityGate> getQualityGate() {
    checkState(qualityGate != null, "Holder has not been initialized");
    return qualityGate;
  }

  public void setEvaluation(@Nullable EvaluatedQualityGate e) {
    this.evaluation = Optional.ofNullable(e);
  }

  @Override
  public Optional<EvaluatedQualityGate> getEvaluation() {
    checkState(evaluation != null, "EvaluatedQualityGate has not been initialized");
    return evaluation;
  }

  @Override
  protected void after() {
    reset();
  }

  public void reset() {
    this.qualityGate = null;
    this.evaluation = null;
  }
}
