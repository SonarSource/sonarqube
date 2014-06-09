/*
 * SonarQube, open source software quality management tool.
 * Copyright (C) 2008-2014 SonarSource
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
package org.sonar.batch.settings;

import org.fest.assertions.MapAssert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.sonar.batch.bootstrap.AnalysisMode;
import org.sonar.batch.bootstrap.ServerClient;

import static org.fest.assertions.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class DefaultSettingsReferentialTest {

  @Rule
  public ExpectedException thrown = ExpectedException.none();

  private static final String JSON_RESPONSE = "[{\"k\":\"sonar.cpd.cross\",\"v\":\"true\"}]";

  private static final String REACTOR_JSON_RESPONSE = "[{\"k\":\"sonar.cpd.cross\",\"v\":\"true\"}," +
    "{\"k\":\"sonar.java.coveragePlugin\",\"v\":\"jacoco\"}]";

  ServerClient client = mock(ServerClient.class);

  private AnalysisMode mode;

  private DefaultSettingsReferential ref;

  @Before
  public void prepare() {
    mode = mock(AnalysisMode.class);
    ref = new DefaultSettingsReferential(client, mode);
  }

  @Test
  public void should_load_project_props() {
    when(client.request("/batch_bootstrap/properties?dryRun=false&project=struts")).thenReturn(REACTOR_JSON_RESPONSE);

    assertThat(ref.projectSettings("struts")).hasSize(2).includes(MapAssert.entry("sonar.cpd.cross", "true"));
  }

  @Test
  public void should_load_global_settings() {
    when(client.request("/batch_bootstrap/properties?dryRun=false")).thenReturn(JSON_RESPONSE);

    assertThat(ref.globalSettings()).hasSize(1).includes(MapAssert.entry("sonar.cpd.cross", "true"));
  }
}
