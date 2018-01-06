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
package org.sonar.scanner.bootstrap;

import com.google.common.collect.ImmutableMap;
import java.util.Collections;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.sonar.api.config.PropertyDefinitions;
import org.sonar.api.utils.log.LogTester;
import org.sonar.api.utils.log.LoggerLevel;
import org.sonar.scanner.repository.settings.SettingsLoader;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class GlobalConfigurationProviderTest {

  public static final String SOME_VALUE = "some_value";
  @Rule
  public ExpectedException thrown = ExpectedException.none();
  @Rule
  public LogTester logTester = new LogTester();

  SettingsLoader settingsLoader;
  GlobalProperties bootstrapProps;

  private GlobalAnalysisMode mode;

  @Before
  public void prepare() {
    settingsLoader = mock(SettingsLoader.class);
    bootstrapProps = new GlobalProperties(Collections.<String, String>emptyMap());
    mode = mock(GlobalAnalysisMode.class);
  }

  @Test
  public void should_load_global_settings() {
    when(settingsLoader.load(null)).thenReturn(ImmutableMap.of("sonar.cpd.cross", "true"));

    GlobalConfiguration globalConfig = new GlobalConfigurationProvider().provide(settingsLoader, bootstrapProps, new PropertyDefinitions(), mode);

    assertThat(globalConfig.get("sonar.cpd.cross")).hasValue("true");
  }

  @Test
  public void should_log_warn_msg_for_each_jdbc_property_if_present() {
    when(settingsLoader.load(null)).thenReturn(ImmutableMap.of("sonar.jdbc.url", SOME_VALUE,
      "sonar.jdbc.username", SOME_VALUE,
      "sonar.jdbc.password", SOME_VALUE));

    new GlobalConfigurationProvider().provide(settingsLoader, bootstrapProps, new PropertyDefinitions(), mode);

    assertThat(logTester.logs(LoggerLevel.WARN)).containsOnly(
      "Property 'sonar.jdbc.url' is not supported any more. It will be ignored. There is no longer any DB connection to the SQ database.",
      "Property 'sonar.jdbc.username' is not supported any more. It will be ignored. There is no longer any DB connection to the SQ database.",
      "Property 'sonar.jdbc.password' is not supported any more. It will be ignored. There is no longer any DB connection to the SQ database.");
  }
}
