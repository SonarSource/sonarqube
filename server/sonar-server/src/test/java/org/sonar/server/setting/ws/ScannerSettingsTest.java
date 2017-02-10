/*
 * SonarQube
 * Copyright (C) 2009-2017 SonarSource SA
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
package org.sonar.server.setting.ws;

import org.junit.Test;
import org.sonar.api.config.PropertyDefinition;
import org.sonar.api.config.PropertyDefinitions;
import org.sonar.core.platform.PluginInfo;
import org.sonar.core.platform.PluginRepository;

import static java.util.Arrays.asList;
import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.sonar.api.PropertyType.LICENSE;

public class ScannerSettingsTest {

  private PropertyDefinitions definitions = new PropertyDefinitions();
  private PluginRepository repository = mock(PluginRepository.class);

  private ScannerSettings underTest = new ScannerSettings(definitions, repository);

  @Test
  public void return_license_keys() throws Exception {
    definitions.addComponents(asList(
      PropertyDefinition.builder("foo").build(),
      PropertyDefinition.builder("myplugin.license.secured").type(LICENSE).build()));
    underTest.start();

    assertThat(underTest.getScannerSettingKeys()).contains("myplugin.license.secured");
  }

  @Test
  public void return_license_hash_keys() throws Exception {
    PluginInfo pluginInfo = mock(PluginInfo.class);
    when(pluginInfo.getKey()).thenReturn("myplugin");
    when(repository.getPluginInfos()).thenReturn(singletonList(pluginInfo));
    underTest.start();

    assertThat(underTest.getScannerSettingKeys()).contains("sonar.myplugin.licenseHash.secured");
  }

  @Test
  public void return_server_settings() throws Exception {
    definitions.addComponents(asList(
      PropertyDefinition.builder("foo").build(),
      PropertyDefinition.builder("myplugin.license.secured").type(LICENSE).build()));
    underTest.start();

    assertThat(underTest.getScannerSettingKeys()).contains("sonar.server_id", "sonar.core.id", "sonar.core.startTime");
  }
}
