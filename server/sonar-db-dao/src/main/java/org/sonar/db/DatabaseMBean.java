/*
 * SonarQube
 * Copyright (C) 2009-2025 SonarSource SA
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
package org.sonar.db;

import com.zaxxer.hikari.HikariDataSource;
import com.zaxxer.hikari.HikariPoolMXBean;
import java.util.function.Function;
import org.sonar.process.systeminfo.BaseSectionMBean;

import static java.util.Optional.ofNullable;

public abstract class DatabaseMBean extends BaseSectionMBean {

  private final DbClient dbClient;

  protected DatabaseMBean(DbClient dbClient) {
    this.dbClient = dbClient;
  }

  public int getPoolActiveConnections() {
    return getIntPropertyOrZero(HikariPoolMXBean::getActiveConnections);
  }

  public int getPoolTotalConnections() {
    return getIntPropertyOrZero(HikariPoolMXBean::getTotalConnections);
  }

  public int getPoolIdleConnections() {
    return getIntPropertyOrZero(HikariPoolMXBean::getIdleConnections);
  }

  public int getPoolMaxConnections() {
    return hikariDatasource().getHikariConfigMXBean().getMaximumPoolSize();
  }

  public int getPoolMinIdleConnections() {
    return hikariDatasource().getHikariConfigMXBean().getMinimumIdle();
  }

  public long getPoolMaxLifeTimeMillis() {
    return hikariDatasource().getHikariConfigMXBean().getMaxLifetime();
  }

  public long getPoolMaxWaitMillis() {
    return hikariDatasource().getHikariConfigMXBean().getConnectionTimeout();
  }

  private HikariDataSource hikariDatasource() {
    return (HikariDataSource) dbClient.getDatabase().getDataSource();
  }

  private int getIntPropertyOrZero(Function<HikariPoolMXBean, Integer> function) {
    return ofNullable(hikariDatasource().getHikariPoolMXBean())
        .map(function)
        .orElse(0);
  }

}
