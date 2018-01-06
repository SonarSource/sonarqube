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
package org.sonar.server.platform.monitoring;

public interface DbConnectionSectionMBean {

  /**
   * Is database schema up-to-date or should it be upgraded ?
   */
  String getMigrationStatus();

  /**
   *
   */
  int getPoolActiveConnections();

  /**
   * The maximum number of active connections that can be allocated from this pool at the same time, or negative for no limit.
   */
  int getPoolMaxActiveConnections();

  int getPoolIdleConnections();

  /**
   * The maximum number of connections that can remain idle in the pool, without extra ones being released, or negative for no limit.
   */
  int getPoolMaxIdleConnections();

  /**
   * The minimum number of connections that can remain idle in the pool, without extra ones being created, or zero to create none.
   */
  int getPoolMinIdleConnections();

  /**
   * The initial number of connections that are created when the pool is started.
   */
  int getPoolInitialSize();

  /**
   * The maximum number of milliseconds that the pool will wait
   * (when there are no available connections) for a connection to be returned before throwing an exception, or -1 to wait indefinitely.
   */
  long getPoolMaxWaitMillis();

  /**
   * Flag to remove abandoned connections if they exceed the {@link #getPoolRemoveAbandonedTimeoutSeconds()}.
   */
  boolean getPoolRemoveAbandoned();

  /**
   * Timeout in seconds before an abandoned connection can be removed.
   */
  int getPoolRemoveAbandonedTimeoutSeconds();
}
