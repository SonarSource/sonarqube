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
package org.sonar.server.database;

import org.apache.commons.configuration.Configuration;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.sonar.api.database.DatabaseProperties;
import org.sonar.jpa.entity.SchemaMigration;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.util.Hashtable;
import javax.naming.Context;
import javax.naming.spi.InitialContextFactory;
import javax.persistence.EntityManagerFactory;
import javax.sql.DataSource;

import static org.junit.Assert.assertEquals;
import static org.mockito.Matchers.anyObject;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.*;

public class JndiDatabaseConnectorTest {

  private Configuration conf;
  private String currentInitialContextFacto;
  private JndiDatabaseConnector connector;
  private int emfCreationCounter;

  @Before
  public void setup() {
    conf = mock(Configuration.class);
    when(conf.getString(eq(DatabaseProperties.PROP_JNDI_NAME), anyString())).thenReturn("test");
    when(conf.getString(eq(DatabaseProperties.PROP_DIALECT))).thenReturn(DatabaseProperties.DIALECT_HSQLDB);
    currentInitialContextFacto = System.getProperty(Context.INITIAL_CONTEXT_FACTORY);
    connector = getTestJndiConnector(conf);
    System.setProperty(Context.INITIAL_CONTEXT_FACTORY, TestInitialContextFactory.class.getName());
  }

  @After
  public void restore() {
    if (currentInitialContextFacto != null) {
      System.setProperty(Context.INITIAL_CONTEXT_FACTORY, currentInitialContextFacto);
    }
  }

  @Test
  public void canBeStartedSeveralTimes() throws Exception {
    connector.start();
    assertEquals(1, emfCreationCounter);

    connector.start();
    assertEquals(1, emfCreationCounter);

    connector.stop();
    connector.start();
    assertEquals(2, emfCreationCounter);
  }

  @Test
  public void transactionIsolationCorrectlySet() throws Exception {
    int fakeTransactionIsolation = 9;
    when(conf.getInteger(eq(DatabaseProperties.PROP_ISOLATION), (Integer) anyObject())).thenReturn(fakeTransactionIsolation);
    connector.start();
    Connection c = connector.getConnection();
    // start method call get a connection to test it, so total number is 2
    verify(c, times(2)).setTransactionIsolation(fakeTransactionIsolation);
  }

  private JndiDatabaseConnector getTestJndiConnector(Configuration conf) {
    JndiDatabaseConnector connector = new JndiDatabaseConnector(conf) {
      @Override
      protected int loadVersion() {
        return SchemaMigration.LAST_VERSION;
      }

      @Override
      public EntityManagerFactory createEntityManagerFactory() {
        emfCreationCounter++;
        return mock(EntityManagerFactory.class);
      }

    };
    return connector;
  }

  public static class TestInitialContextFactory implements InitialContextFactory {

    private Connection c;

    public Context getInitialContext(Hashtable env) {
      Context ctx = mock(Context.class);
      DataSource ds = mock(DataSource.class);
      DatabaseMetaData m = mock(DatabaseMetaData.class);
      c = mock(Connection.class);
      try {
        when(ctx.lookup(anyString())).thenReturn(ds);
        when(ds.getConnection()).thenReturn(c);
        when(m.getURL()).thenReturn("testdbcUrl");
        when(c.getMetaData()).thenReturn(m);
      } catch (Exception e) {
        throw new RuntimeException(e);
      }
      return ctx;
    }
  }

}
