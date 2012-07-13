/*
 * Sonar, open source software quality management tool.
 * Copyright (C) 2008-2012 SonarSource
 * mailto:contact AT sonarsource DOT com
 *
 * Sonar is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * Sonar is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with Sonar; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02
 */
package org.sonar.server.startup;

import org.hamcrest.BaseMatcher;
import org.hamcrest.Description;
import org.junit.Test;
import org.sonar.api.config.Settings;
import org.sonar.server.platform.PersistentSettings;

import java.util.Map;

import static org.mockito.Matchers.argThat;
import static org.mockito.Mockito.*;

public class SetDefaultProjectPermissionsTest {
  @Test
  public void set_default_permissions_if_none() {
    PersistentSettings persistentSettings = mock(PersistentSettings.class);
    Settings settings = new Settings();
    when(persistentSettings.getSettings()).thenReturn(settings);

    new SetDefaultProjectPermissions(persistentSettings).start();

    verify(persistentSettings).saveProperties(argThat(new BaseMatcher<Map<String, String>>() {
      public boolean matches(Object o) {
        Map<String, String> map = (Map<String, String>) o;
        return map.size() == 9 && map.get("sonar.role.admin.TRK.defaultGroups").equals("sonar-administrators");
      }

      public void describeTo(Description description) {
      }
    }));
  }

  @Test
  public void do_not_set_default_permissions_if_exist() {
    PersistentSettings persistentSettings = mock(PersistentSettings.class);
    Settings settings = new Settings().setProperty("sonar.role.admin.TRK.defaultGroups", "custom-group");
    when(persistentSettings.getSettings()).thenReturn(settings);

    new SetDefaultProjectPermissions(persistentSettings).start();

    verify(persistentSettings, never()).saveProperties(any(Map.class));
  }
}
