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
package org.sonar.api.batch.fs.internal;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import org.apache.commons.io.IOUtils;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.sonar.api.batch.fs.InputFile.Status;
import org.sonar.api.batch.fs.InputFile.Type;

import static org.assertj.core.api.Assertions.assertThat;

public class TestInputFileBuilderTest {

  @Rule
  public TemporaryFolder temp = new TemporaryFolder();

  @Test
  public void setContent() throws IOException {
    DefaultInputFile file = TestInputFileBuilder.create("module", "invalidPath")
      .setContents("my content")
      .setCharset(StandardCharsets.UTF_8)
      .build();
    assertThat(file.contents()).isEqualTo("my content");
    assertThat(IOUtils.toString(file.inputStream())).isEqualTo("my content");
  }

  @Test
  public void testGetters() {
    DefaultInputFile file = TestInputFileBuilder.create("module", new File("baseDir"), new File("baseDir", "path"))
      .setStatus(Status.SAME)
      .setType(Type.MAIN)
      .build();

    assertThat(file.type()).isEqualTo(Type.MAIN);
    assertThat(file.status()).isEqualTo(Status.SAME);
    assertThat(file.isPublished()).isTrue();
    assertThat(file.type()).isEqualTo(Type.MAIN);
    assertThat(file.relativePath()).isEqualTo("path");
    assertThat(file.absolutePath()).isEqualTo("baseDir/path");

  }

  @Test
  public void testCreateInputModule() throws IOException {
    File baseDir = temp.newFolder();
    AbstractProjectOrModule module = TestInputFileBuilder.newDefaultInputModule("key", baseDir);
    assertThat(module.key()).isEqualTo("key");
    assertThat(module.getBaseDir()).isEqualTo(baseDir.toPath());
  }
}
