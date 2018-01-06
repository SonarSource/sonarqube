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
package org.sonar.server.setting;

import org.junit.After;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.sonar.server.setting.DatabaseSettingLoader;
import org.sonar.server.setting.DatabaseSettingsEnabler;
import org.sonar.server.setting.ThreadLocalSettings;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

public class DatabaseSettingsEnablerTest {

  @Rule
  public ExpectedException expectedException = ExpectedException.none();

  private ThreadLocalSettings settings = mock(ThreadLocalSettings.class);
  private DatabaseSettingLoader loader = mock(DatabaseSettingLoader.class);
  private DatabaseSettingsEnabler underTest = new DatabaseSettingsEnabler(settings, loader);

  @After
  public void tearDown() {
    underTest.stop();
  }

  @Test
  public void change_loader_at_startup() {
    underTest.start();

    verify(settings).setSettingLoader(loader);
  }
}
