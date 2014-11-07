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

package org.sonar.data.issues;

import org.apache.commons.lang.StringUtils;
import org.apache.ibatis.io.Resources;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.rules.TestName;

import java.io.File;
import java.io.FileReader;
import java.util.Properties;

import static org.fest.assertions.Assertions.assertThat;

public class AbstractTest {

  private static final double ACCEPTED_DURATION_VARIATION_IN_PERCENTS = 10.0;

  private static Properties properties = new Properties();

  @Rule
  public TestName testName = new TestName();

  @BeforeClass
  public static void loadProperties() {
    try {
      properties = new Properties();
      File propertiesFile = Resources.getResourceAsFile("assertions.properties");
      FileReader reader = new FileReader(propertiesFile);
      properties.load(reader);
      properties.putAll(System.getProperties());
    } catch (Exception e) {
      throw new IllegalStateException(e);
    }
  }

  protected String getProperty(String test) {
    String currentUser = StringUtils.defaultString(properties.getProperty("user"), "default");
    String property = currentUser + "." + test;
    String value = properties.getProperty(property);
    if (value == null) {
      throw new IllegalArgumentException(String.format("Property '%s' hasn't been found", property));
    }
    return value;
  }

  protected void assertDurationAround(long duration, long expectedDuration) {
    double variation = 100.0 * (0.0 + duration - expectedDuration) / expectedDuration;
    System.out.printf("Test %s : executed in %d ms (%.2f %% from target)\n", testName.getMethodName(), duration, variation);
    assertThat(Math.abs(variation)).as(String.format("Expected %d ms, got %d ms", expectedDuration, duration)).isLessThan(ACCEPTED_DURATION_VARIATION_IN_PERCENTS);
  }

}
