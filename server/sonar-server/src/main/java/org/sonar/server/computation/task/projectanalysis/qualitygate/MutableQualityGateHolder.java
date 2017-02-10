/*
 * SonarQube
 * Copyright (C) 2009-2017 SonarSource SA
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

import org.sonar.server.computation.task.projectanalysis.component.Component;

public interface MutableQualityGateHolder extends QualityGateHolder {
  /**
   * Sets the quality gate.
   * Settings a quality gate more than once is not allowed and it can never be set to {@code null}.
   *
   * @param qualityGate a {@link Component}, can not be {@code null}
   *
   * @throws NullPointerException if {@code qualityGate} is {@code null}
   * @throws IllegalStateException if the holder has already been initialized
   */
  void setQualityGate(QualityGate qualityGate);

  /**
   * Sets that there is no quality gate for the project of the currently processed {@link ReportQueue.Item}.
   *
   * @throws IllegalStateException if the holder has already been initialized
   */
  void setNoQualityGate();
}
