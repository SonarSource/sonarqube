/*
 * SonarQube
 * Copyright (C) 2009-2025 SonarSource SA
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
package org.sonar.application.command;

import java.io.File;
import java.io.IOException;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import static org.assertj.core.api.Assertions.assertThat;

public class WebJvmOptionsTest {
  @Rule
  public TemporaryFolder temporaryFolder = new TemporaryFolder();

  private File tmpDir;
  private WebJvmOptions underTest;

  @Before
  public void setUp() throws IOException {
    tmpDir = temporaryFolder.newFolder();
  }

  @Test
  public void constructor_sets_mandatory_JVM_options() {
    underTest = new WebJvmOptions(tmpDir);

    assertThat(underTest.getAll()).containsExactly(
      "-Djava.awt.headless=true", "-Dfile.encoding=UTF-8", "-Djava.io.tmpdir=" + tmpDir.getAbsolutePath(), "-XX:-OmitStackTraceInFastThrow",
      "--add-opens=java.base/java.util=ALL-UNNAMED",
      "--add-opens=java.base/java.lang=ALL-UNNAMED",
      "--add-opens=java.base/java.io=ALL-UNNAMED",
      "--add-opens=java.rmi/sun.rmi.transport=ALL-UNNAMED",
      "--add-exports=java.base/jdk.internal.ref=ALL-UNNAMED",
      "--add-opens=java.base/java.nio=ALL-UNNAMED",
      "--add-opens=java.base/sun.nio.ch=ALL-UNNAMED",
      "--add-opens=java.management/sun.management=ALL-UNNAMED",
      "--add-opens=jdk.management/com.sun.management.internal=ALL-UNNAMED");
  }

}
