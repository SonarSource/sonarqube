/*
 * SonarQube
 * Copyright (C) 2009-2018 SonarSource SA
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
package org.sonar.application.config;

import java.util.Properties;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import static org.assertj.core.api.Assertions.assertThat;

public class CommandLineParserTest {

  @Rule
  public ExpectedException expectedException = ExpectedException.none();

  @Test
  public void parseArguments() {
    System.setProperty("CommandLineParserTest.unused", "unused");
    System.setProperty("sonar.CommandLineParserTest.used", "used");

    Properties p = CommandLineParser.parseArguments(new String[] {"-Dsonar.foo=bar"});

    // test environment can already declare some system properties prefixed by "sonar."
    // so we can't test the exact number "2"
    assertThat(p.size()).isGreaterThanOrEqualTo(2);
    assertThat(p.getProperty("sonar.foo")).isEqualTo("bar");
    assertThat(p.getProperty("sonar.CommandLineParserTest.used")).isEqualTo("used");

  }

  @Test
  public void argumentsToProperties_throws_IAE_if_argument_does_not_start_with_minusD() {
    Properties p = CommandLineParser.argumentsToProperties(new String[] {"-Dsonar.foo=bar", "-Dsonar.whitespace=foo bar"});
    assertThat(p).hasSize(2);
    assertThat(p.getProperty("sonar.foo")).isEqualTo("bar");
    assertThat(p.getProperty("sonar.whitespace")).isEqualTo("foo bar");

    expectedException.expect(IllegalArgumentException.class);
    expectedException.expectMessage("Command-line argument must start with -D, for example -Dsonar.jdbc.username=sonar. Got: sonar.bad=true");

    CommandLineParser.argumentsToProperties(new String[] {"-Dsonar.foo=bar", "sonar.bad=true"});
  }
}
