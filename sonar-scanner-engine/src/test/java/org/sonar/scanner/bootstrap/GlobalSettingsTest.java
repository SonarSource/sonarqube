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
package org.sonar.scanner.bootstrap;

import java.util.Collections;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.sonar.api.config.PropertyDefinitions;
import org.sonar.api.utils.log.LogTester;
import org.sonar.api.utils.log.LoggerLevel;
import org.sonar.scanner.bootstrap.GlobalMode;
import org.sonar.scanner.bootstrap.GlobalProperties;
import org.sonar.scanner.bootstrap.GlobalSettings;
import org.sonar.scanner.protocol.input.GlobalRepositories;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

public class GlobalSettingsTest {

  public static final String SOME_VALUE = "some_value";
  @Rule
  public ExpectedException thrown = ExpectedException.none();
  @Rule
  public LogTester logTester = new LogTester();

  GlobalRepositories globalRef;
  GlobalProperties bootstrapProps;

  private GlobalMode mode;

  @Before
  public void prepare() {
    globalRef = new GlobalRepositories();
    bootstrapProps = new GlobalProperties(Collections.<String, String>emptyMap());
    mode = mock(GlobalMode.class);
  }

  @Test
  public void should_load_global_settings() {
    globalRef.globalSettings().put("sonar.cpd.cross", "true");

    GlobalSettings batchSettings = new GlobalSettings(bootstrapProps, new PropertyDefinitions(), globalRef, mode);

    assertThat(batchSettings.getBoolean("sonar.cpd.cross")).isTrue();
  }

  @Test
  public void should_log_warn_msg_for_each_jdbc_property_if_present() {
    globalRef.globalSettings().put("sonar.jdbc.url", SOME_VALUE);
    globalRef.globalSettings().put("sonar.jdbc.username", SOME_VALUE);
    globalRef.globalSettings().put("sonar.jdbc.password", SOME_VALUE);

    new GlobalSettings(bootstrapProps, new PropertyDefinitions(), globalRef, mode);

    assertThat(logTester.logs(LoggerLevel.WARN)).containsOnly(
      "Property 'sonar.jdbc.url' is not supported any more. It will be ignored. There is no longer any DB connection to the SQ database.",
      "Property 'sonar.jdbc.username' is not supported any more. It will be ignored. There is no longer any DB connection to the SQ database.",
      "Property 'sonar.jdbc.password' is not supported any more. It will be ignored. There is no longer any DB connection to the SQ database."
      );
  }
}
