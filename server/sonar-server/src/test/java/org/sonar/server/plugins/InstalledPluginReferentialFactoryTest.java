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
package org.sonar.server.plugins;

import org.junit.Test;
import org.sonar.core.platform.PluginInfo;
import org.sonar.core.platform.PluginRepository;

import static com.google.common.collect.Lists.newArrayList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class InstalledPluginReferentialFactoryTest {

  @Test
  public void should_create_plugin_referential() {
    PluginInfo info = new PluginInfo("foo");
    PluginRepository pluginRepository = mock(PluginRepository.class);
    when(pluginRepository.getPluginInfos()).thenReturn(newArrayList(info));
    InstalledPluginReferentialFactory factory = new InstalledPluginReferentialFactory(pluginRepository);

    assertThat(factory.getInstalledPluginReferential()).isNull();
    factory.start();
    assertThat(factory.getInstalledPluginReferential()).isNotNull();
    assertThat(factory.getInstalledPluginReferential().getPlugins()).hasSize(1);
  }

  @Test(expected = RuntimeException.class)
  public void should_encapsulate_exception() {
    PluginRepository pluginRepository = mock(PluginRepository.class);
    when(pluginRepository.getPluginInfos()).thenThrow(new IllegalArgumentException());
    InstalledPluginReferentialFactory factory = new InstalledPluginReferentialFactory(pluginRepository);
    factory.start();
  }
}
