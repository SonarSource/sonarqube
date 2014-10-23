/*
 * SonarQube, open source software quality management tool.
 * Copyright (C) 2008-2014 SonarSource
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

package org.sonar.server.db.migrations.v50;

import com.google.common.collect.ImmutableSet;
import org.junit.After;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Test;
import org.sonar.core.persistence.DbSession;
import org.sonar.core.persistence.TestDatabase;
import org.sonar.core.persistence.migration.v50.Component;
import org.sonar.core.persistence.migration.v50.Migration50Mapper;
import org.sonar.server.db.DbClient;
import org.sonar.server.db.migrations.DatabaseMigration;

import static org.fest.assertions.Assertions.assertThat;

public class PopulateProjectsUuidColumnsMigrationTest {

  @ClassRule
  public static TestDatabase db = new TestDatabase().schema(PopulateProjectsUuidColumnsMigrationTest.class, "schema.sql");

  DbSession session;

  DbClient dbClient;

  Migration50Mapper mapper;

  DatabaseMigration migration;

  @Before
  public void setUp() throws Exception {
    db.executeUpdateSql("truncate table projects");
    db.executeUpdateSql("truncate table snapshots");
    dbClient = new DbClient(db.database(), db.myBatis());
    session = dbClient.openSession(false);
    mapper = session.getMapper(Migration50Mapper.class);
    migration = new PopulateProjectsUuidColumnsMigration(dbClient);
  }

  @After
  public void tearDown() throws Exception {
    session.close();
  }

  @Test
  public void migrate_projects() throws Exception {
    db.prepareDbUnit(getClass(), "migrate_projects.xml");

    migration.execute();
    session.commit();

    Component root = mapper.selectComponentByKey("org.struts:struts");
    assertThat(root.getUuid()).isNotNull();
    assertThat(root.getProjectUuid()).isEqualTo(root.getUuid());
    assertThat(root.getModuleUuid()).isNull();
    assertThat(root.getModuleUuidPath()).isNull();

    Component module = mapper.selectComponentByKey("org.struts:struts-core");
    assertThat(module.getUuid()).isNotNull();
    assertThat(module.getProjectUuid()).isEqualTo(root.getUuid());
    assertThat(module.getModuleUuid()).isNull();
    assertThat(module.getModuleUuidPath()).isEqualTo(root.getUuid() + ".");

    Component subModule = mapper.selectComponentByKey("org.struts:struts-db");
    assertThat(subModule.getUuid()).isNotNull();
    assertThat(subModule.getProjectUuid()).isEqualTo(root.getUuid());
    assertThat(subModule.getModuleUuid()).isEqualTo(module.getUuid());
    assertThat(subModule.getModuleUuidPath()).isEqualTo(root.getUuid() + "." + module.getUuid() + ".");

    Component directory = mapper.selectComponentByKey("org.struts:struts-core:src/org/struts");
    assertThat(directory.getUuid()).isNotNull();
    assertThat(directory.getProjectUuid()).isEqualTo(root.getUuid());
    assertThat(directory.getModuleUuid()).isEqualTo(subModule.getUuid());
    assertThat(directory.getModuleUuidPath()).isEqualTo(root.getUuid() + "." + module.getUuid() + "." + subModule.getUuid() + ".");

    Component file = mapper.selectComponentByKey("org.struts:struts-core:src/org/struts/RequestContext.java");
    assertThat(file.getUuid()).isNotNull();
    assertThat(file.getProjectUuid()).isEqualTo(root.getUuid());
    assertThat(file.getModuleUuid()).isEqualTo(subModule.getUuid());
    assertThat(file.getModuleUuidPath()).isEqualTo(root.getUuid() + "." + module.getUuid() + "." + subModule.getUuid() + ".");

    // Verify that each generated uuid is unique
    assertThat(ImmutableSet.of(root.getUuid(), module.getUuid(), subModule.getUuid(), directory.getUuid(), file.getUuid())).hasSize(5);
  }

  @Test
  public void migrate_disable_components() throws Exception {
    db.prepareDbUnit(getClass(), "migrate_disable_components.xml");

    migration.execute();
    session.commit();

    Component root = mapper.selectComponentByKey("org.struts:struts");
    assertThat(root.getUuid()).isNotNull();

    Component module = mapper.selectComponentByKey("org.struts:struts-core");
    assertThat(module.getUuid()).isNotNull();
    assertThat(module.getProjectUuid()).isEqualTo(root.getUuid());
    // Module and module path will always be null for removed components
    assertThat(module.getModuleUuid()).isNull();
    assertThat(module.getModuleUuidPath()).isNull();

    Component subModule = mapper.selectComponentByKey("org.struts:struts-db");
    assertThat(subModule.getUuid()).isNotNull();
    assertThat(subModule.getProjectUuid()).isEqualTo(root.getUuid());
    // Module and module path will always be null for removed components
    assertThat(subModule.getModuleUuid()).isNull();
    assertThat(subModule.getModuleUuidPath()).isNull();

    Component directory = mapper.selectComponentByKey("org.struts:struts-core:src/org/struts");
    assertThat(directory.getUuid()).isNotNull();
    assertThat(directory.getProjectUuid()).isEqualTo(root.getUuid());
    // Module and module path will always be null for removed components
    assertThat(directory.getModuleUuid()).isNull();
    assertThat(directory.getModuleUuidPath()).isNull();

    Component file = mapper.selectComponentByKey("org.struts:struts-core:src/org/struts/RequestContext.java");
    assertThat(file.getUuid()).isNotNull();
    assertThat(file.getProjectUuid()).isEqualTo(root.getUuid());
    // Module and module path will always be null for removed components
    assertThat(file.getModuleUuid()).isNull();
    assertThat(file.getModuleUuidPath()).isNull();
  }

  @Test
  public void migrate_provisioned_project() throws Exception {
    db.prepareDbUnit(getClass(), "migrate_provisioned_project.xml");

    migration.execute();
    session.commit();

    Component root = mapper.selectComponentByKey("org.struts:struts");
    assertThat(root.getUuid()).isNotNull();
    assertThat(root.getProjectUuid()).isEqualTo(root.getUuid());
    assertThat(root.getModuleUuid()).isNull();
    assertThat(root.getModuleUuidPath()).isNull();
  }

  @Test
  public void migrate_library() throws Exception {
    db.prepareDbUnit(getClass(), "migrate_library.xml");

    migration.execute();
    session.commit();

    Component root = mapper.selectComponentByKey("org.hamcrest:hamcrest-library");
    assertThat(root.getUuid()).isNotNull();
    assertThat(root.getProjectUuid()).isEqualTo(root.getUuid());
    assertThat(root.getModuleUuid()).isNull();
    assertThat(root.getModuleUuidPath()).isNull();
  }

  @Test
  public void not_migrate_view() throws Exception {
    db.prepareDbUnit(getClass(), "migrate_view.xml");

    migration.execute();
    session.commit();

    Component root = mapper.selectComponentByKey("view");
    assertThat(root.getUuid()).isNull();
    assertThat(root.getProjectUuid()).isNull();
    assertThat(root.getModuleUuid()).isNull();
    assertThat(root.getModuleUuidPath()).isNull();
  }

  @Test
  public void not_migrate_developer() throws Exception {
    db.prepareDbUnit(getClass(), "migrate_developer.xml");

    migration.execute();
    session.commit();

    Component root = mapper.selectComponentByKey("DEV:developer@company.net");
    assertThat(root.getUuid()).isNull();
    assertThat(root.getProjectUuid()).isNull();
    assertThat(root.getModuleUuid()).isNull();
    assertThat(root.getModuleUuidPath()).isNull();
  }

  @Test
  public void not_migrate_technical_projects() throws Exception {
    db.prepareDbUnit(getClass(), "not_migrate_technical_projects.xml");

    migration.execute();

    db.assertDbUnit(getClass(), "not_migrate_technical_projects.xml");
  }

}
