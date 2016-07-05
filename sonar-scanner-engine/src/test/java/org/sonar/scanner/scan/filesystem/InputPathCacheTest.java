/*
 * SonarQube
 * Copyright (C) 2009-2016 SonarSource SA
 * mailto:contact AT sonarsource DOT com
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
package org.sonar.scanner.scan.filesystem;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.sonar.api.batch.fs.InputFile.Status;
import org.sonar.api.batch.fs.InputFile.Type;
import org.sonar.api.batch.fs.InputPath;
import org.sonar.api.batch.fs.internal.DefaultInputFile;
import org.sonar.scanner.scan.filesystem.InputPathCache;
import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;

public class InputPathCacheTest {

  @Rule
  public TemporaryFolder temp = new TemporaryFolder();

  @Before
  public void start() {
  }

  @After
  public void stop() {
  }

  @Test
  public void should_add_input_file() throws Exception {
    InputPathCache cache = new InputPathCache();
    DefaultInputFile fooFile = new DefaultInputFile("foo", "src/main/java/Foo.java").setModuleBaseDir(temp.newFolder().toPath());
    cache.put("struts", fooFile);
    cache.put("struts-core", new DefaultInputFile("foo", "src/main/java/Bar.java")
      .setLanguage("bla")
      .setType(Type.MAIN)
      .setStatus(Status.ADDED)
      .setLines(2)
      .setCharset(StandardCharsets.UTF_8)
      .setModuleBaseDir(temp.newFolder().toPath()));

    DefaultInputFile loadedFile = (DefaultInputFile) cache.getFile("struts-core", "src/main/java/Bar.java");
    assertThat(loadedFile.relativePath()).isEqualTo("src/main/java/Bar.java");
    assertThat(loadedFile.charset()).isEqualTo(StandardCharsets.UTF_8);

    assertThat(cache.filesByModule("struts")).hasSize(1);
    assertThat(cache.filesByModule("struts-core")).hasSize(1);
    assertThat(cache.allFiles()).hasSize(2);
    for (InputPath inputPath : cache.allFiles()) {
      assertThat(inputPath.relativePath()).startsWith("src/main/java/");
    }

    cache.remove("struts", fooFile);
    assertThat(cache.allFiles()).hasSize(1);

    cache.removeModule("struts");
    assertThat(cache.filesByModule("struts")).hasSize(0);
    assertThat(cache.filesByModule("struts-core")).hasSize(1);
    assertThat(cache.allFiles()).hasSize(1);
  }

}
