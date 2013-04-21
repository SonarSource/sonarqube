/*
 * SonarQube, open source software quality management tool.
 * Copyright (C) 2008-2012 SonarSource
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
 * You should have received a copy of the GNU Lesser General Public
 * License along with Sonar; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02
 */
package org.sonar.batch.bootstrapper;

import com.google.common.collect.Maps;
import org.hamcrest.core.Is;
import org.junit.Test;

import java.util.Map;

import static org.junit.Assert.assertThat;

public class LoggingConfigurationTest {

  @Test
  public void testSqlLevel() {
    assertThat(LoggingConfiguration.create().setShowSql(true)
      .getSubstitutionVariable(LoggingConfiguration.PROPERTY_SQL_LOGGER_LEVEL), Is.is(LoggingConfiguration.LEVEL_SQL_VERBOSE));

    assertThat(LoggingConfiguration.create().setShowSql(false)
      .getSubstitutionVariable(LoggingConfiguration.PROPERTY_SQL_LOGGER_LEVEL), Is.is(LoggingConfiguration.LEVEL_SQL_DEFAULT));

    assertThat(LoggingConfiguration.create().setSqlLevel("ERROR")
      .getSubstitutionVariable(LoggingConfiguration.PROPERTY_SQL_LOGGER_LEVEL), Is.is("ERROR"));
  }

  @Test
  public void shouldNotShowSqlByDefault() {
    assertThat(LoggingConfiguration.create()
      .getSubstitutionVariable(LoggingConfiguration.PROPERTY_SQL_LOGGER_LEVEL), Is.is(LoggingConfiguration.LEVEL_SQL_DEFAULT));
  }

  @Test
  public void testSetVerbose() {
    assertThat(LoggingConfiguration.create().setVerbose(true)
      .getSubstitutionVariable(LoggingConfiguration.PROPERTY_ROOT_LOGGER_LEVEL), Is.is(LoggingConfiguration.LEVEL_ROOT_VERBOSE));

    assertThat(LoggingConfiguration.create().setVerbose(false)
      .getSubstitutionVariable(LoggingConfiguration.PROPERTY_ROOT_LOGGER_LEVEL), Is.is(LoggingConfiguration.LEVEL_ROOT_DEFAULT));

    assertThat(LoggingConfiguration.create().setRootLevel("ERROR")
      .getSubstitutionVariable(LoggingConfiguration.PROPERTY_ROOT_LOGGER_LEVEL), Is.is("ERROR"));
  }

  @Test
  public void shouldNotBeVerboseByDefault() {
    assertThat(LoggingConfiguration.create()
      .getSubstitutionVariable(LoggingConfiguration.PROPERTY_ROOT_LOGGER_LEVEL), Is.is(LoggingConfiguration.LEVEL_ROOT_DEFAULT));
  }

  @Test
  public void testSetVerboseProperty() {
    Map<String, String> properties = Maps.newHashMap();
    assertThat(LoggingConfiguration.create().setProperties(properties)
      .getSubstitutionVariable(LoggingConfiguration.PROPERTY_ROOT_LOGGER_LEVEL), Is.is(LoggingConfiguration.LEVEL_ROOT_DEFAULT));

    properties.put("sonar.verbose", "true");
    assertThat(LoggingConfiguration.create().setProperties(properties)
      .getSubstitutionVariable(LoggingConfiguration.PROPERTY_ROOT_LOGGER_LEVEL), Is.is(LoggingConfiguration.LEVEL_ROOT_VERBOSE));

    properties.put("sonar.verbose", "false");
    assertThat(LoggingConfiguration.create().setProperties(properties)
      .getSubstitutionVariable(LoggingConfiguration.PROPERTY_ROOT_LOGGER_LEVEL), Is.is(LoggingConfiguration.LEVEL_ROOT_DEFAULT));
  }

  @Test
  public void testSetShowSqlProperty() {
    Map<String, String> properties = Maps.newHashMap();
    assertThat(LoggingConfiguration.create().setProperties(properties)
      .getSubstitutionVariable(LoggingConfiguration.PROPERTY_SQL_LOGGER_LEVEL), Is.is(LoggingConfiguration.LEVEL_SQL_DEFAULT));

    properties.put("sonar.showSql", "true");
    assertThat(LoggingConfiguration.create().setProperties(properties)
      .getSubstitutionVariable(LoggingConfiguration.PROPERTY_SQL_LOGGER_LEVEL), Is.is(LoggingConfiguration.LEVEL_SQL_VERBOSE));

    properties.put("sonar.showSql", "false");
    assertThat(LoggingConfiguration.create().setProperties(properties)
      .getSubstitutionVariable(LoggingConfiguration.PROPERTY_SQL_LOGGER_LEVEL), Is.is(LoggingConfiguration.LEVEL_SQL_DEFAULT));
  }

  @Test
  public void testDefaultFormat() {
    assertThat(LoggingConfiguration.create()
      .getSubstitutionVariable(LoggingConfiguration.PROPERTY_FORMAT), Is.is(LoggingConfiguration.FORMAT_DEFAULT));
  }

  @Test
  public void testSetFormat() {
    assertThat(LoggingConfiguration.create().setFormat("%d %level")
      .getSubstitutionVariable(LoggingConfiguration.PROPERTY_FORMAT), Is.is("%d %level"));
  }

  @Test
  public void shouldNotSetBlankFormat() {
    assertThat(LoggingConfiguration.create().setFormat(null)
      .getSubstitutionVariable(LoggingConfiguration.PROPERTY_FORMAT), Is.is(LoggingConfiguration.FORMAT_DEFAULT));

    assertThat(LoggingConfiguration.create().setFormat("")
      .getSubstitutionVariable(LoggingConfiguration.PROPERTY_FORMAT), Is.is(LoggingConfiguration.FORMAT_DEFAULT));

    assertThat(LoggingConfiguration.create().setFormat("   ")
      .getSubstitutionVariable(LoggingConfiguration.PROPERTY_FORMAT), Is.is(LoggingConfiguration.FORMAT_DEFAULT));
  }
}
