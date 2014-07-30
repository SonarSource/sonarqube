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
package org.sonar.process;

import org.fest.assertions.Assertions;
import org.junit.Test;

import static org.fest.assertions.Fail.fail;

public class MinimumViableEnvironmentTest {

  /**
   * Verifies that all checks can be verified without error.
   * Test environment does not necessarily follows all checks.
   */
  @Test
  public void check() throws Exception {
    MinimumViableEnvironment mve = new MinimumViableEnvironment();

    try {
      mve.check();
      // ok
    } catch (MinimumViableEnvironment.MessageException e) {
      // also ok. All other exceptions are errors.
    }
  }

  @Test
  public void checkJavaVersion() throws Exception {
    MinimumViableEnvironment mve = new MinimumViableEnvironment();

    // yes, sources are compiled with a supported Java version!
    mve.checkJavaVersion();

    mve.checkJavaVersion("1.6.1_b2");
    try {
      mve.checkJavaVersion("1.5.2");
      fail();
    } catch (MinimumViableEnvironment.MessageException e) {
      Assertions.assertThat(e).hasMessage("Minimal required Java version is 1.6. Got 1.5.2.");
    }
  }

  @Test
  public void checkJavaOption() throws Exception {
    String key = "MinimumViableEnvironmentTest.test.prop";
    MinimumViableEnvironment mve = new MinimumViableEnvironment()
      .setRequiredJavaOption(key, "true");

    try {
      System.setProperty(key, "false");
      mve.checkJavaOptions();
      fail();
    } catch (MinimumViableEnvironment.MessageException e) {
      Assertions.assertThat(e).hasMessage("JVM option '" + key + "' must be set to 'true'. Got 'false'");
    }

    System.setProperty(key, "true");
    mve.checkJavaOptions();
    // do not fail
  }
}
