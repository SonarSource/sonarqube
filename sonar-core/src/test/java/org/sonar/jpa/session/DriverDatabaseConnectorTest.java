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

import org.apache.commons.configuration.PropertiesConfiguration;
import org.hamcrest.Matchers;
import org.junit.After;
import org.junit.Test;
import org.sonar.api.database.DatabaseProperties;

import java.sql.SQLException;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.*;

public class DriverDatabaseConnectorTest {

  private DriverDatabaseConnector connector = null;

  @After
  public void stop() {
    if (connector != null) {
      connector.stop();
    }
  }

  @Test(expected = DatabaseException.class)
  public void failsIfUnvalidConfiguration() throws SQLException {
    PropertiesConfiguration conf = new PropertiesConfiguration();
    conf.setProperty(DatabaseProperties.PROP_URL, "jdbc:foo:bar//xxx");
    conf.setProperty(DatabaseProperties.PROP_DRIVER, MemoryDatabaseConnector.DRIVER);
    conf.setProperty(DatabaseProperties.PROP_USER, "sa");
    conf.setProperty(DatabaseProperties.PROP_PASSWORD, null);
    connector = new DriverDatabaseConnector(conf);
    try {
      connector.start();
    } finally {
      assertFalse(connector.isStarted());
      assertFalse(connector.isOperational());
    }
  }

  @Test(expected = DatabaseException.class)
  public void failsIfSchemaIsNotCreated() {
    connector = new DriverDatabaseConnector(MemoryDatabaseConnector.getInMemoryConfiguration(false));
    try {
      connector.start();
    } finally {
      assertTrue(connector.isStarted());
      assertFalse(connector.isOperational());
    }
  }

  @Test(expected = DatabaseException.class)
  public void failsIfUpToDateSchema() {
    connector = new DriverDatabaseConnector(MemoryDatabaseConnector.getInMemoryConfiguration(true));
    try {
      connector.start();
    } finally {
      assertTrue(connector.isStarted());
      assertFalse(connector.isOperational());
    }
  }

  @Test
  public void deprecatedParametersAreStillValid() {
    PropertiesConfiguration conf = new PropertiesConfiguration();
    conf.setProperty(DatabaseProperties.PROP_DRIVER_DEPRECATED, MemoryDatabaseConnector.DRIVER);
    conf.setProperty(DatabaseProperties.PROP_USER_DEPRECATED, "freddy");
    connector = new DriverDatabaseConnector(conf);

    assertThat(connector.getDriver(), is(MemoryDatabaseConnector.DRIVER));
    assertThat(connector.getUsername(), Matchers.is("freddy"));
  }
}
