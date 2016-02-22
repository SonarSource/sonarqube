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
package org.sonar.db.version.v55;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.sonar.api.utils.System2;
import org.sonar.db.DbTester;
import org.sonar.db.version.MigrationStep;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class FeedActiveRulesLongDateColumnsTest {

  @Rule
  public DbTester db = DbTester.createForSchema(System2.INSTANCE, FeedActiveRulesLongDateColumnsTest.class, "schema.sql");

  static final long NOW = 1500000000000L;

  System2 system = mock(System2.class);

  MigrationStep migration = new FeedActiveRulesLongDateColumns(db.database(), system);

  @Before
  public void setUp() throws Exception {
    when(system.now()).thenReturn(NOW);
  }

  @Test
  public void execute() throws Exception {
    db.prepareDbUnit(getClass(), "execute.xml");

    migration.execute();

    assertThat(db.countSql("select count(*) from active_rules where created_at_ms is not null and updated_at_ms is not null")).isEqualTo(3);
    // Only 1 row has not been updated
    assertThat(db.countSql("select count(*) from active_rules where created_at_ms='1000000000000' and updated_at_ms='1000000000000'")).isEqualTo(1);
  }

}
