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
package org.sonar.server.platform;

import org.junit.Test;
import org.sonar.core.persistence.DatabaseVersion;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class DefaultServerUpgradeStatusTest {

  @Test
  public void shouldBeFreshInstallation() {
    DatabaseVersion dbVersion = mock(DatabaseVersion.class);
    when(dbVersion.getVersion()).thenReturn(null);

    DefaultServerUpgradeStatus status = new DefaultServerUpgradeStatus(dbVersion);
    status.start();

    assertThat(status.isFreshInstall(), is(true));
    assertThat(status.isUpgraded(), is(false));
    assertThat(status.getInitialDbVersion(), is(-1));
  }

  @Test
  public void shouldBeUpgraded() {
    DatabaseVersion dbVersion = mock(DatabaseVersion.class);
    when(dbVersion.getVersion()).thenReturn(50);

    DefaultServerUpgradeStatus status = new DefaultServerUpgradeStatus(dbVersion);
    status.start();

    assertThat(status.isFreshInstall(), is(false));
    assertThat(status.isUpgraded(), is(true));
    assertThat(status.getInitialDbVersion(), is(50));
  }

  @Test
  public void shouldNotBeUpgraded() {
    DatabaseVersion dbVersion = mock(DatabaseVersion.class);
    when(dbVersion.getVersion()).thenReturn(DatabaseVersion.LAST_VERSION);

    DefaultServerUpgradeStatus status = new DefaultServerUpgradeStatus(dbVersion);
    status.start();

    assertThat(status.isFreshInstall(), is(false));
    assertThat(status.isUpgraded(), is(false));
    assertThat(status.getInitialDbVersion(), is(DatabaseVersion.LAST_VERSION));
  }
}
