/*
 * SonarQube
 * Copyright (C) 2009-2017 SonarSource SA
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
package org.sonar.scanner.scan.filesystem;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.util.LinkedList;
import java.util.List;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.sonar.api.batch.bootstrap.ProjectDefinition;
import org.sonar.api.batch.fs.InputFile;
import org.sonar.api.batch.fs.InputFile.Status;
import org.sonar.api.batch.fs.InputFile.Type;
import org.sonar.api.batch.fs.InputPath;
import org.sonar.api.batch.fs.internal.DefaultInputFile;
import org.sonar.api.batch.fs.internal.DefaultInputModule;
import org.sonar.api.batch.fs.internal.TestInputFileBuilder;
import org.sonar.api.scan.filesystem.PathResolver;

import static org.assertj.core.api.Assertions.assertThat;

public class InputComponentStoreTest {
  @Rule
  public TemporaryFolder temp = new TemporaryFolder();

  @Test
  public void should_add_input_file() throws Exception {
    InputComponentStore cache = new InputComponentStore(new PathResolver());

    String mod1key = "struts";
    File mod1baseDir = temp.newFolder();
    ProjectDefinition mod1def = ProjectDefinition.create().setKey(mod1key);
    mod1def.setBaseDir(mod1baseDir);
    DefaultInputModule rootModule = new DefaultInputModule(mod1def, TestInputFileBuilder.nextBatchId());
    cache.put(rootModule);

    String mod2key = "struts-core";
    File mod2baseDir = temp.newFolder();
    ProjectDefinition mod2def = ProjectDefinition.create().setKey(mod2key);
    mod2def.setBaseDir(mod2baseDir);
    mod1def.addSubProject(mod2def);
    cache.put(new DefaultInputModule(mod2def, TestInputFileBuilder.nextBatchId()));

    DefaultInputFile fooFile = new TestInputFileBuilder(mod1key, "src/main/java/Foo.java")
      .setModuleBaseDir(mod1baseDir.toPath())
      .setPublish(true)
      .build();
    cache.put(fooFile);
    cache.put(new TestInputFileBuilder(mod2key, "src/main/java/Bar.java")
      .setLanguage("bla")
      .setPublish(false)
      .setType(Type.MAIN)
      .setStatus(Status.ADDED)
      .setLines(2)
      .setCharset(StandardCharsets.UTF_8)
      .setModuleBaseDir(temp.newFolder().toPath())
      .build());

    DefaultInputFile loadedFile = (DefaultInputFile) cache.getFile(mod2key, "src/main/java/Bar.java");
    assertThat(loadedFile.relativePath()).isEqualTo("src/main/java/Bar.java");
    assertThat(loadedFile.charset()).isEqualTo(StandardCharsets.UTF_8);

    assertThat(cache.filesByModule(mod1key)).hasSize(1);
    assertThat(cache.filesByModule(mod2key)).hasSize(1);
    assertThat(cache.allFiles()).hasSize(2);
    for (InputPath inputPath : cache.allFiles()) {
      assertThat(inputPath.relativePath()).startsWith("src/main/java/");
    }

    List<InputFile> toPublish = new LinkedList<>();
    cache.allFilesToPublish().forEach(toPublish::add);
    assertThat(toPublish).containsOnly(fooFile);

    cache.remove(fooFile);
    assertThat(cache.allFiles()).hasSize(1);

    cache.removeModule(mod1key);
    assertThat(cache.filesByModule(mod1key)).hasSize(0);
    assertThat(cache.filesByModule(mod2key)).hasSize(1);
    assertThat(cache.allFiles()).hasSize(1);
  }

}
