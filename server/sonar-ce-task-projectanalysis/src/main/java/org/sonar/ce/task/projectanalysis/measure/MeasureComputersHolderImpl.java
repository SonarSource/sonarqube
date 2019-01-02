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

import com.google.common.base.Predicates;
import javax.annotation.CheckForNull;
import org.sonar.ce.task.projectanalysis.api.measurecomputer.MeasureComputerWrapper;
import org.sonar.ce.task.projectanalysis.api.measurecomputer.MeasureComputerWrapper;

import static com.google.common.base.Preconditions.checkState;
import static com.google.common.collect.FluentIterable.from;
import static java.util.Objects.requireNonNull;

public class MeasureComputersHolderImpl implements MutableMeasureComputersHolder {

  @CheckForNull
  private Iterable<MeasureComputerWrapper> measureComputers;

  @Override
  public Iterable<MeasureComputerWrapper> getMeasureComputers() {
    checkState(this.measureComputers != null, "Measure computers have not been initialized yet");
    return measureComputers;
  }

  @Override
  public void setMeasureComputers(Iterable<MeasureComputerWrapper> measureComputers) {
    requireNonNull(measureComputers, "Measure computers cannot be null");
    checkState(this.measureComputers == null, "Measure computers have already been initialized");
    this.measureComputers = from(measureComputers).filter(Predicates.notNull()).toList();
  }
}
