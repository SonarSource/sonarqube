/*
 * SonarQube, open source software quality management tool.
 * Copyright (C) 2008-2013 SonarSource
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
package org.sonar.core.persistence;

import java.io.PrintWriter;
import java.sql.Connection;
import java.sql.SQLException;

import javax.sql.DataSource;

import com.codahale.metrics.Timer;
import com.codahale.metrics.Timer.Context;

public class MonitoredDataSource implements DataSource {

  private final DataSource subject;
  private final Timer timer;

  public MonitoredDataSource(DataSource subject, Timer getConnectionTimer) {
    this.subject = subject;
    this.timer = getConnectionTimer;
  }

  public <T> T unwrap(Class<T> paramClass) throws SQLException {
    return subject.unwrap(paramClass);
  }

  public PrintWriter getLogWriter() throws SQLException {
    return subject.getLogWriter();
  }

  public boolean isWrapperFor(Class<?> paramClass) throws SQLException {
    return subject.isWrapperFor(paramClass);
  }

  public Connection getConnection() throws SQLException {
    Context ctx = timer.time();
    try {
      return subject.getConnection();
    } finally {
      ctx.stop();
    }
  }

  public void setLogWriter(PrintWriter paramPrintWriter) throws SQLException {
    subject.setLogWriter(paramPrintWriter);
  }

  public Connection getConnection(String paramString1, String paramString2)
      throws SQLException {
    Context ctx = timer.time();
    try {
      return subject.getConnection(paramString1, paramString2);
    } finally {
      ctx.stop();
    }
  }

  public void setLoginTimeout(int paramInt) throws SQLException {
    subject.setLoginTimeout(paramInt);
  }

  public int getLoginTimeout() throws SQLException {
    return subject.getLoginTimeout();
  }
}
