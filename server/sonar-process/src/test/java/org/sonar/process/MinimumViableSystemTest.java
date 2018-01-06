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
package org.sonar.process;

import com.google.common.collect.ImmutableMap;
import java.io.File;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.fail;

public class MinimumViableSystemTest {

  @Rule
  public TemporaryFolder temp = new TemporaryFolder();

  MinimumViableSystem underTest = new MinimumViableSystem();

  @Test
  public void checkRequiredJavaOptions() {
    String key = "MinimumViableEnvironmentTest.test.prop";

    try {
      System.setProperty(key, "false");
      underTest.checkRequiredJavaOptions(ImmutableMap.of(key, "true"));
      fail();
    } catch (MessageException e) {
      assertThat(e).hasMessage("JVM option '" + key + "' must be set to 'true'. Got 'false'");
    }

    System.setProperty(key, "true");
    // do not fail
    underTest.checkRequiredJavaOptions(ImmutableMap.of(key, "true"));
  }

  @Test
  public void checkWritableTempDir() throws Exception {
    File dir = temp.newFolder();

    underTest.checkWritableDir(dir.getAbsolutePath());

    dir.delete();
    try {
      underTest.checkWritableDir(dir.getAbsolutePath());
      fail();
    } catch (IllegalStateException e) {
      assertThat(e).hasMessage("Temp directory is not writable: " + dir.getAbsolutePath());
    }
  }
}
