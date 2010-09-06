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
import org.sonar.api.database.DatabaseProperties;
import org.sonar.jpa.entity.SchemaMigration;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import java.sql.Connection;

public class MemoryDatabaseConnector extends DriverDatabaseConnector {
  public static final String DRIVER = "org.hsqldb.jdbcDriver";
  public static final String URL = "jdbc:hsqldb:mem:sonar";
  public static final String USER = "sa";
  public static final String PASSWORD = "";
  public static final int ISOLATION = Connection.TRANSACTION_READ_UNCOMMITTED;

  private int version;

  public MemoryDatabaseConnector(Configuration config) {
    super(config);
    version = SchemaMigration.LAST_VERSION;
  }

  public MemoryDatabaseConnector() {
    this(getInMemoryConfiguration(true));
  }

  public MemoryDatabaseConnector(int version) {
    this(getInMemoryConfiguration(true));
    this.version = version;
  }

  protected static Configuration getInMemoryConfiguration(boolean createSchema) {
    PropertiesConfiguration conf = new PropertiesConfiguration();
    conf.setProperty(DatabaseProperties.PROP_URL, URL);
    conf.setProperty(DatabaseProperties.PROP_DRIVER, DRIVER);
    conf.setProperty(DatabaseProperties.PROP_USER, USER);
    conf.setProperty(DatabaseProperties.PROP_PASSWORD, PASSWORD);
    conf.setProperty(DatabaseProperties.PROP_ISOLATION, ISOLATION);
    if (createSchema) {
      conf.setProperty(DatabaseProperties.PROP_HIBERNATE_HBM2DLL, "create-drop");
    }
    return conf;
  }

  @Override
  public void start() {
    try {
      super.start();
    } catch (DatabaseException ex) {
      if (!isStarted()) {
        throw ex;
      }
      setEntityManagerFactory(createEntityManagerFactory());
      setupSchemaVersion(version);
    }
  }

  @Override
  protected EntityManagerFactory createEntityManagerFactory() {
    return super.createEntityManagerFactory();
  }

  protected void setupSchemaVersion(int version) {
    SchemaMigration migration = new SchemaMigration();
    migration.setVersion(version);
    EntityManager manager = null;
    try {
      manager = createEntityManager();
      manager.getTransaction().begin();
      manager.persist(migration);
      manager.getTransaction().commit();

    } finally {
      if (manager != null) {
        manager.close();
      }
    }
  }
}