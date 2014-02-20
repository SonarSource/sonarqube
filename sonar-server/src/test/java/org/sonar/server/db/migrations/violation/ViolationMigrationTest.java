/*
 * SonarQube, open source software quality management tool.
 * Copyright (C) 2008-2013 SonarSource
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
package org.sonar.server.db.migrations.violation;

import org.junit.ClassRule;
import org.junit.Test;
import org.sonar.api.config.Settings;
import org.sonar.core.persistence.TestDatabase;

import java.util.Set;

import static org.fest.assertions.Assertions.assertThat;

public class ViolationMigrationTest {

  @ClassRule
  public static TestDatabase db = new TestDatabase().schema(ViolationMigrationTest.class, "schema.sql");

  @Test
  public void migrate_violations() throws Exception {
    db.prepareDbUnit(getClass(), "migrate_violations.xml");

    new ViolationMigration(db.database(), new Settings()).execute();

    db.assertDbUnit(getClass(), "migrate_violations_result.xml", "issues", "issue_changes");
    assertMigrationEnded();
  }

  @Test
  public void no_violations_to_migrate() throws Exception {
    db.prepareDbUnit(getClass(), "no_violations_to_migrate.xml");

    new ViolationMigration(db.database(), new Settings()).execute();

    db.assertDbUnit(getClass(), "no_violations_to_migrate_result.xml", "issues", "issue_changes");
    assertMigrationEnded();
  }

  private void assertMigrationEnded() {
    assertThat(db.count("select count(id) from rule_failures")).isEqualTo(0);

    // Progress thread is dead
    Set<Thread> threads = Thread.getAllStackTraces().keySet();
    for (Thread thread : threads) {
      assertThat(thread.getName()).isNotEqualTo(Progress.THREAD_NAME);
    }
  }
}
