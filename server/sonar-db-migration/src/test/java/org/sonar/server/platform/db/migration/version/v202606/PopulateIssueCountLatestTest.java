/*
 * SonarQube
 * Copyright (C) SonarSource Sàrl
 * mailto:info AT sonarsource DOT com
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
package org.sonar.server.platform.db.migration.version.v202606;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.sonar.db.MigrationDbTester;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

class PopulateIssueCountLatestTest {

  @RegisterExtension
  public final MigrationDbTester db = MigrationDbTester.createForMigrationStep(PopulateIssueCountLatest.class);

  private final PopulateIssueCountLatest underTest = new PopulateIssueCountLatest(db.database());

  @Test
  void execute_shouldDoNothing() {
    db.executeInsert("issue_count_history",
      "entity_id", "branch", "entity_type", "PROJECT_BRANCH", "dimension_id", 1, "recorded_at_epoch", 100L, "issue_count", 5);

    assertThatCode(underTest::execute).doesNotThrowAnyException();

    assertThat(db.countRowsOfTable("issue_count_history")).isEqualTo(1);
    db.assertTableDoesNotExist("issue_count_latest");
  }
}
