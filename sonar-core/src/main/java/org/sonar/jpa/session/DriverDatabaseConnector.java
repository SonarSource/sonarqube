package org.sonar.jpa.session;

import org.apache.commons.configuration.Configuration;
import org.apache.commons.lang.StringUtils;
import org.sonar.api.database.DatabaseProperties;

import java.sql.Connection;
import java.sql.Driver;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Properties;

public class DriverDatabaseConnector extends AbstractDatabaseConnector {

  private ClassLoader classloader;

  public DriverDatabaseConnector(Configuration configuration) {
    super(configuration, true);
    this.classloader = getClass().getClassLoader();
  }

  public DriverDatabaseConnector(Configuration configuration, ClassLoader classloader) {
    super(configuration, true);
    this.classloader = classloader;
  }

  public String getDriver() {
    String driver = getConfiguration().getString(DatabaseProperties.PROP_DRIVER);
    if (driver == null) {
      driver = getConfiguration().getString(DatabaseProperties.PROP_DRIVER_DEPRECATED);
    }
    if (driver == null) {
      driver = DatabaseProperties.PROP_DRIVER_DEFAULT_VALUE;
    }
    return driver;
  }

  public String getUrl() {
    return getConfiguration().getString(DatabaseProperties.PROP_URL, DatabaseProperties.PROP_URL_DEFAULT_VALUE);
  }

  public String getUsername() {
    String username = getConfiguration().getString(DatabaseProperties.PROP_USER);
    if (username == null) {
      username = getConfiguration().getString(DatabaseProperties.PROP_USER_DEPRECATED);
    }
    if (username == null) {
      username = DatabaseProperties.PROP_USER_DEFAULT_VALUE;
    }
    return username;
  }

  public String getPassword() {
    return getConfiguration().getString(DatabaseProperties.PROP_PASSWORD, DatabaseProperties.PROP_PASSWORD_DEFAULT_VALUE);
  }

  public Connection getConnection() throws SQLException {
    try {
      /*
        The sonar batch downloads the JDBC driver in a separated classloader.
        This is a well-know problem of java.sql.DriverManager. The workaround
        is to use a proxy.
        See http://stackoverflow.com/questions/288828/how-to-use-a-jdbc-driver-from-an-arbitrary-location
       */
      Driver driver = (Driver)classloader.loadClass(getDriver()).newInstance();
      DriverManager.registerDriver(new DriverProxy(driver));

    } catch (Exception e) {
      SQLException ex = new SQLException("SQL driver not found " + getDriver());
      ex.initCause(e);
      throw ex;
    }
    return DriverManager.getConnection(getUrl(), getUsername(), getPassword());
  }

  @Override
  public void setupEntityManagerFactory(Properties factoryProps) {
    factoryProps.put("hibernate.connection.url", getUrl());
    factoryProps.put("hibernate.connection.driver_class", getDriver());
    factoryProps.put("hibernate.connection.username", getUsername());
    if (StringUtils.isNotEmpty(getPassword())) {
      factoryProps.put("hibernate.connection.password", getPassword());
    }
  }
}

/**
 * A Driver that stands in for another Driver.
 * This is necessary because java.sql.DriverManager
 * examines the Driver class class loader.
 */
final class DriverProxy implements Driver {
  private final Driver target;

  DriverProxy(Driver target) {
    if (target == null) {
      throw new NullPointerException();
    }
    this.target = target;
  }

  public Driver getTarget() {
    return target;
  }

  public boolean acceptsURL(String url) throws SQLException {
    return target.acceptsURL(url);
  }

  public Connection connect(
      String url, Properties info
  ) throws SQLException {
    return target.connect(url, info);
  }

  public int getMajorVersion() {
    return target.getMajorVersion();
  }

  public int getMinorVersion() {
    return target.getMinorVersion();
  }

  public java.sql.DriverPropertyInfo[] getPropertyInfo(
      String url, Properties info
  ) throws SQLException {
    return target.getPropertyInfo(url, info);
  }

  public boolean jdbcCompliant() {
    return target.jdbcCompliant();
  }

  @Override
  public String toString() {
    return "Proxy: " + target;
  }

  @Override
  public int hashCode() {
    return target.hashCode();
  }

  @Override
  public boolean equals(Object obj) {
    if (!(obj instanceof org.sonar.jpa.session.DriverProxy)) {
      return false;
    }
    org.sonar.jpa.session.DriverProxy other = (org.sonar.jpa.session.DriverProxy) obj;
    return this.target.equals(other.target);
  }
}