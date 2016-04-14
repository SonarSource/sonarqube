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
package org.sonar.server.db;

import org.junit.Test;
import org.sonar.api.platform.ServerUpgradeStatus;
import org.sonar.db.charset.DatabaseCharsetChecker;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

public class CheckDatabaseCharsetAtStartupTest {

  ServerUpgradeStatus upgradeStatus = mock(ServerUpgradeStatus.class);
  DatabaseCharsetChecker charsetChecker = mock(DatabaseCharsetChecker.class);
  CheckDatabaseCharsetAtStartup underTest = new CheckDatabaseCharsetAtStartup(upgradeStatus, charsetChecker);

  @Test
  public void enforces_utf8_if_fresh_install() {
    when(upgradeStatus.isFreshInstall()).thenReturn(true);
    underTest.start();
    verify(charsetChecker).check(true);

    underTest.stop();
    verifyNoMoreInteractions(charsetChecker);
  }

  @Test
  public void does_not_enforce_utf8_if_not_fresh_install() {
    when(upgradeStatus.isFreshInstall()).thenReturn(false);
    underTest.start();
    verify(charsetChecker).check(false);

    underTest.stop();
    verifyNoMoreInteractions(charsetChecker);
  }
}
