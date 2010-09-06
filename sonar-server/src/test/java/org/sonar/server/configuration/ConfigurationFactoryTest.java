/*
 * Sonar, open source software quality management tool.
 * Copyright (C) 2009 SonarSource SA
 * mailto:contact AT sonarsource DOT com
 *
 * Sonar is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * Sonar is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with Sonar; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02
 */
package org.sonar.server.configuration;

import org.apache.commons.configuration.Configuration;
import org.junit.Test;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThat;

public class ConfigurationFactoryTest {

  @Test
  public void checkExistentFile() {
    Configuration configuration = new ConfigurationFactory().getConfigurationFromPropertiesFile("/org/sonar/server/configuration/ConfigurationFactoryTest/checkExistentFile.properties");
    assertNotNull(configuration);
    assertThat(configuration.getString("this"), is("is a test"));
  }

  @Test(expected = ConfigurationException.class)
  public void failsWhenFileNotFound() {
    new ConfigurationFactory().getConfigurationFromPropertiesFile("unknown.properties");
  }

  @Test
  public void shouldReadEnvironmentVariables() {
    Configuration configuration = new ConfigurationFactory().getConfigurationFromPropertiesFile("/org/sonar/server/configuration/ConfigurationFactoryTest/shouldReadEnvironmentVariables.properties");
    assertNotNull(configuration);
    assertThat(configuration.getString("my.param.one"), is("foo"));
    assertThat(configuration.getString("my.param.two"), is(System.getenv("PATH")));
  }

}
