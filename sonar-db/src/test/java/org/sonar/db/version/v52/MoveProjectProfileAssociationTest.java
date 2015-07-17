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

package org.sonar.db.version.v52;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.sonar.api.utils.System2;
import org.sonar.db.DbTester;
import org.sonar.db.version.MigrationStep;

public class MoveProjectProfileAssociationTest {

  @Rule
  public DbTester db = DbTester.createForSchema(System2.INSTANCE, MoveProjectProfileAssociationTest.class, "schema.sql");

  MigrationStep migration;

  @Before
  public void setUp() {
    db.executeUpdateSql("truncate table projects");
    db.executeUpdateSql("truncate table project_qprofiles");
    db.executeUpdateSql("truncate table properties");
    db.executeUpdateSql("truncate table rules_profiles");

    migration = new MoveProjectProfileAssociation(db.database());
  }

  @Test
  public void migrate_empty_db() throws Exception {
    migration.execute();
  }

  @Test
  public void migrate() throws Exception {
    db.prepareDbUnit(this.getClass(), "migrate.xml");
    migration.execute();
    db.assertDbUnit(this.getClass(), "migrate-result.xml", "rules_profiles", "project_qprofiles");
  }

  @Test
  public void not_migrate_already_migrated_data() throws Exception {
    db.prepareDbUnit(this.getClass(), "migrate-result.xml");
    migration.execute();
    db.assertDbUnit(this.getClass(), "migrate-result.xml", "rules_profiles", "project_qprofiles");
  }

}
