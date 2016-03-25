/*
 * SonarQube
 * Copyright (C) 2009-2016 SonarSource SA
 * mailto:contact AT sonarsource DOT com
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
package org.sonar.server.computation.monitoring;

import org.apache.commons.dbcp.BasicDataSource;
import org.picocontainer.Startable;
import org.sonar.db.DbClient;
import org.sonar.process.jmx.CeDatabaseMBean;
import org.sonar.process.jmx.Jmx;

public class CeDatabaseMBeanImpl implements CeDatabaseMBean, Startable {
  private final DbClient dbClient;

  public CeDatabaseMBeanImpl(DbClient dbClient) {
    this.dbClient = dbClient;
  }

  @Override
  public void start() {
    Jmx.register(OBJECT_NAME, this);
  }

  /**
   * Unregister, if needed
   */
  @Override
  public void stop() {
    Jmx.unregister(OBJECT_NAME);
  }

  @Override
  public int getPoolActiveConnections() {
    return commonsDbcp().getNumActive();
  }

  @Override
  public int getPoolMaxActiveConnections() {
    return commonsDbcp().getMaxActive();
  }

  @Override
  public int getPoolIdleConnections() {
    return commonsDbcp().getNumIdle();
  }

  @Override
  public int getPoolMaxIdleConnections() {
    return commonsDbcp().getMaxIdle();
  }

  @Override
  public int getPoolMinIdleConnections() {
    return commonsDbcp().getMinIdle();
  }

  @Override
  public int getPoolInitialSize() {
    return commonsDbcp().getInitialSize();
  }

  @Override
  public long getPoolMaxWaitMillis() {
    return commonsDbcp().getMaxWait();
  }

  @Override
  public boolean getPoolRemoveAbandoned() {
    return commonsDbcp().getRemoveAbandoned();
  }

  @Override
  public int getPoolRemoveAbandonedTimeoutSeconds() {
    return commonsDbcp().getRemoveAbandonedTimeout();
  }

  private BasicDataSource commonsDbcp() {
    return (BasicDataSource) dbClient.getDatabase().getDataSource();
  }

}
