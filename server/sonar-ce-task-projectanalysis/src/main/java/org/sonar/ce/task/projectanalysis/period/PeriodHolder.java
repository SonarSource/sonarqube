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

/**
 * Repository of period used to compute differential measures.
 * Here are the steps to retrieve the period :
 * - Read the period property ${@link org.sonar.core.config.CorePropertyDefinitions#LEAK_PERIOD}
 * - Try to find the matching snapshots from the property
 * - If a snapshot is found, the period is added to the repository
 */
public interface PeriodHolder {

  /**
   * Finds out whether the holder contains a Period
   *
   * @throws IllegalStateException if the periods haven't been initialized
   */
  boolean hasPeriod();

  /**
   * Retrieve the period from the Holder.
   *
   * @throws IllegalStateException if the period hasn't been initialized
   * @throws IllegalStateException if there is no period
   */
  Period getPeriod();

}
