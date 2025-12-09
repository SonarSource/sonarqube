/*
 * SonarQube
 * Copyright (C) 2009-2025 SonarSource Sàrl
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

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariConfigMXBean;
import com.zaxxer.hikari.HikariDataSource;
import com.zaxxer.hikari.HikariPoolMXBean;
import com.zaxxer.hikari.metrics.MetricsTrackerFactory;
import java.io.PrintWriter;
import java.sql.Connection;
import java.sql.ConnectionBuilder;
import java.sql.SQLException;
import java.sql.SQLFeatureNotSupportedException;
import java.sql.ShardingKeyBuilder;
import java.util.Properties;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadFactory;
import javax.sql.DataSource;
import org.sonar.api.utils.log.Logger;
import org.sonar.api.utils.log.Loggers;

public class ProfiledDataSource extends HikariDataSource {

  static final Logger SQL_LOGGER = Loggers.get("sql");

  private final HikariDataSource delegate;
  private ConnectionInterceptor connectionInterceptor;

  public ProfiledDataSource(HikariDataSource delegate, ConnectionInterceptor connectionInterceptor) {
    this.delegate = delegate;
    this.connectionInterceptor = connectionInterceptor;
  }

  public HikariDataSource getDelegate() {
    return delegate;
  }

  public synchronized void setConnectionInterceptor(ConnectionInterceptor ci) {
    this.connectionInterceptor = ci;
  }

  @Override
  public boolean isAutoCommit() {
    return delegate.isAutoCommit();
  }

  @Override
  public boolean isReadOnly() {
    return delegate.isReadOnly();
  }

  @Override
  public String getTransactionIsolation() {
    return delegate.getTransactionIsolation();
  }

  @Override
  public void setTransactionIsolation(String defaultTransactionIsolation) {
    delegate.setTransactionIsolation(defaultTransactionIsolation);
  }

  @Override
  public String getCatalog() {
    return delegate.getCatalog();
  }

  @Override
  public void setCatalog(String defaultCatalog) {
    delegate.setCatalog(defaultCatalog);
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
  public int getMaximumPoolSize() {
    return delegate.getMaximumPoolSize();
  }

  @Override
  public void setMaximumPoolSize(int maxActive) {
    delegate.setMaximumPoolSize(maxActive);
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
  public PrintWriter getLogWriter() throws SQLException {
    return delegate.getLogWriter();
  }

  @Override
  public void setLogWriter(PrintWriter out) throws SQLException {
    delegate.setLogWriter(out);
  }

  @Override
  public void setLoginTimeout(int seconds) throws SQLException {
    delegate.setLoginTimeout(seconds);
  }

  @Override
  public int getLoginTimeout() throws SQLException {
    return delegate.getLoginTimeout();
  }

  @Override
  public java.util.logging.Logger getParentLogger() throws SQLFeatureNotSupportedException {
    return delegate.getParentLogger();
  }

  @Override
  public <T> T unwrap(Class<T> iface) throws SQLException {
    return delegate.unwrap(iface);
  }

  @Override
  public boolean isWrapperFor(Class<?> iface) throws SQLException {
    return delegate.isWrapperFor(iface);
  }

  @Override
  public void setMetricRegistry(Object metricRegistry) {
    delegate.setMetricRegistry(metricRegistry);
  }

  @Override
  public void setMetricsTrackerFactory(MetricsTrackerFactory metricsTrackerFactory) {
    delegate.setMetricsTrackerFactory(metricsTrackerFactory);
  }

  @Override
  public void setHealthCheckRegistry(Object healthCheckRegistry) {
    delegate.setHealthCheckRegistry(healthCheckRegistry);
  }

  @Override
  public boolean isRunning() {
    return delegate.isRunning();
  }

  @Override
  public HikariPoolMXBean getHikariPoolMXBean() {
    return delegate.getHikariPoolMXBean();
  }

  @Override
  public HikariConfigMXBean getHikariConfigMXBean() {
    return delegate.getHikariConfigMXBean();
  }

  @Override
  public void evictConnection(Connection connection) {
    delegate.evictConnection(connection);
  }

  @Override
  public void close() {
    delegate.close();
  }

  @Override
  public boolean isClosed() {
    return delegate.isClosed();
  }

  @Override
  public String toString() {
    return delegate.toString();
  }

  @Override
  public long getConnectionTimeout() {
    return delegate.getConnectionTimeout();
  }

  @Override
  public void setConnectionTimeout(long connectionTimeoutMs) {
    delegate.setConnectionTimeout(connectionTimeoutMs);
  }

  @Override
  public long getIdleTimeout() {
    return delegate.getIdleTimeout();
  }

  @Override
  public void setIdleTimeout(long idleTimeoutMs) {
    delegate.setIdleTimeout(idleTimeoutMs);
  }

  @Override
  public long getLeakDetectionThreshold() {
    return delegate.getLeakDetectionThreshold();
  }

  @Override
  public void setLeakDetectionThreshold(long leakDetectionThresholdMs) {
    delegate.setLeakDetectionThreshold(leakDetectionThresholdMs);
  }

  @Override
  public long getMaxLifetime() {
    return delegate.getMaxLifetime();
  }

  @Override
  public void setMaxLifetime(long maxLifetimeMs) {
    delegate.setMaxLifetime(maxLifetimeMs);
  }

  @Override
  public int getMinimumIdle() {
    return delegate.getMinimumIdle();
  }

  @Override
  public void setMinimumIdle(int minIdle) {
    delegate.setMinimumIdle(minIdle);
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
  public String getUsername() {
    return delegate.getUsername();
  }

  @Override
  public void setUsername(String username) {
    delegate.setUsername(username);
  }

  @Override
  public long getValidationTimeout() {
    return delegate.getValidationTimeout();
  }

  @Override
  public void setValidationTimeout(long validationTimeoutMs) {
    delegate.setValidationTimeout(validationTimeoutMs);
  }

  @Override
  public String getConnectionTestQuery() {
    return delegate.getConnectionTestQuery();
  }

  @Override
  public void setConnectionTestQuery(String connectionTestQuery) {
    delegate.setConnectionTestQuery(connectionTestQuery);
  }

  @Override
  public String getConnectionInitSql() {
    return delegate.getConnectionInitSql();
  }

  @Override
  public void setConnectionInitSql(String connectionInitSql) {
    delegate.setConnectionInitSql(connectionInitSql);
  }

  @Override
  public DataSource getDataSource() {
    return delegate.getDataSource();
  }

  @Override
  public void setDataSource(DataSource dataSource) {
    delegate.setDataSource(dataSource);
  }

  @Override
  public String getDataSourceClassName() {
    return delegate.getDataSourceClassName();
  }

  @Override
  public void setDataSourceClassName(String className) {
    delegate.setDataSourceClassName(className);
  }

  @Override
  public void addDataSourceProperty(String propertyName, Object value) {
    delegate.addDataSourceProperty(propertyName, value);
  }

  @Override
  public String getDataSourceJNDI() {
    return delegate.getDataSourceJNDI();
  }

  @Override
  public void setDataSourceJNDI(String jndiDataSource) {
    delegate.setDataSourceJNDI(jndiDataSource);
  }

  @Override
  public Properties getDataSourceProperties() {
    return delegate.getDataSourceProperties();
  }

  @Override
  public void setDataSourceProperties(Properties dsProperties) {
    delegate.setDataSourceProperties(dsProperties);
  }

  @Override
  public String getJdbcUrl() {
    return delegate.getJdbcUrl();
  }

  @Override
  public void setJdbcUrl(String jdbcUrl) {
    delegate.setJdbcUrl(jdbcUrl);
  }

  @Override
  public void setAutoCommit(boolean isAutoCommit) {
    delegate.setAutoCommit(isAutoCommit);
  }

  @Override
  public boolean isAllowPoolSuspension() {
    return delegate.isAllowPoolSuspension();
  }

  @Override
  public void setAllowPoolSuspension(boolean isAllowPoolSuspension) {
    delegate.setAllowPoolSuspension(isAllowPoolSuspension);
  }

  @Override
  public long getInitializationFailTimeout() {
    return delegate.getInitializationFailTimeout();
  }

  @Override
  public void setInitializationFailTimeout(long initializationFailTimeout) {
    delegate.setInitializationFailTimeout(initializationFailTimeout);
  }

  @Override
  public boolean isIsolateInternalQueries() {
    return delegate.isIsolateInternalQueries();
  }

  @Override
  public void setIsolateInternalQueries(boolean isolate) {
    delegate.setIsolateInternalQueries(isolate);
  }

  @Override
  public MetricsTrackerFactory getMetricsTrackerFactory() {
    return delegate.getMetricsTrackerFactory();
  }

  @Override
  public Object getMetricRegistry() {
    return delegate.getMetricRegistry();
  }

  @Override
  public Object getHealthCheckRegistry() {
    return delegate.getHealthCheckRegistry();
  }

  @Override
  public Properties getHealthCheckProperties() {
    return delegate.getHealthCheckProperties();
  }

  @Override
  public void setHealthCheckProperties(Properties healthCheckProperties) {
    delegate.setHealthCheckProperties(healthCheckProperties);
  }

  @Override
  public void addHealthCheckProperty(String key, String value) {
    delegate.addHealthCheckProperty(key, value);
  }

  @Override
  public long getKeepaliveTime() {
    return delegate.getKeepaliveTime();
  }

  @Override
  public void setKeepaliveTime(long keepaliveTimeMs) {
    delegate.setKeepaliveTime(keepaliveTimeMs);
  }

  @Override
  public void setReadOnly(boolean readOnly) {
    delegate.setReadOnly(readOnly);
  }

  @Override
  public boolean isRegisterMbeans() {
    return delegate.isRegisterMbeans();
  }

  @Override
  public void setRegisterMbeans(boolean register) {
    delegate.setRegisterMbeans(register);
  }

  @Override
  public String getPoolName() {
    return delegate.getPoolName();
  }

  @Override
  public void setPoolName(String poolName) {
    delegate.setPoolName(poolName);
  }

  @Override
  public ScheduledExecutorService getScheduledExecutor() {
    return delegate.getScheduledExecutor();
  }

  @Override
  public void setScheduledExecutor(ScheduledExecutorService executor) {
    delegate.setScheduledExecutor(executor);
  }

  @Override
  public String getSchema() {
    return delegate.getSchema();
  }

  @Override
  public void setSchema(String schema) {
    delegate.setSchema(schema);
  }

  @Override
  public String getExceptionOverrideClassName() {
    return delegate.getExceptionOverrideClassName();
  }

  @Override
  public void setExceptionOverrideClassName(String exceptionOverrideClassName) {
    delegate.setExceptionOverrideClassName(exceptionOverrideClassName);
  }

  @Override
  public ThreadFactory getThreadFactory() {
    return delegate.getThreadFactory();
  }

  @Override
  public void setThreadFactory(ThreadFactory threadFactory) {
    delegate.setThreadFactory(threadFactory);
  }

  @Override
  public void copyStateTo(HikariConfig other) {
    delegate.copyStateTo(other);
  }

  @Override
  public void validate() {
    delegate.validate();
  }

  @Override
  public ConnectionBuilder createConnectionBuilder() throws SQLException {
    return delegate.createConnectionBuilder();
  }

  @Override
  public ShardingKeyBuilder createShardingKeyBuilder() throws SQLException {
    return delegate.createShardingKeyBuilder();
  }


}
