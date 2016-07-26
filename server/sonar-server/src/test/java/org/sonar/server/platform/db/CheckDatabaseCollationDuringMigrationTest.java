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
package org.sonar.server.platform.db;

import org.junit.After;
import org.junit.Test;
import org.sonar.api.platform.ServerUpgradeStatus;
import org.sonar.db.charset.DatabaseCharsetChecker;
import org.sonar.server.platform.db.CheckDatabaseCollationDuringMigration;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;
import static org.sonar.db.charset.DatabaseCharsetChecker.Flag.AUTO_REPAIR_COLLATION;
import static org.sonar.db.charset.DatabaseCharsetChecker.Flag.ENFORCE_UTF8;

public class CheckDatabaseCollationDuringMigrationTest {
  ServerUpgradeStatus upgradeStatus = mock(ServerUpgradeStatus.class);
  DatabaseCharsetChecker charsetChecker = mock(DatabaseCharsetChecker.class);
  CheckDatabaseCollationDuringMigration underTest = new CheckDatabaseCollationDuringMigration(upgradeStatus, charsetChecker);

  @After
  public void tearDown() {
    underTest.stop();
  }

  @Test
  public void enforce_utf8_and_optionally_repair_collation_if_fresh_install() {
    when(upgradeStatus.isFreshInstall()).thenReturn(true);

    underTest.start();

    verify(charsetChecker).check(ENFORCE_UTF8, AUTO_REPAIR_COLLATION);
  }

  @Test
  public void repair_collation_but_do_not_enforce_utf8_if_db_upgrade() {
    when(upgradeStatus.isFreshInstall()).thenReturn(false);
    when(upgradeStatus.isUpgraded()).thenReturn(true);

    underTest.start();

    verify(charsetChecker).check(AUTO_REPAIR_COLLATION);
  }

  @Test
  public void do_nothing_if_no_db_changes() {
    when(upgradeStatus.isFreshInstall()).thenReturn(false);
    when(upgradeStatus.isUpgraded()).thenReturn(false);

    underTest.start();

    verifyZeroInteractions(charsetChecker);
  }

}
