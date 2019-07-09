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
package org.sonar.scanner.scan.filesystem;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.LinkedList;
import java.util.List;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.sonar.api.batch.bootstrap.ProjectDefinition;
import org.sonar.api.batch.fs.InputFile;
import org.sonar.api.batch.fs.InputFile.Status;
import org.sonar.api.batch.fs.InputFile.Type;
import org.sonar.api.batch.fs.InputPath;
import org.sonar.api.batch.fs.internal.DefaultInputFile;
import org.sonar.api.batch.fs.internal.DefaultInputModule;
import org.sonar.api.batch.fs.internal.DefaultInputProject;
import org.sonar.api.batch.fs.internal.TestInputFileBuilder;
import org.sonar.scanner.scan.branch.BranchConfiguration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

public class InputComponentStoreTest {
  @ClassRule
  public static TemporaryFolder temp = new TemporaryFolder();

  @Test
  public void should_add_input_file() throws Exception {
    String rootModuleKey = "struts";
    String subModuleKey = "struts-core";

    File rootBaseDir = temp.newFolder();

    ProjectDefinition moduleDef = ProjectDefinition.create()
      .setKey(subModuleKey).setBaseDir(rootBaseDir).setWorkDir(temp.newFolder());
    ProjectDefinition rootDef = ProjectDefinition.create()
      .setKey(rootModuleKey).setBaseDir(rootBaseDir).setWorkDir(temp.newFolder()).addSubProject(moduleDef);

    DefaultInputProject rootProject = TestInputFileBuilder.newDefaultInputProject(rootDef);
    DefaultInputModule subModule = TestInputFileBuilder.newDefaultInputModule(moduleDef);

    InputComponentStore store = new InputComponentStore(mock(BranchConfiguration.class));
    store.put(subModule);

    DefaultInputFile fooFile = new TestInputFileBuilder(rootModuleKey, "src/main/java/Foo.java")
      .setModuleBaseDir(rootBaseDir.toPath())
      .setPublish(true)
      .build();
    store.put(rootProject.key(), fooFile);
    store.put(subModuleKey, new TestInputFileBuilder(rootModuleKey, "src/main/java/Bar.java")
      .setLanguage("bla")
      .setPublish(false)
      .setType(Type.MAIN)
      .setStatus(Status.ADDED)
      .setLines(2)
      .setCharset(StandardCharsets.UTF_8)
      .setModuleBaseDir(temp.newFolder().toPath())
      .build());

    DefaultInputFile loadedFile = (DefaultInputFile) store.getFile(subModuleKey, "src/main/java/Bar.java");
    assertThat(loadedFile.relativePath()).isEqualTo("src/main/java/Bar.java");
    assertThat(loadedFile.charset()).isEqualTo(StandardCharsets.UTF_8);

    assertThat(store.filesByModule(rootModuleKey)).hasSize(1);
    assertThat(store.filesByModule(subModuleKey)).hasSize(1);
    assertThat(store.inputFiles()).hasSize(2);
    for (InputPath inputPath : store.inputFiles()) {
      assertThat(inputPath.relativePath()).startsWith("src/main/java/");
    }

    List<InputFile> toPublish = new LinkedList<>();
    store.allFilesToPublish().forEach(toPublish::add);
    assertThat(toPublish).containsExactly(fooFile);
  }

  static class InputComponentStoreTester extends InputComponentStore {
    InputComponentStoreTester() throws IOException {
      super(mock(BranchConfiguration.class));
    }

    InputFile addFile(String moduleKey, String relpath, String language) {
      DefaultInputFile file = new TestInputFileBuilder(moduleKey, relpath)
        .setLanguage(language)
        .build();
      put(moduleKey, file);
      return file;
    }
  }

  @Test
  public void should_add_languages_per_module_and_globally() throws IOException {
    InputComponentStoreTester tester = new InputComponentStoreTester();

    String mod1Key = "mod1";
    tester.addFile(mod1Key, "src/main/java/Foo.java", "java");

    String mod2Key = "mod2";
    tester.addFile(mod2Key, "src/main/groovy/Foo.groovy", "groovy");

    assertThat(tester.languages(mod1Key)).containsExactly("java");
    assertThat(tester.languages(mod2Key)).containsExactly("groovy");
    assertThat(tester.languages()).containsExactlyInAnyOrder("java", "groovy");
  }

  @Test
  public void should_find_files_per_module_and_globally() throws IOException {
    InputComponentStoreTester tester = new InputComponentStoreTester();

    String mod1Key = "mod1";
    InputFile mod1File = tester.addFile(mod1Key, "src/main/java/Foo.java", "java");

    String mod2Key = "mod2";
    InputFile mod2File = tester.addFile(mod2Key, "src/main/groovy/Foo.groovy", "groovy");

    assertThat(tester.filesByModule(mod1Key)).containsExactly(mod1File);
    assertThat(tester.filesByModule(mod2Key)).containsExactly(mod2File);
    assertThat(tester.inputFiles()).containsExactlyInAnyOrder(mod1File, mod2File);
  }
}
