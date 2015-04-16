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
package org.sonar.server.db.migrations.v45;

import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Test;
import org.sonar.api.utils.DateUtils;
import org.sonar.api.utils.System2;
import org.sonar.core.persistence.DbTester;
import org.sonar.server.db.migrations.MigrationStep;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class AddMissingRuleParameterDefaultValuesMigrationTest {

  @ClassRule
  public static DbTester db = new DbTester().schema(AddMissingRuleParameterDefaultValuesMigrationTest.class, "schema.sql");

  MigrationStep migration;
  System2 system = mock(System2.class);

  @Before
  public void setUp() throws Exception {
    db.executeUpdateSql("truncate table rules_parameters");
    db.executeUpdateSql("truncate table active_rules");
    db.executeUpdateSql("truncate table active_rule_parameters");
    migration = new AddMissingRuleParameterDefaultValuesMigrationStep(db.database(), system);
    when(system.now()).thenReturn(DateUtils.parseDate("2014-04-28").getTime());
  }

  @Test
  public void execute() throws Exception {
    db.prepareDbUnit(getClass(), "before.xml");

    migration.execute();

    db.assertDbUnit(getClass(), "after.xml", "rules_parameters", "active_rules", "active_rule_parameters");
  }

  @Test
  public void no_changes() throws Exception {
    db.prepareDbUnit(getClass(), "no_changes.xml");

    migration.execute();

    db.assertDbUnit(getClass(), "no_changes.xml", "rules_parameters", "active_rules", "active_rule_parameters");
  }
}
