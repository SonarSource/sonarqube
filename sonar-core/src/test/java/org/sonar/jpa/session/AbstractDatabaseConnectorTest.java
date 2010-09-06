/*
 * Sonar, open source software quality management tool.
 * Copyright (C) 2009 SonarSource SA
 * mailto:contact AT sonarsource DOT com
 *
 * Sonar is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * Sonar is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with Sonar; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02
 */
package org.sonar.jpa.session;

import org.apache.commons.configuration.Configuration;
import org.apache.commons.configuration.PropertiesConfiguration;
import org.hamcrest.Matchers;
import org.junit.Test;
import org.mockito.Mockito;
import org.sonar.api.database.DatabaseProperties;
import org.sonar.jpa.dialect.HsqlDb;
import org.sonar.jpa.dialect.Oracle;

import javax.persistence.EntityManagerFactory;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.SQLException;
import java.util.Properties;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;

public class AbstractDatabaseConnectorTest {

  @Test
  public void autodetectDialectWhenNotExcplicitlyDefined() {
    MemoryDatabaseConnector connector = new MemoryDatabaseConnector();
    connector.start();
    assertEquals(HsqlDb.class, connector.getDialect().getClass());
    connector.stop();
  }

  @Test
  public void useConfiguredDialectByDefault() {
    Configuration conf = MemoryDatabaseConnector.getInMemoryConfiguration(false);
    conf.setProperty(DatabaseProperties.PROP_DIALECT, DatabaseProperties.DIALECT_ORACLE);

    TestDatabaseConnector connector = new TestDatabaseConnector(conf);
    connector.start();
    assertEquals(Oracle.class, connector.getDialect().getClass());
    connector.stop();
  }

  @Test
  public void getHibernateProperties() {
    PropertiesConfiguration conf = new PropertiesConfiguration();
    conf.setProperty("sonar.foo", "foo value");

    // all properties prefixed by sonar.hibernate are propagated to hibernate configuration (the prefix "sonar." is removed)
    conf.setProperty("sonar.hibernate.foo", "hibernate.foo value");

    // hardcoded property. Should be replaced by sonar.hibernate.hbm2ddl.auto
    conf.setProperty("sonar.jdbc.hibernate.hbm2ddl", "hibernate.hbm2ddl value");

    // the dialect is mandatory if the JDBC url is not set
    conf.setProperty("sonar.jdbc.dialect", DatabaseProperties.DIALECT_ORACLE);

    AbstractDatabaseConnector connector = new TestDatabaseConnector(conf);
    connector.start();

    Properties hibernateProps = connector.getHibernateProperties();
    assertThat(hibernateProps.getProperty("sonar.foo"), Matchers.nullValue()); // not an hibernate property
    assertThat(hibernateProps.getProperty("hibernate.foo"), Matchers.is("hibernate.foo value"));
    assertThat(hibernateProps.getProperty("hibernate.hbm2ddl.auto"), Matchers.is("hibernate.hbm2ddl value"));
    assertThat(hibernateProps.getProperty("hibernate.dialect"), Matchers.is(Oracle.Oracle10gWithDecimalDialect.class.getName()));
  }

  private class TestDatabaseConnector extends AbstractDatabaseConnector {

    public TestDatabaseConnector(Configuration configuration) {
      super(configuration, false);
    }

    @Override
    public void setupEntityManagerFactory(Properties factoryProps) {
    }

    @Override
    protected boolean upToDateSchemaVersion() {
      return true;
    }

    @Override
    public EntityManagerFactory createEntityManagerFactory() {
      return null;
    }

    public Connection getConnection() throws SQLException {
      Connection c = Mockito.mock(Connection.class);
      DatabaseMetaData m = Mockito.mock(DatabaseMetaData.class);
      Mockito.when(c.getMetaData()).thenReturn(m);
      return c;
    }
  }


}
