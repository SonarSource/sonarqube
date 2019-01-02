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
package org.sonar.ce.task.projectanalysis.period;

import javax.annotation.CheckForNull;
import javax.annotation.Nullable;

import static com.google.common.base.Preconditions.checkState;

public class PeriodHolderImpl implements PeriodHolder {

  @CheckForNull
  private Period period = null;
  private boolean initialized = false;

  /**
   * Initializes the periods in the holder.
   *
   * @throws IllegalStateException if the holder has already been initialized
   */
  public void setPeriod(@Nullable Period period) {
    checkState(!initialized, "Period have already been initialized");
    this.period = period;
    this.initialized = true;
  }

  @Override
  public boolean hasPeriod() {
    checkHolderIsInitialized();
    return period != null;
  }

  @Override
  public Period getPeriod() {
    checkHolderIsInitialized();
    checkState(period != null, "There is no period. Use hasPeriod() before calling this method");
    return period;
  }

  private void checkHolderIsInitialized() {
    checkState(initialized, "Period have not been initialized yet");
  }

}
