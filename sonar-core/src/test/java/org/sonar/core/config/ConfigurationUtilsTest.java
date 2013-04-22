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
package org.sonar.core.config;

import com.google.common.collect.Maps;
import org.junit.Test;

import java.util.Map;
import java.util.Properties;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

public class ConfigurationUtilsTest {
  @Test
  public void shouldInterpolateVariables() {
    Properties input = new Properties();
    input.setProperty("hello", "world");
    input.setProperty("url", "${env:SONAR_JDBC_URL}");
    input.setProperty("do_not_change", "${SONAR_JDBC_URL}");
    Map<String, String> variables = Maps.newHashMap();
    variables.put("SONAR_JDBC_URL", "jdbc:h2:mem");

    Properties output = ConfigurationUtils.interpolateVariables(input, variables);

    assertThat(output.size(), is(3));
    assertThat(output.getProperty("hello"), is("world"));
    assertThat(output.getProperty("url"), is("jdbc:h2:mem"));
    assertThat(output.getProperty("do_not_change"), is("${SONAR_JDBC_URL}"));

    // input is not changed
    assertThat(input.size(), is(3));
    assertThat(input.getProperty("hello"), is("world"));
    assertThat(input.getProperty("url"), is("${env:SONAR_JDBC_URL}"));
    assertThat(input.getProperty("do_not_change"), is("${SONAR_JDBC_URL}"));
  }

  @Test
  public void shouldCopyProperties() {
    Properties input = new Properties();
    input.setProperty("hello", "world");
    input.setProperty("foo", "bar");
    Map<String, String> output = Maps.newHashMap();

    ConfigurationUtils.copyProperties(input, output);

    assertThat(output.size(), is(2));
    assertThat(output.get("hello"), is("world"));
    assertThat(output.get("foo"), is("bar"));

    // input is not changed
    assertThat(input.size(), is(2));
    assertThat(input.getProperty("hello"), is("world"));
    assertThat(input.getProperty("foo"), is("bar"));
  }
}
