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
package org.sonar.db.profiling;

import java.io.PrintWriter;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.Collection;
import org.apache.commons.dbcp.BasicDataSource;
import org.sonar.api.utils.log.Logger;
import org.sonar.api.utils.log.Loggers;

public class ProfiledDataSource extends BasicDataSource {

  static final Logger SQL_LOGGER = Loggers.get("sql");

  private final BasicDataSource delegate;
  private ConnectionInterceptor connectionInterceptor;

  public ProfiledDataSource(BasicDataSource delegate, ConnectionInterceptor connectionInterceptor) {
    this.delegate = delegate;
    this.connectionInterceptor = connectionInterceptor;
  }

  public BasicDataSource getDelegate() {
    return delegate;
  }

  public synchronized void setConnectionInterceptor(ConnectionInterceptor ci) {
    this.connectionInterceptor = ci;
  }

  @Override
  public boolean getDefaultAutoCommit() {
    return delegate.getDefaultAutoCommit();
  }

  @Override
  public void setDefaultAutoCommit(boolean defaultAutoCommit) {
    delegate.setDefaultAutoCommit(defaultAutoCommit);
  }

  @Override
  public boolean getDefaultReadOnly() {
    return delegate.getDefaultReadOnly();
  }

  @Override
  public void setDefaultReadOnly(boolean defaultReadOnly) {
    delegate.setDefaultReadOnly(defaultReadOnly);
  }

  @Override
  public int getDefaultTransactionIsolation() {
    return delegate.getDefaultTransactionIsolation();
  }

  @Override
  public void setDefaultTransactionIsolation(int defaultTransactionIsolation) {
    delegate.setDefaultTransactionIsolation(defaultTransactionIsolation);
  }

  @Override
  public String getDefaultCatalog() {
    return delegate.getDefaultCatalog();
  }

  @Override
  public void setDefaultCatalog(String defaultCatalog) {
    delegate.setDefaultCatalog(defaultCatalog);
  }

  @Override
  public synchronized String getDriverClassName() {
    return delegate.getDriverClassName();
  }

  @Override
  public synchronized void setDriverClassName(String driverClassName) {
    delegate.setDriverClassName(driverClassName);
  }

  @Override
  public synchronized ClassLoader getDriverClassLoader() {
    return delegate.getDriverClassLoader();
  }

  @Override
  public synchronized void setDriverClassLoader(ClassLoader driverClassLoader) {
    delegate.setDriverClassLoader(driverClassLoader);
  }

  @Override
  public synchronized int getMaxActive() {
    return delegate.getMaxActive();
  }

  @Override
  public synchronized void setMaxActive(int maxActive) {
    delegate.setMaxActive(maxActive);
  }

  @Override
  public synchronized int getMaxIdle() {
    return delegate.getMaxIdle();
  }

  @Override
  public synchronized void setMaxIdle(int maxIdle) {
    delegate.setMaxIdle(maxIdle);
  }

  @Override
  public synchronized int getMinIdle() {
    return delegate.getMinIdle();
  }

  @Override
  public synchronized void setMinIdle(int minIdle) {
    delegate.setMinIdle(minIdle);
  }

  @Override
  public synchronized int getInitialSize() {
    return delegate.getInitialSize();
  }

  @Override
  public synchronized void setInitialSize(int initialSize) {
    delegate.setInitialSize(initialSize);
  }

  @Override
  public synchronized long getMaxWait() {
    return delegate.getMaxWait();
  }

  @Override
  public synchronized void setMaxWait(long maxWait) {
    delegate.setMaxWait(maxWait);
  }

  @Override
  public synchronized boolean isPoolPreparedStatements() {
    return delegate.isPoolPreparedStatements();
  }

  @Override
  public synchronized void setPoolPreparedStatements(boolean poolingStatements) {
    delegate.setPoolPreparedStatements(poolingStatements);
  }

  @Override
  public synchronized int getMaxOpenPreparedStatements() {
    return delegate.getMaxOpenPreparedStatements();
  }

  @Override
  public synchronized void setMaxOpenPreparedStatements(int maxOpenStatements) {
    delegate.setMaxOpenPreparedStatements(maxOpenStatements);
  }

  @Override
  public synchronized boolean getTestOnBorrow() {
    return delegate.getTestOnBorrow();
  }

  @Override
  public synchronized void setTestOnBorrow(boolean testOnBorrow) {
    delegate.setTestOnBorrow(testOnBorrow);
  }

  @Override
  public synchronized boolean getTestOnReturn() {
    return delegate.getTestOnReturn();
  }

  @Override
  public synchronized void setTestOnReturn(boolean testOnReturn) {
    delegate.setTestOnReturn(testOnReturn);
  }

  @Override
  public synchronized long getTimeBetweenEvictionRunsMillis() {
    return delegate.getTimeBetweenEvictionRunsMillis();
  }

  @Override
  public synchronized void setTimeBetweenEvictionRunsMillis(long timeBetweenEvictionRunsMillis) {
    delegate.setTimeBetweenEvictionRunsMillis(timeBetweenEvictionRunsMillis);
  }

  @Override
  public synchronized int getNumTestsPerEvictionRun() {
    return delegate.getNumTestsPerEvictionRun();
  }

  @Override
  public synchronized void setNumTestsPerEvictionRun(int numTestsPerEvictionRun) {
    delegate.setNumTestsPerEvictionRun(numTestsPerEvictionRun);
  }

  @Override
  public synchronized long getMinEvictableIdleTimeMillis() {
    return delegate.getMinEvictableIdleTimeMillis();
  }

  @Override
  public synchronized void setMinEvictableIdleTimeMillis(long minEvictableIdleTimeMillis) {
    delegate.setMinEvictableIdleTimeMillis(minEvictableIdleTimeMillis);
  }

  @Override
  public synchronized boolean getTestWhileIdle() {
    return delegate.getTestWhileIdle();
  }

  @Override
  public synchronized void setTestWhileIdle(boolean testWhileIdle) {
    delegate.setTestWhileIdle(testWhileIdle);
  }

  @Override
  public synchronized int getNumActive() {
    return delegate.getNumActive();
  }

  @Override
  public synchronized int getNumIdle() {
    return delegate.getNumIdle();
  }

  @Override
  public String getPassword() {
    return delegate.getPassword();
  }

  @Override
  public void setPassword(String password) {
    delegate.setPassword(password);
  }

  @Override
  public synchronized String getUrl() {
    return delegate.getUrl();
  }

  @Override
  public synchronized void setUrl(String url) {
    delegate.setUrl(url);
  }

  @Override
  public String getUsername() {
    return delegate.getUsername();
  }

  @Override
  public void setUsername(String username) {
    delegate.setUsername(username);
  }

  @Override
  public String getValidationQuery() {
    return delegate.getValidationQuery();
  }

  @Override
  public void setValidationQuery(String validationQuery) {
    delegate.setValidationQuery(validationQuery);
  }

  @Override
  public int getValidationQueryTimeout() {
    return delegate.getValidationQueryTimeout();
  }

  @Override
  public void setValidationQueryTimeout(int timeout) {
    delegate.setValidationQueryTimeout(timeout);
  }

  @Override
  public Collection getConnectionInitSqls() {
    return delegate.getConnectionInitSqls();
  }

  @Override
  public void setConnectionInitSqls(Collection connectionInitSqls) {
    delegate.setConnectionInitSqls(connectionInitSqls);
  }

  @Override
  public synchronized boolean isAccessToUnderlyingConnectionAllowed() {
    return delegate.isAccessToUnderlyingConnectionAllowed();
  }

  @Override
  public synchronized void setAccessToUnderlyingConnectionAllowed(boolean allow) {
    delegate.setAccessToUnderlyingConnectionAllowed(allow);
  }

  @Override
  public Connection getConnection() throws SQLException {
    return connectionInterceptor.getConnection(delegate);
  }

  @Override
  public Connection getConnection(String login, String password) throws SQLException {
    return connectionInterceptor.getConnection(this, login, password);
  }

  @Override
  public int getLoginTimeout() throws SQLException {
    return delegate.getLoginTimeout();
  }

  @Override
  public java.util.logging.Logger getParentLogger() {
    return java.util.logging.Logger.getLogger(getClass().getName());
  }

  @Override
  public PrintWriter getLogWriter() throws SQLException {
    return delegate.getLogWriter();
  }

  @Override
  public void setLoginTimeout(int loginTimeout) throws SQLException {
    delegate.setLoginTimeout(loginTimeout);
  }

  @Override
  public void setLogWriter(PrintWriter logWriter) throws SQLException {
    delegate.setLogWriter(logWriter);
  }

  @Override
  public boolean getRemoveAbandoned() {
    return delegate.getRemoveAbandoned();
  }

  @Override
  public void setRemoveAbandoned(boolean removeAbandoned) {
    delegate.setRemoveAbandoned(removeAbandoned);
  }

  @Override
  public int getRemoveAbandonedTimeout() {
    return delegate.getRemoveAbandonedTimeout();
  }

  @Override
  public void setRemoveAbandonedTimeout(int removeAbandonedTimeout) {
    delegate.setRemoveAbandonedTimeout(removeAbandonedTimeout);
  }

  @Override
  public boolean getLogAbandoned() {
    return delegate.getLogAbandoned();
  }

  @Override
  public void setLogAbandoned(boolean logAbandoned) {
    delegate.setLogAbandoned(logAbandoned);
  }

  @Override
  public void addConnectionProperty(String name, String value) {
    delegate.addConnectionProperty(name, value);
  }

  @Override
  public void removeConnectionProperty(String name) {
    delegate.removeConnectionProperty(name);
  }

  @Override
  public void setConnectionProperties(String connectionProperties) {
    delegate.setConnectionProperties(connectionProperties);
  }

  @Override
  public synchronized void close() throws SQLException {
    delegate.close();
  }

  @Override
  public synchronized boolean isClosed() {
    return delegate.isClosed();
  }

  @Override
  public boolean isWrapperFor(Class<?> iface) throws SQLException {
    return delegate.isWrapperFor(iface);
  }

  @Override
  public <T> T unwrap(Class<T> iface) throws SQLException {
    return delegate.unwrap(iface);
  }
}
