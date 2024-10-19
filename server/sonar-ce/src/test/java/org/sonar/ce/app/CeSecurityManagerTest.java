/*
 * SonarQube
 * Copyright (C) 2009-2024 SonarSource SA
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
package org.sonar.ce.app;

import java.util.Properties;
import org.junit.Test;
import org.sonar.ce.security.PluginCeRule;
import org.sonar.process.PluginFileWriteRule;
import org.sonar.process.PluginSecurityManager;
import org.sonar.process.Props;

import static org.junit.Assert.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.sonar.process.ProcessProperties.Property.PATH_HOME;
import static org.sonar.process.ProcessProperties.Property.PATH_TEMP;

public class CeSecurityManagerTest {
  private final PluginSecurityManager pluginSecurityManager = mock(PluginSecurityManager.class);

  @Test
  public void apply_calls_PluginSecurityManager() {
    Properties properties = new Properties();
    properties.setProperty(PATH_HOME.getKey(), "home");
    properties.setProperty(PATH_TEMP.getKey(), "temp");
    Props props = new Props(properties);
    CeSecurityManager ceSecurityManager = new CeSecurityManager(pluginSecurityManager, props);
    ceSecurityManager.apply();

    verify(pluginSecurityManager).restrictPlugins(any(PluginFileWriteRule.class), any(PluginCeRule.class));
  }

  @Test
  public void fail_if_runs_twice() {
    Properties properties = new Properties();
    properties.setProperty(PATH_HOME.getKey(), "home");
    properties.setProperty(PATH_TEMP.getKey(), "temp");
    Props props = new Props(properties);
    CeSecurityManager ceSecurityManager = new CeSecurityManager(pluginSecurityManager, props);
    ceSecurityManager.apply();
    assertThrows(IllegalStateException.class, ceSecurityManager::apply);
  }
}
