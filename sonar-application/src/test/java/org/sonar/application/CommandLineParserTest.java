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
package org.sonar.application;

import org.junit.Test;

import java.util.Properties;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.fail;

public class CommandLineParserTest {

  CommandLineParser parser = new CommandLineParser();

  @Test
  public void parseArguments() {
    System.setProperty("CommandLineParserTest.unused", "unused");
    System.setProperty("sonar.CommandLineParserTest.used", "used");

    Properties p = parser.parseArguments(new String[] {"-Dsonar.foo=bar"});

    // test environment can already declare some system properties prefixed by "sonar."
    // so we can't test the exact number "2"
    assertThat(p.size()).isGreaterThanOrEqualTo(2);
    assertThat(p.getProperty("sonar.foo")).isEqualTo("bar");
    assertThat(p.getProperty("sonar.CommandLineParserTest.used")).isEqualTo("used");

  }

  @Test
  public void argumentsToProperties() {
    Properties p = parser.argumentsToProperties(new String[] {"-Dsonar.foo=bar", "-Dsonar.whitespace=foo bar"});
    assertThat(p).hasSize(2);
    assertThat(p.getProperty("sonar.foo")).isEqualTo("bar");
    assertThat(p.getProperty("sonar.whitespace")).isEqualTo("foo bar");

    try {
      parser.argumentsToProperties(new String[] {"-Dsonar.foo=bar", "sonar.bad=true"});
      fail();
    } catch (IllegalArgumentException e) {
      assertThat(e).hasMessage("Command-line argument must start with -D, for example -Dsonar.jdbc.username=sonar. Got: sonar.bad=true");
    }
  }
}
