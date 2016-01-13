/*
 * SonarQube
 * Copyright (C) 2009-2016 SonarSource SA
 * mailto:contact AT sonarsource DOT com
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
package org.sonar.db.version.v52;

import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Test;
import org.sonar.api.utils.System2;
import org.sonar.db.DbTester;
import org.sonar.db.version.MigrationStep;

public class RemoveDuplicatedComponentKeysTest {

  @ClassRule
  public static DbTester db = DbTester.createForSchema(System2.INSTANCE, RemoveDuplicatedComponentKeysTest.class, "schema.sql");

  MigrationStep migration;

  @Before
  public void setUp() {
    db.executeUpdateSql("truncate table projects");
    db.executeUpdateSql("truncate table issues");

    migration = new RemoveDuplicatedComponentKeys(db.database(), db.getDbClient());
  }

  @Test
  public void migrate_empty_db() throws Exception {
    migration.execute();
  }

  @Test
  public void migrate_components_and_issues() throws Exception {
    db.prepareDbUnit(this.getClass(), "migrate.xml");
    migration.execute();
    db.assertDbUnit(this.getClass(), "migrate-result.xml", "projects", "issues");
  }

  @Test
  public void not_migrate_components_and_issues_already_migrated() throws Exception {
    db.prepareDbUnit(this.getClass(), "migrate-result.xml");
    migration.execute();
    db.assertDbUnit(this.getClass(), "migrate-result.xml", "projects", "issues");
  }

  @Test
  public void keep_enable_component_when_enabled_component_exists() throws Exception {
    db.prepareDbUnit(this.getClass(), "keep_enable_component.xml");
    migration.execute();
    db.assertDbUnit(this.getClass(), "keep_enable_component-result.xml", "projects");
  }

  @Test
  public void keep_last_component_when_no_enabled_components() throws Exception {
    db.prepareDbUnit(this.getClass(), "keep_last_component.xml");
    migration.execute();
    db.assertDbUnit(this.getClass(), "keep_last_component-result.xml", "projects");
  }

}
