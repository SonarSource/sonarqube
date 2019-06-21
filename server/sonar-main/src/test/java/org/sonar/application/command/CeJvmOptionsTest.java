/*
 * SonarQube
 * Copyright (C) 2009-2019 SonarSource SA
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
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class CeJvmOptionsTest {
  @Rule
  public TemporaryFolder temporaryFolder = new TemporaryFolder();

  private File tmpDir;
  private JavaVersion javaVersion = mock(JavaVersion.class);
  private CeJvmOptions underTest;

  @Before
  public void setUp() throws IOException {
    tmpDir = temporaryFolder.newFolder();
  }

  @Test
  public void constructor_sets_mandatory_JVM_options_before_java11() {
    when(javaVersion.isAtLeastJava11()).thenReturn(false);
    underTest = new CeJvmOptions(tmpDir, javaVersion);
    assertThat(underTest.getAll()).containsExactly(
      "-Djava.awt.headless=true", "-Dfile.encoding=UTF-8", "-Djava.io.tmpdir=" + tmpDir.getAbsolutePath());
  }

  @Test
  public void constructor_sets_mandatory_JVM_options_for_java11() {
    when(javaVersion.isAtLeastJava11()).thenReturn(true);
    underTest = new CeJvmOptions(tmpDir, javaVersion);
    assertThat(underTest.getAll()).containsExactly(
      "-Djava.awt.headless=true", "-Dfile.encoding=UTF-8", "-Djava.io.tmpdir=" + tmpDir.getAbsolutePath(),
      "--add-opens=java.base/java.util=ALL-UNNAMED");
  }
}
