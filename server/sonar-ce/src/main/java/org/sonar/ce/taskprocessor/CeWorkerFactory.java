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
package org.sonar.ce.taskprocessor;

import java.util.Set;

/**
 * A factory that will create the CeWorkerFactory with an UUID
 */
public interface CeWorkerFactory {
  /**
   * Create a new CeWorker object with the specified ordinal.
   * Each {@link CeWorker} returned by this method will have a different UUID from the others.
   * All returned {@link CeWorker} will be returned by {@link #getWorkers()}.
   *
   * @return the CeWorker
   */
  CeWorker create(int ordinal);

  /**
   * @return each {@link CeWorker} object returned by {@link #create}.
   */
  Set<CeWorker> getWorkers();
}
