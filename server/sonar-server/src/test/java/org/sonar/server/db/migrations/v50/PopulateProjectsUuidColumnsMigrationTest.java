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
import org.sonar.core.persistence.DbTester;
import org.sonar.core.persistence.migration.v50.Component;
import org.sonar.core.persistence.migration.v50.Migration50Mapper;
import org.sonar.server.db.DbClient;
import org.sonar.server.db.migrations.MigrationStep;

import static org.assertj.core.api.Assertions.assertThat;

public class PopulateProjectsUuidColumnsMigrationTest {

  @ClassRule
  public static DbTester db = new DbTester().schema(PopulateProjectsUuidColumnsMigrationTest.class, "schema.sql");

  DbSession session;

  DbClient dbClient;

  Migration50Mapper mapper;

  MigrationStep migration;

  @Before
  public void setUp() throws Exception {
    db.executeUpdateSql("truncate table projects");
    db.executeUpdateSql("truncate table snapshots");
    dbClient = new DbClient(db.database(), db.myBatis());
    session = dbClient.openSession(false);
    mapper = session.getMapper(Migration50Mapper.class);
    migration = new PopulateProjectsUuidColumnsMigrationStep(dbClient);
  }

  @After
  public void tearDown() throws Exception {
    session.close();
  }

  @Test
  public void migrate_components() throws Exception {
    db.prepareDbUnit(getClass(), "migrate_components.xml");

    migration.execute();
    session.commit();

    Component root = mapper.selectComponentByKey("org.struts:struts");
    assertThat(root.getUuid()).isNotNull();
    assertThat(root.getProjectUuid()).isEqualTo(root.getUuid());
    assertThat(root.getModuleUuid()).isNull();
    assertThat(root.getModuleUuidPath()).isEmpty();

    Component module = mapper.selectComponentByKey("org.struts:struts-core");
    assertThat(module.getUuid()).isNotNull();
    assertThat(module.getProjectUuid()).isEqualTo(root.getUuid());
    assertThat(module.getModuleUuid()).isEqualTo(root.getUuid());
    assertThat(module.getModuleUuidPath()).isEqualTo(root.getUuid());

    Component subModule = mapper.selectComponentByKey("org.struts:struts-db");
    assertThat(subModule.getUuid()).isNotNull();
    assertThat(subModule.getProjectUuid()).isEqualTo(root.getUuid());
    assertThat(subModule.getModuleUuid()).isEqualTo(module.getUuid());
    assertThat(subModule.getModuleUuidPath()).isEqualTo(root.getUuid() + "." + module.getUuid());

    Component directory = mapper.selectComponentByKey("org.struts:struts-core:src/org/struts");
    assertThat(directory.getUuid()).isNotNull();
    assertThat(directory.getProjectUuid()).isEqualTo(root.getUuid());
    assertThat(directory.getModuleUuid()).isEqualTo(subModule.getUuid());
    assertThat(directory.getModuleUuidPath()).isEqualTo(root.getUuid() + "." + module.getUuid() + "." + subModule.getUuid());

    Component file = mapper.selectComponentByKey("org.struts:struts-core:src/org/struts/RequestContext.java");
    assertThat(file.getUuid()).isNotNull();
    assertThat(file.getProjectUuid()).isEqualTo(root.getUuid());
    assertThat(file.getModuleUuid()).isEqualTo(subModule.getUuid());
    assertThat(file.getModuleUuidPath()).isEqualTo(root.getUuid() + "." + module.getUuid() + "." + subModule.getUuid());

    // Verify that each generated uuid is unique
    assertThat(ImmutableSet.of(root.getUuid(), module.getUuid(), subModule.getUuid(), directory.getUuid(), file.getUuid())).hasSize(5);
  }

  @Test
  public void not_migrate_already_migrated_components() throws Exception {
    db.prepareDbUnit(getClass(), "not_migrate_already_migrated_components.xml");

    migration.execute();
    session.commit();

    Component root = mapper.selectComponentByKey("org.struts:struts");
    assertThat(root.getUuid()).isEqualTo("ABCD");
    assertThat(root.getProjectUuid()).isEqualTo("ABCD");
    assertThat(root.getModuleUuid()).isNull();
    assertThat(root.getModuleUuidPath()).isEmpty();

    Component module = mapper.selectComponentByKey("org.struts:struts-core");
    assertThat(module.getUuid()).isEqualTo("BCDE");
    assertThat(module.getProjectUuid()).isEqualTo("ABCD");
    assertThat(module.getModuleUuid()).isEqualTo("ABCD");
    assertThat(module.getModuleUuidPath()).isEqualTo("ABCD");

    Component subModule = mapper.selectComponentByKey("org.struts:struts-db");
    assertThat(subModule.getUuid()).isNotNull();
    assertThat(subModule.getProjectUuid()).isEqualTo("ABCD");
    assertThat(subModule.getModuleUuid()).isEqualTo("BCDE");
    assertThat(subModule.getModuleUuidPath()).isEqualTo("ABCD.BCDE");

    Component directory = mapper.selectComponentByKey("org.struts:struts-core:src/org/struts");
    assertThat(directory.getUuid()).isNotNull();
    assertThat(directory.getProjectUuid()).isEqualTo("ABCD");
    assertThat(directory.getModuleUuid()).isEqualTo(subModule.getUuid());
    assertThat(directory.getModuleUuidPath()).isEqualTo("ABCD.BCDE." + subModule.getUuid());

    Component file = mapper.selectComponentByKey("org.struts:struts-core:src/org/struts/RequestContext.java");
    assertThat(file.getUuid()).isNotNull();
    assertThat(file.getProjectUuid()).isEqualTo("ABCD");
    assertThat(file.getModuleUuid()).isEqualTo(subModule.getUuid());
    assertThat(file.getModuleUuidPath()).isEqualTo("ABCD.BCDE." + subModule.getUuid());

    Component removedFile = mapper.selectComponentByKey("org.struts:struts-core:src/org/struts/RequestContext2.java");
    assertThat(removedFile.getUuid()).isEqualTo("DCBA");
    assertThat(removedFile.getProjectUuid()).isEqualTo("ABCD");
    assertThat(removedFile.getModuleUuid()).isEqualTo("BCDE");
    assertThat(removedFile.getModuleUuidPath()).isEqualTo("ABCD.BCDE");
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
    assertThat(module.getModuleUuidPath()).isEmpty();

    Component subModule = mapper.selectComponentByKey("org.struts:struts-db");
    assertThat(subModule.getUuid()).isNotNull();
    assertThat(subModule.getProjectUuid()).isEqualTo(root.getUuid());
    // Module and module path will always be null for removed components
    assertThat(subModule.getModuleUuid()).isNull();
    assertThat(subModule.getModuleUuidPath()).isEmpty();

    Component directory = mapper.selectComponentByKey("org.struts:struts-core:src/org/struts");
    assertThat(directory.getUuid()).isNotNull();
    assertThat(directory.getProjectUuid()).isEqualTo(root.getUuid());
    // Module and module path will always be null for removed components
    assertThat(directory.getModuleUuid()).isNull();
    assertThat(directory.getModuleUuidPath()).isEmpty();

    Component file = mapper.selectComponentByKey("org.struts:struts-core:src/org/struts/RequestContext.java");
    assertThat(file.getUuid()).isNotNull();
    assertThat(file.getProjectUuid()).isEqualTo(root.getUuid());
    // Module and module path will always be null for removed components
    assertThat(file.getModuleUuid()).isNull();
    assertThat(file.getModuleUuidPath()).isEmpty();
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
    assertThat(root.getModuleUuidPath()).isEmpty();
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
    assertThat(root.getModuleUuidPath()).isEmpty();
  }

  @Test
  public void migrate_view() throws Exception {
    db.prepareDbUnit(getClass(), "migrate_view.xml");

    migration.execute();
    session.commit();

    Component view = mapper.selectComponentByKey("view");
    assertThat(view.getUuid()).isNotNull();
    assertThat(view.getProjectUuid()).isEqualTo(view.getUuid());
    assertThat(view.getModuleUuid()).isNull();
    assertThat(view.getModuleUuidPath()).isEmpty();

    Component subView = mapper.selectComponentByKey("subView");
    assertThat(subView.getUuid()).isNotNull();
    assertThat(subView.getProjectUuid()).isEqualTo(view.getUuid());
    assertThat(subView.getModuleUuid()).isEqualTo(view.getUuid());
    assertThat(subView.getModuleUuidPath()).isEqualTo(view.getUuid());

    Component techProject = mapper.selectComponentByKey("vieworg.struts:struts");
    assertThat(techProject.getUuid()).isNotNull();
    assertThat(techProject.getProjectUuid()).isEqualTo(view.getUuid());
    assertThat(techProject.getModuleUuid()).isEqualTo(subView.getUuid());
    assertThat(techProject.getModuleUuidPath()).isEqualTo(view.getUuid() + "." + subView.getUuid());
  }

  @Test
  public void migrate_developer() throws Exception {
    db.prepareDbUnit(getClass(), "migrate_developer.xml");

    migration.execute();
    session.commit();

    Component dev = mapper.selectComponentByKey("DEV:developer@company.net");
    assertThat(dev.getUuid()).isNotNull();
    assertThat(dev.getProjectUuid()).isEqualTo(dev.getUuid());
    assertThat(dev.getModuleUuid()).isNull();
    assertThat(dev.getModuleUuidPath()).isEmpty();

    Component techDev = mapper.selectComponentByKey("DEV:developer@company.net:org.struts:struts");
    assertThat(techDev.getUuid()).isNotNull();
    assertThat(techDev.getProjectUuid()).isEqualTo(dev.getUuid());
    assertThat(techDev.getModuleUuid()).isEqualTo(dev.getUuid());
    assertThat(techDev.getModuleUuidPath()).isEqualTo(dev.getUuid());
  }

  @Test
  public void migrate_components_without_uuid() throws Exception {
    db.prepareDbUnit(getClass(), "migrate_components_without_uuid.xml");

    migration.execute();
    session.commit();

    // Root project migrated
    Component root = mapper.selectComponentByKey("org.struts:struts");
    assertThat(root.getUuid()).isNotNull();
    assertThat(root.getProjectUuid()).isEqualTo(root.getUuid());
    assertThat(root.getModuleUuid()).isNull();
    assertThat(root.getModuleUuidPath()).isEmpty();

    // Module with a snapshot having no islast=true
    Component module = mapper.selectComponentByKey("org.struts:struts-core");
    assertThat(module.getUuid()).isNotNull();
    assertThat(module.getProjectUuid()).isEqualTo(module.getUuid());
    assertThat(module.getModuleUuid()).isNull();
    assertThat(module.getModuleUuidPath()).isEmpty();

    // File linked on a no more existing project
    Component file = mapper.selectComponentByKey("org.struts:struts-core:src/org/struts/RequestContext.java");
    assertThat(file.getUuid()).isNotNull();
    assertThat(file.getProjectUuid()).isEqualTo(file.getUuid());
    assertThat(file.getModuleUuid()).isNull();
    assertThat(file.getModuleUuidPath()).isEmpty();
  }

  @Test
  public void not_fail_when_module_has_no_root_id() throws Exception {
    db.prepareDbUnit(getClass(), "not_fail_when_module_has_no_root_id.xml");

    migration.execute();
    session.commit();

    // Root project migrated
    Component root = mapper.selectComponentByKey("org.struts:struts");
    assertThat(root.getUuid()).isNotNull();
    assertThat(root.getProjectUuid()).isEqualTo(root.getUuid());
    assertThat(root.getModuleUuid()).isNull();
    assertThat(root.getModuleUuidPath()).isEmpty();

    // The module without uuid will be migrated as a standalone component
    Component module = mapper.selectComponentByKey("org.struts:struts-core");
    assertThat(module.getUuid()).isNotNull();
    assertThat(module.getProjectUuid()).isEqualTo(module.getUuid());
    assertThat(module.getModuleUuid()).isNull();
    assertThat(module.getModuleUuidPath()).isEmpty();
  }

  @Test
  public void not_fail_when_project_has_two_active_snapshots() throws Exception {
    db.prepareDbUnit(getClass(), "not_fail_when_project_has_two_active_snapshots.xml");

    migration.execute();
    session.commit();

    // Root project migrated
    Component root = mapper.selectComponentByKey("org.struts:struts");
    assertThat(root.getUuid()).isNotNull();
    assertThat(root.getProjectUuid()).isEqualTo(root.getUuid());
    assertThat(root.getModuleUuid()).isNull();
    assertThat(root.getModuleUuidPath()).isEmpty();

    // The module linked on second active snapshot should be migrated a standalone component
    Component module = mapper.selectComponentByKey("org.struts:struts-core");
    assertThat(module.getUuid()).isNotNull();
    assertThat(module.getProjectUuid()).isEqualTo(module.getUuid());
    assertThat(module.getModuleUuid()).isNull();
    assertThat(module.getModuleUuidPath()).isEmpty();
  }

}
