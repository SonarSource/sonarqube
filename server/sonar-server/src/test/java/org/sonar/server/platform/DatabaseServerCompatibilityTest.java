/*
 * SonarQube
 * Copyright (C) 2009-2018 SonarSource SA
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
package org.sonar.server.platform;

import java.util.Optional;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.sonar.api.utils.MessageException;
import org.sonar.api.utils.log.LogTester;
import org.sonar.api.utils.log.LoggerLevel;
import org.sonar.server.platform.db.migration.version.DatabaseVersion;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class DatabaseServerCompatibilityTest {

  @Rule
  public ExpectedException thrown = ExpectedException.none();
  @Rule
  public LogTester logTester = new LogTester();

  @Test
  public void fail_if_requires_downgrade() {
    thrown.expect(MessageException.class);
    thrown.expectMessage("Database was upgraded to a more recent of SonarQube. Backup must probably be restored or db settings are incorrect.");

    DatabaseVersion version = mock(DatabaseVersion.class);
    when(version.getStatus()).thenReturn(DatabaseVersion.Status.REQUIRES_DOWNGRADE);
    new DatabaseServerCompatibility(version).start();
  }

  @Test
  public void fail_if_requires_firstly_to_upgrade_to_lts() {
    thrown.expect(MessageException.class);
    thrown.expectMessage("Current version is too old. Please upgrade to Long Term Support version firstly.");

    DatabaseVersion version = mock(DatabaseVersion.class);
    when(version.getStatus()).thenReturn(DatabaseVersion.Status.REQUIRES_UPGRADE);
    when(version.getVersion()).thenReturn(Optional.of(12L));
    new DatabaseServerCompatibility(version).start();
  }

  @Test
  public void log_warning_if_requires_upgrade() {
    DatabaseVersion version = mock(DatabaseVersion.class);
    when(version.getStatus()).thenReturn(DatabaseVersion.Status.REQUIRES_UPGRADE);
    when(version.getVersion()).thenReturn(Optional.of(DatabaseVersion.MIN_UPGRADE_VERSION));
    new DatabaseServerCompatibility(version).start();

    assertThat(logTester.logs()).hasSize(2);
    assertThat(logTester.logs(LoggerLevel.WARN)).contains(
      "Database must be upgraded. Please backup database and browse /setup",
      "\n################################################################################\n" +
        "      Database must be upgraded. Please backup database and browse /setup\n" +
        "################################################################################");
  }

  @Test
  public void do_nothing_if_up_to_date() {
    DatabaseVersion version = mock(DatabaseVersion.class);
    when(version.getStatus()).thenReturn(DatabaseVersion.Status.UP_TO_DATE);
    new DatabaseServerCompatibility(version).start();
    // no error
  }
}
