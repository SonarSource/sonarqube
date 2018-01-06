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

import com.google.common.collect.ImmutableMap;
import java.util.Map;
import javax.annotation.CheckForNull;

import static com.google.common.base.Preconditions.checkState;
import static java.util.Objects.requireNonNull;

public class QualityGateStatusHolderImpl implements MutableQualityGateStatusHolder {
  @CheckForNull
  private QualityGateStatus status;
  @CheckForNull
  private Map<Condition, ConditionStatus> statusPerCondition;

  @Override
  public QualityGateStatus getStatus() {
    checkInitialized();

    return status;
  }

  @Override
  public Map<Condition, ConditionStatus> getStatusPerConditions() {
    checkInitialized();

    return statusPerCondition;
  }

  private void checkInitialized() {
    checkState(status != null, "Quality gate status has not been set yet");
  }

  @Override
  public void setStatus(QualityGateStatus globalStatus, Map<Condition, ConditionStatus> statusPerCondition) {
    checkState(status == null, "Quality gate status has already been set in the holder");
    requireNonNull(globalStatus, "global status can not be null");
    requireNonNull(statusPerCondition, "status per condition can not be null");

    this.status = globalStatus;
    this.statusPerCondition = ImmutableMap.copyOf(statusPerCondition);
  }
}
