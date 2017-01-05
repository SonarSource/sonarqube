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
import org.sonar.scanner.repository.settings.SettingsLoader;
import org.sonarqube.ws.Settings.FieldValues;
import org.sonarqube.ws.Settings.FieldValues.Value;
import org.sonarqube.ws.Settings.FieldValues.Value.Builder;
import org.sonarqube.ws.Settings.Setting;
import org.sonarqube.ws.Settings.Values;

import static java.util.Arrays.asList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class GlobalSettingsTest {

  public static final String SOME_VALUE = "some_value";
  @Rule
  public ExpectedException thrown = ExpectedException.none();
  @Rule
  public LogTester logTester = new LogTester();

  SettingsLoader settingsLoader;
  GlobalProperties bootstrapProps;

  private GlobalMode mode;

  @Before
  public void prepare() {
    settingsLoader = mock(SettingsLoader.class);
    bootstrapProps = new GlobalProperties(Collections.<String, String>emptyMap());
    mode = mock(GlobalMode.class);
  }

  @Test
  public void should_load_global_settings() {
    when(settingsLoader.load(null)).thenReturn(asList(Setting.newBuilder().setKey("sonar.cpd.cross").setValue("true").build()));

    GlobalSettings batchSettings = new GlobalSettings(bootstrapProps, new PropertyDefinitions(), settingsLoader, mode);

    assertThat(batchSettings.getBoolean("sonar.cpd.cross")).isTrue();
  }

  @Test
  public void should_load_global_multivalue_settings() {
    when(settingsLoader.load(null))
      .thenReturn(asList(Setting.newBuilder()
        .setKey("sonar.preview.supportedPlugins")
        .setValues(Values.newBuilder().addValues("java").addValues("php")).build()));

    GlobalSettings batchSettings = new GlobalSettings(bootstrapProps, new PropertyDefinitions(), settingsLoader, mode);

    assertThat(batchSettings.getStringArray("sonar.preview.supportedPlugins")).containsExactly("java", "php");
  }

  @Test
  public void should_load_global_propertyset_settings() {
    Builder valuesBuilder = Value.newBuilder();
    valuesBuilder.getMutableValue().put("filepattern", "**/*.xml");
    valuesBuilder.getMutableValue().put("rulepattern", "*:S12345");
    Value value1 = valuesBuilder.build();
    valuesBuilder.clear();
    valuesBuilder.getMutableValue().put("filepattern", "**/*.java");
    valuesBuilder.getMutableValue().put("rulepattern", "*:S456");
    Value value2 = valuesBuilder.build();
    when(settingsLoader.load(null))
      .thenReturn(asList(Setting.newBuilder()
        .setKey("sonar.issue.exclusions.multicriteria")
        .setFieldValues(FieldValues.newBuilder().addFieldValues(value1).addFieldValues(value2)).build()));

    GlobalSettings batchSettings = new GlobalSettings(bootstrapProps, new PropertyDefinitions(), settingsLoader, mode);

    assertThat(batchSettings.getStringArray("sonar.issue.exclusions.multicriteria")).containsExactly("1", "2");
    assertThat(batchSettings.getString("sonar.issue.exclusions.multicriteria.1.filepattern")).isEqualTo("**/*.xml");
    assertThat(batchSettings.getString("sonar.issue.exclusions.multicriteria.1.rulepattern")).isEqualTo("*:S12345");
    assertThat(batchSettings.getString("sonar.issue.exclusions.multicriteria.2.filepattern")).isEqualTo("**/*.java");
    assertThat(batchSettings.getString("sonar.issue.exclusions.multicriteria.2.rulepattern")).isEqualTo("*:S456");
  }

  @Test
  public void should_log_warn_msg_for_each_jdbc_property_if_present() {
    when(settingsLoader.load(null)).thenReturn(asList(
      Setting.newBuilder().setKey("sonar.jdbc.url").setValue(SOME_VALUE).build(),
      Setting.newBuilder().setKey("sonar.jdbc.username").setValue(SOME_VALUE).build(),
      Setting.newBuilder().setKey("sonar.jdbc.password").setValue(SOME_VALUE).build()));

    new GlobalSettings(bootstrapProps, new PropertyDefinitions(), settingsLoader, mode);

    assertThat(logTester.logs(LoggerLevel.WARN)).containsOnly(
      "Property 'sonar.jdbc.url' is not supported any more. It will be ignored. There is no longer any DB connection to the SQ database.",
      "Property 'sonar.jdbc.username' is not supported any more. It will be ignored. There is no longer any DB connection to the SQ database.",
      "Property 'sonar.jdbc.password' is not supported any more. It will be ignored. There is no longer any DB connection to the SQ database.");
  }
}
