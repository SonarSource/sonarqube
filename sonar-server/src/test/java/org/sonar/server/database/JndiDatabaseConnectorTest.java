/*
 * Sonar, open source software quality management tool.
 * Copyright (C) 2008-2011 SonarSource
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
import org.apache.commons.configuration.PropertiesConfiguration;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.sonar.api.config.Settings;
import org.sonar.api.database.DatabaseProperties;
import org.sonar.jpa.entity.SchemaMigration;
import org.sonar.server.configuration.ServerSettings;

import javax.naming.Context;
import javax.persistence.EntityManagerFactory;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;

public class JndiDatabaseConnectorTest {

  private String currentInitialContextFacto;
  private JndiDatabaseConnector connector;
  private int emfCreationCounter;

  @Before
  public void setup() {
    Settings conf = new Settings();
    conf.setProperty(DatabaseProperties.PROP_DIALECT, DatabaseProperties.DIALECT_HSQLDB);
    conf.setProperty(DatabaseProperties.PROP_URL, "jdbc:hsqldb:mem:sonar");
    conf.setProperty(DatabaseProperties.PROP_DRIVER, "org.hsqldb.jdbcDriver");
    currentInitialContextFacto = System.getProperty(Context.INITIAL_CONTEXT_FACTORY);
    connector = getTestJndiConnector(conf);
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

  private JndiDatabaseConnector getTestJndiConnector(Settings conf) {
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


}
