/*
 * Sonar, open source software quality management tool.
 * Copyright (C) 2008-2011 SonarSource
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
package org.sonar.plugins.emailnotifications;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;

import org.apache.commons.configuration.BaseConfiguration;
import org.apache.commons.configuration.Configuration;
import org.junit.Before;
import org.junit.Test;
import org.sonar.api.CoreProperties;

public class EmailConfigurationTest {

  private EmailConfiguration emailConfiguration;

  @Before
  public void setUp() {
    Configuration configuration = new BaseConfiguration();
    emailConfiguration = new EmailConfiguration(configuration);
  }

  @Test
  public void shouldReturnDefaultValues() {
    assertThat(emailConfiguration.getSmtpHost(), is(""));
    assertThat(emailConfiguration.getSmtpPort(), is("25"));
    assertThat(emailConfiguration.getSmtpUsername(), is(""));
    assertThat(emailConfiguration.getSmtpPassword(), is(""));
    assertThat(emailConfiguration.getSecureConnection(), is(""));
    assertThat(emailConfiguration.getFrom(), is("noreply@nowhere"));
    assertThat(emailConfiguration.getPrefix(), is("[SONAR]"));
    assertThat(emailConfiguration.getServerBaseURL(), is(CoreProperties.SERVER_BASE_URL_DEFAULT_VALUE));
  }

}
