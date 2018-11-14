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
package org.sonar.batch.bootstrapper;

import com.google.common.collect.Maps;
import java.util.Map;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

public class LoggingConfigurationTest {

  @Test
  public void testSetVerbose() {
    assertThat(new LoggingConfiguration(null).setVerbose(true)
      .getSubstitutionVariable(LoggingConfiguration.PROPERTY_ROOT_LOGGER_LEVEL)).isEqualTo(LoggingConfiguration.LEVEL_ROOT_VERBOSE);

    assertThat(new LoggingConfiguration(null).setVerbose(false)
      .getSubstitutionVariable(LoggingConfiguration.PROPERTY_ROOT_LOGGER_LEVEL)).isEqualTo(LoggingConfiguration.LEVEL_ROOT_DEFAULT);

    assertThat(new LoggingConfiguration(null).setRootLevel("ERROR")
      .getSubstitutionVariable(LoggingConfiguration.PROPERTY_ROOT_LOGGER_LEVEL)).isEqualTo("ERROR");
  }

  @Test
  public void testSetVerboseAnalysis() {
    Map<String, String> props = Maps.newHashMap();
    LoggingConfiguration conf = new LoggingConfiguration(null).setProperties(props);
    assertThat(conf.getSubstitutionVariable(LoggingConfiguration.PROPERTY_ROOT_LOGGER_LEVEL)).isEqualTo(LoggingConfiguration.LEVEL_ROOT_DEFAULT);

    props.put("sonar.verbose", "true");
    conf.setProperties(props);
    assertThat(conf.getSubstitutionVariable(LoggingConfiguration.PROPERTY_ROOT_LOGGER_LEVEL)).isEqualTo(LoggingConfiguration.LEVEL_ROOT_VERBOSE);
  }

  @Test
  public void shouldNotBeVerboseByDefault() {
    assertThat(new LoggingConfiguration(null)
      .getSubstitutionVariable(LoggingConfiguration.PROPERTY_ROOT_LOGGER_LEVEL)).isEqualTo(LoggingConfiguration.LEVEL_ROOT_DEFAULT);
  }

  @Test
  public void test_log_listener_setter() {
    LogOutput listener = mock(LogOutput.class);
    assertThat(new LoggingConfiguration(null).setLogOutput(listener).getLogOutput()).isEqualTo(listener);
  }

  @Test
  public void test_deprecated_log_properties() {
    Map<String, String> properties = Maps.newHashMap();
    assertThat(new LoggingConfiguration(null).setProperties(properties)
      .getSubstitutionVariable(LoggingConfiguration.PROPERTY_ROOT_LOGGER_LEVEL)).isEqualTo(LoggingConfiguration.LEVEL_ROOT_DEFAULT);

    properties.put("sonar.verbose", "true");
    LoggingConfiguration conf = new LoggingConfiguration(null).setProperties(properties);
    assertThat(conf.getSubstitutionVariable(LoggingConfiguration.PROPERTY_ROOT_LOGGER_LEVEL)).isEqualTo(LoggingConfiguration.LEVEL_ROOT_VERBOSE);

    properties.put("sonar.verbose", "false");
    conf = new LoggingConfiguration(null).setProperties(properties);
    assertThat(conf.getSubstitutionVariable(LoggingConfiguration.PROPERTY_ROOT_LOGGER_LEVEL)).isEqualTo(LoggingConfiguration.LEVEL_ROOT_DEFAULT);

    properties.put("sonar.verbose", "false");
    properties.put("sonar.log.profilingLevel", "FULL");
    conf = new LoggingConfiguration(null).setProperties(properties);
    assertThat(conf.getSubstitutionVariable(LoggingConfiguration.PROPERTY_ROOT_LOGGER_LEVEL)).isEqualTo("DEBUG");

    properties.put("sonar.verbose", "false");
    properties.put("sonar.log.profilingLevel", "BASIC");
    conf = new LoggingConfiguration(null).setProperties(properties);
    assertThat(conf.getSubstitutionVariable(LoggingConfiguration.PROPERTY_ROOT_LOGGER_LEVEL)).isEqualTo("DEBUG");
  }

  @Test
  public void test_log_level_property() {
    Map<String, String> properties = Maps.newHashMap();
    LoggingConfiguration conf = new LoggingConfiguration(null).setProperties(properties);
    assertThat(conf.getSubstitutionVariable(LoggingConfiguration.PROPERTY_ROOT_LOGGER_LEVEL)).isEqualTo("INFO");

    properties.put("sonar.log.level", "INFO");
    conf = new LoggingConfiguration(null).setProperties(properties);
    assertThat(conf.getSubstitutionVariable(LoggingConfiguration.PROPERTY_ROOT_LOGGER_LEVEL)).isEqualTo("INFO");

    properties.put("sonar.log.level", "DEBUG");
    conf = new LoggingConfiguration(null).setProperties(properties);
    assertThat(conf.getSubstitutionVariable(LoggingConfiguration.PROPERTY_ROOT_LOGGER_LEVEL)).isEqualTo("DEBUG");

    properties.put("sonar.log.level", "TRACE");
    conf = new LoggingConfiguration(null).setProperties(properties);
    assertThat(conf.getSubstitutionVariable(LoggingConfiguration.PROPERTY_ROOT_LOGGER_LEVEL)).isEqualTo("DEBUG");
  }

  @Test
  public void testDefaultFormat() {
    assertThat(new LoggingConfiguration(null)
      .getSubstitutionVariable(LoggingConfiguration.PROPERTY_FORMAT)).isEqualTo(LoggingConfiguration.FORMAT_DEFAULT);
  }

  @Test
  public void testMavenFormat() {
    assertThat(new LoggingConfiguration(new EnvironmentInformation("maven", "1.0"))
      .getSubstitutionVariable(LoggingConfiguration.PROPERTY_FORMAT)).isEqualTo(LoggingConfiguration.FORMAT_MAVEN);
  }

  @Test
  public void testSetFormat() {
    assertThat(new LoggingConfiguration(null).setFormat("%d %level")
      .getSubstitutionVariable(LoggingConfiguration.PROPERTY_FORMAT)).isEqualTo("%d %level");
  }

  @Test
  public void shouldNotSetBlankFormat() {
    assertThat(new LoggingConfiguration(null).setFormat(null)
      .getSubstitutionVariable(LoggingConfiguration.PROPERTY_FORMAT)).isEqualTo(LoggingConfiguration.FORMAT_DEFAULT);

    assertThat(new LoggingConfiguration(null).setFormat("")
      .getSubstitutionVariable(LoggingConfiguration.PROPERTY_FORMAT)).isEqualTo(LoggingConfiguration.FORMAT_DEFAULT);

    assertThat(new LoggingConfiguration(null).setFormat("   ")
      .getSubstitutionVariable(LoggingConfiguration.PROPERTY_FORMAT)).isEqualTo(LoggingConfiguration.FORMAT_DEFAULT);
  }
}
