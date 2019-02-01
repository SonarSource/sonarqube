/*
 * SonarQube
 * Copyright (C) 2009-2019 SonarSource SA
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

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class GlobalConfigurationProviderTest {

  public static final String SOME_VALUE = "some_value";
  @Rule
  public ExpectedException thrown = ExpectedException.none();
  @Rule
  public LogTester logTester = new LogTester();

  GlobalServerSettings globalServerSettings;
  RawScannerProperties scannerProps;

  @Before
  public void prepare() {
    globalServerSettings = mock(GlobalServerSettings.class);
    scannerProps = new RawScannerProperties(Collections.emptyMap());
  }

  @Test
  public void should_load_global_settings() {
    when(globalServerSettings.properties()).thenReturn(ImmutableMap.of("sonar.cpd.cross", "true"));

    GlobalConfiguration globalConfig = new GlobalConfigurationProvider().provide(globalServerSettings, scannerProps, new PropertyDefinitions());

    assertThat(globalConfig.get("sonar.cpd.cross")).hasValue("true");
  }
}
