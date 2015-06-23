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

import java.util.List;
import org.sonar.api.CoreProperties;

/**
 * Repository of periods used to compute differential measures.
 * Here are the steps to retrieve these periods :
 * - Read the 5 period properties ${@link CoreProperties#TIMEMACHINE_PERIOD_PREFIX}
 * - Try to find the matching snapshots from the properties
 * - If a snapshot is found, a new period is added to the repository
 */
public interface PeriodsHolder {

  int MAX_NUMBER_OF_PERIODS = 5;

  /**
   * Return the list of differential periods, ordered by increasing index.
   *
   * @throws IllegalStateException if the periods haven't been initialized
   */
  List<Period> getPeriods();

  /**
   * Finds out whether the holder contains a Period for the specified index.
   *
   * @throws IllegalStateException if the periods haven't been initialized
   * @throws IndexOutOfBoundsException if i is either &lt; 1 or &gt; 5
   */
  boolean hasPeriod(int i);

  /**
   * Retrieves the Period for the specified index from the Holder.
   *
   * @throws IllegalStateException if the periods haven't been initialized
   * @throws IllegalStateException if there is no period for the specified index (see {@link #hasPeriod(int)})
   * @throws IndexOutOfBoundsException if i is either &lt; 1 or &gt; 5
   */
  Period getPeriod(int i);

}
