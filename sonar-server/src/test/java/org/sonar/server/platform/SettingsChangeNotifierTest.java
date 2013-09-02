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
package org.sonar.server.platform;

import org.junit.Test;
import org.mockito.ArgumentMatcher;
import org.sonar.api.config.SettingsChangeHandler;
import org.sonar.core.resource.ResourceDao;
import org.sonar.core.user.UserDao;

import static org.fest.assertions.Assertions.assertThat;
import static org.mockito.Matchers.argThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

public class SettingsChangeNotifierTest {
  @Test
  public void onGlobalPropertyChange() {
    SettingsChangeHandler handler = mock(SettingsChangeHandler.class);
    SettingsChangeNotifier notifier = new SettingsChangeNotifier(mock(ResourceDao.class), mock(UserDao.class), new SettingsChangeHandler[] {handler});

    notifier.onPropertyChange("foo", "bar", null, null);

    verify(handler).onChange(argThat(new ArgumentMatcher<SettingsChangeHandler.SettingsChange>() {
      @Override
      public boolean matches(Object o) {
        SettingsChangeHandler.SettingsChange change = (SettingsChangeHandler.SettingsChange) o;
        return change.key().equals("foo") && change.newValue().equals("bar") && change.componentKey() == null && change.userLogin() == null;
      }
    }));
  }

  @Test
  public void no_handlers() {
    SettingsChangeNotifier notifier = new SettingsChangeNotifier(mock(ResourceDao.class), mock(UserDao.class));

    assertThat(notifier.changeHandlers).isEmpty();

    // does not fail
    notifier.onPropertyChange("foo", "bar", null, null);
  }
}
