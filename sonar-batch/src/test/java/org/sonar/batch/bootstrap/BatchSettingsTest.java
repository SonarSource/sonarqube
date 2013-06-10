/*
 * SonarQube, open source software quality management tool.
 * Copyright (C) 2008-2013 SonarSource
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
package org.sonar.batch.bootstrap;

import org.apache.commons.configuration.BaseConfiguration;
import org.apache.commons.configuration.Configuration;
import org.junit.Test;
import org.sonar.api.batch.bootstrap.ProjectDefinition;
import org.sonar.api.batch.bootstrap.ProjectReactor;
import org.sonar.api.config.PropertyDefinitions;

import java.util.Collections;
import java.util.Map;

import static org.fest.assertions.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class BatchSettingsTest {

  private static final String JSON_RESPONSE = "[{\"k\":\"sonar.cpd.cross\",\"v\":\"true\"}]";

  private static final String REACTOR_JSON_RESPONSE = "[{\"k\":\"sonar.cpd.cross\",\"v\":\"true\"}," +
    "{\"k\":\"sonar.java.coveragePlugin\",\"v\":\"jacoco\",\"p\":\"struts\"}," +
    "{\"k\":\"sonar.java.coveragePlugin\",\"v\":\"cobertura\",\"p\":\"struts-core\"}]";

  ServerClient client = mock(ServerClient.class);
  ProjectDefinition project = ProjectDefinition.create().setKey("struts");
  Configuration deprecatedConf = new BaseConfiguration();
  BootstrapSettings bootstrapSettings = new BootstrapSettings(new BootstrapProperties(Collections.<String, String> emptyMap()));

  @Test
  public void should_load_system_props() {
    when(client.request("/batch_bootstrap/properties")).thenReturn(JSON_RESPONSE);
    System.setProperty("BatchSettingsTest.testSystemProp", "system");
    // Reconstruct bootstrap settings to get system property
    bootstrapSettings = new BootstrapSettings(new BootstrapProperties(Collections.<String, String> emptyMap()));

    BatchSettings batchSettings = new BatchSettings(bootstrapSettings, new PropertyDefinitions(), client, deprecatedConf);

    assertThat(batchSettings.getString("BatchSettingsTest.testSystemProp")).isEqualTo("system");
  }

  @Test
  public void should_load_project_props() {
    when(client.request("/batch_bootstrap/properties")).thenReturn(JSON_RESPONSE);
    when(client.request("/batch_bootstrap/properties?project=struts")).thenReturn(REACTOR_JSON_RESPONSE);
    project.setProperty("project.prop", "project");

    BatchSettings batchSettings = new BatchSettings(bootstrapSettings, new PropertyDefinitions(), client, deprecatedConf);
    batchSettings.init(new ProjectReactor(project));

    assertThat(batchSettings.getString("project.prop")).isEqualTo("project");
  }

  @Test
  public void should_load_global_settings() {
    when(client.request("/batch_bootstrap/properties")).thenReturn(JSON_RESPONSE);

    BatchSettings batchSettings = new BatchSettings(bootstrapSettings, new PropertyDefinitions(), client, deprecatedConf);

    assertThat(batchSettings.getBoolean("sonar.cpd.cross")).isTrue();
  }

  @Test
  public void should_load_project_root_settings() {
    when(client.request("/batch_bootstrap/properties")).thenReturn(JSON_RESPONSE);
    when(client.request("/batch_bootstrap/properties?project=struts")).thenReturn(REACTOR_JSON_RESPONSE);

    BatchSettings batchSettings = new BatchSettings(bootstrapSettings, new PropertyDefinitions(), client, deprecatedConf);
    batchSettings.init(new ProjectReactor(project));

    assertThat(batchSettings.getString("sonar.java.coveragePlugin")).isEqualTo("jacoco");
  }

  @Test
  public void should_keep_module_settings_for_later() {
    when(client.request("/batch_bootstrap/properties")).thenReturn(JSON_RESPONSE);
    when(client.request("/batch_bootstrap/properties?project=struts")).thenReturn(REACTOR_JSON_RESPONSE);

    BatchSettings batchSettings = new BatchSettings(bootstrapSettings, new PropertyDefinitions(), client, deprecatedConf);
    batchSettings.init(new ProjectReactor(project));

    Map<String, String> moduleSettings = batchSettings.getModuleProperties("struts-core");

    assertThat(moduleSettings).hasSize(1);
    assertThat(moduleSettings.get("sonar.java.coveragePlugin")).isEqualTo("cobertura");
  }

  @Test
  public void system_props_should_override_build_props() {
    when(client.request("/batch_bootstrap/properties")).thenReturn(JSON_RESPONSE);
    System.setProperty("BatchSettingsTest.testSystemProp", "system");
    project.setProperty("BatchSettingsTest.testSystemProp", "build");

    BatchSettings batchSettings = new BatchSettings(bootstrapSettings, new PropertyDefinitions(), client, deprecatedConf);

    assertThat(batchSettings.getString("BatchSettingsTest.testSystemProp")).isEqualTo("system");
  }

  @Test
  public void should_forward_to_deprecated_commons_configuration() {
    when(client.request("/batch_bootstrap/properties")).thenReturn(JSON_RESPONSE);
    when(client.request("/batch_bootstrap/properties?project=struts")).thenReturn(REACTOR_JSON_RESPONSE);

    BatchSettings batchSettings = new BatchSettings(bootstrapSettings, new PropertyDefinitions(), client, deprecatedConf);
    batchSettings.init(new ProjectReactor(project));

    assertThat(deprecatedConf.getString("sonar.cpd.cross")).isEqualTo("true");
    assertThat(deprecatedConf.getString("sonar.java.coveragePlugin")).isEqualTo("jacoco");

    batchSettings.removeProperty("sonar.cpd.cross");
    assertThat(deprecatedConf.getString("sonar.cpd.cross")).isNull();
    assertThat(deprecatedConf.getString("sonar.java.coveragePlugin")).isEqualTo("jacoco");

    batchSettings.clear();
    assertThat(deprecatedConf.getString("sonar.cpd.cross")).isNull();
    assertThat(deprecatedConf.getString("sonar.java.coveragePlugin")).isNull();
  }

  @Test
  public void project_should_be_optional() {
    when(client.request("/batch_bootstrap/properties")).thenReturn(JSON_RESPONSE);
    BatchSettings batchSettings = new BatchSettings(bootstrapSettings, new PropertyDefinitions(), client, deprecatedConf);
    assertThat(batchSettings.getProperties()).isNotEmpty();
  }
}
