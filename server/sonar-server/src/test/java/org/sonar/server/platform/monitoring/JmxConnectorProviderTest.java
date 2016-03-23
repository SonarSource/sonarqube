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
package org.sonar.server.platform.monitoring;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.sonar.api.config.Settings;
import org.sonar.process.ProcessEntryPoint;
import org.sonar.process.jmx.JmxConnector;

import static org.assertj.core.api.Assertions.assertThat;

public class JmxConnectorProviderTest {

  @Rule
  public ExpectedException expectedException = ExpectedException.none();

  Settings settings = new Settings();
  JmxConnectorProvider underTest = new JmxConnectorProvider();

  @Test
  public void provide_JmxConnector() {
    settings.setProperty(ProcessEntryPoint.PROPERTY_SHARED_PATH, "path/");
    JmxConnector connector = underTest.provide(settings);

    assertThat(connector).isNotNull();
    // cache
    assertThat(underTest.provide(settings)).isSameAs(connector);
  }

  @Test
  public void throw_IAE_if_ipc_shared_path_is_not_set() {
    expectedException.expect(IllegalArgumentException.class);
    expectedException.expectMessage("Property process.sharedDir is not set");

    underTest.provide(settings);
  }
}
