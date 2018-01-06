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
package org.sonar.ce;

import java.util.Set;
import java.util.concurrent.locks.Lock;

/**
 * CeDistributedInformation is the interface to be implemented in order
 * to implement information shared by all CE nodes
 */
public interface CeDistributedInformation {
  Set<String> getWorkerUUIDs();

  /**
   * This method must be called once the workers of the current Compute Engine node
   * are up so that they are shared with other Compute Engine nodes
   */
  void broadcastWorkerUUIDs();

  /**
   * Acquire a lock among all the Compute Engines
   */
  Lock acquireCleanJobLock();
}
