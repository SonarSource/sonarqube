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
package org.sonar.application;

import java.io.IOException;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.sonar.application.config.AppSettings;
import org.sonar.application.config.AppSettingsLoader;
import org.sonar.application.config.TestAppSettings;
import org.sonar.process.MessageException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.data.MapEntry.entry;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;
import static org.sonar.process.ProcessProperties.Property.CLUSTER_ENABLED;
import static org.sonar.process.ProcessProperties.Property.PATH_DATA;
import static org.sonar.process.ProcessProperties.Property.PATH_LOGS;
import static org.sonar.process.ProcessProperties.Property.PATH_TEMP;
import static org.sonar.process.ProcessProperties.Property.PATH_WEB;

public class AppReloaderImplTest {

  @Rule
  public ExpectedException expectedException = ExpectedException.none();

  private AppSettingsLoader settingsLoader = mock(AppSettingsLoader.class);
  private FileSystem fs = mock(FileSystem.class);
  private AppState state = mock(AppState.class);
  private AppLogging logging = mock(AppLogging.class);
  private AppReloaderImpl underTest = new AppReloaderImpl(settingsLoader, fs, state, logging);

  @Test
  public void reload_configuration_then_reset_all() throws IOException {
    AppSettings settings = new TestAppSettings().set("foo", "bar");
    AppSettings newSettings = new TestAppSettings()
      .set("foo", "newBar")
      .set("newProp", "newVal");
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
    AppSettings settings = new TestAppSettings().set(CLUSTER_ENABLED.getKey(), "true");

    expectedException.expect(IllegalStateException.class);
    expectedException.expectMessage("Restart is not possible with cluster mode");

    underTest.reload(settings);

    verifyZeroInteractions(logging);
    verifyZeroInteractions(state);
    verifyZeroInteractions(fs);
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
    AppSettings settings = new TestAppSettings().set(propertyKey, "val1");
    AppSettings newSettings = new TestAppSettings()
      .set(propertyKey, "val2");
    when(settingsLoader.load()).thenReturn(newSettings);

    expectedException.expect(MessageException.class);
    expectedException.expectMessage("Property [" + propertyKey + "] cannot be changed on restart: [val1] => [val2]");

    underTest.reload(settings);

    verifyZeroInteractions(logging);
    verifyZeroInteractions(state);
    verifyZeroInteractions(fs);
  }
}
