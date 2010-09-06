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
package org.sonar.server.mavendeployer;

import org.apache.commons.io.FileUtils;
import org.junit.Test;

import java.io.File;
import java.io.IOException;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;

public class MojoTest {
  @Test
  public void shouldCopyJar() throws IOException {
    File jar = FileUtils.toFile(getClass().getResource("/org/sonar/server/mavendeployer/MojoTest/shouldCopyJar.jar"));
    Mojo mojo = Mojo.createMaven2Plugin("1.0", jar);
    File toDir = new File("target/test-tmp/MojoTest/");
    mojo.copyTo(toDir);
    assertThat(new File(toDir, "shouldCopyJar.jar").exists(), is(true));
  }

  @Test
  public void shouldDeleteTemporaryDirectory() throws IOException {
    String version = "" + System.currentTimeMillis();
    assertThat(getTmpDir(version).exists(), is(false));

    File jar = FileUtils.toFile(getClass().getResource("/org/sonar/server/mavendeployer/MojoTest/shouldCopyJar.jar"));
    Mojo mojo = Mojo.createMaven2Plugin(version, jar);
    File toDir = new File("target/test-tmp/MojoTest/");
    mojo.copyTo(toDir);
    assertThat(new File(toDir, "shouldCopyJar.jar").exists(), is(true));
    assertThat(getTmpDir(version).exists(), is(false));
  }

  @Test
  public void shouldUpdateVersion() {
    Mojo mojo = Mojo.createMaven2Plugin("1.0", null);
    assertEquals("<plugin>...<version>1234</version><mojos>...", mojo.updateVersion("<plugin>...<version>1.8-SNAPSHOT</version><mojos>...", "1234"));
    assertEquals("<plugin>...<version>1234</version><mojos><version>1.0</version>...", mojo.updateVersion("<plugin>...<version>1.8-SNAPSHOT</version><mojos><version>1.0</version>...", "1234"));
  }

  private File getTmpDir(String version) {
    File tmpDir = new File(System.getProperty("java.io.tmpdir"), "sonar-" + version);
    return tmpDir;
  }
}
