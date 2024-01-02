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
package org.sonar.application;

import com.google.common.collect.ImmutableMap;
import java.io.IOException;
import org.junit.Test;
import org.sonar.application.config.AppSettings;
import org.sonar.application.config.AppSettingsLoader;
import org.sonar.application.config.TestAppSettings;
import org.sonar.process.MessageException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.data.MapEntry.entry;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.sonar.process.ProcessProperties.Property.CLUSTER_ENABLED;
import static org.sonar.process.ProcessProperties.Property.PATH_DATA;
import static org.sonar.process.ProcessProperties.Property.PATH_LOGS;
import static org.sonar.process.ProcessProperties.Property.PATH_TEMP;
import static org.sonar.process.ProcessProperties.Property.PATH_WEB;

public class AppReloaderImplTest {


  private final AppSettingsLoader settingsLoader = mock(AppSettingsLoader.class);
  private final FileSystem fs = mock(FileSystem.class);
  private final AppState state = mock(AppState.class);
  private final AppLogging logging = mock(AppLogging.class);
  private final AppReloaderImpl underTest = new AppReloaderImpl(settingsLoader, fs, state, logging);

  @Test
  public void reload_configuration_then_reset_all() throws IOException {
    AppSettings settings = new TestAppSettings(ImmutableMap.of("foo", "bar"));
    AppSettings newSettings = new TestAppSettings(ImmutableMap.of("foo", "newBar", "newProp", "newVal"));
    when(settingsLoader.load()).thenReturn(newSettings);

    underTest.reload(settings);

    assertThat(settings.getProps().rawProperties())
      .contains(entry("foo", "newBar"))
      .contains(entry("newProp", "newVal"));
    verify(logging).configure();
    verify(state).reset();
    verify(fs).reset();
  }

  @Test
  public void throw_ISE_if_cluster_is_enabled() throws IOException {
    AppSettings settings = new TestAppSettings(ImmutableMap.of(CLUSTER_ENABLED.getKey(), "true"));

    assertThatThrownBy(() -> {
      underTest.reload(settings);

      verifyNoInteractions(logging);
      verifyNoInteractions(state);
      verifyNoInteractions(fs);
    })
      .isInstanceOf(IllegalStateException.class)
      .hasMessage("Restart is not possible with cluster mode");
  }

  @Test
  public void throw_MessageException_if_path_properties_are_changed() throws IOException {
    verifyFailureIfPropertyValueChanged(PATH_DATA.getKey());
    verifyFailureIfPropertyValueChanged(PATH_LOGS.getKey());
    verifyFailureIfPropertyValueChanged(PATH_TEMP.getKey());
    verifyFailureIfPropertyValueChanged(PATH_WEB.getKey());
  }

  @Test
  public void throw_MessageException_if_cluster_mode_changed() throws IOException {
    verifyFailureIfPropertyValueChanged(CLUSTER_ENABLED.getKey());
  }

  private void verifyFailureIfPropertyValueChanged(String propertyKey) throws IOException {
    AppSettings settings = new TestAppSettings(ImmutableMap.of(propertyKey, "val1"));
    AppSettings newSettings = new TestAppSettings(ImmutableMap.of(propertyKey, "val2"));
    when(settingsLoader.load()).thenReturn(newSettings);

    assertThatThrownBy(() -> {
      underTest.reload(settings);

      verifyNoInteractions(logging);
      verifyNoInteractions(state);
      verifyNoInteractions(fs);
    })
      .isInstanceOf(MessageException.class)
      .hasMessage("Property [" + propertyKey + "] cannot be changed on restart: [val1] => [val2]");
  }
}
