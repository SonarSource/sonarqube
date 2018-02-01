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
package org.sonar.server.platform;

import org.sonar.api.ce.ComputeEngineSide;
import org.sonar.api.server.ServerSide;

@ComputeEngineSide
@ServerSide
public interface WebServer {

  /**
   * WebServer is standalone when property {@link org.sonar.process.ProcessProperties.Property#CLUSTER_ENABLED} is {@code false} or
   * undefined.
   */
  boolean isStandalone();

  /**
   * The startup leader is the first node to be started in a cluster. It's the only one
   * to create and populate datastores.
   * A standard node is automatically marked as "startup leader" when cluster
   * is disabled (default).
   */
  boolean isStartupLeader();

}
