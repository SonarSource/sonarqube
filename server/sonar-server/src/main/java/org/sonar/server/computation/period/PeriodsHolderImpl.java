/*
 * SonarQube, open source software quality management tool.
 * Copyright (C) 2008-2014 SonarSource
 * mailto:contact AT sonarsource DOT com
 *
 * SonarQube is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * SonarQube is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */

package org.sonar.server.computation.period;

import com.google.common.base.Preconditions;
import java.util.ArrayList;
import java.util.List;

public class PeriodsHolderImpl implements PeriodsHolder {

  private boolean isPeriodsInitialized = false;
  private List<Period> periods = new ArrayList<>();

  public void setPeriods(List<Period> periods) {
    this.periods = periods;
    isPeriodsInitialized = true;
  }

  @Override
  public List<Period> getPeriods() {
    Preconditions.checkArgument(isPeriodsInitialized, "Periods have not been initialized yet");
    return periods;
  }

}
