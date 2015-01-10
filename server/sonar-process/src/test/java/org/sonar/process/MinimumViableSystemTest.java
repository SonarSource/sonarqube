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

import org.assertj.core.api.Assertions;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.File;

import static org.junit.Assert.fail;

public class MinimumViableSystemTest {

  @Rule
  public TemporaryFolder temp = new TemporaryFolder();

  /**
   * Verifies that all checks can be verified without error.
   * Test environment does not necessarily follows all checks.
   */
  @Test
  public void check() throws Exception {
    MinimumViableSystem mve = new MinimumViableSystem();

    try {
      mve.check();
      // ok
    } catch (MessageException e) {
      // also ok. All other exceptions are errors.
    }
  }

  @Test
  public void checkJavaVersion() throws Exception {
    MinimumViableSystem mve = new MinimumViableSystem();

    // yes, sources are compiled with a supported Java version!
    mve.checkJavaVersion();
    mve.checkJavaVersion("1.6");

    try {
      mve.checkJavaVersion("1.9");
      fail();
    } catch (MessageException e) {
      Assertions.assertThat(e).hasMessage("Supported versions of Java are 1.6, 1.7 and 1.8. Got 1.9.");
    }
  }

  @Test
  public void checkJavaOption() throws Exception {
    String key = "MinimumViableEnvironmentTest.test.prop";
    MinimumViableSystem mve = new MinimumViableSystem()
      .setRequiredJavaOption(key, "true");

    try {
      System.setProperty(key, "false");
      mve.checkJavaOptions();
      fail();
    } catch (MessageException e) {
      Assertions.assertThat(e).hasMessage("JVM option '" + key + "' must be set to 'true'. Got 'false'");
    }

    System.setProperty(key, "true");
    mve.checkJavaOptions();
    // do not fail
  }

  @Test
  public void checkWritableTempDir() throws Exception {
    File dir = temp.newFolder();
    MinimumViableSystem mve = new MinimumViableSystem();

    mve.checkWritableDir(dir.getAbsolutePath());

    dir.delete();
    try {
      mve.checkWritableDir(dir.getAbsolutePath());
      fail();
    } catch (IllegalStateException e) {
      Assertions.assertThat(e).hasMessage("Temp directory is not writable: " + dir.getAbsolutePath());
    }
  }
}
